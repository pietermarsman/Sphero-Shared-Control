package webcam;

import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_INTER_LINEAR;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;

;

public class WebcamVideoFile implements InputStream {

	private String file = "D://vid.avi";
	OpenCVFrameGrabber grabber;

	public WebcamVideoFile() {
		grabber = null;
		init();
	}

	public void init() {
		grabber = new OpenCVFrameGrabber(file);
		try {
			grabber.start();
		} catch (Exception e) {
			// Exits from 20 to 29 means WebcamVideoFile class exits
			e.printStackTrace();
			System.exit(20);
		}
	}

	@Override
	public IplImage nextImage() {
		IplImage img = null;
		try {
			grabber.flush();
			img = grabber.grab();
		} catch (Exception e) {
			// Exits from 20 to 29 means WebcamVideoFile class exits
			e.printStackTrace();
			System.exit(21);
		}
		IplImage small_img = null;
		small_img = IplImage.create(
				new CvSize((int) Math.round(img.width() / 2.0), (int) Math.round(img.height() / 2.0)), img.depth(), 3);
		cvResize(img, small_img, CV_INTER_LINEAR);
		return small_img;
	}

	@Override
	public void exit() {
		System.out.println("Exit WebcamVideoFile");
	}

	@Override
	public boolean ready() {
		return true;
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
