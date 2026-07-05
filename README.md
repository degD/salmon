# Salmon (Working Title)

A fast, offline-first, open-source guitar tuner for Android.
Most tuner apps today have evolved into subscription platforms, 
requiring internet connectivity, accounts, advertisements, or 
cloud services for functionality that should work entirely on-device.

This app is inspired a lot from open-source tuners like 
daTuner and gStrings. But the main inspiration has come 
from the GuitarTuna closed source tuner app.

## Development

1. Grab the source code and clone.
2. Install latest TarsosDSP .jar Android library file.
3. Put TarsosDSP under app/libs.
4. Start development.

## Goals

* Super useful UI
* Real-time microphone input
* Pitch detection
* Frequency display
* Note detection
* Cents offset visualization
* Guitar tuning mode (E2 A D G B E4)
* Dark mode support
* Offline operation

## Future Goals

* Alternate tunings
* Bass guitar support
* Ukulele support
* Calibration adjustment (A4 = 432-445 Hz)
* Noise robustness improvements
* F-Droid distribution

## Technology Stack (Kotlin)

* Compose UI
* TarsosDSP for pitch detection (for now)

### Pitch Detection

Initial candidate algorithms:

* YIN
* McLeod Pitch Method (MPM)

Research candidates:

* Hybrid YIN + Harmonic Analysis
* Probabilistic pitch tracking

Goals:

* Stable note detection
* Minimal jitter
* Robustness against guitar harmonics

## Privacy

Salmon is designed with privacy as a core requirement.

The application:

* Does not require internet access
* Does not collect analytics
* Does not track users
* Does not require account creation
* Does not transmit audio recordings

## Distribution

Planned release channels:

* GitHub Releases
* F-Droid

Google Play publication is undecided.

## Contributing

Contributions, bug reports, and feature requests are welcome.

Areas where help is particularly useful:

* DSP algorithms
* Android audio testing
* UI/UX design
* Device compatibility testing
* Documentation

## License

Undecided Yet...
