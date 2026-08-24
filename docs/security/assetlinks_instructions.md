# App Links Verification — assetlinks.json

To make the password-reset deep link open AfyaHASET **silently** (no "Open with"
chooser), host this file at:

```
https://www.hasethospital.or.tz/.well-known/assetlinks.json
```

- Must be served over HTTPS with `Content-Type: application/json`
- Must be reachable without redirects
- `package_name` below matches the manifest (`com.haset.hasetapp`)
- The SHA-256 below is the **DEBUG** keystore fingerprint (for testing).
  Before publishing to Play Store, replace it with your **release/upload
  keystore** fingerprint, or better, the one Google re-signs with if using
  Play App Signing (Play Console → Setup → App signing key).

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.haset.hasetapp",
    "sha256_cert_fingerprints": [
      "18:99:6F:A3:38:57:65:68:EE:56:88:C7:BB:26:7A:61:4A:76:FD:FA:E1:D3:D1:92:F0:40:C6:22:45:94:02:48"
    ]
  }
}]
```

## Verify after hosting

1. `adb shell pm verify-app-links --re-verify com.haset.hasetapp`
2. Wait a few minutes, then check:
   `adb shell pm get-app-links com.haset.hasetapp`
   → domain should show as **verified**
3. Test link: open `https://www.hasethospital.or.tz/reset?oobCode=test` in
   Chrome → should route straight into ForgotPasswordActivity.

> Note: until verification succeeds, Android shows an app-chooser dialog for
> reset links. Functionality is identical; only UX differs.
