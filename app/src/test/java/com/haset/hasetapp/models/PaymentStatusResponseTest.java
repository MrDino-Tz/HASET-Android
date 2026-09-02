package com.haset.hasetapp.models;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaymentStatusResponseTest {

    @Test
    public void doesNotTreatApiSuccessWithPendingPaymentStatusAsSettled() {
        PaymentStatusResponse.Transaction transaction = new PaymentStatusResponse.Transaction();
        transaction.setStatus("success");
        transaction.setPaymentStatus("pending");

        assertFalse(transaction.isSuccess());
        assertTrue(transaction.isProcessing());
    }

    @Test
    public void treatsCompletedPaymentStatusAsSettled() {
        PaymentStatusResponse.Transaction transaction = new PaymentStatusResponse.Transaction();
        transaction.setStatus("processing");
        transaction.setPaymentStatus("COMPLETED");

        assertTrue(transaction.isSuccess());
        assertFalse(transaction.isProcessing());
    }

    @Test
    public void treatsProcessingStatusWithoutPaymentStatusAsPending() {
        PaymentStatusResponse.Transaction transaction = new PaymentStatusResponse.Transaction();
        transaction.setStatus("processing");

        assertFalse(transaction.isSuccess());
        assertTrue(transaction.isProcessing());
    }

    @Test
    public void topLevelSuccessDoesNotSettleWithoutTransaction() {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setStatus("success");

        assertFalse(response.isSuccess());
    }
}
