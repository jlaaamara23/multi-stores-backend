package com.example.multi_stores.controller;

import com.example.multi_stores.dto.PaymentIntentRequest;
import com.example.multi_stores.dto.PaymentIntentResponse;
import com.example.multi_stores.service.PaymentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponse> createIntent(@Valid @RequestBody PaymentIntentRequest request) {
        try {
            PaymentIntentResponse response = paymentService.createPaymentIntent(request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmPayment(@RequestBody Map<String, Object> body) {
        String paymentIntentId = body != null && body.get("paymentIntentId") != null
                ? body.get("paymentIntentId").toString() : null;
        @SuppressWarnings("unchecked")
        List<Number> orderIdsRaw = body != null && body.get("orderIds") instanceof List
                ? (List<Number>) body.get("orderIds") : null;
        if (paymentIntentId == null || orderIdsRaw == null || orderIdsRaw.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<Long> orderIds = orderIdsRaw.stream().map(Number::longValue).toList();
        try {
            paymentService.confirmPaymentSuccess(paymentIntentId, orderIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature
    ) {
        // Stripe sends payment_intent.succeeded etc. Here we only handle success.
        // In production: verify signature with webhook secret and parse event.
        // For simplicity we accept POST with body like {"paymentIntentId":"pi_xxx","orderId":123}
        try {
            if (payload != null && payload.contains("paymentIntentId")) {
                // Simple JSON parsing - in production use ObjectMapper or Stripe SDK Event
                String piId = extractValue(payload, "paymentIntentId");
                Long orderId = extractOrderId(payload);
                if (piId != null) {
                    paymentService.handlePaymentSuccess(piId, orderId);
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String extractValue(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int start = json.indexOf("\"", i + key.length() + 3) + 1;
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    private Long extractOrderId(String json) {
        int i = json.indexOf("\"orderId\"");
        if (i < 0) return null;
        int start = json.indexOf(":", i) + 1;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end > start) {
            try {
                return Long.parseLong(json.substring(start, end).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
