use ndata::dataobject::*;
use std::process::Command;
use std::process::Stdio;

pub fn execute(_o: DataObject) -> DataObject {
let ax = arch();
let mut o = DataObject::new();
o.put_string("a", &ax);
o
}

pub fn arch() -> String {
let args = ["--print-architecture"];
let output = Command::new("dpkg")
  .args(args)
  .stdout(Stdio::piped())
  .output()
  .unwrap();
String::from_utf8(output.stdout).unwrap().trim().to_string()
}

