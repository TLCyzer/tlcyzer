# TLCyzer

TLCyzer is a free and open source smartphone app for the quantitative evaluation of thin-layer chromatographic analyses in medicine quality screening, described and validated in a [scientific study](https://doi.org/10.1038/s41598-022-17527-y).

The app is available in the [Play Store](https://play.google.com/store/apps/details?id=de.uni.tuebingen.tlceval)

## Building

For building the image, processing [rust](https://www.rust-lang.org) is required. Please follow the [installation instructions](https://www.rust-lang.org/tools/install).
Furthermore, the code needs to be cross compiled for all common Android architectures we leverage [cross](https://github.com/cross-rs/cross).
Pleas also follow the [installation instructions](https://github.com/cross-rs/cross#dependencies) for cross.

The regular gradle build then automatically creates the bindings and builds the binaries.