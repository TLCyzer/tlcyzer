//! Spots whose bounding square crosses the image border must not panic.
//! Before the fix, integrate_spots computed `qw - iw` instead of `iw - qw`
//! (an underflow for every spot) and find_scaling never clipped its strip
//! to the image.

use image::{GrayImage, Luma};
use std::collections::HashMap;
use tlc_blob_integration::{find_scaling, integrate_spots};
use tlc_common::Circle;

fn gradient_image() -> GrayImage {
    GrayImage::from_fn(200, 100, |x, y| Luma([((x + y) % 256) as u8]))
}

fn one_spot(x: f32, y: f32, r: f32) -> HashMap<u32, Circle> {
    let mut blobs = HashMap::new();
    blobs.insert(1u32, Circle::new(x, y, r));
    blobs
}

#[test]
fn interior_spot_value_is_unchanged() {
    // Value recorded from the unpatched code in a release build, where the
    // underflow wraps instead of panicking and interior spots were unaffected.
    let result = integrate_spots(&gradient_image(), &one_spot(100.0, 50.0, 10.0), 0.15);
    assert_eq!(result[&1], 12722);
}

#[test]
fn spot_crossing_each_edge_does_not_panic() {
    let image = gradient_image();
    for (x, y) in [
        (195.0, 50.0),
        (5.0, 50.0),
        (100.0, 95.0),
        (100.0, 5.0),
        (198.0, 98.0),
    ] {
        let result = integrate_spots(&image, &one_spot(x, y, 10.0), 0.15);
        assert!(result[&1] > 0, "spot at ({}, {}) integrated to 0", x, y);
    }
}

#[test]
fn spot_larger_than_image_does_not_panic() {
    integrate_spots(&gradient_image(), &one_spot(100.0, 50.0, 80.0), 0.15);
}

#[test]
fn find_scaling_clips_to_image() {
    let (min, max) = find_scaling(&gradient_image(), &one_spot(195.0, 50.0, 10.0));
    assert!(min <= max);
}
