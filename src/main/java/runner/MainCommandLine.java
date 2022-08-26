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
		if (arguments.token == null || arguments.token.isBlank()) {
			throw new IllegalArgumentException("Token must be non-empty");
		}
		if (arguments.commandFile == null) {
			throw new IllegalArgumentException("Command file must be non-empty");
		}
		if (!arguments.commandFile.exists() || !arguments.commandFile.canRead()) {
			throw new IllegalArgumentException("Cannot read from file: " + arguments.commandFile
					+ " check that file exists and you have read permissions");
		}
	}
}
