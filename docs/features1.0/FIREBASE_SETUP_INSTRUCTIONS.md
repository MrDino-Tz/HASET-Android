# Firebase Setup Instructions - URGENT

## The Problem
Your app is crashing because it's using a **placeholder** `google-services.json` file with dummy Firebase credentials. You need to replace it with your actual Firebase configuration.

## Quick Fix (5 minutes)

### Step 1: Create Firebase Project
1. Go to https://console.firebase.google.com/
2. Click "Add project" or use existing project
3. Name it (e.g., "HASETApp")
4. Click "Continue" and "Create project"

### Step 2: Add Android App
1. In Firebase Console, click the Android icon (⚙️ Settings > Project settings)
2. Click "Add app" and select Android
3. **Package name:** `com.haset.hasetapp` (MUST match exactly!)
4. App nickname: HASETApp (optional)
5. Click "Register app"

### Step 3: Download Configuration File
1. Download the `google-services.json` file
2. **Replace** the existing file at: `app/google-services.json`
3. Make sure it's in the `app/` folder, NOT `app/src/`

### Step 4: Enable Firebase Services
1. In Firebase Console, go to **Authentication**
   - Click "Get started"
   - Select "Email/Password"
   - Enable it and click "Save"

2. Go to **Realtime Database**
   - Click "Create Database"
   - Choose location (closest to you)
   - Start in **"Test mode"** (for development)
   - Click "Enable"

### Step 5: Rebuild and Run
```bash
./gradlew clean
./gradlew :app:assembleDebug
```

Then run the app on your device/emulator.

## Verify Your Setup

Your `google-services.json` should have:
- Real project_id (not "hasetapp-app")
- Real project_number (not "123456789000")
- Real api_key (not "AIzaSyDummyKeyForPlaceholder123456789")
- Package name: "com.haset.hasetapp"

## Current Status
✅ Google Services plugin is now properly applied
✅ Build configuration is correct
❌ Need real Firebase credentials

## What I Fixed
Changed in `app/build.gradle`:
```gradle
// Before (WRONG):
id 'com.google.gms.google-services' version '4.4.0' apply false

// After (CORRECT):
id 'com.google.gms.google-services' version '4.4.0'
```

This removed the `apply false` flag that was preventing Firebase initialization.

## Test Accounts (After Setup)
Once Firebase is configured, create test accounts:

**Patient:**
- Email: patient@test.com
- Password: test123456

**Doctor:**
- Email: doctor@test.com
- Password: test123456

## Need Help?
If you get stuck, check the detailed guide in `SETUP_GUIDE.md`
