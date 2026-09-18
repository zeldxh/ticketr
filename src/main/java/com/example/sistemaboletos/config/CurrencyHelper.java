package com.example.sistemaboletos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("currencyHelper")
public class CurrencyHelper {

    private final String primaryCurrency;
    private final double usdToCrc;

    public CurrencyHelper(
            @Value("${app.currency.primary:CRC}") String primaryCurrency,
            @Value("${app.currency.usd-to-crc:520.0}") double usdToCrc) {
        this.primaryCurrency = primaryCurrency == null ? "CRC" : primaryCurrency.trim().toUpperCase();
        this.usdToCrc = usdToCrc;
    }

    public String primaryCode() {
        return isUsdPrimary() ? "USD" : "CRC";
    }

    public String secondaryCode() {
        return isUsdPrimary() ? "CRC" : "USD";
    }

    public String primarySymbol() {
        return isUsdPrimary() ? "$" : "CRC";
    }

    public String secondarySymbol() {
        return isUsdPrimary() ? "CRC" : "$";
    }

    public double secondaryAmount(Double primaryAmount) {
        if (primaryAmount == null) {
            return 0.0;
        }
        return isUsdPrimary() ? primaryAmount * usdToCrc : primaryAmount / usdToCrc;
    }

    private boolean isUsdPrimary() {
        return "USD".equals(primaryCurrency);
    }
}
