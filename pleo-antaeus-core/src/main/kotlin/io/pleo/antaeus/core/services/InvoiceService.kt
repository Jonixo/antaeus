/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val invoiceDal: InvoiceDal) {

    fun fetchAll(): List<Invoice> {
        return invoiceDal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return invoiceDal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchByStatus(status: InvoiceStatus): List<Invoice> {
        return invoiceDal.fetchInvoicesByStatus(status)
    }

    fun updateInvoice(invoice: Invoice): Invoice? {
        return invoiceDal.updateInvoice(invoice) ?: throw InvoiceNotFoundException(invoice.id)
    }

    fun markAsPaid(paid: Invoice): Invoice? {
        val updated = paid.copy(status = InvoiceStatus.PAID)
        return invoiceDal.updateInvoice(updated) ?: throw InvoiceNotFoundException(paid.id)
    }

    fun markAsFailed(failed: Invoice): Invoice? {
        val updated = failed.copy(status = InvoiceStatus.FAILED)
        return invoiceDal.updateInvoice(updated) ?: throw InvoiceNotFoundException(failed.id)
    }
}
