package com.example.multi_stores.service;

import com.example.multi_stores.dto.PaymentIntentRequest;
import com.example.multi_stores.dto.PaymentIntentResponse;
import com.example.multi_stores.payment.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final StripeClient stripeClient;
    private final OrderService orderService;

    public PaymentService(StripeClient stripeClient, OrderService orderService) {
        this.stripeClient = stripeClient;
        this.orderService = orderService;
    }

    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) throws StripeException {
        String currency = request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency() : "ils";
        String clientSecret = stripeClient.createPaymentIntent(request.getAmount(), currency);
        return PaymentIntentResponse.builder()
                .clientSecret(clientSecret)
                .paymentIntentId(null)
                .build();
    }

    /**
     * Confirms payment with Stripe and completes the given orders (for frontend after confirmCardPayment).
     */
    public void confirmPaymentSuccess(String paymentIntentId, List<Long> orderIds) {
        if (paymentIntentId == null || orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("paymentIntentId and orderIds required");
        }
        try {
            PaymentIntent intent = stripeClient.retrievePaymentIntent(paymentIntentId);
            if (!"succeeded".equals(intent.getStatus())) {
                throw new IllegalStateException("Payment not succeeded: " + intent.getStatus());
            }
            for (Long orderId : orderIds) {
                orderService.completeOrder(orderId);
            }
            log.info("Payment {} confirmed, orders {} completed.", paymentIntentId, orderIds);
        } catch (StripeException e) {
            log.error("Stripe error verifying payment {}", paymentIntentId, e);
            throw new RuntimeException("Payment verification failed", e);
        }
    }

    /**
     * Handles webhook from Stripe – verifies payment and completes order.
     */
    public void handlePaymentSuccess(String paymentIntentId, Long orderId) {
        try {
            PaymentIntent intent = stripeClient.retrievePaymentIntent(paymentIntentId);
            if ("succeeded".equals(intent.getStatus())) {
                if (orderId != null) {
                    orderService.completeOrder(orderId);
                }
                log.info("Payment {} confirmed, order {} completed.", paymentIntentId, orderId);
            }
        } catch (StripeException e) {
            log.error("Stripe error verifying payment {}", paymentIntentId, e);
            throw new RuntimeException("Payment verification failed", e);
        }
    }
}
