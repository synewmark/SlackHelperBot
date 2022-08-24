package runner;

import java.io.IOException;

import com.google.devtools.common.options.OptionsParser;

public class MainCommandLine {
	static Arguments arguments;

	public static void main(String[] args) throws IOException {
		OptionsParser parser = OptionsParser.newOptionsParser(Arguments.class);
		parser.parseAndExitUponError(args);
		arguments = parser.getOptions(Arguments.class);
		SlackRunner slackRunner = new SlackRunner(arguments.token, arguments.commandFile);
		slackRunner.execute();
		System.out.println(slackRunner.specialEquals(arguments.token));
	}

}
