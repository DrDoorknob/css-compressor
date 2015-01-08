/**
 * Copyright 2015 American Well Systems
 * All rights reserved.
 *
 * It is illegal to use, reproduce or distribute
 * any part of this Intellectual Property without
 * prior written authorization from American Well.
 */
package ru.artlebedev.csscompressor;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author david.keyworth
 *
 */
public class CliConfigBuilder extends ConfigBuilder {

	Options options;

	public CliConfigBuilder(final String[] args) throws ParseException {
		options = new Options();
		options.addOption(new Option("help", "print this message"));

		//ROOT(
      	options.addOption(new Option("root", true, "string"));
		//INPUT_PATH(
	  	options.addOption(new Option("inpath", true, "string"));
		//OUTPUT_PATH(
      	options.addOption(new Option("outpath", true, "string"));
		//OUTPUT_WRAPPER(
      	options.addOption(new Option("outwrapper", true, "string or array"));
		//MODULES(
      	options.addOption(new Option("modules", true, "object"));
		//CHARSET(
      	options.addOption(new Option("charset", true, "string"));
		//PREPROCESS(
      	options.addOption(new Option("preprocess", true, "string"));

      	CommandLineParser parser = new BasicParser();
      	final CommandLine cmdLine = parser.parse(options, args);

      	if (cmdLine.hasOption("help")) {
		    printUsage();
		    System.exit(1);
		  }

      	for (ConfigOption configOption : ConfigOption.values()) {
            String optionName = configOption.getName();

            //if (cmdLine.hasOption(optionName)) {
              configOption.update(cmdLine.getOptionValue(optionName, configOption.getDefaultValue()), this);
            //}
          }

	}

	@Override
	protected Config build() throws Exception {
		return super.build();
	}

	private void printUsage() {
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.setWidth(120);
	    formatter.setSyntaxPrefix("Usage: ");

	    formatter.printHelp(
	        "java -jar css-compressor.jar [options] config-json-file",
	        options);
	  }
}
