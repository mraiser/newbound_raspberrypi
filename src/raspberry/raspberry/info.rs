use ndata::dataobject::*;
use crate::raspberry::raspberry::device::device;
use crate::raspberry::raspberry::cpu::cpu;
use crate::raspberry::raspberry::memory::memory;
use crate::raspberry::raspberry::disks::disks;
use crate::raspberry::raspberry::temp::temp;
use crate::raspberry::raspberry::os::os;
use crate::raspberry::raspberry::arch::arch;

pub fn execute(_o: DataObject) -> DataObject {
let ax = info();
let mut o = DataObject::new();
o.put_object("a", ax);
o
}

pub fn info() -> DataObject {
let mut o = DataObject::new();

o.put_string("system", &device()); 
o.put_object("cpu", cpu());
o.put_object("memory", memory());
o.put_array("disks", disks());
o.put_object("temp", temp());
o.put_object("os", os());
o.put_string("arch", &arch());

o
}

