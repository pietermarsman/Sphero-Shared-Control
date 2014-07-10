package main;

import static com.googlecode.javacv.cpp.opencv_core.cvCreateMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateSeq;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RETR_CCOMP;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindContours;
import output.ScreenCanvasFrame;
import output.VideoWriter;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;

import webcam.Image;
import webcam.InputStream;
import webcam.WebcamLaptop;
import control.Command;

public class Test {

	public static void main(String[] args) {
	}

	private static void a() {
		for (int dir = 0; dir < 360; dir += 45) {
			Command c = new Command(1.0, dir);
			System.out.println(c + ": (" + c.getX() + "," + (-c.getY()) + ")");
			System.out.println("    " + Command.fromCartesian(c.getX(), -c.getY()));
		}
		System.out.println();
		for (int x = -1; x <= 1; x++)
			for (int y = -1; y <= 1; y++)
				System.out.println("(" + x + "," + y + "): " + Command.fromCartesian(x, y));
	}

	private static void b() {
		InputStream is = new WebcamLaptop(-1);
		Image image = new Image(is.nextImage());
		ScreenCanvasFrame scf = new ScreenCanvasFrame(image);
		while (true) {
			image.newImage(is.nextImage(), false);
			scf.refreshImage();
			image.release();
		}
	}
}
