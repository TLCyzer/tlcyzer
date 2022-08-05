#![allow(dead_code)]

use image::DynamicImage;
use log::{debug, info};
use std::collections::HashMap;
use std::path::PathBuf;
use tlc_background_removal::BackgroundFitter;
use tlc_blob_detection;
use tlc_common::{read_image, Circle, Quad};
use tlc_plate_detection::Detector;
use tlc_plate_extraction;
use tlc_reference_percent_fitter::ReferencePercentFitter;

mod java_glue;
pub use crate::java_glue::*;

struct TlcProcessor {
    input: DynamicImage,
    save_path: PathBuf,
    background_removed: Option<DynamicImage>,
    background_fitter: Option<BackgroundFitter>,
    integrated_blobs: Option<HashMap<u32, u64>>,
}

impl TlcProcessor {
    fn new(path: String) -> Self {
        #[cfg(target_os = "android")]
        android_logger::init_once(
            android_logger::Config::default()
                .with_min_level(log::Level::Debug)
                .with_tag("TlcNi"),
        );
        log_panics::init(); // log panics rather than printing them
        info!("init log system - done");

        let image = read_image(path.clone()).unwrap();
        let mut path_buf = PathBuf::from(path.clone());
        path_buf.pop();
        TlcProcessor {
            input: image,
            save_path: path_buf,
            background_removed: None,
            background_fitter: None,
            integrated_blobs: None,
        }
    }

    fn detect_plate(&self) -> Vec<i32> {
        let detector = Detector::new(&self.input);
        let plate = detector.corners_or_default();

        plate.to_simple_vec()
    }

    fn warp_plate(&mut self, coords: &[i32], orientation: u32) -> bool {
        let plate = Quad::from_simple_vec(coords.to_vec());

        let mut save_path = self.save_path.clone();
        save_path.push("warped.png");
        debug!("Warp Save path {:#?}", save_path);

        let correct_rotation = match orientation {
            90 => self.input.rotate90(),
            180 => self.input.rotate180(),
            270 => self.input.rotate270(),
            _ => self.input.clone(),
        };
        let maybe_crop = tlc_plate_extraction::unwarp_crop(
            &correct_rotation,
            &plate,
            save_path.into_os_string().into_string().unwrap(),
        );

        match maybe_crop {
            Ok(crop) => {
                let mut blob_save_path = self.save_path.clone();
                blob_save_path.push("blobs.png");
                debug!("Blobs Save path {:#?}", blob_save_path);

                self.background_fitter = Some(BackgroundFitter::new(
                    &crop,
                    blob_save_path.into_os_string().into_string().unwrap(),
                ));
                return true;
            }
            _ => return false,
        }
    }

    fn check_potentital_dark_blobs(&self) -> bool {
        match &self.background_fitter {
            Some(fitter) => fitter.has_potential_dark_blobs(),
            None => false,
        }
    }

    fn fit_background(&mut self, dark_blobs: bool) -> Result<(), String> {
        match &self.background_fitter {
            Some(fitter) => {
                let cleaned = fitter
                    .remove_background(dark_blobs)
                    .expect("Removing background failed");

                self.background_removed = Some(DynamicImage::ImageLuma8(cleaned));

                Ok(())
            }
            None => Err("Plane warping failed!".to_string()),
        }
    }

    fn detect_blobs(&self) -> Result<Vec<i32>, String> {
        match &self.background_removed {
            Some(cleaned) => {
                let blobs = tlc_blob_detection::detect_blobs(&cleaned.to_luma());
                let ret: Vec<i32> = blobs
                    .iter()
                    .map(|(k, v)| {
                        let mut coord_vec = v.to_simple_vec();
                        coord_vec.insert(0, *k as i32);
                        coord_vec
                    })
                    .flatten()
                    .collect();
                Ok(ret)
            }
            None => Err("Background removal failed".to_string()),
        }
    }

    fn integrate_blobs(
        &mut self,
        blobs: &[i32],
        cut_off_percentage: f32,
    ) -> Result<Vec<i32>, String> {
        match &self.background_removed {
            Some(cleaned) => {
                let blob_chunks = blobs.chunks(4);

                let mut blob_map: HashMap<u32, Circle> = HashMap::new();
                for chunk in blob_chunks {
                    let blob: Vec<i32> = chunk.into_iter().map(|v| *v).collect();
                    let key = blob[0];
                    let circle = Circle::from_simple_vec(blob[1..].to_vec());
                    blob_map.insert(key as u32, circle);
                }

                let integrated = tlc_blob_integration::integrate_spots(
                    &cleaned.to_luma(),
                    &blob_map,
                    cut_off_percentage,
                );
                self.integrated_blobs = Some(integrated.clone());

                let ret: Vec<i32> = integrated
                    .iter()
                    .map(|(k, v)| vec![*k as i32, *v as i32])
                    .flatten()
                    .collect();
                Ok(ret)
            }
            None => Err("Background removal failed".to_string()),
        }
    }

    fn fit_percentages(&self, key_percentage: &[f32]) -> Result<Vec<f32>, String> {
        match &self.integrated_blobs {
            Some(integrants) => {
                let perc_chunks = key_percentage.chunks(2);
                let mut perc_map: HashMap<u32, f32> = HashMap::new();
                for chunk in perc_chunks {
                    let key_perc: Vec<f32> = chunk.into_iter().map(|v| *v).collect();
                    let key = key_perc[0] as u32;
                    let perc = key_perc[1] as f32;
                    perc_map.insert(key, perc);
                }

                let perc_fitter = ReferencePercentFitter::new(&integrants, &perc_map);
                let percentages = perc_fitter.evaluate(&integrants);

                let ret: Vec<f32> = percentages
                    .iter()
                    .map(|(k, v)| vec![*k as f32, *v as f32])
                    .flatten()
                    .collect();

                Ok(ret)
            }
            None => Err("Blob integration failed".to_string()),
        }
    }
}

/*
    let mut ref_data = HashMap::new();
    ref_data.insert(left_key, 100u8);
    ref_data.insert(right_key, 80u8);

    let perc_fitter = ReferencePercentFitter::new(&integrated, &ref_data);
    let percentages = perc_fitter.evaluate(&integrated);
    println!("Int: {:?}", integrated);
    println!("Blobs: {:?}", blobs);
    println!("Percentages: {:?}", percentages);
*/
