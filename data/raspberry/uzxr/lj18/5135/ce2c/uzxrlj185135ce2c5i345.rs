let mut sa = DataArray::new();
sa.push_string("df");
let oo = system_call(sa);
let err = oo.get_string("err");
if err != "".to_string() { 
  panic!("Unknown Error: {}", &err); 
}

let s = oo.get_string("out");
let mut rows = s.split("\n");
let mut keyrow = rows.next().unwrap().to_owned();
while keyrow.contains("  ") { keyrow = keyrow.replace("  ", " "); }
let mut disks = DataArray::new();
loop {
  let row = rows.next();
  if row.is_some(){
    let mut row = row.unwrap().to_owned();
    while row.contains("  ") { row = row.replace("  ", " "); }
    let mut disk = DataObject::new();
    let mut keys = keyrow.split(" ");
    for val in row.split(" ") {
      let key = keys.next().unwrap();
      disk.put_string(key, val);
    }
    disks.push_object(disk);
  }
  else { break; }
}

disks