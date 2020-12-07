package io.pleo.antaeus.core.services


import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.CurrencyExchangeProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

// Money
private val MONEY = Money(BigDecimal(100), Currency.DKK)
private val MONEY_CURRENCY_NOT_FOUND = Money(BigDecimal(404), Currency.DKK)
private val MONEY_NETWORK_ERROR = Money(BigDecimal(504), Currency.DKK)

// Invoice
private val INVOICE_SUCCESS = Invoice(100, 100, MONEY, InvoiceStatus.PENDING)
private val INVOICE_FAILURE = INVOICE_SUCCESS.copy(id = 101, customerId = 101)
private val INVOICE_CUSTOMER_NOT_FOUND = INVOICE_SUCCESS.copy(id = 102, customerId = 404)
private val INVOICE_CURRENCY_MISMATCH = INVOICE_SUCCESS.copy(id = 103, customerId = 103)
private val INVOICE_NETWORK_ERROR = INVOICE_SUCCESS.copy(id = 504)

// Customer
private val CUSTOMER_DKK =  Customer(100, Currency.DKK)
private val CUSTOMER_EUR =  Customer(103, Currency.EUR)


class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(INVOICE_SUCCESS) } returns true
        every { charge(INVOICE_FAILURE) } returns false
        every { charge(INVOICE_CUSTOMER_NOT_FOUND) } throws CustomerNotFoundException(404)
        every { charge(INVOICE_CURRENCY_MISMATCH) } throws CurrencyMismatchException(103, 103)
        every { charge(INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY.copy(currency = Currency.EUR))) } returns true
        every { charge(INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_CURRENCY_NOT_FOUND)) } throws CurrencyMismatchException(103, 103)
        every { charge(INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_NETWORK_ERROR)) } throws CurrencyMismatchException(103, 103)
        every { charge(INVOICE_NETWORK_ERROR) } throws NetworkException()
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetch(100) } returns INVOICE_SUCCESS
        every { fetch(200) } throws InvoiceNotFoundException(200)
    }

    private val customerService = mockk<CustomerService> {
        every { fetch(100) } returns CUSTOMER_DKK
        every { fetch(103) } returns CUSTOMER_EUR
        every { fetch(404) } throws CustomerNotFoundException(404)
    }

    private val currencyExchangeProvider = mockk<CurrencyExchangeProvider> {
        every { exchangeCurrency(MONEY, Currency.EUR) } returns MONEY.copy(currency = Currency.EUR)
        every { exchangeCurrency(MONEY_CURRENCY_NOT_FOUND, Currency.EUR) } throws CurrencyNotFoundException(Currency.EUR)
        every { exchangeCurrency(MONEY_NETWORK_ERROR, Currency.EUR) } throws NetworkException()
    }

    private val notificationService = mockk<NotificationService> {
        every { notifyCustomersAboutSuccess(INVOICE_SUCCESS) }
        every { notifyCustomersAboutFailure(INVOICE_FAILURE) }
    }


    private val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService,
            customerService = customerService,
            currencyExchangeProvider = currencyExchangeProvider,
            notificationService = notificationService
    )

    @Test
    fun `will return a success outcome if the billing is done`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_SUCCESS)
        Assertions.assertEquals(true, outcome)
    }

    @Test
    fun `will return a failure outcome if the billing is failed`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_FAILURE)
        Assertions.assertEquals(false, outcome)
    }

    @Test
    fun `will return a failure outcome if the customer is not found`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_CUSTOMER_NOT_FOUND)
        Assertions.assertEquals(false, outcome)
    }


    @Test
    fun `will convert money and return a failure outcome if there is a conversion error`() {
        val invoice = INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_CURRENCY_NOT_FOUND)
        val outcome = billingService.tryPaymentRequest(invoice)
        Assertions.assertEquals(false, outcome)
    }

    @Test
    fun `will convert money and return a failure outcome after too many tentative requests with network error`() {
        val invoice = INVOICE_CURRENCY_MISMATCH.copy(amount = MONEY_NETWORK_ERROR)
        val outcome = billingService.tryPaymentRequest(invoice)
        Assertions.assertEquals(false, outcome)
    }

    @Test
    fun `will return a failure after too many tentative requests with network error`() {
        val outcome = billingService.tryPaymentRequest(INVOICE_NETWORK_ERROR)
        Assertions.assertEquals(false, outcome)
    }
}