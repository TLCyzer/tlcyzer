use image;
use image::{DynamicImage, GenericImageView, GrayImage};
use linregress::{FormulaRegressionBuilder, RegressionDataBuilder};
use log::debug;
use std::collections::HashMap;
use tlc_common::{
    attenuate_generic, HDRGrayImage, HDRtoLDRGray, InvertGrayImage, LDRToHDRGray, SaturatingSub,
};

pub struct BackgroundFitter {
    input: DynamicImage,
    background_fit: Result<HDRGrayImage, String>,
    save_path: String,
}

fn coord_to_poly(x: f64, y: f64) -> Vec<f64> {
    //Test: [2, 3] -> [ 1.,  2.,  3.,  4.,  6.,  9.,  8., 12., 18., 27., 16., 24., 36., 54., 81.]
    return vec![
        x,
        y, // D1
        x.powf(2.0),
        x * y,
        y.powf(2.0), // D2
        x.powf(3.0),
        x.powf(2.0) * y,
        x * y.powf(2.0),
        y.powf(3.0), //D3
        x.powf(4.0),
        x.powf(3.0) * y,
        x.powf(2.0) * y.powf(2.0),
        x * y.powf(3.0),
        y.powf(4.0), // D4
    ];
}

//TODO support for dark/bright dots
impl BackgroundFitter {
    pub fn new(image: &DynamicImage, save_path: String) -> Self {
        let input: DynamicImage = image.clone();

        let downscale_factor = 4u32 * 4u32;
        let background_fit = BackgroundFitter::fit_background(&input, downscale_factor);
        return BackgroundFitter {
            input: input,
            background_fit: background_fit,
            save_path: save_path,
        };
    }

    pub fn has_potential_dark_blobs(&self) -> bool {
        let rgb = self.input.to_rgb8();

        let c_hist = imageproc::stats::cumulative_histogram(&rgb);

        let g = c_hist.channels[1];
        let b = c_hist.channels[2];

        match (g.last(), b.last()) {
            (Some(g_max), Some(b_max)) => g_max >= b_max,
            (_, _) => true,
        }
    }

    pub fn remove_background(&self, blobs_dark: bool) -> Result<GrayImage, String> {
        let (width, height) = self.input.dimensions();
        debug!("{:?}", self.input.dimensions());
        match &self.background_fit {
            Ok(bf) => {
                let gray = self.input.to_luma8().convert();

                // Both images are in f64
                let img = if blobs_dark { gray.invert() } else { gray };
                let bg = if blobs_dark { bf.invert() } else { bf.clone() };

                let ldr_bg: GrayImage = bg.convert();
                let mut save_path = std::path::PathBuf::from(self.save_path.clone());
                save_path.pop();
                save_path.push("background_fit.png");
                ldr_bg
                    .save(save_path.into_os_string().into_string().unwrap())
                    .unwrap();

                let subtracted: Vec<u8> = img
                    .into_vec()
                    .iter()
                    .zip(bg.into_vec().iter())
                    .map(|(g, b)| attenuate_generic(g.saturating_sub(b)))
                    .collect();

                let maybe_ret = GrayImage::from_vec(width, height, subtracted);
                match maybe_ret {
                    Some(ret) => {
                        ret.save(&self.save_path).expect("Saving failed!");
                        Ok(ret)
                    }
                    None => Err("Could not create background removal image".to_string()),
                }
            }
            Err(err) => Err(err.to_string()),
        }
    }

    fn fit_background(
        input_image: &DynamicImage,
        scale_factor: u32,
    ) -> Result<HDRGrayImage, String> {
        let (input, target) =
            BackgroundFitter::build_input_target_from_image(input_image, scale_factor);
        let (formula, data) = BackgroundFitter::build_training_formula_data(input, target);
        debug!("To optimize: {}", formula);
        let (width, height) = input_image.dimensions();
        let (parameters, intercept) = BackgroundFitter::perform_fit(formula, data);
        BackgroundFitter::eval_fit(parameters, intercept, width, height)
    }

    fn build_training_formula_data(
        input: Vec<Vec<f64>>,
        target: Vec<f64>,
    ) -> (String, HashMap<String, Vec<f64>>) {
        debug!("Fitting Targets: {}", target.len());

        let mut ret: HashMap<String, Vec<f64>> = HashMap::new();
        ret.insert("Y".to_string(), target);

        let poly_len = input[0].len();

        let mut formula: String = "Y ~ ".to_string();
        for i in 0..poly_len {
            if i != 0 {
                formula = formula + " + ";
            }
            let name = format!("X{}", i + 1);
            let vec: Vec<f64> = input.iter().map(|v| v[i]).collect();
            ret.insert(name.clone(), vec);
            formula = formula + &name;
        }
        return (formula.to_string(), ret);
    }

    fn build_input_target_from_image(
        input_image: &DynamicImage,
        scale_factor: u32,
    ) -> (Vec<Vec<f64>>, Vec<f64>) {
        let (width, height) = input_image.dimensions();
        debug!("Inputs: {}", width * height);
        let gray = input_image.to_luma8();
        let mut target: Vec<f64> = Vec::new();
        let mut input: Vec<Vec<f64>> = Vec::new();

        for (x, y, p) in gray.enumerate_pixels() {
            let idx = x + y * width;
            if idx % scale_factor == 0 {
                input.push(coord_to_poly(x as f64, y as f64));
                target.push(p[0] as f64);
            }
        }

        return (input, target);
    }

    fn perform_fit(formula: String, data: HashMap<String, Vec<f64>>) -> (Vec<f64>, f64) {
        let reg_data = RegressionDataBuilder::new()
            .build_from(data)
            .expect("Regression data building failed!");
        let fitted = FormulaRegressionBuilder::new()
            .data(&reg_data)
            .formula(formula)
            .fit_without_statistics()
            .expect("Formula could ne be evaluated!");

        let intercept = fitted[0];
        let parameters: Vec<_> = fitted.iter().cloned().skip(1).collect();

        (parameters, intercept)
    }

    fn eval_fit(
        parameters: Vec<f64>,
        intercept_value: f64,
        width: u32,
        height: u32,
    ) -> Result<HDRGrayImage, String> {
        let mut predicted: Vec<f64> = Vec::new();
        for y in 0..height {
            for x in 0..width {
                let poly = coord_to_poly(x as f64, y as f64);
                let eval = poly
                    .iter()
                    .zip(parameters.iter())
                    .map(|(a, b)| a * b)
                    .fold(0f64, |sum, x| sum + x)
                    + intercept_value;
                predicted.push(eval);
            }
        }

        Ok(HDRGrayImage::from_raw(width, height, predicted).unwrap())
    }
}

#[cfg(test)]
mod test {
    use crate::background_fitter::coord_to_poly;
    use crate::BackgroundFitter;
    use assert_approx_eq::assert_approx_eq;
    use image::{DynamicImage, GenericImageView, GrayImage};

    #[test]
    fn test_poly_gen() {
        let given: (f64, f64) = (2.0, 3.0);
        let when: Vec<f64> = coord_to_poly(given.0, given.1);
        let then: Vec<f64> = vec![
            2., 3., 4., 6., 9., 8., 12., 18., 27., 16., 24., 36., 54., 81.,
        ];

        assert!(when.iter().zip(then.iter()).all(|(a, b)| a == b));
    }

    fn setup_simple_test_image(width: u32, height: u32) -> DynamicImage {
        let mut raw_vec: Vec<u8> = Vec::with_capacity((width * height) as usize);
        for i in 0..(width * height) {
            let px = (i as f64 / (width * height) as f64) * 255f64;
            raw_vec.push(px as u8);
        }
        return DynamicImage::ImageLuma8(GrayImage::from_vec(width, height, raw_vec).unwrap());
    }

    fn setup_sine_test_image(width: u32, height: u32) -> DynamicImage {
        let mut raw_vec: Vec<u8> = Vec::with_capacity((width * height) as usize);
        for i in 0..(width * height) {
            let y = i / width;

            let sy = (y as f64 / width as f64) * std::f64::consts::PI;

            let val = (sy.cos() * -1f64 + 1f64) / 2f64;
            let px = val * 255f64;
            raw_vec.push(px as u8);
        }
        return DynamicImage::ImageLuma8(GrayImage::from_vec(width, height, raw_vec).unwrap());
    }

    fn perform_fit(given_image: &DynamicImage) -> Vec<f64> {
        let (input, target) = BackgroundFitter::build_input_target_from_image(&given_image, 1);
        let (formula, data) = BackgroundFitter::build_training_formula_data(input, target);
        let (parameters, intercept) = BackgroundFitter::perform_fit(formula, data); // When
        let (width, height) = given_image.dimensions();
        let when_image = BackgroundFitter::eval_fit(parameters, intercept, width, height).unwrap();
        when_image.into_vec()
    }

    #[test]
    fn test_fit_with_python() {
        let given = setup_simple_test_image(100, 100);

        let (input, target) = BackgroundFitter::build_input_target_from_image(&given, 1);
        let (formula, data) = BackgroundFitter::build_training_formula_data(input, target);
        let (when_parameters, when_intercept) = BackgroundFitter::perform_fit(formula, data); // When

        // Gathered from Python ("GT" implementation)
        let then_parameters: Vec<f64> = vec![
            2.40605974e-02,
            2.54862176e+00,
            2.21587116e-05,
            3.88941304e-05,
            2.86814280e-05,
            -1.55986152e-07,
            -3.36758977e-07,
            -2.66241654e-07,
            -2.23416068e-07,
            5.82453186e-10,
            6.71342905e-10,
            9.50078920e-10,
            6.71344681e-10,
            5.82453853e-10,
        ];
        let then_intercept = -0.46808754775427985;

        // Make sure length is the same
        assert_eq!(when_parameters.len(), then_parameters.len());
        // Make sure it is not empty
        assert!(when_parameters.len() > 0);

        // Check all parameters
        for i in 0..then_parameters.len() {
            assert_approx_eq!(when_parameters[i], then_parameters[i], 1e-3f64);
        }

        assert_approx_eq!(when_intercept, then_intercept, 5e-3f64);
    }

    #[test]
    fn test_simple_fit_result() {
        let given_image = setup_simple_test_image(100, 100);
        let when: Vec<f64> = perform_fit(&given_image);

        let then: Vec<f64> = given_image
            .to_luma()
            .into_vec()
            .iter()
            .map(|x| *x as f64)
            .collect();

        for i in 0..then.len() {
            assert_approx_eq!(when[i], then[i], 1f64);
        }
    }

    #[test]
    fn test_complex_fit_result() {
        let given_image = setup_sine_test_image(100, 100);
        let when: Vec<f64> = perform_fit(&given_image);

        let then: Vec<f64> = given_image
            .to_luma()
            .into_vec()
            .iter()
            .map(|x| *x as f64)
            .collect();

        for i in 0..then.len() {
            assert_approx_eq!(when[i], then[i], 1f64);
        }
    }
}
