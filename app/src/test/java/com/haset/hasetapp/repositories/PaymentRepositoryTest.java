package com.haset.hasetapp.repositories;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaymentRepositoryTest {
    @Test
    public void paymentErrorMessageUsesBackendMessage() {
        String message = PaymentRepository.paymentErrorMessage(
                "{\"status\":\"error\",\"message\":\"Unauthorized. A valid Firebase token is required.\"}",
                "mobile_money"
        );

        assertEquals("Unauthorized. A valid Firebase token is required.", message);
    }

    @Test
    public void paymentErrorMessageIncludesValidationDetailAndReference() {
        String message = PaymentRepository.paymentErrorMessage(
                "{\"message\":\"The given data was invalid.\",\"errors\":{\"payment_account\":[\"The payment account field is required.\"]},\"transaction_id\":25}",
                "mobile_money"
        );

        assertEquals(
                "The given data was invalid. The payment account field is required. Reference: 25.",
                message
        );
    }

    @Test
    public void paymentErrorMessageKeepsMobileMoneyFallback() {
        String message = PaymentRepository.paymentErrorMessage("not-json", "mobile_money");

        assertEquals("Payment could not be started. Please try again later.", message);
    }

    @Test
    public void paymentErrorMessageKeepsCardFallback() {
        String message = PaymentRepository.paymentErrorMessage("{}", "card");

        assertEquals(
                "Card payment is temporarily unavailable. Please use mobile money or try again later.",
                message
        );
    }
}
