#!/usr/bin/env bash
# Export the upload certificate (.pem) for Google Play App Signing enrollment.

set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE_PROPS="keystore.properties"
OUTPUT="upload_certificate.pem"

if [[ ! -f "$KEYSTORE_PROPS" ]]; then
  echo "ERROR: $KEYSTORE_PROPS not found."
  exit 1
fi

get_prop() {
  grep "^$1=" "$KEYSTORE_PROPS" | cut -d= -f2-
}

KEYSTORE="$(get_prop storeFile)"
STORE_PASSWORD="$(get_prop storePassword)"
KEY_ALIAS="$(get_prop keyAlias)"

if [[ ! -f "$KEYSTORE" ]]; then
  echo "ERROR: Keystore not found: $KEYSTORE"
  exit 1
fi

keytool -export -rfc \
  -keystore "$KEYSTORE" \
  -alias "$KEY_ALIAS" \
  -storepass "$STORE_PASSWORD" \
  -file "$OUTPUT"

echo "Exported: $OUTPUT"
echo ""
echo "Certificate details (for Firebase / Play Console):"
keytool -list -v -keystore "$KEYSTORE" -storepass "$STORE_PASSWORD" -alias "$KEY_ALIAS" \
  | grep -E "Owner:|SHA1:|SHA256:"
