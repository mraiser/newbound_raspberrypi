String[] call = { "vcgencmd", "measure_temp" };
String[] sa = BotUtil.systemCall(call);

String msg = sa[0].trim()+sa[1].trim();
return msg;