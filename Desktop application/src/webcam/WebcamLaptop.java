package webcam;

import java.util.logging.Level;

import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class WebcamLaptop implements InputStream {

	private OpenCVFrameGrabber grabber;
	private boolean running;

	public WebcamLaptop(int webcam) {
		running = false;
		grabber = null;
		this.init(webcam);
	}

	public WebcamLaptop() {
		this(0);
	}

	public void init(int webcam) {
		main.SpheroExperiment.Log.log(Level.INFO, "Opening laptop webcam stream");
		grabber = new OpenCVFrameGrabber(webcam);
		try {
			grabber.start();
		} catch (Exception e) {
			e.printStackTrace();
			// Exits from 10 to 19 means WebcamLaptop class exits
			System.exit(10);
		}
		running = true;
		main.SpheroExperiment.Log.log(Level.INFO, "Finished opening laptop webcam stream.");
	}

	@Override
	public IplImage nextImage() {
		if (running) {
			try {
				return grabber.grab();
			} catch (Exception e) {
				e.printStackTrace();
				// Exits from 10 to 19 means WebcamLaptop class exits
				System.exit(11);
			}
		}
		return null;
	}

	public void exit() {
		running = false;
		try {
			grabber.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		grabber = null;
	}

	public boolean ready() {
		return running;
	}

	@Override
	public void flush() {
		try {
			grabber.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
