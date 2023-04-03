use ndata::dataobject::*;
use std::path::Path;
use core::time::Duration;

pub fn execute(_o: DataObject) -> DataObject {
let ax = cpu();
let mut o = DataObject::new();
o.put_object("a", ax);
o
}

pub fn cpu() -> DataObject {
let mut o = DataObject::new();

let f = Path::new("/proc/stat");
if f.exists(){ 
  let s1 = std::fs::read_to_string(&f).unwrap();
  std::thread::sleep(Duration::from_millis(1000));
  let s2 = std::fs::read_to_string(&f).unwrap();
  
  let mut a = s1.split("\n");
  let mut row = a.next().unwrap().to_owned();
  let mut i = 0;
  loop { 
    if a.next().unwrap().starts_with("cpu") { i += 1; }
    else { break; }
  }
  
  while row.contains("  ") { row = row.replace("  ", " "); }
  let mut a = row.split(" ");
  let _x = a.next();
  let _x = a.next();
  let _x = a.next();
  let _x = a.next();
  let start = a.next().unwrap().parse::<i64>().unwrap() as f64 / (i as f64);
  
  let mut a = s2.split("\n");
  let mut row = a.next().unwrap().to_owned();
  while row.contains("  ") { row = row.replace("  ", " "); }
  let mut a = row.split(" ");
  let _x = a.next();
  let _x = a.next();
  let _x = a.next();
  let _x = a.next();
  let end = a.next().unwrap().parse::<i64>().unwrap() as f64 / (i as f64);
  
  let total = (end - start) as f64;
  let u = 1.0 - (10.0 * total / 1000.0);
  
  o.put_int("count", i);
  o.put_float("usage", u);
}

o
}

