package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.CurrencyExchangeProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
private const val NUM_MAX_RETRY = 5


class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val currencyExchangeProvider: CurrencyExchangeProvider,
    private val notificationService: NotificationService
) {

    fun processPendingInvoices () {
        logger.info { "Starting billing task..." }

        val pendingInvoices = invoiceService.fetchInvoicesByStatus(InvoiceStatus.PENDING)
        logger.info {"Number of pending invoices: ${pendingInvoices.size}"}

        for (invoice in pendingInvoices){
            if(requestPayment(invoiceId = invoice.id))
                notificationService.notifyCustomersAboutSuccess(invoice)
            else
                notificationService.notifyCustomersAboutFailure(invoice)
        }

        logger.info { "Billing executed..." }



    }

    fun requestPayment(invoiceId: Int): Boolean {
        return try {
            val invoice = invoiceService.fetch(invoiceId)
            if (invoice.status == InvoiceStatus.PAID || invoice.status == InvoiceStatus.FAILED ) { true }
            else {
                val paymentOutcome = tryPaymentRequest(invoice)
                if (paymentOutcome) {
                    invoiceService.markAsPaid(invoice)
                    logger.info { "Invoice id: ${invoice.id} is successfully processed and paid" }
                } else {
                    invoiceService.markAsFailed(invoice)
                    logger.info { "Invoice id: ${invoice.id} is successfully processed and failed" }
                }
                paymentOutcome
            }
        } catch (infe: InvoiceNotFoundException) {
            logger.debug(infe) { "Cannot find invoice" }
            false
        }
    }


    fun tryPaymentRequest(invoice: Invoice, numOfRetry: Int = 0): Boolean {

        if (numOfRetry < NUM_MAX_RETRY) {
            return try {
                val paid = paymentProvider.charge(invoice)
                paid
            } catch (cusnfe: CustomerNotFoundException) {
                logger.warn(cusnfe) { "Cannot find customer for invoice id ${invoice.id}" }
                false

            } catch (cme: CurrencyMismatchException) {
                logger.warn(cme) { "Currency doesn't match, trying exchange" }
                val exchangedInvoice = exchangeCurrency(invoice)
                exchangedInvoice?.let { tryPaymentRequest(exchangedInvoice) } ?: false

            } catch (ne: NetworkException) {
                logger.warn(ne) { "Network error, number of retries: ${numOfRetry + 1}" }
                retryPaymentRequest(invoice, numOfRetry)

            }
        }

        return false
    }

    private fun exchangeCurrency(invoice: Invoice, numOfRetry: Int = 0): Invoice? {
        if (numOfRetry < NUM_MAX_RETRY) {
            return try {
                val customer = customerService.fetch(invoice.customerId)
                val exchangedAmount = currencyExchangeProvider.exchangeCurrency(invoice.amount, customer.currency)
                invoice.copy(amount = exchangedAmount)

            } catch (cusnfe: CustomerNotFoundException) {
                logger.warn(cusnfe) { "Cannot find customer" }
                null
            } catch (curnfe: CurrencyNotFoundException) {
                logger.warn(curnfe) { "Currency not found" }
                null
            } catch (ne: NetworkException) {
                logger.warn(ne) { "Network error, number of retries: ${numOfRetry + 1}" }
                retryExchangeCurrency(invoice, numOfRetry)

            }
        }

        return null
    }

    private fun retryExchangeCurrency(invoice: Invoice, numOfRetry: Int = 0): Invoice? {
        TimeUnit.SECONDS.sleep(5L)
        return exchangeCurrency(invoice, numOfRetry + 1)
    }

    private fun retryPaymentRequest(invoice: Invoice, numOfRetry: Int = 0): Boolean {
        TimeUnit.SECONDS.sleep(5L)
        return tryPaymentRequest(invoice, numOfRetry + 1)
    }

}
