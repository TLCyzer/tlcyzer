use image;
use image::{DynamicImage, GenericImageView};
use imageproc;
use itertools::Itertools;
use log::info;
use na::{distance_squared, Point2, Vector2, Vector3};
use std::collections::HashMap;
use tlc_common::Quad;

fn degrees_to_radians(degrees: u32) -> f32 {
    degrees as f32 * std::f32::consts::PI / 180.0
}

fn radians_to_degrees(radians: f32) -> u32 {
    (radians * 180f32 / std::f32::consts::PI) as u32
}

trait Intersection {
    fn intersect(&self, other: &Self, width: u32, height: u32) -> Option<Point2<f32>>;
    fn create_image_intersection(&self, width: u32, height: u32) -> Option<Vec<Point2<f32>>>;
}

impl Intersection for imageproc::hough::PolarLine {
    fn intersect(
        &self,
        other: &imageproc::hough::PolarLine,
        width: u32,
        height: u32,
    ) -> Option<Point2<f32>> {
        let maybe_own_is = self.create_image_intersection(width, height);
        let maybe_other_is = other.create_image_intersection(width, height);

        return match (maybe_own_is, maybe_other_is) {
            (Some(own_is), Some(other_is)) => {
                let x = other_is[0] - own_is[0];
                let sd: Vector2<f32> = own_is[1] - own_is[0];
                let od: Vector2<f32> = other_is[1] - other_is[0];

                let cross = sd.perp(&od);
                if cross.abs() < 1e-8 {
                    None
                } else {
                    let t1 = x.perp(&od) / cross;
                    let intersection = own_is[0] + sd * t1;
                    Some(intersection)
                }
            }
            (_, _) => None,
        };
    }

    fn create_image_intersection(&self, width: u32, height: u32) -> Option<Vec<Point2<f32>>> {
        let r = self.r;
        let m = self.angle_in_degrees;
        let w = width as f32;
        let h = height as f32;

        // Vertical line
        if m == 0 {
            return if r >= 0.0 && r <= w {
                Some(vec![Point2::new(r, 0.0), Point2::new(r, h)])
            } else {
                None
            };
        }

        // Horizontal line
        if m == 90 {
            return if r >= 0.0 && r <= h {
                Some(vec![Point2::new(0.0, r), Point2::new(w, r)])
            } else {
                None
            };
        }

        let theta = degrees_to_radians(m);
        let sin = theta.sin();
        let cos = theta.cos();

        let right_y = cos.mul_add(-w, r) / sin;
        let left_y = r / sin;
        let bottom_x = sin.mul_add(-h, r) / cos;
        let top_x = r / cos;

        let mut start = None;

        if right_y >= 0.0 && right_y <= h {
            let right_intersect = Point2::new(w, right_y);
            if let Some(s) = start {
                return Some(vec![s, right_intersect]);
            }
            start = Some(right_intersect);
        }

        if left_y >= 0.0 && left_y <= h {
            let left_intersect = Point2::new(0.0, left_y);
            if let Some(s) = start {
                return Some(vec![s, left_intersect]);
            }
            start = Some(left_intersect);
        }

        if bottom_x >= 0.0 && bottom_x <= w {
            let bottom_intersect = Point2::new(bottom_x, h);
            if let Some(s) = start {
                return Some(vec![s, bottom_intersect]);
            }
            start = Some(bottom_intersect);
        }

        if top_x >= 0.0 && top_x <= w {
            let top_intersect = Point2::new(top_x, 0.0);
            if let Some(s) = start {
                return Some(vec![s, top_intersect]);
            }
        }

        None
    }
}

pub struct Detector {
    input: DynamicImage,
    detection_scale: DynamicImage,
    downscale_factor: u32,
    width: u32,
    height: u32,
}

impl Detector {
    pub fn new(image: &DynamicImage) -> Self {
        let input: DynamicImage = image.clone();

        let mut downscale_factor = 1u32;
        loop {
            let (w, h) = image.dimensions();
            let (nw, nh) = (
                w / 2u32.pow(downscale_factor.into()),
                h / 2u32.pow(downscale_factor.into()),
            );

            if nw <= 256 && nh <= 256 {
                let scaled = image.resize_exact(nw, nh, image::imageops::FilterType::Nearest);
                return Detector {
                    input: input,
                    detection_scale: scaled,
                    downscale_factor: downscale_factor,
                    width: nw,
                    height: nh,
                };
            }
            downscale_factor += 1;
        }
    }

    pub fn corners_or_default(&self) -> Quad {
        match self.detect_corners() {
            Some(rect) => rect,
            None => {
                let (width, height) = self.input.dimensions();
                let left = width as f32 / 10f32;
                let right = width as f32 - left;
                let top = height as f32 / 10f32;
                let bottom = height as f32 - top;

                Quad {
                    top_left: Point2::new(left, top),
                    top_right: Point2::new(right, top),
                    bottom_right: Point2::new(right, bottom),
                    bottom_left: Point2::new(left, bottom),
                }
            }
        }
    }

    pub fn detect_corners(&self) -> Option<Quad> {
        let (width, height) = self.detection_scale.dimensions();
        let min_dim = if width < height { width } else { height };

        // Process the image to help hough line detection
        let grayscaled = self.detection_scale.to_luma8();

        let contrast = imageproc::contrast::adaptive_threshold(&grayscaled, min_dim / 3);
        let canny_edge = imageproc::edges::canny(&contrast, 50.0, 100.0);

        // Detect all lines in the processed image
        let lines_detected = imageproc::hough::detect_lines(
            &canny_edge,
            imageproc::hough::LineDetectionOptions {
                vote_threshold: 40,
                suppression_radius: 8,
            },
        );

        let intersection_points_dom_line = self.find_corners(&lines_detected);
        let corners = self.intersections_to_corners(&intersection_points_dom_line);
        // At this points all matched intersections a matched to their respective corner
        // It is still possible that one corner is not visible in the image
        // In that case that corner is set to the line intersecting with the image

        let red = image::Rgb([255, 0, 0]);
        let blue = image::Rgb([0, 0, 255]);
        let green = image::Rgb([0, 255, 0]);
        let magenta = image::Rgb([255, 0, 255]);

        let mut intersection_marked = self.detection_scale.to_rgb8();
        let colors = vec![red, blue, green, magenta];
        let intersection_color = corners
            .iter()
            .map(|(key, value)| (value, colors[*key as usize]));
        for (inter, color) in intersection_color {
            imageproc::drawing::draw_cross_mut(
                &mut intersection_marked,
                color,
                inter.x as i32,
                inter.y as i32,
            );
        }

        if corners.len() == 4 {
            // We found all 4 corners
            let upscale = 2u32.pow(self.downscale_factor.into()) as f32;
            let tl = corners[&0] * upscale;
            let tr = corners[&1] * upscale;
            let br = corners[&2] * upscale;
            let bl = corners[&3] * upscale;

            Some(Quad {
                top_left: tl,
                top_right: tr,
                bottom_right: br,
                bottom_left: bl,
            })
        } else {
            None
        }
    }

    fn find_corners(
        &self,
        lines_detected: &Vec<imageproc::hough::PolarLine>,
    ) -> Vec<(Point2<f32>, imageproc::hough::PolarLine)> {
        // Gather all horizontal and all vertical lines
        let angle_threshold = 2;
        let horizontal_lines: Vec<imageproc::hough::PolarLine> = lines_detected
            .iter()
            .map(|&x| x)
            .filter(|l| {
                l.angle_in_degrees >= 90 - angle_threshold
                    && l.angle_in_degrees <= 90 + angle_threshold
            })
            .collect();
        let vertical_lines: Vec<imageproc::hough::PolarLine> = lines_detected
            .iter()
            .map(|&x| x)
            .filter(|l| {
                l.angle_in_degrees >= 180 - angle_threshold
                    || l.angle_in_degrees <= 0 + angle_threshold
            })
            .collect();

        info!(
            "Total lines: {}. Horizontal: {}. Vertical: {}",
            lines_detected.len(),
            horizontal_lines.len(),
            vertical_lines.len()
        );

        // Gather all intersection points between the horizontal and vertical lines
        let mut intersection_points_dom_line: Vec<(Point2<f32>, imageproc::hough::PolarLine)> =
            Vec::new();
        for h in &horizontal_lines {
            for v in &vertical_lines {
                let maybe_intersection = h.intersect(&v, self.width, self.height);
                match maybe_intersection {
                    Some(intersection) => {
                        let left_t = 0f32;
                        let right_t = self.width as f32 - left_t;
                        let top_t = 0f32;
                        let bottom_t = self.height as f32 - top_t;
                        if (intersection.x >= left_t && intersection.x < right_t)
                            && (intersection.y >= top_t && intersection.y < bottom_t)
                        {
                            let line = if self.width > self.height { h } else { v };
                            intersection_points_dom_line.push((intersection, *line));
                        }
                    }
                    None => {}
                }
            }
        }

        return intersection_points_dom_line;
    }

    fn intersections_to_corners(
        &self,
        intersection_points_dom_line: &Vec<(Point2<f32>, imageproc::hough::PolarLine)>,
    ) -> HashMap<u8, Point2<f32>> {
        // Now find the respective image corner the intersection belongs to
        let tl_corner = Point2::new(0f32, 0f32);
        let tr_corner = Point2::new(self.width as f32, 0f32);
        let br_corner = Point2::new(self.width as f32, self.height as f32);
        let bl_corner = Point2::new(0f32, self.height as f32);

        let to_find = vec![
            (tl_corner, 0u8),
            (tr_corner, 1u8),
            (br_corner, 2u8),
            (bl_corner, 3u8), // Add proper enums
        ];

        let corners_map: HashMap<u8, Vec<(Point2<f32>, f32, imageproc::hough::PolarLine)>> =
            intersection_points_dom_line
                .iter() // For each intersection
                .map(|&(point, line)| {
                    to_find // Check all possible corners
                        .iter()
                        .map(|&(corner, cidx)| {
                            // Calculate the distance
                            (cidx, (point, distance_squared(&corner, &point), line))
                        })
                        .fold(
                            // And assign the point to the closest image corner
                            None,
                            |maybe_cmp: Option<(
                                u8,
                                (Point2<f32>, f32, imageproc::hough::PolarLine),
                            )>,
                             x| match maybe_cmp {
                                Some((_, group)) => {
                                    if group.1 <= (x.1).1 {
                                        maybe_cmp
                                    } else {
                                        Some(x)
                                    }
                                }
                                None => Some(x),
                            },
                        )
                        .unwrap() // This is safe. There should be one corner which matches
                })
                .into_group_map();

        // A corner can now own multiple intersections.
        let averaged_corners: HashMap<u8, (Point2<f32>, imageproc::hough::PolarLine)> = corners_map
            .iter()
            .map(|(&key, group)| {
                let div = Vector3::repeat(group.len() as f32);

                let avg_vector = group // Each intersection for the corner is averaged.
                    .iter()
                    .map(|p| p.0.to_homogeneous())
                    .fold(Vector3::new(0f32, 0f32, 0f32), |sum, x| sum + x)
                    .component_div(&div);

                let summed_lines = group
                    .iter()
                    .map(|p| ((p.2).r, degrees_to_radians((p.2).angle_in_degrees)))
                    .map(|(r, alpha)| (r, alpha.sin(), alpha.cos()))
                    .fold((0f32, 0f32, 0f32), |sum, x| {
                        (sum.0 + x.0, sum.1 + x.1, sum.2 + x.2)
                    });
                let avg_lines = (
                    summed_lines.0 / group.len() as f32,
                    summed_lines.1 / group.len() as f32,
                    summed_lines.2 / group.len() as f32,
                );
                let alpha = avg_lines.1.atan2(avg_lines.2);
                let avg_line = imageproc::hough::PolarLine {
                    r: summed_lines.0,
                    angle_in_degrees: radians_to_degrees(alpha),
                };

                (
                    key,
                    (Point2::from_homogeneous(avg_vector).unwrap(), avg_line), // TODO: we need to average all lines in the group too
                )
            })
            .collect();
        info!("Avg corner/lines: {:?}", averaged_corners);

        let line_pairs = if self.width > self.height {
            vec![(0, 1), (2, 3)]
        } else {
            vec![(0, 3), (1, 2)]
        };

        let corners: HashMap<u8, Point2<f32>> = line_pairs
            .iter()
            .map(|&(l1, l2)| {
                match (
                    averaged_corners.contains_key(&l1),
                    averaged_corners.contains_key(&l2),
                ) {
                    (true, true) => Some(vec![
                        (l1, averaged_corners[&l1].0),
                        (l2, averaged_corners[&l2].0),
                    ]),
                    (true, false) => {
                        match self.find_alternative_corner(
                            &averaged_corners[&l1].1,
                            to_find[l2 as usize].0,
                        ) {
                            Some(closest_corner) => {
                                Some(vec![(l1, averaged_corners[&l1].0), (l2, closest_corner)])
                            }
                            None => None,
                        }
                    }
                    (false, true) => {
                        match self.find_alternative_corner(
                            &averaged_corners[&l2].1,
                            to_find[l1 as usize].0,
                        ) {
                            Some(closest_corner) => {
                                Some(vec![(l1, closest_corner), (l2, averaged_corners[&l2].0)])
                            }
                            None => None,
                        }
                    }
                    (false, false) => None,
                }
            })
            .filter_map(|x| x)
            .flatten()
            .collect();

        corners
    }

    fn find_alternative_corner(
        &self,
        line_to_check: &imageproc::hough::PolarLine,
        corner: Point2<f32>,
    ) -> Option<Point2<f32>> {
        let maybe_img_its = line_to_check.create_image_intersection(self.width, self.height);
        match maybe_img_its {
            Some(its) => {
                let maybe_x = its
                    .iter()
                    .map(|&x| (x, distance_squared(&corner, &x)))
                    .fold(
                        None,
                        |maybe_cmp: Option<(Point2<f32>, f32)>, x: (Point2<f32>, f32)| {
                            match maybe_cmp {
                                Some(cmp) => {
                                    if cmp.1 < x.1 {
                                        Some(cmp)
                                    } else {
                                        Some(x)
                                    }
                                }
                                None => Some(x),
                            }
                        },
                    );
                match maybe_x {
                    Some(x) => Some(x.0),
                    None => None,
                }
            }
            None => None,
        }
    }
}
