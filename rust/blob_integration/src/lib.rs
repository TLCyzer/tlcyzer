#![feature(clamp)]

use image::{GenericImageView, GrayImage};
use nalgebra::Point2;
use std::collections::HashMap;
use tlc_common::{Circle, Quad};

pub fn integrate_spots(
    image: &GrayImage,
    blobs: &HashMap<u32, Circle>,
    cut_off_percentage: f32,
) -> HashMap<u32, u64> {
    let (min_val, max_val) = find_scaling(image, blobs);

    blobs
        .iter()
        // scale the image first
        .map(|(key, circle)| {
            let quad = circle.to_quad();
            let (qw, qh) = quad.dimensions();
            let (iw, ih) = image.dimensions();
            let mut scaled_img: Vec<u32> = image
                .view(
                    (quad.top_left.x as u32).clamp(0, qw as u32 - iw),
                    (quad.top_right.y as u32).clamp(0, qh as u32 - ih),
                    qw as u32,
                    qh as u32,
                )
                .pixels()
                .map(|(_, _, p)| p[0])
                // Scale the image
                .map(|x| (x as f32 - min_val as f32) / (max_val as f32 - min_val as f32) * 255f32)
                // Make sure that it is a int so it is sortable
                .map(|x| x as u32)
                .collect();
            // Sort in descending order
            scaled_img.sort_by(|a, b| b.cmp(a));
            (*key, scaled_img)
        })
        // Then integrated the top x percent values
        .map(|(key, sorted_values)| {
            let num_values = sorted_values.len();
            let cutoff_idx = (num_values as f32 * cut_off_percentage) as usize;

            let integrated = sorted_values[..cutoff_idx]
                .iter()
                .fold(0f64, |sum, &x| sum + x as f64);

            (key, integrated as u64)
        })
        .collect()
}

pub fn find_bounding_box_from_blobs(width: u32, height: u32, blobs: &HashMap<u32, Circle>) -> Quad {
    // Start with the respective maximum and minimum values
    let initial_quad = Quad {
        top_left: Point2::new((width - 1) as f32, (height - 1) as f32),
        top_right: Point2::new(0f32, (height - 1) as f32),
        bottom_right: Point2::new(0f32, 0f32),
        bottom_left: Point2::new((width - 1) as f32, 0f32),
    };
    // Find the strip containing all blobs
    blobs
        .iter()
        .map(|(_, circle)| circle.to_quad())
        .fold(initial_quad, |cur_best, candidate| {
            let top_left = Point2::new(
                // X should get smaller
                if cur_best.top_left.x > candidate.top_left.x {
                    candidate.top_left.x
                } else {
                    cur_best.top_left.x
                },
                // Y should also get smaller
                if cur_best.top_left.y > candidate.top_left.y {
                    candidate.top_left.y
                } else {
                    cur_best.top_left.y
                },
            );
            let top_right = Point2::new(
                // X should get larger
                if cur_best.top_right.x < candidate.top_right.x {
                    candidate.top_right.x
                } else {
                    cur_best.top_right.x
                },
                // Y should get smaller
                if cur_best.top_right.y > candidate.top_right.y {
                    candidate.top_right.y
                } else {
                    cur_best.top_right.y
                },
            );
            let bottom_right = Point2::new(
                // X should get larger
                if cur_best.bottom_right.x < candidate.bottom_right.x {
                    candidate.bottom_right.x
                } else {
                    cur_best.bottom_right.x
                },
                // Y should also get larger
                if cur_best.bottom_right.y < candidate.bottom_right.y {
                    candidate.bottom_right.y
                } else {
                    cur_best.bottom_right.y
                },
            );
            let bottom_left = Point2::new(
                // X should get smaller
                if cur_best.bottom_left.x > candidate.bottom_left.x {
                    candidate.bottom_left.x
                } else {
                    cur_best.bottom_left.x
                },
                // Y should get larger
                if cur_best.bottom_left.y < candidate.bottom_left.y {
                    candidate.bottom_left.y
                } else {
                    cur_best.bottom_left.y
                },
            );

            Quad {
                top_left,
                top_right,
                bottom_right,
                bottom_left,
            }
        })
}

pub fn find_scaling(image: &GrayImage, blobs: &HashMap<u32, Circle>) -> (u8, u8) {
    let (width, height) = image.dimensions();

    let bounding_box = find_bounding_box_from_blobs(width, height, blobs);
    let (bw, bh) = bounding_box.dimensions();

    let img_strip = image.view(
        bounding_box.top_left.x as u32,
        bounding_box.top_right.y as u32,
        bw as u32,
        bh as u32,
    );

    img_strip
        .pixels()
        .map(|(_, _, p)| p[0])
        .fold((255u8, 0u8), |min_max, candidate| {
            let (min, max) = min_max;
            // If the min is larger than the candidate replace it
            let n_min = if min > candidate { candidate } else { min };
            // If the max is smaller than the candidate replace it
            let n_max = if max < candidate { candidate } else { max };
            (n_min, n_max)
        })
}
