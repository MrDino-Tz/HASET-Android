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
}
