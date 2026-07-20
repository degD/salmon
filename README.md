# Salmon

A fast, offline-first, open-source guitar tuner for Android.
Most tuner apps today have evolved into subscription platforms, 
requiring internet connectivity, accounts, advertisements, or 
cloud services for functionality that should work entirely on-device.

## Disclaimer

While this app is "ready in principle", it may not be "fun to use".
The idea was to create a "useful alternative", later on I focused
mostly on building a "proof of concept". There could be bugs and stuff
that doesn't quite work. 

## Development

1. Grab the source code and clone.
2. Install latest TarsosDSP .jar Android library file.
3. Put TarsosDSP under app/libs.
4. Start development.

## Features

* Useful UI
* Real-time microphone input
* Pitch detection
* Note detection
* Cents offset visualization
* Guitar tuning mode (E2 A D G B E4)
* Offline operation

## Future Goals

* Alternate tunings
* Other instruments support
* Calibration adjustment (A4 = 432-445 Hz)
* Noise robustness improvements
* F-Droid distribution

## Technology Stack (Kotlin)

* Compose UI
* TarsosDSP for pitch detection (FFT-YIN) 

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

## Resources

* [Correct audio](https://freesound.org/people/StavSounds/sounds/546084/)
* [Guitar headstock source](https://freesvg.org/piemaster-gretsch-jet-firebird)
* [Color palette](https://colorhunt.co/palette/ff9e20215e611d2128f4f2f2)
* Icon designed by me

## License

MIT 
