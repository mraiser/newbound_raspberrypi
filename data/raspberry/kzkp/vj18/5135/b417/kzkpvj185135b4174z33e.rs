let mut mem = DataObject::new();

let f = Path::new("/proc/meminfo");
if f.exists(){ 
  let s = std::fs::read_to_string(&f).unwrap();
  let a = s.split("\n");
  for key in a {
    if key != "" {
      let mut a = key.split(":");
      let key = a.next().unwrap().trim();
      let val = a.next().unwrap().trim();
      let mut a = val.split(" ");
      let val = a.next().unwrap().trim();
      let val = val.parse::<i64>().unwrap();
      mem.put_int(key, val);
    }
  }
}
else { mem.put_string("err", "N/A"); }

mem