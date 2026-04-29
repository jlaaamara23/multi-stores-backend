package com.example.multi_stores.dto;

public class PaymentIntentResponse {

    private String clientSecret;
    private String paymentIntentId;

    public PaymentIntentResponse() {}

    public PaymentIntentResponse(String clientSecret, String paymentIntentId) {
        this.clientSecret = clientSecret;
        this.paymentIntentId = paymentIntentId;
    }

    public static PaymentIntentResponseBuilder builder() {
        return new PaymentIntentResponseBuilder();
    }

    public static final class PaymentIntentResponseBuilder {
        private String clientSecret;
        private String paymentIntentId;

        private PaymentIntentResponseBuilder() {}

        public PaymentIntentResponseBuilder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }
        public PaymentIntentResponseBuilder paymentIntentId(String paymentIntentId) {
            this.paymentIntentId = paymentIntentId;
            return this;
        }
        public PaymentIntentResponse build() {
            return new PaymentIntentResponse(clientSecret, paymentIntentId);
        }
    }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
}
