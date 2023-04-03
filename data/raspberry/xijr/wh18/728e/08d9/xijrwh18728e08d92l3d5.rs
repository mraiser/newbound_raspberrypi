let mut o = DataObject::new();

let s = "/etc/os-release";
let f = Path::new(s);
if f.exists(){ 
  return read_properties(s.to_string());
}

DataObject::new()