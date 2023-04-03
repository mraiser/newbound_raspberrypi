use ndata::dataobject::*;
use std::path::Path;
pub fn execute(_o: DataObject) -> DataObject {
let ax = device();
let mut o = DataObject::new();
o.put_string("a", &ax);
o
}

pub fn device() -> String {
let f = Path::new("/proc/device-tree/model");
if f.exists(){ return std::fs::read_to_string(&f).unwrap(); }
"UNKNOWN DEVICE".to_string()
}

