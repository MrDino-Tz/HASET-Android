# HASETApp - Publish & Market Checklist

## Legal & Compliance (Required for Play Store)
- [ ] **Privacy Policy** - Required by Google Play, critical for healthcare data
- [ ] **Terms of Service**
- [ ] **Medical Disclaimer** - Liability protection for health advice
- [ ] Review **HIPAA compliance** (if serving US users) or equivalent data protection regulations

## App Store Preparation
- [ ] **Screenshots** (6-8 screenshots in multiple device sizes: phone, tablet)
- [ ] **Feature Graphic** (1024x512px)
- [ ] **App Icon** variants for all densities
- [ ] **Privacy Policy URL** - Hosted on a website
- [ ] **Promotional Video** (optional but recommended - 30sec)
- [ ] **App Description** with relevant keywords
- [ ] **Category** selection: Medical/Health & Fitness

## Technical Requirements
- [ ] **Crashlytics** integration (add Firebase Crashlytics dependency)
- [ ] **Firebase Analytics** - Track user behavior
- [ ] **App Indexing** - Enable deep linking for Google search
- [ ] **Build signed APK/AAB** for release
- [ ] **Version code update** (increment from current v1)

## Security & Performance
- [ ] **ProGuard/R8** rules verified (already configured)
- [ ] **SSL Pinning** for API calls
- [ ] **APK size optimization** (currently using ABI filters - good)
- [ ] **Privacy Sandbox** compliance (Android 13+)

## Security Configurations (CRITICAL - Currently Missing)
- [ ] **Network Security Config** - Create `res/xml/network_security_config.xml`
  - Configure domain allowlist
  - Set `cleartextTrafficPermitted="false"` for production
- [ ] **Disable cleartextTraffic** - Set `android:usesCleartextTraffic="false"` in AndroidManifest.xml
  - Currently set to `true` (SECURITY RISK)
- [ ] **Enable certificate pinning** on Retrofit/OkHttp for Firebase and API calls
- [ ] **Obfuscate Firebase API keys** - Use restricted keys in google-services.json

## ProGuard Rules Review
- [ ] **Verify all dependencies** have ProGuard rules (Firebase, Glide, Retrofit, Room, Cloudinary)
- [ ] **Test release build** - Ensure no crashes due to obfuscation
- [ ] **Keep model classes** used with Firebase properly configured
- [ ] **Enable logging removal** in release build (-assumenosideeffects)

## App Bundle Optimization
- [ ] **Enable split APKs** by ABI for smaller download sizes
- [ ] **Configure resource shrinking** (already enabled with shrinkResources)
- [ ] **Enable R8 full mode** for better optimization
- [ ] **Test App Bundle** with bundletool before release

## App Rating System
- [x] **In-App Rating** - Implemented via AppRatingHelper.java
- [ ] **Auto-prompt** - Triggers after 5 launches + 3 days
- [x] **Manual button** - Added to Profile screen ("Rate App")
- [ ] **Google Play In-App Review API** - Add when app is published (requires Play Core library + published app)
- [ ] **Rating link** - Opens Play Store (market:// or https://)
- [ ] **Test rating flow** - Verify Play Store opens correctly

### Rating Configuration
| Setting | Current Value |
|---------|---------------|
| Launches before prompt | 5 |
| Days before prompt | 3 |
| Persisted in | SharedPreferences |
| Location | Profile > Rate App |

## Marketing Essentials
- [ ] **Landing page/website** with app download links
- [ ] **Support contact email** displayed in app
- [ ] **Social media presence**
- [ ] **Screenshots for website** (use Play Store visuals)

## Business Setup
- [ ] **Test payments** completed with Zeno/M-Pesa
- [ ] **Doctor/Pharmacy agreements** - Terms of use
- [ ] **Revenue model** defined (commission, subscription, etc.)
- [ ] **Bank account** setup for doctor payouts
- [ ] **Customer support** channel (email/chat)

## Pre-Launch Checklist
- [ ] All features tested on **physical devices**
- [ ] **Crash-free sessions** > 99% (via Crashlytics)
- [ ] **Battery optimization** tested
- [ ] **Offline mode** functionality verified
- [ ] **Push notifications** tested in production
- [ ] **App review** by Play Store team

## Testing Required
- [ ] **Payment flow** - Test M-Pesa/Tigo Pesa/Airtel Money end-to-end
- [ ] **Chat functionality** - File upload, images, voice messages
- [ ] **Doctor appointment booking** - Complete booking flow
- [ ] **Pharmacy orders** - Cart, checkout, order history
- [ ] **Push notifications** - All notification types
- [ ] **Multi-user role** - Patient, Doctor, Admin flows
- [ ] **Dark mode** - Theme switching works correctly
- [ ] **Swahili localization** - All strings translated

## Documentation
- [ ] **Update README.md** with release version and date
- [ ] **Update MASTER_README.md** with latest build info
- [ ] **Create CHANGELOG.md** - List of all changes from v1
- [ ] **Screenshot assets** - Add to project or marketing folder

## Post-Launch
- [ ] Monitor **Crashlytics** dashboard
- [ ] Review **Analytics** for user insights
- [ ] Gather **user reviews** and respond
- [ ] Plan **feature updates**
- [ ] **Weekly performance reports** - Monitor app stability
- [ ] **User feedback** collection and bug fixing
- [ ] **Backend monitoring** - Server uptime and API health

---

## Known Issues / Technical Debt

### Critical Security Issues
1. **Cleartext Traffic Enabled** - `android:usesCleartextTraffic="true"` in AndroidManifest.xml
   - Must be disabled before release or use Network Security Config to allowlist specific domains
2. **No SSL Pinning** - API calls to backend are vulnerable to MITM attacks
3. **Firebase Rules** - Ensure database/storage rules are production-ready (not open)

### Dependencies to Update
- Firebase BOM 32.7.0 is outdated (current: 33.x)
- Room 2.6.1 is good but check for updates

### Backend Preparation
- [ ] **Laravel backend** must be deployed and accessible
- [ ] **Zeno integration** tested with real transactions
- [ ] **ngrok/domain** URL updated in Constants.java
- [ ] **SSL certificate** installed on production server
- [ ] **Database migrations** run on production

### Play Store Specific
- [ ] **Content Rating** - Complete medical app questionnaire
- [ ] **Target Audience** - Configure in Play Console
- [ ] **News & Articles** - Add disclosure if applicable
- [ ] **Medical disclaimer** in app store listing

---

**Version:** 1.0.0  
**Last Updated:** Feb 2026
