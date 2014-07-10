package webcam;

import static com.googlecode.javacv.cpp.opencv_highgui.*;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class WebcamImageFile extends Thread implements InputStream {

	private final String file = "C:/Users/Gebruiker/Google Drive/Studie/Scriptie/Voorbeelden/example1.jpg";
	private IplImage img;

	public WebcamImageFile() {
		img = null;
		this.start();
	}

	@Override
	public IplImage nextImage() {
		return img;
	}

	@Override
	public void exit() {
		System.out.println("Exit WebcamImageFile");
	}

	public boolean ready() {
		return img != null;
	}

	@Override
	public void run() {
		img = cvLoadImage(file);
	}

	@Override
	public void flush() {

	}
}
