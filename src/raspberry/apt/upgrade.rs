use ndata::dataobject::*;
use ndata::dataarray::DataArray;
use flowlang::flowlang::system::system_call::system_call;

pub fn execute(_o: DataObject) -> DataObject {
let ax = upgrade();
let mut o = DataObject::new();
o.put_object("a", ax);
o
}

pub fn upgrade() -> DataObject {
let mut cmd = DataArray::new();
cmd.push_string("sudo");
cmd.push_string("apt-get");
cmd.push_string("upgrade");
cmd.push_string("-y");
system_call(cmd)
}

