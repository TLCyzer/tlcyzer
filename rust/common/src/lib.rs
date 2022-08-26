extern crate num;

use image::{DynamicImage, GrayImage, ImageBuffer, ImageResult, Luma, Pixel};
use imageproc::map::map_pixels;
use log::{debug, error};
use nalgebra::Point2;
use num::{FromPrimitive, ToPrimitive};

pub type HDRGrayImage = ImageBuffer<Luma<f64>, Vec<f64>>;

pub trait InvertGrayImage {
    fn invert(&self) -> Self;
}

impl InvertGrayImage for GrayImage {
    fn invert(&self) -> Self {
        map_pixels(self, |_x, _y, p| {
            let mut pc = p;
            pc.invert();
            pc
        })
    }
}

impl InvertGrayImage for HDRGrayImage {
    fn invert(&self) -> Self {
        map_pixels(self, |_x, _y, p| {
            let mut pc = p;
            let maxu8 = std::u8::MAX as f64;
            pc[0] = maxu8 - p[0];
            pc
        })
    }
}

pub trait LDRToHDRGray {
    fn convert(&self) -> HDRGrayImage;
}

pub trait HDRtoLDRGray {
    fn convert(&self) -> GrayImage;
}

impl LDRToHDRGray for GrayImage {
    fn convert(&self) -> HDRGrayImage {
        let data: Vec<u8> = self.to_vec();
        let to_f64: Vec<f64> = data.iter().map(|x| *x as f64).collect();
        HDRGrayImage::from_raw(self.width(), self.height(), to_f64).unwrap()
    }
}

impl HDRtoLDRGray for HDRGrayImage {
    fn convert(&self) -> GrayImage {
        let data: Vec<f64> = self.to_vec();
        let to_u8: Vec<u8> = data.iter().map(|x| attenuate_generic(*x)).collect();
        GrayImage::from_raw(self.width(), self.height(), to_u8).unwrap()
    }
}

pub trait ColorSpaceConversion {
    fn to_linear(&mut self);
    fn to_srgb(&mut self);
}

impl ColorSpaceConversion for HDRGrayImage {
    fn to_linear(&mut self) {
        self.pixels_mut().for_each(|p| {
            let old_zo_val = p[0] / std::u8::MAX as f64;
            let new_val = if old_zo_val >= 0.04045 {
                ((old_zo_val + 0.055) / 1.055).powf(2.4)
            } else {
                old_zo_val / 12.92
            };
            *p = Luma([new_val * std::u8::MAX as f64])
        });
    }

    fn to_srgb(&mut self) {
        self.pixels_mut().for_each(|p| {
            let old_zo_val = p[0] / std::u8::MAX as f64;
            let new_val = if old_zo_val >= 0.0031308 {
                1.055 * old_zo_val.powf(1.0 / 2.4)
            } else {
                old_zo_val * 12.92
            };
            *p = Luma([new_val * std::u8::MAX as f64])
        });
    }
}

pub trait SaturatingSub {
    fn saturating_sub(&self, other: &Self) -> Self;
}

impl SaturatingSub for f64 {
    fn saturating_sub(&self, other: &Self) -> Self {
        (self - other).max(0f64)
    }
}

pub trait StatsImage<P>
where
    P: image::Pixel<Subpixel = u8> + 'static,
{
    fn mean(&self) -> P;
    fn median(&self) -> P;
    fn max(&self) -> P;
    fn min(&self) -> P;
}

impl StatsImage<Luma<u8>> for GrayImage {
    fn mean(&self) -> Luma<u8> {
        let len = self.len();
        let sum = self.pixels().fold(0u32, |sum, x| sum + x[0] as u32);
        Luma([(sum as f32 / len as f32) as u8])
    }

    fn median(&self) -> Luma<u8> {
        let mut nums: Vec<u8> = self.pixels().map(|p| p[0]).collect();
        nums.sort();

        let mid = nums.len() / 2;
        Luma([if nums.len() % 2 == 0 {
            (nums[mid - 1] + nums[mid]) / 2
        } else {
            nums[mid]
        }])
    }

    fn max(&self) -> Luma<u8> {
        let max = self
            .pixels()
            .fold(0u8, |m, x| if m <= x[0] { x[0] } else { m });
        Luma([max])
    }

    fn min(&self) -> Luma<u8> {
        let min = self
            .pixels()
            .fold(255u8, |m, x| if m >= x[0] { x[0] } else { m });
        Luma([min])
    }
}

pub fn auto_canny(image: &GrayImage) -> GrayImage {
    let otsu_level = imageproc::contrast::otsu_level(image);
    let low_canny_threshold = (otsu_level / 2) as f32;
    let high_canny_threshold = otsu_level as f32;
    imageproc::edges::canny(image, low_canny_threshold, high_canny_threshold)
}

pub fn read_image(path: String) -> ImageResult<DynamicImage> {
    let res_image = image::open(path.clone());

    match res_image {
        Ok(image) => {
            let mut ret_image = image;
            match rexif::parse_file(&path) {
                Ok(exif) => {
                    for entry in &exif.entries {
                        if entry.tag == rexif::ExifTag::Orientation {
                            match entry.value {
                                rexif::TagValue::U16(ref v) => {
                                    let n = v[0];
                                    match n {
                                        3 => {
                                            // Upside down
                                            ret_image = ret_image.rotate180();
                                        }
                                        6 => {
                                            // Rotated left
                                            ret_image = ret_image.rotate90();
                                        }
                                        8 => {
                                            // Rotated right
                                            ret_image = ret_image.rotate270();
                                        }
                                        _ => {} // Do nothing
                                    }
                                }
                                _ => {}
                            }
                        }
                    }
                }
                Err(e) => error!("Error in {}: {}", &path, e),
            }
            Ok(ret_image)
        }
        Err(err) => Err(err),
    }
}

pub fn attenuate_generic<T: PartialOrd + FromPrimitive + ToPrimitive + std::fmt::Debug>(
    channel: T,
) -> u8 {
    if channel >= FromPrimitive::from_u8(255).unwrap() {
        255u8
    } else if channel <= FromPrimitive::from_u8(0).unwrap() {
        0u8
    } else {
        channel
            .to_u8()
            .unwrap_or_else(|| panic!("Conversion to u8 failed!, {:?}", channel))
    }
}

#[derive(Debug)]
pub struct Quad {
    pub top_left: Point2<f32>,
    pub top_right: Point2<f32>,
    pub bottom_right: Point2<f32>,
    pub bottom_left: Point2<f32>,
}

impl Quad {
    pub fn dimensions(&self) -> (f32, f32) {
        let width = (self.top_right.x - self.top_left.x).abs();
        let height = (self.bottom_right.y - self.top_right.y).abs();

        (width, height)
    }

    pub fn aspect_ratio(&self) -> f32 {
        let (width, height) = self.dimensions();

        width / height
    }

    pub fn to_tuple_vec(&self) -> Vec<(f32, f32)> {
        vec![
            (self.top_left.x, self.top_left.y),
            (self.top_right.x, self.top_right.y),
            (self.bottom_right.x, self.bottom_right.y),
            (self.bottom_left.x, self.bottom_left.y),
        ]
    }

    pub fn to_simple_vec(&self) -> Vec<i32> {
        vec![
            self.top_left.x as i32,
            self.top_left.y as i32,
            self.top_right.x as i32,
            self.top_right.y as i32,
            self.bottom_right.x as i32,
            self.bottom_right.y as i32,
            self.bottom_left.x as i32,
            self.bottom_right.y as i32,
        ]
    }

    pub fn from_simple_vec(coords: Vec<i32>) -> Self {
        let points: Vec<Point2<f32>> = coords
            .chunks(2)
            .map(|m| Point2::new(m[0] as f32, m[1] as f32))
            .collect();

        let mut y_sorted = points;
        y_sorted.sort_by(|a, b| a.y.partial_cmp(&b.y).unwrap());
        debug!("{:#?}", y_sorted);

        let (top_left, top_right) = if y_sorted[0].x <= y_sorted[1].x {
            (y_sorted[0], y_sorted[1])
        } else {
            (y_sorted[1], y_sorted[0])
        };

        let (bottom_left, bottom_right) = if y_sorted[2].x <= y_sorted[3].x {
            (y_sorted[2], y_sorted[3])
        } else {
            (y_sorted[3], y_sorted[2])
        };

        Quad {
            top_left,
            top_right,
            bottom_right,
            bottom_left,
        }
    }
}

#[derive(Debug)]
pub struct Circle {
    pub center: Point2<f32>,
    pub radius: f32,
}

impl Circle {
    pub fn new(center_x: f32, center_y: f32, radius: f32) -> Self {
        Circle {
            center: Point2::new(center_x, center_y),
            radius,
        }
    }
    pub fn to_quad(&self) -> Quad {
        let top_left = Point2::new(self.center.x - self.radius, self.center.y - self.radius);
        let top_right = Point2::new(self.center.x + self.radius, self.center.y - self.radius);
        let bottom_left = Point2::new(self.center.x - self.radius, self.center.y + self.radius);
        let bottom_right = Point2::new(self.center.x + self.radius, self.center.y + self.radius);

        Quad {
            top_left,
            top_right,
            bottom_right,
            bottom_left,
        }
    }

    pub fn to_tuples(&self) -> (f32, f32, f32) {
        (self.center.x, self.center.y, self.radius)
    }

    pub fn to_simple_vec(&self) -> Vec<i32> {
        vec![
            self.center.x as i32,
            self.center.y as i32,
            self.radius as i32,
        ]
    }

    pub fn from_simple_vec(coords: Vec<i32>) -> Self {
        Circle {
            center: Point2::new(coords[0] as f32, coords[1] as f32),
            radius: coords[2] as f32,
        }
    }
}
