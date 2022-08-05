use image::buffer::ConvertBuffer;
use image::{GrayImage, ImageBuffer, Luma, Rgb, RgbImage};
use imageproc;
use imageproc::region_labelling::{connected_components, Connectivity};
use itertools::Itertools;
use nalgebra::Point2;
use std::collections::HashMap;
use tlc_common::{Circle, Quad, StatsImage};

pub fn detect_blobs(image: &GrayImage) -> HashMap<u32, Circle> {
    let (width, height) = image.dimensions();

    let regions = get_labeled_regions(&image);
    let grouped = regions
        .enumerate_pixels()
        .filter(|(_, _, p)| p[0] != 0)
        .map(|(x, y, p)| (p[0], (x, y)))
        .into_group_map();

    let added_intensity: HashMap<u32, Vec<(u32, u32, Luma<u8>)>> = grouped
        .iter()
        .map(|(key, coords)| {
            let coords_val: Vec<(u32, u32, Luma<u8>)> = coords
                .iter()
                .map(|(x, y)| {
                    let p = image.get_pixel(*x, *y);
                    (*x, *y, *p)
                })
                .collect();

            (*key, coords_val)
        })
        .collect();

    let center: HashMap<u32, (f64, f64)> = added_intensity
        .iter()
        .map(|(key, cv)| {
            let center = cv
                .iter()
                .map(|(x, y, p)| (x * p[0] as u32, y * p[0] as u32))
                .fold((0u64, 0u64), |sum, x| {
                    (sum.0 + x.0 as u64, sum.1 + x.1 as u64)
                });
            let sum = cv
                .iter()
                .map(|(_x, _y, p)| p[0])
                .fold(0u64, |sum, x| sum + x as u64);

            (
                *key,
                (center.0 as f64 / sum as f64, center.1 as f64 / sum as f64),
            )
        })
        .collect();

    let aspect_ratio_tolerance = 0.75;
    let size_min = width.max(height) as f32 * 0.02;
    let size_max = width.max(height) as f32 * 0.25;
    let bounding_box: HashMap<u32, Quad> = added_intensity
        .iter()
        .map(|(key, cv)| {
            let points: Vec<Point2<f32>> = cv
                .iter()
                .map(|(x, y, _p)| Point2::new(*x as f32, *y as f32))
                .collect();

            let left =
                points
                    .iter()
                    .map(|p| p.x)
                    .fold(width as f32, |min, x| if x <= min { x } else { min });

            let right = points
                .iter()
                .map(|p| p.x)
                .fold(0f32, |max, x| if x >= max { x } else { max });

            let top =
                points
                    .iter()
                    .map(|p| p.y)
                    .fold(height as f32, |min, y| if y <= min { y } else { min });

            let bottom = points
                .iter()
                .map(|p| p.y)
                .fold(0f32, |max, y| if y >= max { y } else { max });
            let top_left = Point2::new(left, top);
            let top_right = Point2::new(right, top);
            let bottom_right = Point2::new(right, bottom);
            let bottom_left = Point2::new(left, bottom);

            (
                *key,
                Quad {
                    top_left,
                    top_right,
                    bottom_right,
                    bottom_left,
                },
            )
        })
        .filter(|(_key, bbox)| {
            let bbac = bbox.aspect_ratio();

            bbac >= 1.0 - aspect_ratio_tolerance && bbac <= 1.0 + aspect_ratio_tolerance
        })
        .filter(|(_key, bbox)| {
            let (bbw, bbh) = bbox.dimensions();
            (bbw >= size_min && bbw <= size_max) && (bbh >= size_min && bbh <= size_max)
        })
        .collect();

    let center_radius: HashMap<u32, Circle> = bounding_box
        .iter()
        .map(|(key, bbox)| (key, (bbox, center[key])))
        .map(|(key, (bbox, center))| {
            let cx = Point2::new(center.0 as f32, center.1 as f32);
            let tl_dist = (bbox.top_left - cx).norm().abs() as f64;
            let tr_dist = (bbox.top_right - cx).norm().abs() as f64;
            let br_dist = (bbox.bottom_right - cx).norm().abs() as f64;
            let bl_dist = (bbox.bottom_left - cx).norm().abs() as f64;

            (
                *key,
                Circle::new(
                    center.0 as f32,
                    center.1 as f32,
                    tl_dist.min(tr_dist).min(br_dist).min(bl_dist) as f32,
                ),
            )
        })
        .collect();

    let mark_color = Rgb([255, 255, 0]);
    let mut marked_spots: RgbImage = image.clone().convert();
    for (_, circle) in &center_radius {
        imageproc::drawing::draw_hollow_circle_mut(
            &mut marked_spots,
            (circle.center.x as i32, circle.center.y as i32),
            circle.radius as i32,
            mark_color,
        )
    }
    //marked_spots.save("marked_spots.jpg").unwrap();

    return center_radius;
}

fn get_labeled_regions(image: &GrayImage) -> ImageBuffer<Luma<u32>, Vec<u32>> {
    let thresh = image.mean()[0];
    let (width, height) = image.dimensions();
    let max_dim = width.max(height);

    let thresholded = imageproc::contrast::threshold(&image, thresh);
    let opened = imageproc::morphology::open(
        &thresholded,
        imageproc::distance_transform::Norm::LInf,
        (max_dim as f32 * 0.0075) as u8,
    );
    //opened.save("thresholded.jpg").unwrap();

    let background_color = image.min();
    let regions = connected_components(&opened, Connectivity::Four, background_color);

    return regions;
}
