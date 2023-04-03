use ndata::dataobject::*;
use ndata::dataarray::DataArray;
use flowlang::flowlang::system::system_call::system_call;

pub fn execute(_o: DataObject) -> DataObject {
let ax = temp();
let mut o = DataObject::new();
o.put_object("a", ax);
o
}

pub fn temp() -> DataObject {
let mut o = DataObject::new();

let mut sa = DataArray::new();
sa.push_string("vcgencmd");
sa.push_string("measure_temp");
let oo = system_call(sa);
let err = oo.get_string("err");
if err != "".to_string() { 
  o.put_string("temp_c", &err); 
  o.put_string("temp_f", "N/A"); 
}
else {
  let s = oo.get_string("out");
  let mut i = s.split("=");
  let _s = i.next();
  let s = i.next().unwrap();
  let n = s.len();
  let s = &s[0..n-3];
  let f = s.parse::<f64>();
  if f.is_err() { 
    o.put_string("temp_c", &s); 
    o.put_string("temp_f", "N/A"); 
  }
  else { 
    let c = f.unwrap();
    o.put_float("temp_c", c); 
    let f = (c * 9.0/5.0) + 32.0;
    o.put_float("temp_f", f); 
  }
}

o
}

