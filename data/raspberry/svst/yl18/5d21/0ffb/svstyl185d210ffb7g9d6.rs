let mut cmd = DataArray::new();
cmd.push_string("sudo");
cmd.push_string("apt-get");
cmd.push_string("update");
cmd.push_string("--allow-releaseinfo-change");
system_call(cmd)