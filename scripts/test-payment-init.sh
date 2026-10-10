#!/usr/bin/env bash
# Probe the production payment-init endpoint with an appointment-style payload.
#
# Usage:
#   scripts/test-payment-init.sh anonymous
#   TEST_EMAIL='someone@example.com' TEST_PASSWORD='StrongPass123' scripts/test-payment-init.sh email
#   FIREBASE_ID_TOKEN='...' scripts/test-payment-init.sh token
#
# Optional payload overrides:
#   DOCTOR_ID, CONSULTATION_ID, AMOUNT, PROVIDER, PAYMENT_ACCOUNT, BUYER_EMAIL,
#   BUYER_NAME, BUYER_PHONE, IDEMPOTENCY_KEY

set -euo pipefail
cd "$(dirname "$0")/.."

MODE="${1:-anonymous}"
FIREBASE_API_KEY="${FIREBASE_API_KEY:-AIzaSyB6XncMhXdlT0fScdU6Fq7Nw_toPmf-tRU}"
PAYMENT_API_BASE_URL="${PAYMENT_API_BASE_URL:-https://payments.hasethospital.or.tz/public/api}"

require_jq() {
  if ! command -v jq >/dev/null 2>&1; then
    echo "ERROR: jq is required." >&2
    exit 1
  fi
}

firebase_post() {
  local endpoint="$1"
  local body="$2"
  curl -sS -X POST "https://identitytoolkit.googleapis.com/v1/${endpoint}?key=${FIREBASE_API_KEY}" \
    -H "Content-Type: application/json" \
    --data "$body"
}

require_jq

TOKEN="${FIREBASE_ID_TOKEN:-}"
LOCAL_ID="${USER_ID:-}"
AUTH_LABEL="provided token"

case "$MODE" in
  token)
    if [[ -z "$TOKEN" ]]; then
      echo "ERROR: FIREBASE_ID_TOKEN is required for token mode." >&2
      exit 1
    fi
    LOCAL_ID="${LOCAL_ID:-token_user}"
    ;;
  anonymous)
    AUTH_LABEL="anonymous Firebase user"
    RESPONSE="$(firebase_post "accounts:signUp" '{"returnSecureToken":true}')"
    TOKEN="$(printf "%s" "$RESPONSE" | jq -r '.idToken // empty')"
    LOCAL_ID="$(printf "%s" "$RESPONSE" | jq -r '.localId // empty')"
    ;;
  email)
    AUTH_LABEL="email/password Firebase user"
    if [[ -z "${TEST_EMAIL:-}" || -z "${TEST_PASSWORD:-}" ]]; then
      echo "ERROR: TEST_EMAIL and TEST_PASSWORD are required for email mode." >&2
      exit 1
    fi
    SIGNUP_BODY="$(jq -n \
      --arg email "$TEST_EMAIL" \
      --arg password "$TEST_PASSWORD" \
      '{email: $email, password: $password, returnSecureToken: true}')"
    RESPONSE="$(firebase_post "accounts:signUp" "$SIGNUP_BODY")"
    TOKEN="$(printf "%s" "$RESPONSE" | jq -r '.idToken // empty')"
    LOCAL_ID="$(printf "%s" "$RESPONSE" | jq -r '.localId // empty')"
    if [[ -z "$TOKEN" ]]; then
      ERROR_MESSAGE="$(printf "%s" "$RESPONSE" | jq -r '.error.message // empty')"
      if [[ "$ERROR_MESSAGE" == "EMAIL_EXISTS" ]]; then
        RESPONSE="$(firebase_post "accounts:signInWithPassword" "$SIGNUP_BODY")"
        TOKEN="$(printf "%s" "$RESPONSE" | jq -r '.idToken // empty')"
        LOCAL_ID="$(printf "%s" "$RESPONSE" | jq -r '.localId // empty')"
      fi
      if [[ -z "$TOKEN" ]]; then
        echo "Firebase email auth failed:"
        printf "%s\n" "$RESPONSE" | jq .
        exit 1
      fi
    fi
    ;;
  *)
    echo "ERROR: mode must be one of: anonymous, email, token" >&2
    exit 1
    ;;
esac

if [[ -z "$TOKEN" ]]; then
  echo "ERROR: could not obtain Firebase ID token for $AUTH_LABEL." >&2
  exit 1
fi

LOCAL_ID="${LOCAL_ID:-codex_payment_test_user}"
DOCTOR_ID="${DOCTOR_ID:-test_doctor}"
CONSULTATION_ID="${CONSULTATION_ID:-codex_appointment_test}"
AMOUNT="${AMOUNT:-500}"
PROVIDER="${PROVIDER:-Vodacom}"
PAYMENT_ACCOUNT="${PAYMENT_ACCOUNT:-0712345678}"
BUYER_EMAIL="${BUYER_EMAIL:-${TEST_EMAIL:-support@hasethospital.or.tz}}"
BUYER_NAME="${BUYER_NAME:-Codex Payment Test}"
BUYER_PHONE="${BUYER_PHONE:-$PAYMENT_ACCOUNT}"
IDEMPOTENCY_KEY="${IDEMPOTENCY_KEY:-codextest$(date +%s)}"

PAYLOAD="$(jq -n \
  --arg user_id "$LOCAL_ID" \
  --arg doctor_id "$DOCTOR_ID" \
  --arg consultation_id "$CONSULTATION_ID" \
  --arg amount "$AMOUNT" \
  --arg provider "$PROVIDER" \
  --arg payment_account "$PAYMENT_ACCOUNT" \
  --arg buyer_email "$BUYER_EMAIL" \
  --arg buyer_name "$BUYER_NAME" \
  --arg buyer_phone "$BUYER_PHONE" \
  '{
    user_id: $user_id,
    doctor_id: $doctor_id,
    consultation_id: $consultation_id,
    amount: ($amount | tonumber),
    payment_method: "mobile_money",
    provider: $provider,
    payment_account: $payment_account,
    buyer_email: $buyer_email,
    buyer_name: $buyer_name,
    buyer_phone: $buyer_phone
  }')"

echo "Auth mode: $AUTH_LABEL"
echo "Payload:"
printf "%s\n" "$PAYLOAD" | jq '{user_id, doctor_id, consultation_id, amount, payment_method, provider, payment_account, buyer_email}'
echo ""
echo "Payment init response:"
curl -sS -i -X POST "${PAYMENT_API_BASE_URL%/}/mobile/payment/initiate" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  --data "$PAYLOAD"
