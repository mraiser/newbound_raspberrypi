let store = DataStore::new();
let mut d;
if store.exists("runtime", "controls_available") { d = store.get_data("runtime", "controls_available").get_object("data"); }
else { d = DataObject::new(); }

let mut b = false;
if !d.has("raspberry:memory") {
  let o = DataObject::from_string("{\"title\":\"Memory Used\",\"type\":\"raspberry:memory\",\"big\":false,\"position\":\"inline\",\"groups\":[\"admin\"]}");
  d.put_object("raspberry:memory", o);
  b = true;
}
if !d.has("raspberry:cpu") {
  let o = DataObject::from_string("{\"title\":\"CPU Used\",\"type\":\"raspberry:cpu\",\"big\":false,\"position\":\"inline\",\"groups\":[\"admin\"]}");
  d.put_object("raspberry:cpu", o);
  b = true;
}
if !d.has("raspberry:disk") {
  let o = DataObject::from_string("{\"title\":\"Disk Used\",\"type\":\"raspberry:disk\",\"big\":false,\"position\":\"inline\",\"groups\":[\"admin\"]}");
  d.put_object("raspberry:disk", o);
  b = true;
}
if !d.has("raspberry:temperature") {
  let o = DataObject::from_string("{\"title\":\"CPU Temp\",\"type\":\"raspberry:temperature\",\"big\":false,\"position\":\"inline\",\"groups\":[\"admin\"]}");
  d.put_object("raspberry:temperature", o);
  b = true;
}

if b { write("runtime".to_string(), "controls_available".to_string(), d.clone(), DataArray::new(), DataArray::new()); }
  
d