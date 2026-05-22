# BinLeaf - AI Real Estate Assistant Helper

BinLeaf is an advanced real estate assistant app written in Kotlin and Jetpack Compose. It allows FSBO sellers and agents to capture home photos, generate rich descriptions, analyze pricing, and capture neighborhood showing leads.

## Google Maps Geocoding Integration Setup

BinLeaf uses the **Google Maps Geocoding API** to automatically reverse-geocode a physical location into coordinates and property addresses when using the live GPS scan action.

To get this feature functional with your own API key, follow these instructions:

### 1. Enable the Google Maps Geocoding API
1. Navigate to the [Google Cloud Console](https://console.cloud.google.com/).
2. Select an existing project or create a new Google Cloud project.
3. Open the navigation menu and choose **APIs & Services** > **Library**.
4. In the library search bar, type `Geocoding API`.
5. Select the **Geocoding API** from the results and click **Enable**.

### 2. Generate an API Key
1. In the Google Cloud Console, go to **APIs & Services** > **Credentials**.
2. Click **+ CREATE CREDENTIALS** at the top of the interface and select **API key**.
3. Copy the newly created key to your clipboard.
4. *(Optional but Highly Recommended)* Apply credentials restrictions under the API key settings to restrict calls exclusively to your Android application signing package to prevent quota abuse.

### 3. Add `GOOGLE_MAPS_API_KEY` to the Application

Configure your API key securely using the platform Secrets panel:
- In **Google AI Studio**, paste your key into the **Secrets panel** under the key:
  `GOOGLE_MAPS_API_KEY`
- If developing locally in your IDE, save the key in a `.env` file at the root of your workspace:
  ```properties
  GOOGLE_MAPS_API_KEY=AIzaSyYourKeyHere...
  ```

*Secure Compilation Disclaimer:* Our workflow is integrated with the standard Secrets Gradle Plugin. No keys are hardcoded in Java/Kotlin sources; they are loaded dynamically via generated `BuildConfig` fields, securing raw credentials securely.

---

## 🔒 Security Warning
I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. **Do not share this APK file publicly or with unauthorized individuals** to prevent potential misuse.
