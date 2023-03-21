let f = Path::new("/proc/device-tree/model");
if f.exists(){ return std::fs::read_to_string(&f).unwrap(); }
"UNKNOWN DEVICE".to_string()