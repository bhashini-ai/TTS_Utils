package ai.bhashini.tts.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineOptions {
	public Options options = new Options();
	public BooleanOption help = new BooleanOption("h", "help", "Print usage");

	public CommandLineOptions() {
		options.addOption(help);
	}

	public void printHelp(String className) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + className, options);
	}

	public void parse(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine line = parser.parse(options, args);
		for (Option option : options.getOptions()) {
			if (option instanceof StringOption) {
				((StringOption) option).parseOptionValue(line);
			} else if (option instanceof BooleanOption) {
				((BooleanOption) option).parseOptionValue(line);
			}
		}
	}

	public void printValues() {
		for (Option option : options.getOptions()) {
			if (!option.equals(help)) {
				System.out.println(option);
			}
		}
	}

	public static class StringOption extends Option {
		private static final long serialVersionUID = 1915530027480316531L;

		String strValue = null;

		public StringOption(String option, String longOption, String description) throws IllegalArgumentException {
			super(option, longOption, true, description);
			this.setType(String.class);
		}

		public String getStringValue() {
			return strValue;
		}

		public void parseOptionValue(CommandLine line) {
			if (line.hasOption(this)) {
				strValue = line.getOptionValue(this);
			}
		}

		@Override
		public String toString() {
			return getLongOpt() + " = " + strValue;
		}
	}

	public static class IntegerOption extends StringOption {
		private static final long serialVersionUID = -4968645230403367311L;

		int intValue;

		public IntegerOption(String option, String longOption, int defaultValue, String description)
				throws IllegalArgumentException {
			super(option, longOption, description + " (default=" + defaultValue + ")");
			this.setType(Integer.class);
			intValue = defaultValue;
		}

		public int getIntValue() {
			return intValue;
		}

		@Override
		public void parseOptionValue(CommandLine line) {
			super.parseOptionValue(line);
			if (strValue != null) {
				try {
					intValue = Integer.parseInt(strValue);
				} catch (NumberFormatException e) {
					System.err.println(e.getClass().getCanonicalName() + ": " + this.getLongOpt() + "=" + strValue
							+ " is not an integer");
				}
			}
		}

		@Override
		public String toString() {
			return getLongOpt() + " = " + intValue;
		}
	}

	public static class DoubleOption extends StringOption {
		private static final long serialVersionUID = 2537029674708812362L;

		double doubleValue;

		public DoubleOption(String option, String longOption, double defaultValue, String description)
				throws IllegalArgumentException {
			super(option, longOption, description + " (default=" + defaultValue + ")");
			this.setType(Double.class);
			doubleValue = defaultValue;
		}

		public double getDoubleValue() {
			return doubleValue;
		}

		@Override
		public void parseOptionValue(CommandLine line) {
			super.parseOptionValue(line);
			if (strValue != null) {
				try {
					doubleValue = Double.parseDouble(strValue);
				} catch (NumberFormatException e) {
					System.err.println(e.getClass().getCanonicalName() + ": " + this.getLongOpt() + "=" + strValue
							+ " is not a double");
				}
			}
		}

		@Override
		public String toString() {
			return getLongOpt() + " = " + doubleValue;
		}
	}

	public static class BooleanOption extends Option {
		private static final long serialVersionUID = -3560946957031467944L;

		boolean boolValue;

		public BooleanOption(String option, String longOption, String description) throws IllegalArgumentException {
			super(option, longOption, false, description);
			this.setType(Boolean.class);
		}

		public boolean getBoolValue() {
			return boolValue;
		}

		public void parseOptionValue(CommandLine line) {
			if (line.hasOption(this)) {
				boolValue = true;
			}
		}

		@Override
		public String toString() {
			return getLongOpt() + " = " + boolValue;
		}
	}

}
