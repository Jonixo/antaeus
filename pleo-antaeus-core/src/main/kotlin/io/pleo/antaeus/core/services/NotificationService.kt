package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice

class NotificationService{
    fun notifyCustomersAboutFailure(List :  List<Invoice>){
        for(invoice in List){
            //Send a notification by e-mail or sms
        }
    }
    fun notifyCustomersAboutSuccess(List :  List<Invoice>){
        for(invoice in List){
            //Send a notification by e-mail or sms
        }
    }
}