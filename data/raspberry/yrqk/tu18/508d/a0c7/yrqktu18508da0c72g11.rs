let mut o = DataObject::new();

o.put_string("system", &device()); 
o.put_object("cpu", cpu());
o.put_object("memory", memory());
o.put_array("disks", disks());
o.put_object("temp", temp());
o.put_object("os", os());
o.put_string("arch", &arch());

o