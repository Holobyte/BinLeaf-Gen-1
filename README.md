# BinLeaf - AI Real Estate Assistant Helper

BinLeaf is an advanced real estate assistant app written in Kotlin and Jetpack Compose. It allows FSBO sellers and agents to capture home photos, generate rich descriptions, analyze pricing, and capture neighborhood showing leads.

## Address Detection Setup

BinLeaf uses the Android Native Geocoder to automatically reverse-geocode a physical location into coordinates and property addresses when you snap or upload a photo. No external coordinates database, paid maps billing, or Google Maps billing account is required.

To get the AI generation fully functional, configure your Gemini API Key using the platform Secrets panel:
- In **Google AI Studio**, paste your key into the **Secrets panel** under the key:
  `GEMINI_API_KEY`
- If developing locally in your IDE, save the key in a `.env` file at the root of your workspace:
  ```properties
  GEMINI_API_KEY=MY_GEMINI_API_KEY
  ```

*Secure Compilation Disclaimer:* Our workflow is integrated with the standard Secrets Gradle Plugin. No keys are hardcoded in Java/Kotlin sources; they are loaded dynamically via generated `BuildConfig` fields, securing raw credentials securely.

---

## 🔒 Security Warning
I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. **Do not share this APK file publicly or with unauthorized individuals** to prevent potential misuse.
