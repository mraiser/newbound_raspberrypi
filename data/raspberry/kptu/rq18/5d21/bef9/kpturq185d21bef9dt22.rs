let mut cmd = DataArray::new();
cmd.push_string("sudo");
cmd.push_string("apt-get");
cmd.push_string("--just-print");
cmd.push_string("upgrade");
system_call(cmd)