package io.pleo.antaeus.core.external


import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money

interface CurrencyExchangeProvider {
    /*
        Convert the provided money to target currency.

        Returns:
          `Money`: the converted amount of money in requested currency.

        Throws:
          `CurrencyNotFoundException`: when the currency is not found.
          `NetworkException`: when a network error happens.
    */

    fun exchangeCurrency(source: Money, target: Currency): Money
}