# Privacy Policy for Salmon

**Last updated: 2026-09-30**

## Summary

Salmon is a guitar tuner for Android. It listens to your instrument through the device microphone, calculates the pitch on-device, and displays tuning guidance. **No audio ever leaves your device.** Salmon has no internet permission, no analytics, no ads, no accounts, and no telemetry of any kind.

## 1. Information we do NOT collect

Salmon does not collect, store, or transmit any of the following:

- Personal information (name, email, phone number, address, etc.)
- Account information or credentials
- Audio recordings or samples captured by the microphone
- Device identifiers (IMEI, MAC address, Android ID, advertising ID, etc.)
- Location data
- Usage statistics, analytics, or crash reports
- Contact lists, calendars, photos, files, or any other user data
- Any data at all — locally or remotely

There are no third-party SDKs for advertising, analytics, attribution, or crash reporting bundled with the application.

---

## 2. Microphone access

Salmon requests a single Android permission:

- **`RECORD_AUDIO`** — used to capture audio from the device microphone so that the pitch of the incoming signal can be analyzed.

**How the microphone data is used:**

- Audio is captured in short, continuous buffers by the Android `AudioRecord` API.
- Each buffer is passed to an on-device DSP pipeline (TarsosDSP, using the FFT-YIN pitch estimation algorithm).
- The pipeline computes a single frequency value (in Hertz) and a confidence probability for each buffer.
- The frequency is converted to a musical note and a cents offset, which is displayed on screen.

**What happens to the audio data afterwards:**

- Each audio buffer is discarded immediately after the frequency is calculated. Nothing is written to disk.
- Audio is never saved, cached, queued, or transmitted anywhere.
- The microphone stream exists only while the app is in the foreground and stops when the app is closed.

You can revoke microphone permission at any time via **Android Settings → Apps → Salmon → Permissions → Microphone**. If permission is not granted, the app cannot perform its core function and will exit.

---

## 3. Internet access

Salmon **does not request the `INTERNET` permission** and therefore cannot access the network. There is no code path in the application that can send or receive data over Wi-Fi, mobile data, Bluetooth, or any other transport.

This is verifiable: the `AndroidManifest.xml` in the public source declares no network-related permissions, and the codebase contains no HTTP, socket, or IPC code beyond standard Android UI and audio APIs.

---

## 4. Data stored on your device

Salmon stores no persistent user data. The following are held **in memory only** and are lost when the app is closed or the process is destroyed:

- The currently selected tuning mode (Auto / Manual)
- The currently selected target note
- Which strings have been marked as correctly tuned during the current session
- UI animation state

No settings, preferences, logs, caches, or databases are written to internal or external storage.

---

## 5. Third-party components

Salmon uses the following open-source library:

- **TarsosDSP** — a Java audio processing library for pitch detection and signal analysis. TarsosDSP runs entirely on-device and does not perform any network communication.

No third-party services (analytics, ads, CDNs, crash reporting, cloud APIs) are integrated.

---

## 6. Children's privacy

Salmon does not collect any information from anyone, including children under the age of 13. The application is safe for use by all ages because it does not gather, store, or transmit any personal data.

---

## 7. Legal basis (GDPR) and your rights

Because Salmon does not collect or process any personal data, there is no personal data for us to disclose, correct, delete, or port. If you are in the European Economic Area and believe we hold data about you, please contact us at the address above — but in practice we hold none.

---

## 8. Changes to this policy

If this policy changes, the updated version will be published at the same URL and the "Last updated" date at the top will be revised. Because the application is open source, any change in data handling behaviour would also be visible as a commit in the public repository.

---

## 9. Verification

You do not have to take our word for any of the above. The complete source code is available for inspection, and you can verify the claims in this policy by:

1. Reading `AndroidManifest.xml` to confirm no `INTERNET` permission is requested.
2. Reading `TunerFunctionality.kt` to confirm that audio buffers are consumed in-memory and never written or transmitted.
3. Searching the codebase for any networking, analytics, or storage APIs (there are none).
4. Building the app yourself from source and observing its behaviour with a network monitor.

---

## 10. Contact

For questions, corrections, or concerns about this policy, please open an issue on the project's GitHub repository or contact the maintainer at:

**Email:** degederyam@gmail.com
**Repository:** [https://github.com/degd/salmon](https://github.com/degd/salmon)

