#!/bin/bash
# Profile Photo Debug Log Tracker
# This script filters logcat for profile photo related logs

echo "=== Profile Photo Debug Log Tracker ==="
echo "Filtering logs for: ProfilePhotoHelper, CloudinaryUpload, Glide"
echo "Press Ctrl+C to stop"
echo ""

adb logcat -c  # Clear existing logs

# Filter for profile photo related logs
adb logcat | grep -E "(ProfilePhotoHelper|CloudinaryUpload|Glide|HASETApplication.*Cloudinary)" --color=always
