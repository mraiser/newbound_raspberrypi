use ndata::dataobject::*;
use ndata::dataarray::DataArray;
use flowlang::flowlang::system::system_call::system_call;

pub fn execute(_o: DataObject) -> DataObject {
let ax = list_available();
let mut o = DataObject::new();
o.put_object("a", ax);
o
}

pub fn list_available() -> DataObject {
let mut cmd = DataArray::new();
cmd.push_string("sudo");
cmd.push_string("apt-get");
cmd.push_string("--just-print");
cmd.push_string("upgrade");
system_call(cmd)
}

