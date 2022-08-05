use image::{DynamicImage, GenericImage};
use imageproc;
use log::debug;
use tlc_common::Quad;

type QuadCropArray = [(f32, f32); 4];

pub fn unwarp_crop(
    image: &DynamicImage,
    quad: &Quad,
    save_path: String,
) -> Result<DynamicImage, String> {
    let from: QuadCropArray = [
        (quad.top_left.x, quad.top_left.y),
        (quad.top_right.x, quad.top_right.y),
        (quad.bottom_right.x, quad.bottom_right.y),
        (quad.bottom_left.x, quad.bottom_left.y),
    ];

    let (to, max_width, max_height) = propose_destination(quad);

    let maybe_projection =
        imageproc::geometric_transformations::Projection::from_control_points(from, to);

    debug!("FROM {:#?} TO {:#?}", from, to);
    match maybe_projection {
        Some(projection) => {
            debug!("{:#?}", projection);
            let mut warped = imageproc::geometric_transformations::warp(
                &image.to_rgb(),
                &projection,
                imageproc::geometric_transformations::Interpolation::Bilinear,
                image::Rgb([0, 0, 0]),
            );
            let crop = warped
                .sub_image(0, 0, (max_width - 1.0) as u32, (max_height - 1.0) as u32)
                .to_image();
            match crop.save(save_path) {
                Ok(_) => Ok(DynamicImage::ImageRgb8(crop)),
                Err(_) => Err("Saving the warped image failed!".to_string()),
            }
        }
        None => Err(format!(
            "Could not find a valid projection! FROM {:?} TO {:?}",
            from, to
        )),
    }
}

fn propose_destination(quad: &Quad) -> (QuadCropArray, f32, f32) {
    // max height of the rect in x direction
    let width_a = ((quad.bottom_right.x - quad.bottom_left.x).powf(2.0)
        + (quad.bottom_right.y - quad.bottom_left.y).powf(2.0))
    .sqrt();
    let width_b = ((quad.top_right.x - quad.top_left.x).powf(2.0)
        + (quad.top_right.y - quad.top_left.y).powf(2.0))
    .sqrt();
    let max_width = width_a.max(width_b).floor();

    // max height of the rect in y direction
    let height_a = ((quad.top_right.x - quad.bottom_right.x).powf(2.0)
        + (quad.top_right.y - quad.bottom_right.y).powf(2.0))
    .sqrt();
    let height_b = ((quad.top_left.x - quad.bottom_left.x).powf(2.0)
        + (quad.top_left.y - quad.bottom_left.y).powf(2.0))
    .sqrt();
    let max_height = height_a.max(height_b).floor();

    return (
        [
            (0.0, 0.0),
            (max_width - 1.0, 0.0),
            (max_width - 1.0, max_height - 1.0),
            (0.0, max_height - 1.0),
        ],
        max_width,
        max_height,
    );
}

#[cfg(test)]
mod test {
    use crate::{propose_destination, QuadCropArray};
    use assert_approx_eq::assert_approx_eq;
    use imageproc;
    use tlc_common::Quad;

    #[test]
    fn test_destination_proposal() {
        let given = Quad::from_simple_vec(vec![1728, 3710, 155, 3710, 131, 819, 1720, 819]);
        let (when, _, _) = propose_destination(&given);
        let then: QuadCropArray = [
            (0f32, 0f32),
            (1588f32, 0f32),
            (1588f32, 2890f32),
            (0f32, 2890f32),
        ];

        assert!(when
            .iter()
            .zip(then.iter())
            .all(|(a, b)| a.0 == b.0 && a.1 == b.1));
    }

    #[test]
    fn test_destination_proposal_angled() {
        let given = Quad::from_simple_vec(vec![1703, 3645, 196, 3710, 147, 892, 1712, 843]);
        let (when, _, _) = propose_destination(&given);
        let then: QuadCropArray = [
            (0f32, 0f32),
            (1564f32, 0f32),
            (1564f32, 2817f32),
            (0f32, 2817f32),
        ];

        assert!(when
            .iter()
            .zip(then.iter())
            .all(|(a, b)| a.0 == b.0 && a.1 == b.1));
    }

    enum TransformationClass {
        Translation,
        Affine,
        Projection,
    }

    pub struct TestProjection {
        pub transform: [f32; 9],
        inverse: [f32; 9],
        class: TransformationClass,
    }

    #[test]
    fn test_projection_matrix() {
        let quad = Quad::from_simple_vec(vec![1720, 3694, 155, 3694, 122, 811, 1720, 811]);

        let (given_to, _, _) = propose_destination(&quad);
        let given_from: QuadCropArray = [
            (quad.top_left.x, quad.top_left.y),
            (quad.top_right.x, quad.top_right.y),
            (quad.bottom_right.x, quad.bottom_right.y),
            (quad.bottom_left.x, quad.bottom_left.y),
        ];

        // Horrible hack to expose transform
        let projection = imageproc::geometric_transformations::Projection::from_control_points(
            given_from, given_to,
        )
        .unwrap();

        let projection_exp: TestProjection = unsafe { std::mem::transmute(projection) };
        let when = projection_exp.transform;

        let then = [
            9.93602223e-01f32,
            -1.13731784e-02f32,
            -1.11995824e+02f32,
            -6.93889390e-16f32,
            9.73355112e-01f32,
            -7.89390996e+02f32,
            -1.62630326e-19f32,
            -7.12158949e-06f32,
            1.00000000e+00f32,
        ];

        for i in 0..then.len() {
            assert_approx_eq!(when[i], then[i], 1e-4);
        }
    }
}
