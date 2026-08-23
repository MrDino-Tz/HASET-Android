package com.haset.hasetapp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.haset.hasetapp.models.PaymentRequest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaymentRequestTest {
    @Test
    public void mobileMoneyRequestMatchesOpenApiContract() {
        PaymentRequest request = new PaymentRequest(
                "user-123",
                "doctor-456",
                "consult-789",
                1500.0,
                "Vodacom",
                "0712345678"
        );

        JsonObject json = new Gson().toJsonTree(request).getAsJsonObject();

        assertEquals("consult-789", json.get("consultation_id").getAsString());
        assertEquals(1500, json.get("amount").getAsLong());
        assertEquals("mobile_money", json.get("payment_method").getAsString());
        assertEquals("Vodacom", json.get("provider").getAsString());
        assertEquals("0712345678", json.get("payment_account").getAsString());
    }

    @Test
    public void cardRequestOmitsMobileMoneyFieldsAndUsesApprovedCallbacks() {
        PaymentRequest request = new PaymentRequest(
                "temporary-user",
                "doctor_registration",
                "registration-123",
                2000.0,
                "card",
                "",
                "",
                null,
                null,
                null,
                "https://hasethospital.or.tz/payment"
        );

        JsonObject json = new Gson().toJsonTree(request).getAsJsonObject();
        assertEquals("card", json.get("payment_method").getAsString());
        org.junit.Assert.assertFalse(json.has("provider"));
        org.junit.Assert.assertFalse(json.has("payment_account"));
        assertEquals("https://hasethospital.or.tz/payment/success",
                json.get("redirect_url").getAsString());
        org.junit.Assert.assertFalse(json.has("cancel_url"));
        org.junit.Assert.assertFalse(json.getAsJsonObject("customer").has("name"));
        assertEquals("support@hasethospital.or.tz",
                json.getAsJsonObject("customer").get("email").getAsString());
        org.junit.Assert.assertFalse(json.getAsJsonObject("customer").has("phone"));
    }

    @Test
    public void cardRequestNormalizesCustomerPhoneToDocumentedPattern() {
        PaymentRequest request = new PaymentRequest(
                "temporary-user",
                "doctor_registration",
                "registration-123",
                2000.0,
                "card",
                "",
                "",
                "patient@example.com",
                "Asha Mushi",
                "255683859574",
                "https://hasethospital.or.tz/payment"
        );

        JsonObject customer = new Gson().toJsonTree(request).getAsJsonObject()
                .getAsJsonObject("customer");

        assertEquals("+255683859574", customer.get("phone").getAsString());
    }
}
