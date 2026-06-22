package com.haset.hasetapp.models;

public class CancelPaymentRequest {
    private int transaction_id;

    public CancelPaymentRequest(int transactionId) {
        this.transaction_id = transactionId;
    }

    public int getTransactionId() { return transaction_id; }
    public void setTransactionId(int transactionId) { this.transaction_id = transactionId; }
}
