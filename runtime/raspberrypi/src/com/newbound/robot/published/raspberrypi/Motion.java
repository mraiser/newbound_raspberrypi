package com.newbound.robot.published.raspberrypi;

// Bare-bones noise-canceling motion detection.
// Just instantiate and start passing video files into into the processImage function.
// Dependencies:
//   Newbound Network http://newboundnetwork.com
//   JSON-java https://github.com/stleary/JSON-java

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

import com.newbound.robot.Callback;
import com.newbound.util.RollingAverage;

public class Motion 
{
	BufferedImage last = null; // The last image we processed
	JSONArray event = null; // The current motion event, if any
	long lastwhen = 0;

	int SIZEX, SIZEY, MAX; // Width, height, delta threshold
	double FACTOR; // noise cancellation factor

	Callback ONEVENTBEGIN = null; // Called when motion first detected
	Callback ONEVENTEND = null; // Called after a motion detection event has ended
		
	int[][][] LAST, DELTA, DELTA2;
	
	int MAPFRAMES;
	int PADMILLIS = 30000; // FIXME - Make configurable
	RollingAverage[][][] MAP;
	
	public Motion(int width, int height, double tolerance, double noisefactor, Callback onEventBegin, Callback onEventEnd) 
	{
		this(width, height, tolerance, noisefactor, 10, onEventBegin, onEventEnd);
	}
	
	public Motion(int width, int height, double tolerance, double noisefactor, int nummapframes, Callback onEventBegin, Callback onEventEnd) 
	{
		SIZEX = width;
		SIZEY = height;
		MAX = (int)(tolerance * 256);
		FACTOR = noisefactor;
		
		ONEVENTBEGIN = onEventBegin;
		ONEVENTEND = onEventEnd;
		
		LAST = new int[SIZEX][SIZEY][3];
		MAP = new RollingAverage[SIZEX][SIZEY][3];
		DELTA = new int[SIZEX][SIZEY][3];
		DELTA2 = new int[SIZEX][SIZEY][3];
		
		MAPFRAMES = nummapframes;
		
		int x = width;
		while (x-->0) 
		{
			int y = height;
			while (y-->0) 
			{
				int i = 3;
				while (i-->0) MAP[x][y][i] = new RollingAverage(MAPFRAMES, 0);
			}
		}
	}

	public boolean processImage(File f, long time, int index) throws IOException // Compare this file to the last and return true if motion is detected
	{
		BufferedImage src = ImageIO.read(f); // Read the image file
		
		// Resize it for scanning
		BufferedImage next = new BufferedImage(SIZEX, SIZEY, BufferedImage.TYPE_INT_RGB);
		Graphics gg = next.createGraphics();
		gg.drawImage(src, 0, 0, SIZEX, SIZEY, null);
		gg.dispose();
		
		int score = 0; // signal, motion detection score
		int adj = 0; // signal, motion detection score
		int noise = 0; // noise
		
		for (int x=0;x<SIZEX;x++) for (int y=0;y<SIZEY;y++) // Process every pixel and calculate sums of signal and noise
		{
			// Get current raw rgb value of pixel
			int raw = next.getRGB(x, y);
			int r = (raw)&0xFF;
			int g = (raw>>8)&0xFF;
			int b = (raw>>16)&0xFF;
			int[] rgb2 = new int[] { r, g, b }; 
			
			RollingAverage[] map = MAP[x][y]; // Get the noise level for this pixel
			
			int i = 3;
			while (i-->0)
			{
				DELTA[x][y][i] = Math.abs(LAST[x][y][i] - rgb2[i]); // Calculate the amount of shift for each pixel
				DELTA2[x][y][i] = Math.max(DELTA[x][y][i]-(int)(FACTOR*map[i].value()), 0); // Adjust shift down by map value
				map[i].add(DELTA[x][y][i]);
				
				score += DELTA[x][y][i];
				adj += DELTA2[x][y][i];
				noise += map[i].value();
			}
			
			LAST[x][y] = rgb2;
		}
		
		boolean motion = false;  // Has motion been detected?
		
		// Normalize by number of pixels
		int count = SIZEX*SIZEY; // number of pixels
		score /= count; // signal, motion detection score
		adj /= count; // adjusted score
		noise /= count; // noise

		try {
			if (adj > MAX) // If signal is greater than noise by more than MAX, we have motion
			{
				// Build the metadata
				JSONObject jo = new JSONObject();
				jo.put("time", time);
				jo.put("index", index);
				jo.put("score", score);
				jo.put("adjusted", adj);
				jo.put("noise", noise);

				if (event == null) // Start a new event
				{
					event = new JSONArray();
					if (ONEVENTBEGIN != null)
						ONEVENTBEGIN.execute(jo); // Call the external event begin callback if there is one
				}
				event.put(jo);
				motion = true;
			} else if (event != null && time - lastwhen > PADMILLIS) // No motion, so conclude the current event if there is one
			{
				JSONObject jo = new JSONObject();
				jo.put("list", event);
				if (ONEVENTEND != null) ONEVENTEND.execute(jo);

				event = null;
			}
		}
		catch (Exception x) { x.printStackTrace(); }
		
		last = next;
		lastwhen = time;
		
		return motion;
	}
	
	// The runtime buffered images used to detect motion are pretty cool to look at-- the following functions will generate those images for you. Just call after processImage.

	public BufferedImage lastImage() { return toImage(LAST); }
	public BufferedImage lastDelta() { return toImage(DELTA); }
	public BufferedImage lastDelta2() { return toImage(DELTA2); }
	public BufferedImage lastMap() { return toImage(MAP); }

	public BufferedImage toImage(int[][][] bytes)
	{
		BufferedImage newImage = new BufferedImage(SIZEX, SIZEY, BufferedImage.TYPE_INT_RGB);
		for (int x=0;x<SIZEX;x++) for (int y=0;y<SIZEY;y++) newImage.setRGB(x, y, ((bytes[x][y][2] & 0xFF) << 16) + ((bytes[x][y][1] & 0xFF) << 8) + (bytes[x][y][0] & 0xFF));
		return newImage;
	}
	
	public BufferedImage toImage(RollingAverage[][][] bytes)
	{
		BufferedImage newImage = new BufferedImage(SIZEX, SIZEY, BufferedImage.TYPE_INT_RGB);
		for (int x=0;x<SIZEX;x++) for (int y=0;y<SIZEY;y++) newImage.setRGB(x, y, (((int)bytes[x][y][2].value() & 0xFF) << 16) + (((int)bytes[x][y][1].value() & 0xFF) << 8) + ((int)bytes[x][y][0].value() & 0xFF));
		return newImage;
	}

}
