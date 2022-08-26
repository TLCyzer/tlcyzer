use flapigen::{JavaConfig, LanguageConfig};
use std::{env, path::Path};

fn main() {
    env_logger::init();
    let out_dir = env::var("OUT_DIR").unwrap();
    let in_src = Path::new("src").join("java_glue.rs.in");
    let out_src = Path::new(&out_dir).join("java_glue.rs");

    let swig_gen = flapigen::Generator::new(LanguageConfig::JavaConfig(
        JavaConfig::new(
            Path::new("..").join("target"),
            "de.uni.tuebingen.tlceval.ni".into(),
        )
        .use_null_annotation_from_package("androidx.annotation".into()),
    ))
    .rustfmt_bindings(false);

    swig_gen.expand("android bindings", &in_src, &out_src);
    println!("cargo:rerun-if-changed={}", in_src.display());
}
