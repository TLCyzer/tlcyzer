use linregress::{FormulaRegressionBuilder, RegressionDataBuilder};
use std::collections::HashMap;

pub struct ReferencePercentFitter {
    parameters: Vec<f64>,
    intercept: f64,
}

impl ReferencePercentFitter {
    pub fn new(integrated: &HashMap<u32, u64>, reference_values: &HashMap<u32, f32>) -> Self {
        // Build the input target data
        let ref_int_val_wperc: Vec<(u64, f32)> = reference_values
            .iter()
            .map(|(key, ref_perc)| {
                let int = integrated[key];
                (int, *ref_perc)
            })
            .collect();
        let target: Vec<f64> = ref_int_val_wperc
            .iter()
            .map(|(_, perc)| *perc as f64)
            .collect();
        let input: Vec<f64> = ref_int_val_wperc
            .iter()
            .map(|(int, _)| *int as f64)
            .collect();

        // Build the actual fitting structs
        let mut data: HashMap<String, Vec<f64>> = HashMap::new();
        data.insert("Y".to_string(), target);
        data.insert("X".to_string(), input);

        let formula: String = "Y ~ X".to_string();
        println!("To optimize: {}", formula);
        println!("Fitting data: {:?}", data);
        let reg_data = RegressionDataBuilder::new()
            .build_from(data)
            .expect("Regression data building failed!");
        println!("{:?}", reg_data);

        // And fit the model
        let fitted = FormulaRegressionBuilder::new()
            .data(&reg_data)
            .formula(formula)
            .fit_without_statistics()
            .expect("Formula could not be evaluated!");

        // Get the intercept and parameters
        let intercept = fitted[0];
        let parameters: Vec<_> = fitted.iter().cloned().skip(1).collect();

        ReferencePercentFitter {
            parameters,
            intercept,
        }
    }

    pub fn evaluate(&self, integrated_blobs: &HashMap<u32, u64>) -> HashMap<u32, f32> {
        integrated_blobs
            .iter()
            .map(|(key, int)| {
                (
                    *key,
                    ((*int as f64 * self.parameters[0]) + self.intercept as f64) as f32,
                )
            })
            .collect()
    }
}

#[cfg(test)]
mod test {
    use crate::ReferencePercentFitter;
    use assert_approx_eq::assert_approx_eq;
    use std::collections::HashMap;

    fn setup_example() -> (
        HashMap<u32, u64>,
        HashMap<u32, f32>,
        HashMap<u32, f32>,
        f64,
        f64,
    ) {
        let mut integrants: HashMap<u32, u64> = HashMap::new();
        let mut references: HashMap<u32, f32> = HashMap::new();

        //From python
        let mut ground_truth: HashMap<u32, f32> = HashMap::new();

        integrants.insert(0, 584007);
        integrants.insert(1, 522476);
        integrants.insert(2, 627150);
        integrants.insert(3, 935728);

        references.insert(0, 80f32);
        references.insert(3, 110f32);

        ground_truth.insert(0, 80f32);
        ground_truth.insert(1, 74.75f32);
        ground_truth.insert(2, 83.67f32);
        ground_truth.insert(3, 110f32);

        (
            integrants,
            references,
            ground_truth,
            8.529487861117192e-05f64,
            30.187193826925323f64,
        )
    }

    #[test]
    fn perform_simple_text() {
        let (integrants, references, then, then_m, then_b) = setup_example();

        let perc_fitter = ReferencePercentFitter::new(&integrants, &references);
        let when_m = perc_fitter.parameters[0];
        let when_b = perc_fitter.intercept;

        let when = perc_fitter.evaluate(&integrants);

        assert_approx_eq!(when_m, then_m);
        assert_approx_eq!(when_b, then_b);

        for k in then.keys() {
            assert_approx_eq!(when[k], then[k], 1f32);
        }
    }
}
