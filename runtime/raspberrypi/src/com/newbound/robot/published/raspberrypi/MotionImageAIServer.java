package com.newbound.robot.published.raspberrypi;

import com.newbound.net.mime.Base64Coder;
import com.newbound.net.mime.MultipartUtility;
import com.newbound.robot.BotUtil;
import com.newbound.robot.Callback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class MotionImageAIServer implements Motion
{
	String charset = "UTF-8";
	String requestURL = "http://localhost:8080/upload";
	JSONArray lastObjects = new JSONArray();
	JSONArray event = null; // The current motion event, if any
	long lastwhen = 0;
	Callback ONEVENTBEGIN = null; // Called when motion first detected
	Callback ONEVENTEND = null; // Called after a motion detection event has ended
	int PADMILLIS = 30000; // FIXME - Make configurable

	public void init(Properties p, Callback onEventBegin, Callback onEventEnd)
	{
		ONEVENTBEGIN = onEventBegin;
		ONEVENTEND = onEventEnd;

		requestURL = p.getProperty("imageai-url", requestURL);
	}

	@Override
	public JSONObject settings(JSONObject jo) throws Exception
	{
		jo.put("imageai-url", requestURL);
		return jo;
	}

	public boolean processImage(File f, long time, int index) throws IOException // Compare this file to the last and return true if motion is detected
	{
		MultipartUtility multipart = new MultipartUtility(requestURL, charset);

		multipart.addHeaderField("User-Agent", "Newbound");
		multipart.addFilePart("file", f);

		List<String> response = multipart.finish();
		String val = "";
		for (String line : response) {
			val += line;
		}
		JSONObject result = new JSONObject(val);
		JSONArray ja = result.getJSONArray("objects");;
		int l1 = lastObjects.length();
		int l2 = ja.length();
		boolean motion = l1 != l2;
		if (!motion)
		{
			// NOTE: This assumes that finding the same objects in a different order is sufficient to constitute motion
			for (int i=0; i<l1; i++)
			{
				JSONObject obj1 = lastObjects.getJSONObject(i);
				JSONObject obj2 = ja.getJSONObject(i);
				if (!obj1.getString("name").equals(obj2.getString("name")))
				{
					motion = true;
					break;
				}

				JSONArray b1 = obj1.getJSONArray("box_points");
				JSONArray b2 = obj2.getJSONArray("box_points");
				double olap = overlap(b1, b2);
				if (olap<0.8) // FIXME - make configurable
				{
					motion = true;
					break;
				}
			}
		}
		lastObjects = ja;

		if (motion)
		{
			// Build the metadata
			JSONObject jo = new JSONObject();
			jo.put("time", time);
			jo.put("index", index);
			jo.put("detected", ja);

			File f2 = new File(f.getParentFile(), index+"_detected.jpg");
			byte[] ba = Base64Coder.decode(result.getString("img"));
			BotUtil.writeFile(f2, ba);
			// FIXME - Motion should not understand file system hierarchy
			String path = f2.getCanonicalPath();
			int i = path.indexOf("generated");
			if (i>0) path = path.substring(i);
			jo.put("img", path);

			if (event == null) // Start a new event
			{
				event = new JSONArray();
				if (ONEVENTBEGIN != null)
					ONEVENTBEGIN.execute(jo); // Call the external event begin callback if there is one
			}
			event.put(jo);
			motion = true;
			lastwhen = time;
		} else if ((event != null) && (time - lastwhen > PADMILLIS)) // No motion, so conclude the current event if there is one
		{
			JSONObject jo = new JSONObject();
			jo.put("list", event);
			if (ONEVENTEND != null) ONEVENTEND.execute(jo);

			event = null;
		}


		return motion;
	}

	private double overlap(JSONArray box0, JSONArray box1)
	{
		double l0 = box0.getInt(0);
		double t0 = box0.getInt(1);
		double r0 = box0.getInt(2);
		double b0 = box0.getInt(3);

		double l1 = box1.getInt(0);
		double t1 = box1.getInt(1);
		double r1 = box1.getInt(2);
		double b1 = box1.getInt(3);

		if ((l0 > r1 || l1 > r0) || (b0 < t1 || b1 < t0)) return 0d;

		double areaOverlap = (Math.max(l0,l1)-Math.min(r0,r1))*(Math.max(t0,t1)-Math.min(b0,b1));
		double area0 = (r0 - l0) * (b0 - t0);
		double area1 = (r1 - l1) * (b1 - t1);

		return areaOverlap / (area0 + area1 - areaOverlap);
	}
}
