#!/usr/bin/env bash
# Build a signed release App Bundle (AAB) for Google Play Console.

set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE_PROPS="keystore.properties"

if [[ ! -f "$KEYSTORE_PROPS" ]]; then
  echo "ERROR: $KEYSTORE_PROPS not found."
  echo "Copy keystore.properties.example to keystore.properties and fill in your credentials."
  exit 1
fi

KEYSTORE="$(grep '^storeFile=' "$KEYSTORE_PROPS" | cut -d= -f2-)"
if [[ ! -f "$KEYSTORE" ]]; then
  echo "ERROR: Keystore not found: $KEYSTORE"
  echo "Place your .jks file in the project root, e.g.:"
  echo "  scp user@server:/path/keyforAfyaNew.jks ./keyforAfyaNew.jks"
  exit 1
fi

echo "Building signed release bundle..."
./gradlew bundleRelease

AAB="app/build/outputs/bundle/release/app-release.aab"
if [[ -f "$AAB" ]]; then
  echo ""
  echo "SUCCESS: $AAB"
  ls -lh "$AAB"
  echo ""
  echo "Upload to Play Console → Release → Testing/Production."
fi
