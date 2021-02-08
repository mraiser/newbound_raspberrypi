package com.newbound.robot.published.raspberrypi;

import com.newbound.robot.Callback;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public interface Motion
{
	public void init(Properties p, Callback onEventBegin, Callback onEventEnd); // FIXME - Motion should not handle/generate events
	public JSONObject settings(JSONObject jo) throws Exception;
	public boolean processImage(File f, long time, int index) throws IOException;
}
