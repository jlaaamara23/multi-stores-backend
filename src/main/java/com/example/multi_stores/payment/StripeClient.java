package com.example.multi_stores.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * עטיפה ל-Stripe SDK – יצירת PaymentIntent ואימות תשלום.
 */
@Component
public class StripeClient {

    private static final Logger log = LoggerFactory.getLogger(StripeClient.class);

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isBlank()) {
            Stripe.apiKey = stripeApiKey;
        }
    }

    /**
     * יוצר PaymentIntent ב-Stripe ומחזיר client_secret ל-Frontend.
     */
    public String createPaymentIntent(BigDecimal amountInMainUnit, String currency) throws StripeException {
        long amountInSmallestUnit = amountInMainUnit.multiply(BigDecimal.valueOf(100)).longValue();
        if (amountInSmallestUnit < 1) {
            throw new IllegalArgumentException("Amount must be at least 0.01 in currency unit");
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInSmallestUnit)
                .setCurrency(currency != null && !currency.isBlank() ? currency : "ils")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        log.info("Created PaymentIntent: {}", intent.getId());
        return intent.getClientSecret();
    }

    /**
     * מחזיר PaymentIntent לפי מזהה (לצורך אימות ב-webhook).
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }
}
