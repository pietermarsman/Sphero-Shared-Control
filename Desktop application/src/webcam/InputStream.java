package webcam;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * @author Pieter Marsman
 */
public interface InputStream {

	/**
	 * @return the next image from the inputstream
	 */
	public IplImage nextImage();

	/**
	 * exits the inputstream
	 */
	public void exit();

	/**
	 * @return if the inputstream is ready
	 */
	public boolean ready();

	/**
	 * flushes the inputstream
	 */
	public void flush();
}
