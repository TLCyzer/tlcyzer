# TLCyzer

TLCyzer is a free and open-source smartphone app for the quantitative evaluation of thin-layer chromatographic analyses in medicine quality screening, described and validated in a [scientific study](https://doi.org/10.1038/s41598-022-17527-y).

The app is available in the [Play Store](https://play.google.com/store/apps/details?id=de.uni.tuebingen.tlceval)

## Building

For building the image, processing [rust](https://www.rust-lang.org) is required. Please follow the [installation instructions](https://www.rust-lang.org/tools/install).
Furthermore, the code needs to be cross compiled for all common Android architectures we leverage [cross](https://github.com/cross-rs/cross).
Please also follow the [installation instructions](https://github.com/cross-rs/cross#dependencies) for cross.

The regular Gradle build then automatically creates the bindings and builds the binaries.

## Test images

For evaluating the application we provide images in the [supplementary materials](https://www.nature.com/articles/s41598-022-17527-y#Sec24), which are taken under the proposed capture setup.
The reference spots in these images are always the left, middle, and right with 60%, 80%, and 100% respectively.
The other remaining spots are the samples to be measured.

### Instructions 

The same [supplementary materials](https://www.nature.com/articles/s41598-022-17527-y#Sec24) also contain a PDF with instructions for the application on page 6 and a detailed capture box technical drawing.
