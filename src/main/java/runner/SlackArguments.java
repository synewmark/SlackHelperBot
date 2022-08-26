package runner;

import java.io.File;

import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParsingException;

public class Arguments extends OptionsBase {
	@Option(name = "commandFile", abbrev = 'c', help = "Location of command CSV file", defaultValue = "", converter = FileConverter.class)
	public File commandFile;
	@Option(name = "token", abbrev = 't', help = "API Token for Slack", defaultValue = "")
	public String token;

	public static class FileConverter implements Converter<File> {
		@Override
		public File convert(String input) throws OptionsParsingException {
			if (input == null || input.isBlank()) {
				return null;
			}
			return new File(input);
		}

		@Override
		public String getTypeDescription() {
			return "File from String";
		}
	}
}
