let mut cmd = DataArray::new();
cmd.push_string("sudo");
cmd.push_string("apt-get");
cmd.push_string("upgrade");
cmd.push_string("-y");
system_call(cmd)