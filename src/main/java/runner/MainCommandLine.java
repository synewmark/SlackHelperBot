package runner;

import java.io.IOException;

import com.google.devtools.common.options.OptionsParser;

public class MainCommandLine {
	static Arguments arguments;

	public static void main(String[] args) throws IOException {
		OptionsParser parser = OptionsParser.newOptionsParser(Arguments.class);
		parser.parseAndExitUponError(args);
		arguments = parser.getOptions(Arguments.class);
		checkArgs(arguments);
		SlackRunner slackRunner = new SlackRunner(arguments.token, arguments.commandFile);
		slackRunner.execute();
	}

	private static void checkArgs(Arguments arguments) {
		if (arguments.token == null) {
			System.err.println("Token must be non-empty");
		}
		if (!arguments.commandFile.exists() || !arguments.commandFile.canRead()) {
			System.err.println("Cannot read from file: " + arguments.commandFile
					+ " check that file exists and you have read permissions");
		}
	}
}
