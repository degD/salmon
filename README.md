# Salmon (Working Title)

A fast, offline-first, open-source guitar tuner for Android.

## Motivation

Most tuner apps today have evolved into subscription platforms, requiring internet connectivity, accounts, advertisements, or cloud services for functionality that should work entirely on-device.

Salmon aims to provide a simple alternative:

* Works completely offline
* No account required
* No advertisements
* No telemetry
* Open source
* Fast startup
* Focused on tuning rather than gamification

## Current Status

🚧 Work in Progress

The project is currently in the architecture and prototyping phase.

## Goals

### MVP

* Real-time microphone input
* Pitch detection
* Frequency display
* Note detection
* Cents offset visualization
* Guitar tuning mode (E A D G B e)
* Dark mode support
* Offline operation

### Future Goals

* Alternate tunings
* Bass guitar support
* Ukulele support
* Calibration adjustment (A4 = 432-445 Hz)
* Noise robustness improvements
* F-Droid distribution

## Technology Stack (Kotlin)

* Compose UI
* Pitch Detection
* Audio Capture
* Settings

### Pitch Detection

Initial candidates:

* YIN
* McLeod Pitch Method (MPM)

Research candidates:

* Hybrid YIN + Harmonic Analysis
* Probabilistic pitch tracking

Goals:

* Stable note detection
* Minimal jitter
* Robustness against guitar harmonics

### Visualization

Compose Canvas

Components:

* Tuning needle
* Cents deviation indicator
* Confidence feedback
* String selection interface

## Architecture

```text
Microphone
    │
    ▼
Audio Capture
    │
    ▼
Preprocessing
    │
    ▼
Pitch Detection
    │
    ▼
Confidence Filter
    │
    ▼
Temporal Smoothing
    │
    ▼
Note Mapping
    │
    ▼
Compose UI
```

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

License has not been finalized.

Candidates:

* MIT
* Apache-2.0
* GPLv3

## Inspiration

* GuitarTuna
* gStrings
* DaTuner

The goal is not to replicate every feature of these applications, but to provide a lightweight, offline, and open alternative focused on tuning.
