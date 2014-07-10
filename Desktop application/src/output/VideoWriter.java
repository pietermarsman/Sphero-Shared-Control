package output;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import webcam.Image;

import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.FrameRecorder.Exception;
import com.googlecode.javacv.OpenCVFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import control.Goals;

import static com.googlecode.javacv.cpp.opencv_highgui.CV_FOURCC;

/**
 * @author Pieter Marsman
 */
public class VideoWriter {

	private static final String time = String.valueOf(new Date().getTime());

	private FrameRecorder fr;
	private CvSize size;
	private String folder;
	private Goals goals;
	private HashMap<String, Integer> pathCount;
	private boolean recording;

	/**
	 * Initialize this class with a reference to the image class
	 * 
	 * @param img
	 */
	public VideoWriter(Image img, Goals goals) {
		this.fr = null;
		this.size = img.getSize();
		this.goals = goals;
		folder = "D://Pieter//video//" + time;
		recording = false;
		pathCount = new HashMap<String, Integer>();
	}

	private void initializeFrameRecorder(String filename) {
		recording = true;
		try {
			if (fr != null)
				fr.stop();
			fr = new OpenCVFrameRecorder(filename, size.width(), size.height());
			fr.setVideoCodec(CV_FOURCC('M', 'J', 'P', 'G'));
			fr.setFrameRate(20);
			fr.setPixelFormat(1);
			fr.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Video files are saved for every path, so there is a new path created.
	 */
	public void newPath() {
		String path = "";
		if (goals.getPrevious() != null)
			path += goals.getPrevious().getName();
		path += goals.getGoal().getName();
		if (pathCount.get(path) == null)
			pathCount.put(path, 1);
		else
			pathCount.put(path, pathCount.get(path) + 1);
		System.out.println(path);
		(new File(folder + "//" + path)).mkdirs();
		String filename = folder + "//" + path + "//" + pathCount.get(path) + ".avi";
		initializeFrameRecorder(filename);
	}

	/**
	 * @param img
	 *            image that should be added to the file
	 */
	public void record(IplImage img) {
		if (recording) {
			try {
				fr.record(img);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops the video writer
	 */
	public void exit() {
		if (recording) {
			try {
				fr.stop();
				fr.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
