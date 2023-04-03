let args = ["--print-architecture"];
let output = Command::new("dpkg")
  .args(args)
  .stdout(Stdio::piped())
  .output()
  .unwrap();
String::from_utf8(output.stdout).unwrap().trim().to_string()