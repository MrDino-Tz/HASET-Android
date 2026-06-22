# ZenoPay Backend Requirements for HASETApp

**Version:** 1.2  
**Date:** April 4, 2026  
**Purpose:** Requirements document for ZenoPay integration with HASET Android App

---

## Overview

HASETApp is a doctor-patient appointment management Android application that uses ZenoPay for payment processing. This document outlines the backend requirements needed for the payment system to work correctly.

---

## Payment Flow (ZenoPay Integration)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PAYMENT FLOW                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. App → Backend: Initiate Payment (with webhook_url)               │
│     └── Backend → ZenoPay: Request payment + webhook_url              │
│                                                                       │
│  2. ZenoPay → Backend: Webhook (payment_status: COMPLETED)          │
│     └── Backend: Update transaction status in database                │
│                                                                       │
│  3. App polls → Backend: Check payment status                        │
│     └── Backend: Return updated status from database                │
│                                                                       │
│  4. If status = success: Update doctor wallet (60% share)           │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Required Behavior

### 1. Payment Status Values

The backend MUST return one of the following transaction statuses:

| Status | Meaning | App Action |
|--------|---------|------------|
| `success` | Payment completed successfully | Complete transaction, update doctor wallet |
| `failed` | Payment failed (insufficient funds, declined, etc.) | Show error to user |
| `cancelled` | User cancelled the USSD prompt | Show error to user |
| `expired` | USSD session timed out (>50 seconds) | Show error to user |
| `declined` | Payment was declined | Show error to user |
| `processing` | Payment in progress | Continue polling |

### 2. Payment Request Format (App → Backend)

The app sends this to backend at `POST /api/payment/initiate`:

```json
{
  "user_id": "user-123",
  "doctor_id": "doctor-456",
  "amount": 200.00,
  "provider": "Vodacom",
  "payment_account": "0744963858",
  "webhook_url": "https://your-domain.com/api/payment/webhook",
  "order_id": "HASET-1234567890",
  "buyer_email": "patient@example.com",
  "buyer_name": "John Doe",
  "buyer_phone": "0744963858"
}
```

**Important:** Your backend must pass `webhook_url` to ZenoPay so ZenoPay can notify your backend when payment completes.

### 3. API Response Requirements

#### Endpoint: `GET /api/payment/status?transaction_id={id}`

**Required Response Fields:**

```json
{
  "status": "success",
  "transaction": {
    "id": 31,
    "status": "success",           // REQUIRED: Must be one of: success, failed, cancelled, expired, declined, processing
    "amount": "200.00",
    "currency": "TZS",
    "provider": "Vodacom",
    "zeno_status": "COMPLETED",    // REQUIRED: ZenoPay's internal status
    "created_at": "2026-04-03T16:54:00.000000Z",
    "updated_at": "2026-04-03T16:54:10.000000Z"
  }
}
```

#### Important Notes:

1. **Status Field is Critical**
   - The `transaction.status` field determines the app behavior
   - Must be lowercase: `success`, `failed`, `cancelled`, `expired`, `declined`, `processing`

2. **Provider Field**
   - Must return the provider name (e.g., `Vodacom`, `Airtel`, `Tigo`, `Halotel`)
   - Currently returning `null`

3. **Zeno Status**
   - Include `zeno_status` for debugging
   - This helps identify the actual payment gateway status

---

## Webhook Integration (REQUIRED)

ZenoPay sends webhooks to notify your backend of payment status changes. You MUST implement a webhook endpoint.

### Webhook Setup

Include `webhook_url` in your payment request to ZenoPay:

```json
{
  "order_id": "your-order-id",
  "buyer_email": "customer@example.com",
  "buyer_name": "John Doe",
  "buyer_phone": "0744963858",
  "amount": 1000,
  "webhook_url": "https://your-domain.com/api/payment/webhook"
}
```

### Webhook Endpoint Requirements

**Endpoint:** `POST /api/payment/webhook`

1. **Must be publicly accessible via HTTPS**
2. **Verify `x-api-key` header** to ensure requests are from ZenoPay
3. **Return HTTP 200 OK** immediately after receiving

### Webhook Payload (Mobile Money)

```json
{
  "order_id": "677e43274d7cb",
  "payment_status": "COMPLETED",
  "reference": "1003020496",
  "metadata": {
    "product": "HASET Appointment Payment"
  }
}
```

### Webhook Payload (Dynamic Lipa)

```json
{
  "order_id": "677e43274d7cb",
  "payment_status": "COMPLETED",
  "reference": "1003020496",
  "amount": 1000,
  "currency": "TZS",
  "payment_method": "M-Pesa",
  "timestamp": "2025-11-05T12:45:23+03:00",
  "metadata": {
    "product_name": "HASET Appointment",
    "customer_id": "CUST-1029"
  }
}
```

### Webhook Response

Return this to acknowledge receipt:

```json
{
  "status": "received"
}
```

### Webhook Authentication (Node.js Example)

```javascript
app.post('/api/payment/webhook', (req, res) => {
  // Verify the API key
  const apiKey = req.headers['x-api-key'];
  if (apiKey !== process.env.ZENOPAY_API_KEY) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  // Process the webhook
  const { order_id, payment_status, reference, metadata } = req.body;

  if (payment_status === 'COMPLETED') {
    // Update your database - set transaction status to 'success'
    // Update doctor wallet (60% share)
    console.log(`Payment completed for order: ${order_id}`);
  }

  // Return 200 OK immediately
  res.status(200).json({ status: 'received' });
});
```

### Webhook Authentication (Python/Flask Example)

```python
@app.route('/api/payment/webhook', methods=['POST'])
def payment_webhook():
    # Verify the API key
    api_key = request.headers.get('x-api-key')
    if api_key != os.getenv('ZENOPAY_API_KEY'):
        return jsonify({'error': 'Unauthorized'}), 401
    
    # Process the webhook
    data = request.json
    order_id = data.get('order_id')
    payment_status = data.get('payment_status')
    reference = data.get('reference')
    
    if payment_status == 'COMPLETED':
        # Update your database - set transaction status to 'success'
        # Update doctor wallet (60% share)
        print(f'Payment completed for order: {order_id}')
    
    # Return 200 OK immediately
    return jsonify({'status': 'received'}), 200
```

### Webhook Best Practices

- Always verify the `x-api-key` header to ensure the request is from ZenoPay
- Process webhooks asynchronously to avoid timeouts
- Return a 200 OK response immediately after receiving the webhook
- Implement idempotency to handle duplicate webhook deliveries
- Log all webhook requests for debugging and audit purposes
- Use HTTPS for your webhook endpoint to ensure secure communication

### Important: USSD Push Behavior

ZenoPay USSD pushes are **asynchronous** - once initiated, they cannot be cancelled in real-time. 

**What happens:**
1. App initiates payment → Backend sends request to ZenoPay
2. ZenoPay queues USSD push (takes 1-30 seconds)
3. Backend returns success to app → App starts polling
4. User clicks "Cancel" in app → Backend marks as cancelled
5. USSD popup may still arrive on user's phone

**User Experience:**
- Inform users that USSD may arrive but to IGNORE it
- If USSD arrives, user should NOT enter PIN
- Payment will NOT process if transaction is marked as cancelled
- Backend must ignore ZenoPay webhooks for cancelled transactions

---

## Expected Scenarios

### Scenario 1: Payment Successful ✅

**User completes USSD with correct PIN**

```json
{
  "status": "success",
  "transaction": {
    "id": 31,
    "status": "success",
    "amount": "200.00",
    "provider": "Vodacom",
    "zeno_status": "COMPLETED"
  }
}

**App Action:** Closes payment activity, updates doctor wallet (60% share)

---

### Scenario 2: Insufficient Funds ❌

**User has insufficient balance**

```json
{
  "status": "success",
  "transaction": {
    "id": 31,
    "status": "failed",
    "amount": "200.00",
    "provider": "Vodacom",
    "zeno_status": "INSUFFICIENT_FUNDS"
  }
}

**App Action:** Shows error dialog: "Payment was unsuccessful or cancelled."

---

### Scenario 3: User Cancelled USSD ❌

**User dismisses/cancels the USSD prompt**

```json
{
  "status": "success",
  "transaction": {
    "id": 31,
    "status": "cancelled",
    "amount": "200.00",
    "provider": "Vodacom",
    "zeno_status": "CANCELLED"
  }
}

**App Action:** Shows error dialog: "Payment was unsuccessful or cancelled."

---

### Scenario 4: USSD Timeout ❌

**User doesn't respond to USSD within 50 seconds**

```json
{
  "status": "success",
  "transaction": {
    "id": 31,
    "status": "expired",
    "amount": "200.00",
    "provider": "Vodacom",
    "zeno_status": "EXPIRED"
  }
}

**App Action:** Shows error dialog with message about timeout

---

### Scenario 5: Payment Declined ❌

**Bank/Provider declines the payment**

```json
{
  "status": "success",
  "transaction": {
    "id": 31,
    "status": "declined",
    "amount": "200.00",
    "provider": "Vodacom",
    "zeno_status": "DECLINED"
  }
}

**App Action:** Shows error dialog: "Payment was unsuccessful or cancelled."

---

## Backend Implementation Checklist

### Critical (Must Fix)

- [ ] **Implement webhook endpoint** at `/api/payment/webhook`
- [ ] **Verify `x-api-key` header** in webhook requests
- [ ] **Update transaction status** when webhook received with `COMPLETED`
- [ ] **Return HTTP 200** immediately after processing webhook

### Payment Status Updates

- [ ] Update transaction status when USSD session completes
- [ ] Update transaction status when USSD is cancelled
- [ ] Update transaction status on insufficient funds
- [ ] Update transaction status on timeout (>50 seconds)
- [ ] Update transaction status on decline
- [ ] Include `provider` field in response
- [ ] Include `zeno_status` field in response

### Cancel Payment Endpoint

The app sends a cancel request when user clicks "Cancel" during payment. IMPORTANT: ZenoPay USSD pushes are asynchronous and cannot be cancelled in real-time.

**Endpoint:** `POST /api/payment/cancel`

**Required Behavior:**
1. Mark transaction status as `cancelled` in database
2. Log the cancellation for audit purposes
3. Return success (the payment won't go through even if USSD arrives)

**Implementation (Node.js):**
```javascript
app.post('/api/payment/cancel', async (req, res) => {
  const { transaction_id } = req.body;
  
  // Mark as cancelled in database
  await db.transaction.update(transaction_id, {
    status: 'cancelled',
    cancelled_at: new Date()
  });
  
  // Log for audit
  console.log(`Payment ${transaction_id} cancelled by user`);
  
  // Return success - payment won't process
  res.json({ status: 'cancelled' });
});
```

### Testing

- [ ] Test webhook endpoint with ZenoPay sandbox
- [ ] Test all payment scenarios before deployment
- [ ] Verify doctor wallet updates correctly (60% share)

---

## Troubleshooting

### Issue: App keeps polling forever

**Cause:** Backend always returns `"status": "processing"`

**Solution:** 
1. Implement webhook endpoint
2. ZenoPay will call webhook when payment completes
3. Update status to `success`, `failed`, `cancelled`, `expired`, or `declined`

### Issue: Provider shows as null

**Cause:** Backend not including provider in response

**Solution:** Add provider to the transaction object in response

### Issue: Can't detect insufficient balance

**Cause:** No status update when payment fails

**Solution:** ZenoPay webhook will include `INSUFFICIENT_FUNDS` in zeno_status - update your transaction accordingly

---

## ZenoPay Documentation Links

- Webhooks: https://docs.zenopay.net/webhooks/
- Order Status: https://docs.zenopay.net/order-status/
- Mobile Money: https://docs.zenopay.net/mobile-payment/

---

## Contact

For questions about the Android app payment integration:
- View source code: `/app/src/main/java/com/haset/hasetapp/`
- Payment logic: `PaymentRepository.java`, `PaymentViewModel.java`
- API Documentation: `PAYMENT_API_DOCUMENTATION.md`

---

**End of Requirements Document**
