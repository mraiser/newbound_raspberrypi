String[] cmd = { "apt-get", "update", "--allow-releaseinfo-change" };
Process proc = Runtime.getRuntime().exec(cmd);
String[] sa = BotUtil.systemCall(proc, null, -1);
if (sa[1].startsWith("E: Command line option --allow-releaseinfo-change")){
  cmd = new String[] { "apt-get", "update" };
  proc = Runtime.getRuntime().exec(cmd);
  sa = BotUtil.systemCall(proc, null, -1);
}
JSONObject jo = new JSONObject();
jo.put("out", sa[0]);
jo.put("err", sa[1]);
return jo;