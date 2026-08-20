# HASET Payment API Integration Report

**Date:** March 10, 2026  
**Status:** Analyzed  
**App Version:** HASETApp Android

---

## Executive Summary

This document details the analysis of the HASET Payment Backend API (Laravel) against the Android app implementation. The analysis covers endpoint compatibility, data model alignment, and security considerations.

---

## API Documentation Summary

**Base URL:** `https://payments.hasethospital.or.tz`

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payment/initiate` | Initiate mobile money payment |
| GET | `/api/payment/status` | Check payment status |
| POST | `/api/payment/cancel` | Cancel payment |
| GET | `/api/payment/balance` | Get account balance |
| POST | `/api/payment/payout` | Disburse funds to doctors |
| POST | `/api/payment/callback` | Webhook for payment updates |

---

## ✅ Complete Match - No Action Needed

### 1. API Base URL
- **Android:** `https://payments.hasethospital.or.tz/api/`
- **API Doc:** `https://payments.hasethospital.or.tz`
- **Status:** ✅ MATCH

### 2. Initiate Payment Endpoint
- **Android:** `@POST("payment/initiate")`
- **API Doc:** `POST /api/payment/initiate`
- **Status:** ✅ MATCH

### 3. Request Fields - Initiate Payment
| Field | Android Model | API Doc | Status |
|-------|---------------|---------|--------|
| user_id | ✅ `PaymentRequest.user_id` | string (optional) | ✅ |
| doctor_id | ✅ `PaymentRequest.doctor_id` | string (required) | ✅ |
| amount | ✅ `PaymentRequest.amount` | number (required) | ✅ |
| provider | ✅ `PaymentRequest.provider` | string (required) | ✅ |
| payment_account | ✅ `PaymentRequest.payment_account` | string (required) | ✅ |

### 4. Check Status Endpoint
- **Android:** `@GET("payment/status")`
- **API Doc:** `GET /api/payment/status`
- **Status:** ✅ MATCH

### 5. Cancel Payment Endpoint
- **Android:** `@POST("payment/cancel")`
- **API Doc:** `POST /api/payment/cancel`
- **Status:** ✅ MATCH

### 6. Payout Endpoint
- **Android:** `@POST("payment/payout")`
- **API Doc:** `POST /api/payment/payout`
- **Status:** ✅ MATCH

### 7. Payout Request Fields
| Field | Android Model | API Doc | Status |
|-------|---------------|---------|--------|
| request_id | ✅ `PayoutRequest.request_id` | string (required) | ✅ |
| doctor_id | ✅ `PayoutRequest.doctor_id` | string (required) | ✅ |
| amount | ✅ `PayoutRequest.amount` | number (required) | ✅ |
| phone_number | ✅ `PayoutRequest.phone_number` | string (required) | ✅ |
| provider | ✅ `PayoutRequest.provider` | string (required) | ✅ |
| admin_id | ✅ `PayoutRequest.admin_id` | string (required) | ✅ |
| password | ✅ `PayoutRequest.password` | string (required) | ✅ |

### 8. Balance Endpoint
- **Android:** `@GET("payment/balance")`
- **API Doc:** `GET /api/payment/balance`
- **Status:** ✅ MATCH

### 9. Response - Payment Status Transaction Fields
| Field | Android Model | API Doc | Status |
|-------|---------------|---------|--------|
| id | ✅ `Transaction.id` | int | ✅ |
| status | ✅ `Transaction.status` | string | ✅ |
| amount | ✅ `Transaction.amount` | string/decimal | ✅ |
| currency | ✅ `Transaction.currency` | string | ✅ |
| provider | ✅ `Transaction.provider` | string | ✅ |
| created_at | ✅ `Transaction.created_at` | timestamp | ✅ |
| updated_at | ✅ `Transaction.updated_at` | timestamp | ✅ |
| external_reference | ✅ `Transaction.external_reference` (NEWLY ADDED) | string | ✅ ADDED |

### 10. Status Checking Logic
- **Android:** `isSuccess()`, `isFailed()`, `isProcessing()`
- **API Values:** "success", "failed", "processing", "pending"
- **Status:** ✅ MATCH

---

## 🔴 CRITICAL ISSUES - LEFT IN APP FOR NOW

### 1. Admin Password Sent from Client
**Severity:** CRITICAL  
**Issue:** Admin password is included in `PayoutRequest` and sent from the Android app.

**Current Implementation:**
```java
// PayoutRequest.java
private String password; // Sent from app!
```

**Risk:** If APK is reverse-engineered, attackers can access admin password and drain all funds.

**Recommendation:** Move payout approval to server-side (Cloud Functions) or use API key authentication.

**Status:** Left in app for now (as requested)

---

### 2. No API Key Authentication
**Severity:** CRITICAL  
**Issue:** API documentation states "No API key authentication required for public endpoints"

**Risk:** Anyone can access:
- Account balance
- Initiate payouts
- Cancel payments

**Recommendation:** Implement API key for sensitive endpoints.

**Status:** Left in app for now (as requested)

---

## 🟠 HIGH PRIORITY - FIXED

### 1. Amount Validation Mismatch
**Status:** ✅ FIXED

| | Before (Android) | After (Android) | API Doc |
|---|---|---|---|
| Min Amount | 100 TZS | **50 TZS** | 50 TZS |
| Max Amount | 10,000,000 TZS | **5,000,000 TZS** | 5,000,000 TZS |

**Changes Made:**
```java
// Constants.java
public static final double MIN_PAYMENT_AMOUNT = 50.0;   // Was: 100.0
public static final double MAX_PAYMENT_AMOUNT = 5000000.0; // Was: 10000000.0
```

---

## 🟡 MEDIUM PRIORITY - FIXED

### 1. Debug Mode Using Development URL
**Status:** ✅ FIXED

| | Before | After |
|---|---|---|
| IS_DEBUG_MODE | `true` | `false` |
| API URL | ngrok development | **Production** |

**Changes Made:**
```java
// Constants.java
public static final boolean IS_DEBUG_MODE = false;  // Was: true
public static final String API_BASE_URL = IS_DEBUG_MODE ? DEVELOPMENT_API_URL : PRODUCTION_API_URL;
```

---

### 2. Missing external_reference Field
**Status:** ✅ FIXED

Added to `PaymentStatusResponse.Transaction` class:
```java
// PaymentStatusResponse.java
private String external_reference;

public String getExternalReference() { return external_reference; }
public void setExternalReference(String externalReference) { this.external_reference = externalReference; }
```

---

## 📋 REMAINING RECOMMENDATIONS

### Priority 1 - Security (Not Implemented - Left for Later)
1. **Remove admin password from client** - Requires backend changes
2. **Add API key authentication** - Requires backend changes
3. **Implement SSL Pinning** - Android app change

### Priority 2 - Future Enhancements
1. **Add webhook support** - Currently using polling only
2. **Add retry logic** - For failed network requests
3. **Add request signing** - For additional security

---

## Files Modified

| File | Changes |
|------|---------|
| `Constants.java` | - Updated `MIN_PAYMENT_AMOUNT` from 100 to 50<br>- Updated `MAX_PAYMENT_AMOUNT` from 10,000,000 to 5,000,000<br>- Set `IS_DEBUG_MODE` to `false` |
| `PaymentStatusResponse.java` | - Added `external_reference` field to `Transaction` class |

---

## Testing Checklist

- [ ] Payment initiation with minimum amount (50 TZS)
- [ ] Payment initiation with maximum amount (5,000,000 TZS)
- [ ] Payment initiation with amount below minimum (should fail)
- [ ] Payment initiation with amount above maximum (should fail)
- [ ] Payment status check returns external_reference
- [ ] Production build uses correct API URL

---

## API Response Examples (Reference)

### Initiate Payment Success
```json
{
  "status": "success",
  "message": "Payment initiated successfully.",
  "transaction_id": 15,
  "order_reference": "HASET15T1773141947"
}
```

### Payment Status
```json
{
  "status": "success",
  "transaction": {
    "id": 15,
    "status": "processing",
    "amount": "1000.00",
    "currency": "TZS",
    "provider": "Vodacom",
    "created_at": "2026-03-10T11:25:47.000000Z",
    "updated_at": "2026-03-10T11:25:47.000000Z",
    "external_reference": "1003020496"
  }
}
```

### Webhook Callback
```json
{
  "order_id": "HASET15T1773141947",
  "payment_status": "COMPLETED",
  "reference": "1003020496"
}
```

---

**Document Version:** 1.0  
**Last Updated:** March 10, 2026
