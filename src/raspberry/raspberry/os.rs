use ndata::dataobject::*;
use std::path::Path;
use flowlang::flowlang::file::read_properties::read_properties;

pub fn execute(_o: DataObject) -> DataObject {
let ax = os();
let mut o = DataObject::new();
o.put_object("a", ax);
o
}

pub fn os() -> DataObject {
let mut o = DataObject::new();

let s = "/etc/os-release";
let f = Path::new(s);
if f.exists(){ 
  return read_properties(s.to_string());
}

DataObject::new()
}

