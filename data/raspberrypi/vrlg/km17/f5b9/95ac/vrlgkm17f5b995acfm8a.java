String[] cmd = { "apt-key", "adv", "--keyserver", "hkp://keyserver.ubuntu.com", "--recv-keys", key };
Process proc = Runtime.getRuntime().exec(cmd);
String[] sa = BotUtil.systemCall(proc, null, -1);
JSONObject jo = new JSONObject();
jo.put("out", sa[0]);
jo.put("err", sa[1]);
return jo;