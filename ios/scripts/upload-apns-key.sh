#!/usr/bin/env bash
# Helper for uploading APNs Auth Key (.p8) to Firebase Cloud Messaging.
# Firebase has no official CLI upload command — use the console link below.

set -euo pipefail

PROJECT_ID="${FIREBASE_PROJECT_ID:-hasetapp-4eeba}"
BUNDLE_ID="${IOS_BUNDLE_ID:-com.haset.hasetapp}"

echo "HASET iOS APNs setup"
echo "===================="
echo ""
echo "1. Create APNs key (Apple Developer, one-time):"
echo "   https://developer.apple.com/account/resources/authkeys/list"
echo "   → + → enable 'Apple Push Notifications service (APNs)' → Register → Download .p8"
echo "   Save Key ID and Team ID (Membership page)."
echo ""
echo "2. Upload to Firebase (manual — required for iPhone push):"
echo "   https://console.firebase.google.com/project/${PROJECT_ID}/settings/cloudmessaging"
echo "   → iOS app (${BUNDLE_ID}) → APNs Authentication Key → Upload"
echo ""

if [[ -n "${APNS_KEY_PATH:-}" ]]; then
  if [[ -f "$APNS_KEY_PATH" ]]; then
    echo "Found key file: $APNS_KEY_PATH"
    echo "Key ID (set APNS_KEY_ID): ${APNS_KEY_ID:-not set}"
    echo "Team ID (set APNS_TEAM_ID): ${APNS_TEAM_ID:-not set}"
    echo ""
    echo "Open Firebase Cloud Messaging settings now..."
    npx firebase-tools open notifications --project "$PROJECT_ID" 2>/dev/null || true
  else
    echo "APNS_KEY_PATH set but file missing: $APNS_KEY_PATH"
    exit 1
  fi
else
  echo "Optional: set APNS_KEY_PATH to verify your .p8 file exists before uploading."
  npx firebase-tools open notifications --project "$PROJECT_ID" 2>/dev/null || true
fi
