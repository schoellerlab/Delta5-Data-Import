import java.text.DecimalFormat;
import java.util.*;
import java.io.*;

public class Importer {
	//The known values of the standards used in this test with
	//KNOWN_VALUE1 being the standard that the Delta5 uses first
	//and KNOWN_VALUE2 being the standard that the Delta5 uses next
	private static final double KNOWN_VALUE1 = -9.04;
	private static final double KNOWN_VALUE2 = 146.68;

	//The known values of standards
	private static final double MTW_KNOWN_VALUE = -9.04;
	private static final double RAMP_KNOW_VALUE = 146.68;

	//This value unknown to date
	private static final double WS5_KNOWN_VALUE = 0;

	//The number of injections per subject/standard
	private static final int NUMBER_OF_INJECTIONS = 7;

	//How many standards are being used
	private static final int NUMBER_OF_STANDARDS = 3;

	//How many columns desired in output file
	//Number of data members needed + Number of columns desired for analysis
	private static final int NUMBER_OF_COLUMNS = 6;

	//Which columns of the raw data that are needed. Keep in mind that the first
	//column has an index of 0.
	private static int[] COLUMNS_NEEDED = {0,1,2,3};

	//How many injections are used for the calculations. (The last x injections)
	private static int INJECTIONS_USED = 3;

	public File main (String args[]) throws FileNotFoundException {
		//The scanner used for reading input
		Scanner stdin = null;
		//The file assigned to the input reading scanner
		File inFile = null;
		if(args[0] != null){
			inFile = new File(args[0]);
		}
		//Initializes the scanner to the desired file
		try{
			stdin = new Scanner(inFile);
		}
		catch(FileNotFoundException e) {
			System.out.println("The specified file was not found or read priviliges are denied. Please note " +
					"that file names are case sensitive and that you must specify the file extension.");
			return null;
		}

		//Creates the data structure that will take input, be used for calculations and 
		//be read from for output to the new file.
		LinkedList<Object[]> data = new LinkedList<Object[]>();

		//Adds blank rows for standards and 1 extra for whitespace
		for(int i = 0; i <= NUMBER_OF_STANDARDS; i ++) {
			data.add(new Object[NUMBER_OF_COLUMNS]);
		}

		//Adds the actual data
		while(stdin.hasNext()) {
			data.add(lineReader(stdin, data.size()));
		}

		//The file that the data will be written to 
		File outFile;
		try{
			outFile = new File(args[1]);
		}
		catch(ArrayIndexOutOfBoundsException e) {
			//The default name of the output file
			outFile = new File("TEE.csv");
		}
		//The object that writes the data to the output file
		PrintWriter stdout = null;
		//Initializes the PrintWriter to the output file
		try{
			stdout = new PrintWriter(outFile);
		}
		catch(FileNotFoundException e) {
			System.out.println("Write privileges prohibited. Please make sure the file " +
					"you are attempting to write to is closed.");
			return null;
		}

		//Calculations section
		//Calculation of all of the standards
		int stdCount = 0;
		//TODO Optimize Loop to not go through all of the data, just up until standards are done.
		for(int i = (NUMBER_OF_STANDARDS + 1 + NUMBER_OF_INJECTIONS - 2); i < data.size() - 2; i += NUMBER_OF_INJECTIONS) {
			if(stdCount < NUMBER_OF_STANDARDS) {
				//Averages the first run of this standard
				double standard1 = averageCalc(0, i, data);
				//Averages the second run of this standard
				double standard2 = averageCalc(0, ((data.size() - 3) - 
						(NUMBER_OF_INJECTIONS * (NUMBER_OF_STANDARDS - (stdCount + 1)))), data);
				//Puts the standard name and average value in the correct place
				data.get(stdCount)[4] = data.get(i)[1];
				data.get(stdCount)[5] = trimDouble((standard1 + standard2 )/2);
				stdCount ++;
			}
		}

		//Calculation of the average, standard deviation, and adjusted value for each subject ID
		for(int i = (NUMBER_OF_STANDARDS + 1 + NUMBER_OF_INJECTIONS - 2); i < data.size() - 2; i += NUMBER_OF_INJECTIONS) {
			//Average Calculation
			data.get(i)[4] = averageCalc(0, i, data);

			//Standard deviation calculation
			double temp = stDevCalc(0, i,data);
			if(temp < 0.30) {
				data.get(i+(INJECTIONS_USED - 1))[4] = temp;
			}
			else{
				int toExclude = toExclude(i, data);
				data.get(i)[4] = averageCalc(toExclude, i, data);
				data.get(i+(INJECTIONS_USED - 1))[4] = stDevCalc(toExclude, i, data);
			}
			//Adjusted value calculation
			data.get(i)[5] = adjustedCalc(i, data);
		}

		//An iterator for traversing the data
		Iterator<Object[]> iter = data.iterator();
		//Calls a method that writes the data to the output file
		while(iter.hasNext()) {
			linePrinter(stdout, iter.next());
		}
		//Closes the input scanner and output writer
		stdin.close();
		stdout.close();
		System.out.println("This was successful!");
		
		return outFile;
	}

	private static Object[] lineReader(Scanner stdin, int row){
		//An array for holding the specific elements of the row
		String[] thisLine = null;
		thisLine = stdin.nextLine().split(",");
		//An array for processing the data and returning the ideal version
		Object[] trimmedVersion = new Object[6];
		//Trim off all but the first 4 columns
		for(int i = 0; i < COLUMNS_NEEDED.length; i ++) {
			try{
				trimmedVersion[i] = Double.valueOf(thisLine[i]);
			}
			catch(NumberFormatException e) {
				trimmedVersion[i] = thisLine[i];
			}
		}
		return trimmedVersion;
	}

	private static void linePrinter(PrintWriter stdout, Object[] line) {
		if(line[0] == null) {
			stdout.print("");
		}
		else {
			stdout.print(line[0]);
		}
		for(int i = 1; i < line.length; i ++) {
			if(line[i] == null) {
				stdout.print(", ");
			}
			else {
				stdout.print(", " + line[i]);
			}
		}
		stdout.println();
	}


	private static double averageCalc(int exclude, int i, List<Object[]> data) {
		//Average Calculation
		if(exclude == 0) {
			double sum = 0;
			for(int j = 0; j < INJECTIONS_USED; j ++){
				sum += (Double) data.get(i)[3];
				i ++;
			}
			return trimDouble(sum/INJECTIONS_USED);
		}
		else{
			double sum = 0;
			for(int j = 0; j < INJECTIONS_USED; j ++) {
				if(j != (exclude - 1)) {
					sum += (Double) data.get(i)[3];
				}
				i ++;
			}
			return trimDouble(sum/(INJECTIONS_USED - 1));
		}
	}

	private static double trimDouble(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
	}

	private static double stDevCalc(int exclude, int i, List<Object[]> data) {
		double avg = (Double) data.get(i)[4];
		double[] devs = new double[INJECTIONS_USED];
		double stDev = 0;
		for(int j = 0; j < devs.length; j ++) {
			if(j != (exclude -1)) {
				devs[j] = ((Double) data.get(i + j)[3]);
				devs[j] -= avg;
				stDev += Math.pow(devs[j], 2);
			}
		}
		if(exclude == 0) {
			stDev = stDev / (INJECTIONS_USED - 1);
		}
		else {
			stDev = stDev / (INJECTIONS_USED -2);
		}
		stDev = Math.sqrt(stDev);
		return trimDouble(stDev);
	}

	private static int toExclude(int i, List<Object[]> data) {
		double avg = (Double) data.get(i)[4];
		double max = Math.abs((Double) data.get(i)[3] - avg);
		int toReturn = 1;
		int count = 2;
		for(int j = 1; j < INJECTIONS_USED; j ++) {
			if(Math.abs((Double) data.get(i + j)[3] - avg) > max) {
				max = Math.abs((Double) data.get(i + j)[3] - avg);
				toReturn = count;
			}
			count ++;
		}
		return toReturn;
	}

	private static double adjustedCalc(int i, List<Object[]> data) {
		double avg1 = (Double) data.get(0)[5];
		double avg2 = (Double) data.get(1)[5];
		double myAvg = (Double) data.get(i)[4];
		double toReturn = (myAvg - avg1) * (KNOWN_VALUE2 - KNOWN_VALUE1) / (avg2 - avg1) + KNOWN_VALUE1;

		/*double toReturn = (((Double)data.get(i)[4] - (Double)data.get(0)[5]) * 
				(KNOWN_VALUE2 - KNOWN_VALUE1) / ((Double) data.get(0)[5] - (Double) data.get(1)[5]) + KNOWN_VALUE1);*/ 
		return trimDouble(toReturn);
	}
}
