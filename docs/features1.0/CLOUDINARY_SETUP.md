# Cloudinary Setup Guide

## Step 1: Create a Cloudinary Account

1. Go to https://cloudinary.com
2. Click "Sign Up" (it's free)
3. Complete the registration

## Step 2: Get Your Credentials

1. After signing up, you'll be taken to the Dashboard
2. You'll see your credentials displayed:
   - **Cloud Name** (e.g., `dxyz123abc`)
   - **API Key** (e.g., `123456789012345`)
   - **API Secret** (e.g., `abcdefghijklmnopqrstuvwxyz123456`)

## Step 3: Add Credentials to Your App

1. Open `app/src/main/res/values/strings.xml`
2. Find these lines:
   ```xml
   <string name="cloudinary_cloud_name">YOUR_CLOUD_NAME</string>
   <string name="cloudinary_api_key">YOUR_API_KEY</string>
   <string name="cloudinary_api_secret">YOUR_API_SECRET</string>
   ```
3. Replace the placeholder values with your actual credentials:
   ```xml
   <string name="cloudinary_cloud_name">dxyz123abc</string>
   <string name="cloudinary_api_key">123456789012345</string>
   <string name="cloudinary_api_secret">abcdefghijklmnopqrstuvwxyz123456</string>
   ```

## Step 4: Test the Integration

1. Build and run your app
2. Try uploading a news post with an image
3. Check the logs for "Cloudinary initialized successfully"
4. The image should upload to Cloudinary and you'll get a URL back

## Free Tier Limits

- **25 GB** storage
- **25 GB** bandwidth per month
- **20,000** transformations per month
- Perfect for development and small to medium apps

## Security Note

⚠️ **Important**: The API Secret is sensitive. For production apps, consider:
- Using environment variables
- Storing in a secure backend server
- Using Cloudinary's unsigned uploads with upload presets

## Troubleshooting

- **"Cloudinary not initialized"**: Check that credentials are correctly added to strings.xml
- **"Authentication failed"**: Verify your API Key and API Secret are correct
- **"Upload failed"**: Check your internet connection and Cloudinary account status

## Documentation

- Cloudinary Android SDK: https://cloudinary.com/documentation/android_integration
- Cloudinary Dashboard: https://cloudinary.com/console

