package webcam;

import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.OpenKinectFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class WebcamKinect extends Thread implements InputStream {

	private OpenKinectFrameGrabber grabber;
	private boolean started;

	public WebcamKinect() {
		grabber = null;
		started = false;
		this.start();
	}

	public void run() {
		grabber = new OpenKinectFrameGrabber(0);
		try {
			grabber.start();
		} catch (Exception e) {
			e.printStackTrace();
			// Exits from 10 to 19 means WebcamLaptop class exits
			System.exit(10);
		}
		started = true;
	}

	@Override
	public IplImage nextImage() {
		IplImage img = null;
		try {
			img = grabber.grabVideo();
		} catch (Exception e) {
			e.printStackTrace();
			// Exits from 10 to 19 means WebcamLaptop class exits
			System.exit(11);
		}
		return img;
	}

	public void exit() {
		try {
			grabber.flush();
		} catch (Exception e) {
			e.printStackTrace();
			// Exits from 10 to 19 means WebcamLaptop class exits
			System.exit(12);
		}
	}

	public boolean ready() {
		return started;
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
