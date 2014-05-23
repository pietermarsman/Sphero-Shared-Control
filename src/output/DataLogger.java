package output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

import main.Constant;
import main.ExperimentState;
import main.Sphero;
import main.StaticMethods;
import main.Urdiales;
import control.Command;
import control.Goals;

/**
 * @author Pieter Marsman
 */
public class DataLogger {

	private BufferedWriter fop = null;
	private FileWriter file;
	public static final String folder = "D://Pieter//data//";
	private DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Constant.LOCATION);
	private final int rows = 23;
	private boolean logging;

	/**
	 * Initialise the datalogger with a filename and writes the header;
	 */
	public DataLogger() {
		// Logging is set to true in order to write the headers
		logging = true;
		try {
			String filename = folder + new Date().getTime() + ".csv";
			(new File(folder)).mkdirs();
			file = new FileWriter(filename, true);
			fop = new BufferedWriter(file);
			String header = "Timestamp;path;goal;reached goal;state;rating;traveled distance;location;angle(h);velocity(h);smoothness(h);directness(h);safety(h);angle(c);velocity(c);smoothness(c);directness(c);safety(c);angle(s);velocity(s);smoothness(s);directness(s); safety(s)\n";
			writeRow(header);
		} catch (IOException e) {
			main.SpheroExperiment.Log.log(Level.SEVERE, "Something did go wrong while creating files");
			e.printStackTrace();
		}
		logging = false;
	}

	/**
	 * Logs a row into the file containing
	 * 
	 * @param urdiales
	 *            meassure
	 * @param human
	 *            command
	 * @param computer
	 *            command
	 * @param shared
	 *            command
	 * @param goalReached
	 *            during time between now and previous logging
	 * @param goals
	 *            current and previous goals
	 * @param state
	 *            in which the experiment is in
	 */
	public void log(Urdiales urdiales, Command human, Command computer, Command shared, boolean goalReached, Goals goals, ExperimentState state, Sphero sphero,
			long startTime) {
		// timestamp
		String row = Long.toString(new Date().getTime() - startTime) + Constant.CSV_SEPARATOR;
		// path
		if (goals.getGoal() != null) {
			if (goals.getPrevious() != null)
				row += goals.getPrevious().getName();
			row += goals.getGoal().getName() + Constant.CSV_SEPARATOR;
			// goal
			row += goals.getGoal().getPoint() + Constant.CSV_SEPARATOR;
		} else {
			row += StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, 2);
		}
		// goal
		row += goalReached + Constant.CSV_SEPARATOR;
		// state
		row += state + Constant.CSV_SEPARATOR;
		// rating
		row += Constant.CSV_SEPARATOR;
		// traveled distance
		row += Math.round(sphero.getTraveledDistance()) + Constant.CSV_SEPARATOR;
		// location sphero
		row += sphero.getCenter() + Constant.CSV_SEPARATOR;
		// Commands
		String humanRow = "", computerRow = "", sharedRow = "";
		if (human != null)
			humanRow = efficiency(urdiales, human);
		else
			humanRow = StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, 5);
		if (computer != null)
			computerRow = efficiency(urdiales, computer);
		else
			computerRow = StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, 5);
		if (shared != null)
			sharedRow = efficiency(urdiales, shared);
		else
			sharedRow = StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, 5);
		row += humanRow + computerRow + sharedRow;
		writeRow(row);
	}

	private String formatDouble(double bd) {
		return df.format(bd);
	}

	/**
	 * @param urdiales
	 *            measures
	 * @param c
	 *            Command
	 * @return string that can be written to a csv file with the command and the
	 *         efficiencies of urdiales.
	 */
	private String efficiency(Urdiales urdiales, Command c) {
		return c.getDirection() + Constant.CSV_SEPARATOR + formatDouble(c.getVelocity()) + Constant.CSV_SEPARATOR + formatDouble(urdiales.smoothness(c))
				+ Constant.CSV_SEPARATOR + formatDouble(urdiales.direction(c)) + Constant.CSV_SEPARATOR + formatDouble(urdiales.safety(c))
				+ Constant.CSV_SEPARATOR;
	}

	/**
	 * @param text
	 *            that should be written to a csv file
	 */
	private void writeRow(String text) {
		if (logging) {
			try {
				fop.write(text);
				fop.newLine();
				fop.flush();
			} catch (IOException e) {
				main.SpheroExperiment.Log.log(Level.SEVERE, "Could not write to file");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Closes the file
	 */
	public void close() {
		try {
			logging = false;
			fop.flush();
			fop.close();
		} catch (IOException e) {
			main.SpheroExperiment.Log.log(Level.SEVERE, "Error while closing File Output Stream");
			e.printStackTrace();
		}
	}

	/**
	 * A row with status "PAUSED" is writen to the csv file
	 */
	public void pauseExpiriment() {
		writeRow(StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, rows));
		writeRow(new Date().getTime() + StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, 4) + "PAUSED"
				+ StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, rows - 4));
		writeRow(StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, rows));
	}

	/**
	 * An empty row is writen to the csv file to indicate the end of a path
	 */
	public void goalReached() {
		writeRow(StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, rows));
	}

	/**
	 * @param b
	 *            to turn datalogging on and off
	 */
	public void setDataLogging(boolean b) {
		try {
			fop.newLine();
		} catch (IOException e) {
			main.SpheroExperiment.Log.log(Level.SEVERE, "Could not write to file");
			e.printStackTrace();
		}
		logging = b;
	}

	public void logRating(int rating) {
		writeRow(StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, 5) + String.valueOf(rating)
				+ StaticMethods.concatenationRepeat(Constant.CSV_SEPARATOR, rows - 5 - 1));
	}
}
