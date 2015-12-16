package enlarger;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Small utility to double the size of eclipse icons for QHD monitors.
 * 
 * @author David Levy
 * @since 2014/04/10
 *
 */
public class FixIcons {

	private static final Logger logger = Logger.getGlobal();
	
	private static Options options = new Options();
	
	static
	{
		logger.setLevel(Level.INFO);
		Option baseDir = new Option("b", "baseDir", true,
				"This is the base directory where we'll parse jars/zips");
		Option outputDir = new Option("o", "outputDir", true,
				"This is the base directory where we'll place output");
		Option resizeFactor = new Option("z", "resizeFactor", true,
				"This is the resize factor. Default is 2.");
		Option parallelThreads = new Option("p", "parallelThreads", true,
				"Number of parallel threads. Default is available CPU cores.");
		Option saveGifInPngFormat = new Option("g", "saveGifInPngFormat", false,
				"Save .gif files in PNG format for much better quality.");
		Option help = new Option("h", "help", false,
				"Show help");
		baseDir.setRequired(true);
		outputDir.setRequired(true);
		
		options.addOption(baseDir);
		options.addOption(outputDir);
		options.addOption(resizeFactor);
		options.addOption(parallelThreads);
		options.addOption(saveGifInPngFormat);
		options.addOption(help);
	}

	public static final void main(String[] args) {

			try {
			GnuParser parser = new GnuParser();
			CommandLine commandLine = parser.parse(options, args);
			if(!commandLine.hasOption("b") || !commandLine.hasOption("o") || commandLine.hasOption("h"))
			{
				printHelp();
				return;
			}
			
			String baseDirArg = commandLine.getOptionValue("b");
			logger.info("Base directory: " + baseDirArg);

			String outputDirArg = commandLine.getOptionValue("o");
			logger.info("Output directory: " + outputDirArg);

			File base = new File(baseDirArg);
			if (!base.exists() || !base.canRead() || !base.isDirectory()) {
				logger.severe("Unable to read from base directory");
				return;
			}

			File output = new File(outputDirArg);
			if(!output.exists())
			{
				if(!output.mkdirs())
				{
					logger.severe("Can't create directory '"+outputDirArg+"'");
					printHelp();
					return;
				}
			}
			if (!output.exists() || !output.canRead() || !output.canWrite()
					|| !output.isDirectory()) {
				logger.severe("Unable to write to output director");
				return;
			}

			if (base.list() == null || base.list().length == 0) {
				logger.severe("The base directory is empty");
				return;
			}

			if (output.list() != null && output.list().length != 0) {
				logger.severe("The output directory is not empty");
				return;
			}
			String resizeFactorStr = commandLine.getOptionValue("z");
			float resizeFactor = 2;
			if(resizeFactorStr!=null)
			{
				try
				{
					resizeFactor = Float.parseFloat(resizeFactorStr);
				} catch (NumberFormatException e)
				{
					logger.severe("Can't parse provided resizeFactor'" +resizeFactorStr+"'");
					return;
				}
			}
			logger.info("Resize factor: " + resizeFactor);

			String parallelThreadsStr = commandLine.getOptionValue("p");
			int parallelThreads = Runtime.getRuntime().availableProcessors();
			if(parallelThreadsStr!=null)
			{
				try
				{
					parallelThreads = Integer.parseInt(parallelThreadsStr);
				} catch (NumberFormatException e)
				{
					logger.severe("Can't parse provided parallelThreads'" +parallelThreadsStr+"'");
					return;
				}
			}
			logger.info("Parallel threads: " + parallelThreads);

			boolean saveGifInPngFormat = commandLine.hasOption("g");

			new FixIconsProcessor(resizeFactor, saveGifInPngFormat).process(base, output, parallelThreads);

		} catch (ParseException e) {
			logger.severe("Unable to parse arguments: " + e.getMessage());
			printHelp();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unexpected error: " + e.getMessage(), e);
			printHelp();
		}
	}
	
	private static void printHelp()
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "java -jar eclipse-icon-enlarger.jar", options );
	}
}
