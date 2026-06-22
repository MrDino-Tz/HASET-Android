# HASET Payment API Documentation

**Base URL:** `https://payments.hasethospital.or.tz`  
**Content-Type:** `application/json`  
**CORS:** Enabled for all origins

---

## 1. Initiate Payment
Triggers a USSD push to the customer's phone.

**POST** `/api/payment/initiate`

**Request Body:**
```json
{
  "user_id": "user_123",
  "doctor_id": "doctor_001",
  "amount": 1000,
  "provider": "Vodacom",
  "payment_account": "0664072400"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| user_id | string | No | Patient/user ID |
| doctor_id | string | Yes | Doctor ID |
| amount | number | Yes | Amount in TZS (min: 50, max: 5,000,000) |
| provider | string | Yes | Mobile network e.g. `Vodacom`, `Airtel`, `Tigo`, `Halotel` |
| payment_account | string | Yes | Phone number e.g. `0664072400` |

**Success Response (200):**
```json
{
  "status": "success",
  "message": "Payment initiated successfully. Please check your phone to complete the payment.",
  "transaction_id": 27,
  "order_reference": "HASET27T1775153750",
  "zeno_status": "PENDING",
  "payment_channel": "Vodacom"
}
```

**Error Response (400):**
```json
{
  "status": "error",
  "message": "Payment initiation failed",
  "transaction_id": 27
}
```

**Duplicate Request (429):**
```json
{
  "status": "error",
  "message": "A payment request is already active for this doctor. Please wait for the USSD prompt on your phone.",
  "transaction_id": 25
}
```

**Example - cURL:**
```bash
curl -X POST https://payments.hasethospital.or.tz/api/payment/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user_123",
    "doctor_id": "doctor_001",
    "amount": 1000,
    "provider": "Vodacom",
    "payment_account": "0664072400"
  }'
```

**Example - JavaScript (fetch):**
```javascript
const response = await fetch('https://payments.hasethospital.or.tz/api/payment/initiate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    user_id: 'user_123',
    doctor_id: 'doctor_001',
    amount: 1000,
    provider: 'Vodacom',
    payment_account: '0664072400'
  })
});
const data = await response.json();
console.log(data);
```

**Example - Dart (Flutter):**
```dart
final response = await http.post(
  Uri.parse('https://payments.hasethospital.or.tz/api/payment/initiate'),
  headers: {'Content-Type': 'application/json'},
  body: jsonEncode({
    'user_id': 'user_123',
    'doctor_id': 'doctor_001',
    'amount': 1000,
    'provider': 'Vodacom',
    'payment_account': '0664072400',
  }),
);
final data = jsonDecode(response.body);
```

---

## 2. Check Payment Status
Poll this endpoint to check if payment was completed.

**GET** `/api/payment/status?transaction_id=27`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| transaction_id | integer | Yes | Transaction ID from initiate response |

**Success Response (200):**
```json
{
  "status": "success",
  "transaction": {
    "id": 27,
    "status": "success",
    "amount": "1000.00",
    "currency": "TZS",
    "provider": "Vodacom",
    "created_at": "2026-04-02T18:15:50.000000Z",
    "updated_at": "2026-04-02T18:16:10.000000Z"
  }
}
```

**Transaction Status Values:**
| Status | Meaning |
|--------|---------|
| `pending` | Payment created, waiting |
| `processing` | USSD sent, waiting for customer |
| `success` | Payment completed |
| `failed` | Payment failed or cancelled |

**Example - cURL:**
```bash
curl -X GET "https://payments.hasethospital.or.tz/api/payment/status?transaction_id=27"
```

**Example - JavaScript:**
```javascript
const response = await fetch(
  'https://payments.hasethospital.or.tz/api/payment/status?transaction_id=27'
);
const data = await response.json();
// Poll every 5 seconds until status is 'success' or 'failed'
```

**Polling Example - JavaScript:**
```javascript
async function pollPaymentStatus(transactionId) {
  const interval = setInterval(async () => {
    const response = await fetch(
      `https://payments.hasethospital.or.tz/api/payment/status?transaction_id=${transactionId}`
    );
    const data = await response.json();
    const status = data.transaction.status;

    if (status === 'success') {
      clearInterval(interval);
      console.log('Payment successful!');
    } else if (status === 'failed') {
      clearInterval(interval);
      console.log('Payment failed.');
    }
  }, 5000); // poll every 5 seconds
}
```

---

## 3. Cancel Payment

**POST** `/api/payment/cancel`

**Request Body:**
```json
{
  "transaction_id": 27
}
```

**Success Response (200):**
```json
{
  "status": "success",
  "message": "Transaction cancelled"
}
```

**Example - cURL:**
```bash
curl -X POST https://payments.hasethospital.or.tz/api/payment/cancel \
  -H "Content-Type: application/json" \
  -d '{"transaction_id": 27}'
```

---

## 4. Get Account Balance

**GET** `/api/payment/balance`

**Success Response (200):**
```json
{
  "status": "success",
  "data": {
    "balance": 45000.00,
    "currency": "TZS"
  }
}
```

**Example - cURL:**
```bash
curl -X GET https://payments.hasethospital.or.tz/api/payment/balance
```

---

## 5. Payout (Admin Only)
Disburse funds to a doctor's mobile money account.

**POST** `/api/payment/payout`

**Request Body:**
```json
{
  "request_id": "payout_001",
  "doctor_id": "doctor_001",
  "amount": 5000,
  "phone_number": "0664072400",
  "provider": "Vodacom",
  "admin_id": "admin_001",
  "password": "your_admin_password"
}
```

**Success Response (200):**
```json
{
  "status": "success",
  "message": "Funds successfully disbursed to 0664072400",
  "transaction_id": 28
}
```

---

## 6. Payment Webhook (Callback)
ZenoPay calls this automatically when payment status changes. Configure in your ZenoPay dashboard.

**POST** `/api/payment/callback`

**Webhook URL:** `https://payments.hasethospital.or.tz/api/payment/callback`

**Payload from ZenoPay:**
```json
{
  "order_id": "HASET27T1775153750",
  "payment_status": "COMPLETED",
  "reference": "1003020496"
}
```

**Response:**
```json
{
  "status": "received"
}
```

---

## Complete Payment Flow

```
1. App calls POST /api/payment/initiate
        ↓
2. Customer receives USSD push on phone
        ↓
3. Customer enters PIN to authorize payment
        ↓
4. ZenoPay sends callback to /api/payment/callback
        ↓
5. App polls GET /api/payment/status?transaction_id=X
        ↓
6. Status changes to "success" → Payment complete
```

---

## Error Codes

| HTTP Code | Meaning |
|-----------|---------|
| 200 | Success |
| 400 | Bad request / payment failed |
| 401 | Unauthorized (wrong admin password) |
| 404 | Transaction not found |
| 422 | Validation error |
| 429 | Duplicate payment request |
| 500 | Server error |

---

## Notes
- All amounts are in **TZS (Tanzanian Shillings)**
- Phone numbers can be in format `0693002400` or `255664072400`
- Supported providers: `Vodacom`, `Airtel`, `Tigo`, `Halotel`
- USSD session expires after **50 seconds** - poll status every 5 seconds
- Duplicate payments within 2 minutes are automatically blocked
