package output;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.Urdiales;
import control.Command;

/**
 * @author Pieter Marsman
 */
public class Log {
	
	private static Logger log;
	
	public Log(Logger log) {
		this.log = log;
		setLogger();
	}

	/**
	 * Initializes the logger
	 */
	private void setLogger() {
		log.setLevel(Level.INFO);
		Handler handler = null;
		try {
			String folder = "C://Gebruikers//Tom//logs//";
			String filename = folder + new Date().getTime() + ".log";
			(new File(folder)).mkdirs();
			handler = new FileHandler(filename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.addHandler(handler);
		log.info("System started");
	}
}
