package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice

class NotificationService{
    fun notifyCustomersAboutFailure(invoice: Invoice){
        //Send a notification by e-mail or sms
    }
    fun notifyCustomersAboutSuccess(invoice: Invoice){
        //Send a notification by e-mail or sms
    }
}