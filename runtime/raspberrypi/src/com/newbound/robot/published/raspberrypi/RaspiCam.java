package com.newbound.robot.published.raspberrypi;

// DVR implementation based on raspivid and ffmpeg
// Everything is static for easy access and because underlying services are at the system level
// Dependencies:
//     ffmpeg, http://ffmpeg.org
//     raspivid, https://www.raspberrypi.org/documentation/usage/camera/raspicam/raspivid.md
//     Newbound Network, http://newboundnetwork.com
//     JSON-java, https://github.com/stleary/JSON-java

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import com.newbound.robot.BotBase;
import com.newbound.robot.BotUtil;
import com.newbound.robot.Callback;
import com.newbound.thread.PeriodicTask;

public class RaspiCam 
{
    // CONFIGURABLE SETTINGS
    private static boolean START = false; // Start this service on init
    private static boolean STARTRV = false; // Start the raspivid process on init
    private static boolean STARTMOT = false; // Enable motion detection on init
    private static boolean NOTIFY = false; // Send notifications to Ninja Smoke app
    
    private static String RVC = "raspivid"; // The raspivid command. The system command line call used to start up the raspivid service. Use full path if not in system path.

    private static File CAP = null; // The directory raspivid should store captured video in
    private static File ARC = null; // The directory this service should move captured video to
    private static File HTM = null;  // The directory this service should output generated files to
    
    private static int WIDTH = 640; // The width of the video raspivid should capture
    private static int HEIGHT = 480; // The height of the video raspivid should capture
    private static int ROT = 0; // The number of degrees raspivid should rotate the captured video
    private static int FPS = 10; // The number of frames per second at which raspivid should capture video

    private static int MOD = 5; // Motion detection sample modulus. This service will check for motion once in every MOD number of video segments generated by raspivid, so approximately every MOD x 2 seconds.
    private static int MOTX = 64; // Width of the samples used for motion detection
    private static int MOTY = 48; // Height of the samples used for motion detection
    private static double TOLERANCE = 0.05; // Tolerance. Percent change that indicates motion
    private static double FACTOR = 2;
    private static int CAPDAYS = 2; // Number of days worth of video to store locally
    
    // RUNTIME VARIABLES
    private static boolean ON = false; // Is this service running?
    private static Process RVP = null; // The system process running raspivid
    private static Motion MOT = null; // The motion detector
    private static Callback OEB = null; // Call this when a motion detection event begins
    private static Callback OEE = null; // Call this after a motion detection event has ended
    private static String RAW = "264";
    
    public static void init(Properties p, Callback onEventBegin, Callback onEventEnd) throws Exception
    {
        // Load all properties into local attributes
        
        START = p.getProperty("start", "false").equals("true");

        File rpdir = BotBase.getBot("raspberrypi").getRootDir();

        if (p.getProperty("capture") != null) CAP = new File(p.getProperty("capture"));
        else CAP = new File(rpdir, "capture");
        if (p.getProperty("archive") != null) ARC = new File(p.getProperty("archive"));
        else ARC = new File(rpdir, "archive");
        if (p.getProperty("html") != null) HTM = new File(p.getProperty("html"));
        else HTM = new File(rpdir, "html");

        OEB = onEventBegin;
        OEE = onEventEnd;
        
        WIDTH = Integer.parseInt(p.getProperty("width", ""+WIDTH));
        HEIGHT = Integer.parseInt(p.getProperty("height", ""+HEIGHT));
        ROT = Integer.parseInt(p.getProperty("rotation", ""+ROT));
        FPS = Integer.parseInt(p.getProperty("fps", ""+FPS));
        CAPDAYS = Integer.parseInt(p.getProperty("days", ""+CAPDAYS));

        MOD = Integer.parseInt(p.getProperty("motion-modulus", ""+MOD));

        MOTX = Integer.parseInt(p.getProperty("motion-width", ""+MOTX));
        MOTY = Integer.parseInt(p.getProperty("motion-height", ""+MOTY));
        
        TOLERANCE = Double.parseDouble(p.getProperty("motion-tolerance", ""+TOLERANCE));
        FACTOR = Double.parseDouble(p.getProperty("noise-factor", ""+FACTOR));

        RVC = p.getProperty("raspivid", RVC);

        STARTMOT = p.getProperty("motion-detection", "false").equals("true");
        STARTRV = p.getProperty("start-raspivid", "false").equals("true");
        NOTIFY = p.getProperty("notify", "true").equals("true");
        
        // Create directories and delete any stale video
//        BotUtil.deleteDir(CAP);
        CAP.mkdirs();
        ARC.mkdirs();
        HTM.mkdirs();
        
        if (START) // This service starting up
        {
            if (STARTMOT) // Start up motion detection
            {
                MOT = new Motion(MOTX, MOTY, TOLERANCE, FACTOR, onEventBegin(), onEventEnd());
            }           
                
            if (STARTRV) // Start the raspivid system process
            {
                try
                {
                	if (RVC.startsWith("ffmpeg") || RVC.equals("avfoundation")) RAW = "mp4";
                		
                    String[] list = CAP.list();
                    int i = list.length;
                    while (i-->0) if (list[i].endsWith("."+RAW)) new File(CAP, list[i]).delete(); 

                	String[] cmd;
                    if (RVC.equals("ffmpeg"))
                    {
                        String t = "transpose=none:landscape";
                        if (ROT != 0){
                            if (ROT == 90) t = "transpose=1:landscape";
                            else if (ROT == 180) t = "transpose=2,transpose=2:landscape";
                            else if (ROT == 270) t = "transpose=2:landscape";
                        }
//                    	cmd = new String[] { "ffmpeg", "-f", "v4l2", "-framerate", ""+FPS, "-channel", "0", "-video_size", WIDTH+"x"+HEIGHT, "-i", "/dev/video0", "-pix_fmt", "nv12", "-c:v", "cedrus264", "-segment_time", "2", "-f", "segment", CAP.getCanonicalPath()+"/%d.mp4" };
                        cmd = new String[] { "ffmpeg", "-f", "v4l2", "-framerate", ""+FPS, "-channel", "0", "-video_size", WIDTH+"x"+HEIGHT, "-i", "/dev/video0", "-c:v", "libx264", "-crf", "22", "-map", "0", "-segment_time", "2", "-g", "2", "-sc_threshold", "0", "-force_key_frames", "expr:gte(t,n_forced*2)", "-vf", t, "-f", "segment", CAP.getCanonicalPath()+"/%d.mp4" };
                    }
                    else if (RVC.equals("ffmpeg/h264_omx"))
                    {
                        // ffmpeg -f v4l2 -c:v mjpeg -i /dev/video0 -c:v copy -segment_time 2 -g 2 -sc_threshold 0 -f segment /root/Newbound/runtime/raspberrypi/capture/%d.mp4
                        cmd = new String[] { "ffmpeg", "-f", "v4l2", "-c:v", "mjpeg", "-i", "/dev/video0", "-c:v", "h264_omx", "-segment_time", "2", "-g", "2", "-sc_threshold", "0", "-f", "segment", CAP.getCanonicalPath()+"/%d.mp4" };
                    }
                    else if (RVC.equals("ffmpeg/mjpeg"))
                	{
                	    // ffmpeg -f v4l2 -c:v mjpeg -i /dev/video0 -c:v copy -segment_time 2 -g 2 -sc_threshold 0 -f segment /root/Newbound/runtime/raspberrypi/capture/%d.mp4
                    	cmd = new String[] { "ffmpeg", "-f", "v4l2", "-c:v", "mjpeg", "-i", "/dev/video0", "-c:v", "copy", "-segment_time", "2", "-g", "2", "-sc_threshold", "0", "-f", "segment", CAP.getCanonicalPath()+"/%d.mp4" };
                	}
                	else if (RVC.equals("avfoundation"))
                	{
                    	cmd = new String[] { "ffmpeg", "-f", "avfoundation", "-framerate", ""+FPS, "-video_size", WIDTH+"x"+HEIGHT, "-i", "0:0", "-vf", "rotate=\""+ROT+"*(PI/180)\"", "-pix_fmt", "nv12", "-segment_time", "2", "-f", "segment", CAP.getCanonicalPath()+"/%d.mp4" };
                	}
                	else
                	{
	                    int g = (FPS-1)*2; // Forces raspivid generated video segments to be as close to two seconds as possible. Because FPS/2 with a "-sg 2000" in the command line didn't seem to work right. YMMV.
	
	                    // The full command line plus arguments to start up raspivid
	                    cmd = new String[] { RVC, "-fps", ""+FPS, "-g", ""+g, "-sg", "1000", "-t", "0", "-rot", ""+ROT, "-w", ""+WIDTH, "-h", ""+HEIGHT, "-o", CAP.getCanonicalPath()+"/%d.264" };
                	}

                    String s = "";
                    for (i=0;i<cmd.length;i++) s += cmd[i]+" ";
                    System.out.println(s);
                      
                    RVP = Runtime.getRuntime().exec(cmd);
                    
                    new Thread(new Runnable() {
						
						@Override
						public void run() 
						{
							int i = 1;
							InputStream is = RVP.getInputStream();
							try
							{
								while (i != -1) 
								{
									i = is.read();
									System.out.print(i);
								}
							}
							catch (Exception x) { x.printStackTrace(); }
						}
					}).start();
                    
                    new Thread(new Runnable() {
						
						@Override
						public void run() 
						{
							int i = 1;
							InputStream is = RVP.getErrorStream();
							try
							{
								while (i != -1) i = is.read(); 
							}
							catch (Exception x) { x.printStackTrace(); }
						}
					}).start();
                }
                catch (Exception x) { x.printStackTrace(); }
            }

            if (!ON) // Because we don't turn "OFF" on restart and we don't want multiples
            {
                BotBase.getBot("botmanager").addPeriodicTask(killer());
                BotBase.getBot("botmanager").addPeriodicTask(mover());
            }
        }
        ON = START;
    }
    
    
    public static JSONObject settings() throws Exception // Everything you could possibly want to know about how this process is configured
    {
        JSONObject jo = new JSONObject();
        
        jo.put("start", START);
        jo.put("start-raspivid", STARTRV);
        jo.put("motion-detection", STARTMOT);
        jo.put("notify", NOTIFY);

        jo.put("capture", CAP.getCanonicalPath());
        jo.put("archive", ARC.getCanonicalPath());
        jo.put("html", HTM.getCanonicalPath());
        
        jo.put("raspivid-command", "raspivid");

        jo.put("rotation", ROT);
        jo.put("fps", FPS);
        jo.put("width", WIDTH);
        jo.put("height", HEIGHT);
        
        jo.put("motion-modulus", MOD);
        jo.put("motion-capturedays", CAPDAYS);
        jo.put("motion-tolerance", TOLERANCE);
        jo.put("noise-factor", FACTOR);
        jo.put("motion-width", MOTX);
        jo.put("motion-height", MOTY);
        
        return jo;
    }
    
    public static void restart(Properties p) throws Exception // Restart this service with the existing event callbacks
    {
        restart(p, OEB, OEE);
    }
    
    public static void restart(Properties p, Callback a, Callback b) throws Exception // Restart this service with new event callbacks
    {
        try { RVP.destroy(); } catch (Exception x) {}
        init(p, a, b);
    }
    
    public static void shutdown() // Stop this service
    {
        ON = false;
        try { RVP.destroy(); } catch (Exception x) {}
    }
    
    private static Callback onEventBegin() // Internal callback. Called when motion is first detected
    {
        return OEB; // We ignore this. Call the External callback if one exists
    }
    
    private static Callback onEventEnd() // Internal callback. Called after a motion detection event has ended
    {
        return new Callback() 
        {
            public void execute(JSONObject result) 
            {
                try
                {
                    JSONArray ja = result.getJSONArray("list"); // The list of video segments in this event

                    int n = ja.length();
                    long start = ja.getJSONObject(0).getLong("time") - 2000; // Start time in milliseconds of this event
                    long stop =  ja.getJSONObject(n-1).getLong("time") + 2000; // End time in milliseconds of this event
                    
//                  JSONObject jo = buildVideo(start, stop, "mp4"); // Generate the event video in the mp4 format
                    String name = timestamp(start)+"_"+timestamp(stop)+".mp4"; //jo.getString("name"); // The name of the generated mp4 event video
                    String id = name.substring(0,  name.lastIndexOf('.')); // The generated event ID

                    // Move the event video
                    File f = new File(HTM, "event");
                    f.mkdirs();
                    f = new File(f, name);
                    File f2 = new File(HTM, "generated");
                    f2 = new File(f2, "mp4");
                    f2 = new File(f2, name);
                    f2.renameTo(f);
                    
                    // Store the event metadata
                    result.put("mp4", name);
                    result.put("time", start);
                    
                    f = new File(HTM, "event");
                    f.mkdirs();
                    f = new File(f, id+".json");
                    BotUtil.writeFile(f, result.toString().getBytes());
                    
                    // Add event to the day's list of events
                    f = getGenerated(start, "txt").getParentFile().getParentFile().getParentFile();
                    f.mkdirs();
                    f = new File(f, "events.txt");
                    FileWriter fw = new FileWriter(f, true);
                    fw.write(start+"="+id+"\n");
                    fw.close();
                    
                    // Call the external event callback if one exists
                    if (OEE != null) OEE.execute(result);
                }
                catch (Exception x) { x.printStackTrace(); }
            }
        };
    }

    // Generate a snapshot from captured video at approximately the designated time
    // NOTE: We grab the first frame of the segment containing the designated time. Since segment sizes are approximately 2 seconds long, this is not terribly precise. 
    // ffmpeg is capable of seeking to the correct frame, but the overhead on the CPU is probably not worth it. YMMV.
    
    public static File jpeg(Long time) throws Exception 
    {
        long otime = time;
        File f2 = getGenerated(time, "jpg");
        while (!f2.exists() && otime - time < 60000) { time -= 1000; f2 = getGenerated(time, "jpg"); }
/*
        if (!f2.exists())
        {
            File f = getArchived(time, RAW);
            
            String[] cmd = {"ffmpeg", "-y", "-i", f.getCanonicalPath(), "-vframes", "1", "-s", WIDTH+"x"+HEIGHT, "-f", "image2", f2.getCanonicalPath()};
              
            String s = "";
            for (int i=0;i<cmd.length;i++) s += cmd[i]+" ";
            System.out.println(s);
              
            InputStream is = null;
            Process proc = Runtime.getRuntime().exec(cmd);
            String[] sa = BotUtil.systemCall(proc, is, -1);
            s = sa[0]+"/"+sa[1];
              
            System.out.println(s);
        }
*/
        return f2;
    }
    
    public static JSONObject events(long start, long stop) throws Exception // Return the list of events detected between the designated start and stop times
    {
        long oneday = 1000 * 60 * 60 * 24;
        stop += oneday;
        long time = start;
        
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();
        jo.put("list", ja);
        while (time<stop)
        {
            File f = getGenerated(time, "txt").getParentFile().getParentFile().getParentFile();
            f = new File(f, "events.txt");
            if (f.exists())
            {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String oneline = null;
                while ((oneline = br.readLine()) != null) try
                {
                    int i = oneline.indexOf("=");
//                  long t2 = Long.parseLong(oneline.substring(0, i));
                    String id = oneline.substring(i+1);
                    
                    f = new File(HTM, "event");
                    f = new File(f, id+".json");
                    if (f.exists())
                    {
                        JSONObject jo2 = new JSONObject(new String(BotUtil.readFile(f)));
                        ja.put(jo2);
                    }
                }
                catch (Exception x) { x.printStackTrace(); }
                br.close();
            }
            time += oneday;
        }
        return jo;
    }
    
    public static JSONObject info() throws Exception // Everything you could possibly want to know about the state of this process and how it is configured
    {
        JSONObject jo = new JSONObject();
        jo.put("on", ON);
        jo.put("first", first());
        jo.put("last", last());
        jo.put("settings", settings());

        return jo;
    }
    
    public static JSONObject first() throws Exception // Returns the earliest detected video on disk
    {
        File dst = new File(HTM, "generated");
        dst = new File(dst, "jpg");
        
        if (!dst.exists()) return null;
        
        Calendar d = Calendar.getInstance();

        int year = d.get(d.YEAR);
        int month = 12;
        int day = 31;
        int hour = 23;
        int minute = 59;
        int index = Integer.MAX_VALUE;

        String[] list = dst.list();
        int i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j<year) year = j; } catch (Exception x) {}

        dst = new File(dst, ""+year);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j<month) month = j; } catch (Exception x) {}

        dst = new File(dst, ""+month);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j<day) day = j; } catch (Exception x) {}

        dst = new File(dst, ""+day);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j<hour) hour = j; } catch (Exception x) {}

        dst = new File(dst, ""+hour);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j<minute) minute = j; } catch (Exception x) {}

        dst = new File(dst, ""+minute);
        list = dst.list();
        i = list.length*5;
        while (i-->0) try { int j = Integer.parseInt(list[i].substring(0, list[i].lastIndexOf("."))); if (j<index) index = j; } catch (Exception x) {}

        dst = new File(dst, index+".jpg");

        d.set(d.YEAR, year);
        d.set(d.MONTH, month-1);
        d.set(d.DATE, day);
        d.set(d.HOUR_OF_DAY, hour);
        d.set(d.MINUTE, minute);

        long time = d.getTime().getTime();

        JSONObject jo = new JSONObject();
        jo.put("path", dst.getCanonicalPath());
        jo.put("time", time);
        jo.put("index", index);

        return jo;
    }
    
    public static JSONObject last() throws Exception // Returns the most recent detected video on disk
    {
        File dst = new File(HTM, "generated");
        dst = new File(dst, "jpg");
        
        if (!dst.exists()) return null;
        
        Calendar d = Calendar.getInstance();

        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int minute = 0;
        int index = 0;

        String[] list = dst.list();
        int i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j>year) year = j; } catch (Exception x) {}

        dst = new File(dst, ""+year);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j>month) month = j; } catch (Exception x) {}

        dst = new File(dst, ""+month);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j>day) day = j; } catch (Exception x) {}

        dst = new File(dst, ""+day);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j>hour) hour = j; } catch (Exception x) {}

        dst = new File(dst, ""+hour);
        list = dst.list();
        i = list.length;
        while (i-->0) try { int j = Integer.parseInt(list[i]); if (j>minute) minute = j; } catch (Exception x) {}

        dst = new File(dst, ""+minute);
        list = dst.list();
        i = list.length*5;
        while (i-->0) try { int j = Integer.parseInt(list[i].substring(0, list[i].lastIndexOf("."))); if (j>index) index = j; } catch (Exception x) {}

        dst = new File(dst, index+".jpg");

        d.set(d.YEAR, year);
        d.set(d.MONTH, month-1);
        d.set(d.DATE, day);
        d.set(d.HOUR_OF_DAY, hour);
        d.set(d.MINUTE, minute);

        long time = d.getTime().getTime();

        JSONObject jo = new JSONObject();
        jo.put("path", dst.getCanonicalPath());
        jo.put("time", time);
        jo.put("index", index);

        return jo;
    }

    public static int getBacklog() { return CAP.list().length; } // Return the number of unprocessed raspivid generated video segmnents. Backlog x 2 = Approximate length of unprocessed video, which translates directly to lag when viewing in browser
    
    public static File getArchived(long time, String format) { return getFile(ARC, time, format); } // Return the archived video segment of the designated format containing the designated time 
    public static File getGenerated(long time, String format) { return getFile(new File(new File(HTM, "generated"), format), time, format); } // Return the generated file of the designated format containing the designated time

    public static File getFile(File src, long time, String format) // Return the child of the given directory of the designated format for the designated time
    {
        Calendar d = Calendar.getInstance();
        d.setTimeInMillis(time);
        File f2 = new File(src, ""+d.get(d.YEAR));
        f2 = new File(f2, ""+(d.get(d.MONTH)+1));
        f2 = new File(f2, ""+d.get(d.DATE));
        f2 = new File(f2, ""+d.get(d.HOUR_OF_DAY));
        f2 = new File(f2, ""+d.get(d.MINUTE));
        f2.mkdirs();
        
        // NOTE: Because the files are only approximately 2 seconds long, there are slightly more than 30 of them (we get 33 of them). 
        // You could probably optimize this by either hard-coding the 33 or storing the max value and using that.
        
        double n = src.equals(ARC) ? Math.max(30, f2.list().length) : getArchived(time, format).getParentFile().list().length;
        int index =  1+(int)((int)(n * d.get(d.SECOND))/60.0);
        f2 = new File(f2, index+"."+format);

        return f2;
    }

    // Generate a video file of the designated format by asking ffmpeg to assemble the archived video segments captured between the designated start and stop times.
    // Returns a JSONObject containing the video metadata
    public static JSONObject buildVideo(long start, long stop, String format) throws IOException 
    {
        File f4 = new File(HTM, "generated");
        f4 = new File(f4, format);
        f4.mkdirs();
        f4 = new File(f4, timestamp(start)+"_"+timestamp(stop)+"."+format);

        if (!f4.exists())
        {
            long time = start;
            Calendar d = Calendar.getInstance();
            d.setTimeInMillis(time);
    
            File f1;
            File f2;
            File f3 = BotBase.newTempFile();
            FileWriter fw = new FileWriter(f3);
    
            double n = Math.max(30, getArchived(time, RAW).getParentFile().list().length);
            System.out.println("BUILDING VIDEO FROM: "+getArchived(time, RAW).getParentFile());
            int first = 1 + (int)((n * d.get(d.SECOND))/60.0);
            System.out.println("N: "+n+" / first: "+first);
            System.out.println("time: "+time+" / stop: "+stop);
    
            long start2 = -1;
            long stop2 = -1;
    
            while (time < stop)
            {
              d.setTime(new Date(time));
              f2 = getArchived(time, RAW).getParentFile();
              f2.mkdirs();
    
              int n1 = Math.max(31, f2.list().length+1);
              for (int i=first;i<n1;i++)
              {
                  int sec = (60*(i-1))/(n1-1);
                d.set(d.SECOND, sec);
                f1 = new File(f2, i+"."+RAW);
                System.out.println("sec: "+sec+" / file: "+f1.exists()+" "+f1);
                System.out.println("time: "+time+" / stop: "+stop);
    
                if (f1.exists()) 
                {
                  fw.write("file "+f1.getCanonicalPath()+"\n");
                  stop2 = d.getTime().getTime();
                  if (start2 == -1) start2 = stop2;
                }
                if (d.getTime().getTime()>stop) break;
              }
              time += 60000;
              first = 1;
            }
    
            fw.close();
    
            String[] cmd;
            
            if (RVC.equals("ffmpeg") || RVC.equals("ffmpeg/mjpeg"))
            {
                // ffmpeg -r 30 -y -f concat -safe 0 -i segments.txt -c:v libx264 -movflags faststart segments.mp4
                cmd = new String[] {"ffmpeg", "-r", ""+FPS, "-y", "-f", "concat", "-safe", "0", "-i", f3.getCanonicalPath(), "-c:v", "libx264", "-movflags", "faststart", f4.getCanonicalPath()};
            }
            else
            {
            	cmd = new String[] {"ffmpeg", "-r", ""+FPS, "-y", "-f", "concat", "-safe", "0", "-i", f3.getCanonicalPath(), "-c", "copy", "-movflags", "faststart", f4.getCanonicalPath()};
            }
    
            String s = "";
            for (int i=0;i<cmd.length;i++) s += cmd[i]+" ";
            System.out.println(s);
              
            InputStream is = null;
            Process proc = Runtime.getRuntime().exec(cmd);
            String[] sa = BotUtil.systemCall(proc, is, -1);
            s = sa[0]+"/"+sa[1];
    
            System.out.println(s);
    
            f3.delete();
        }
        
        JSONObject jo = new JSONObject();
        try {
            jo.put("name", f4.getName());
            jo.put("start", start);
            jo.put("stop", stop);
        }
        catch (Exception x) { x.printStackTrace(); }

        return jo;
    }

    public static String timestamp(long time) // Generate a timestamp in the format: YYYYMMDDhhmmss
    {
        Calendar d = Calendar.getInstance();
        d.setTimeInMillis(time);
        
        String s = ""+d.get(d.YEAR)+psf(d.get(d.MONTH)+1)+psf(d.get(d.DATE))+psf(d.get(d.HOUR_OF_DAY))+psf(d.get(d.MINUTE))+psf(d.get(d.SECOND));

        return s;
    }

    private static String psf(int i) // Make all numbers two characters long
    {
        return i < 10 ? "0"+i : ""+i;
    }

    static long day = 24l*60l*60l*1000l;
    private static PeriodicTask killer() // Every 15 minutes, delete any files older than the specified number of days on a rolling basis
    {
        PeriodicTask pt = new PeriodicTask(day / 96, true, "raspicam file remover") 
        {
            Calendar d = Calendar.getInstance();
            
            public void run() 
            {
                if (!ON) this.mRepeat = false;
                else 
                {
                    long millis = System.currentTimeMillis()-(CAPDAYS*day);
                    
                    cullOld(millis, ARC);
                    cullOld(millis, new File(HTM, "generated"));
                    cullOld(millis, new File(HTM, "event"));
                }
            }
        };
        
        return pt;

    }
                    
    // Keep moving files from captured to archived until there are no more files to move. Then rest for a second and do it again. This could probably be tweaked for better performance. YMMV.
    // Also, generate snapshots and check for motion if it's time to do so.
    private static PeriodicTask mover() 
    {
        PeriodicTask pt = new PeriodicTask(100, true, "raspicam file mover") 
        {
            Calendar d = Calendar.getInstance();
            
            public void run() 
            {
                if (!ON) this.mRepeat = false; // Stop if this service is "OFF"
                else while (ON && CAP.list().length>1)
                {
                    // Find the first video and last videos to move. This could be eliminated if your filesystem is guaranteed to return files in the order they were generated
                    // Don't move the last file because it might not be done being written to disk.
                    int min = Integer.MAX_VALUE;
                    int max = -1;
    
                    String[] list = CAP.list();
                    int n = list.length;
                    for (int i=0;i<n;i++) try
                    {
                      if (list[i].endsWith("."+RAW))
                      {
                        int j = Integer.parseInt(list[i].substring(0, list[i].lastIndexOf(".")));
                        if (j<min) min = j;
                        if (j>max) max = j;
                      }
                    }
                    catch (Exception x) { x.printStackTrace(); }
    
                    // Move 'em!
                    int index = 0;
                    long time = 0;
                    File f2 = null;
                    File f4 = null;
                    while (min<max) try
                    {
                      File f1 = new File(CAP, min+"."+RAW);
                      File f3 = new File(CAP, (min+1)+"."+RAW);
    
                      if (f1.exists() && f3.exists())
                      {
                        time = f1.lastModified();
                        
                        d.setTimeInMillis(time);
                        f2 = getArchived(f1.lastModified(), RAW).getParentFile();
                        f2.mkdirs();
                        
                        index = f2.list().length+1;
                        f2 = new File(f2, index+"."+RAW);
    
                        f4 = getGenerated(time, "jpg");
                        f4 = new File(f4.getParentFile(), index+".jpg");
/*    
                        if (RVC.equals("ffmpeg"))
                        {
                            String[] cmd = {"ffmpeg", "-i", f1.getCanonicalPath(), "-pix_fmt", "nv12", f2.getCanonicalPath()};
                            
                            InputStream is = null;
                            Process proc = Runtime.getRuntime().exec(cmd);
                            String[] sa = BotUtil.systemCall(proc, is, -1);
                            String s = sa[0]+"/"+sa[1];
                            System.out.println(s);
                        	
                            cmd = new String[]{"rm", "-f", f1.getCanonicalPath()};
                            
                            is = null;
                            proc = Runtime.getRuntime().exec(cmd);
                            sa = BotUtil.systemCall(proc, is, -1);
                            s = sa[0]+"/"+sa[1];
                            System.out.println(s);
                        }
                        else 
*/
                        	f1.renameTo(f2);
                      }
                    }
                    catch (Exception x) { x.printStackTrace(); }
                    finally { min++; }
                    
                    if (f4 != null) try
                    {
                      String[] cmd = {"ffmpeg", "-y", "-i", f2.getCanonicalPath(), "-vframes", "1", "-s", WIDTH+"x"+HEIGHT, "-f", "image2", f4.getCanonicalPath()};

                      String s = "";
                      for (int i=0;i<cmd.length;i++) s += cmd[i]+" ";
                      System.out.println(s);
                      
                      InputStream is = null;
                      Process proc = Runtime.getRuntime().exec(cmd);
                      String[] sa = BotUtil.systemCall(proc, is, -1);
                      s = sa[0]+"/"+sa[1];
                      System.out.println(s);
                      
                      // Check for motion if motion detection is enabled
                      if (MOT != null) MOT.processImage(f4, time, index);
                    }
                    catch (Exception x) { x.printStackTrace(); }
                }
            }
        };
        
        return pt;
    }
    
    public static String rescan() throws Exception
    {
        File lastyear = new File(ARC, Calendar.getInstance().get(Calendar.YEAR)+"");
        
        File year = lastyear;
        while (true)
        {
            File f = new File(lastyear.getParentFile(), ((Integer.parseInt(lastyear.getName())-1)+""));
            if (!f.exists()) break;
            year = f;
        }
        
        while (year.exists())
        {
            scanYear(year);
            year = new File(lastyear.getParentFile(), ((Integer.parseInt(lastyear.getName())-1)+""));
        }
        
        return "OK ";
    }

    private static void scanYear(File year) 
    {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, Integer.parseInt(year.getName()));
        
        for (int m=1;m<13;m++)
        {
            c.set(Calendar.MONTH, m-1);
            File month = new File(year, ""+m);
            if (month.exists()) for (int d=1;d<32;d++)
            {
                c.set(Calendar.DATE, d);
                File day = new File(month, ""+d);
                if (day.exists()) for (int h=0;h<24;h++)
                {
                    c.set(Calendar.HOUR_OF_DAY, h);
                    File hour = new File(day, ""+h);
                    if (hour.exists()) for (int min=0;min<60;min++)
                    {
                        c.set(Calendar.MINUTE, min);
                        File minute = new File(hour, ""+min);
                        if (minute.exists()) 
                        {
                            int n = minute.list().length+1;
                            for (int i=1;i<n;i++)
                            {
                                int second = (int)(((double)i)/((double)n-1d)*60);
                                c.set(Calendar.SECOND, second);
                                File f = new File(minute, i+"."+RAW);
                                if (f.exists()) try
                                {
                                    long time = c.getTimeInMillis();
                                    File f4 = getGenerated(time, "jpg");
                                    if (!f4.exists())
                                    {
                                        f4.getParentFile().mkdirs();
                                        String[] cmd = {"ffmpeg", "-y", "-i", f.getCanonicalPath(), "-vframes", "1", "-s", WIDTH+"x"+HEIGHT, "-f", "image2", f4.getCanonicalPath()};
                                        
                                        String s = "";
                                        for (int ii=0;ii<cmd.length;ii++) s += cmd[ii]+" ";
                                        System.out.println(s);
                                        
                                        InputStream is = null;
                                        Process proc = Runtime.getRuntime().exec(cmd);
                                        String[] sa = BotUtil.systemCall(proc, is, -1);
                                        s = sa[0]+"/"+sa[1];
                                        System.out.println(s);
                                    }                                   
                                    // Check for motion if motion detection is enabled
                                    if (MOT != null) MOT.processImage(f4, time, second);
                                    
                                }
                                catch (Exception x) { x.printStackTrace(); }
                            }
                        }
                    }
                }
            }
        }
    }


    private static void cullOld(long millis, File root) // Remove files and sub-folders older than designated storage period
    {
        cullOld(root, root, millis);
    }

    private static void cullOld(File root, File f, long millis) // Remove files older than designated storage period and delete this subfolder if it is empty
    {
      String[] list = f.list();
      int i = list.length;
      while (i-->0)
      {
        File f2 = new File(f,list[i]);
        if (f2.isDirectory()) cullOld(root, f2, millis);
        else if (f2.lastModified()<millis) f2.delete();
      }
      if (!f.equals(root) && f.list().length == 0) BotUtil.deleteDir(f);
    }

    public static JSONObject timelapse(Long start, Long stop, Long interval, int fps) throws Exception {
        String name = start+"_"+stop+"_"+interval+"_"+fps;
//        File p = getGenerated(start, "timelapse").getParentFile();
//        File txt = new File(p, name+".txt");
//        File mp4 = new File(p, name+".mp4");
        File txt = BotBase.newTempFile();
        File mp4 = BotBase.getTempFile(txt.getName()+".mp4");
        mp4.deleteOnExit();
        FileWriter fw = new FileWriter(txt);
        long when = start;
        while (when < stop)
        {
            File next = jpeg(when);
            fw.write("file '"+next.getCanonicalPath()+"'\n");
            when += interval;
        }
        fw.close();

        // ffmpeg -f image2pipe -framerate 30 -i - -s 640x480 timelapse.mp4
        String[] cmd = { "ffmpeg", "-f", "concat", "-safe", "0", "-i", txt.getCanonicalPath(), "-s", "640x480", "-framerate", ""+fps, mp4.getCanonicalPath() };
        String s = "";
        for (int i=0;i<cmd.length;i++) s += cmd[i]+" ";
        System.out.println(s);

        InputStream is = null;
        Process proc = Runtime.getRuntime().exec(cmd);
        String[] sa = BotUtil.systemCall(proc, is, -1);
        s = sa[0]+"/"+sa[1];

        System.out.println(s);

        JSONObject jo = new JSONObject();
        jo.put("status", "ok");
        jo.put("msg", mp4.getName());
        jo.put("len", mp4.length());

        return jo;
    }
}
