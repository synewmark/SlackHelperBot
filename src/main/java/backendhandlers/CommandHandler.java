package backendhandlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;

import com.slack.api.methods.SlackApiException;

import model.Command;

public class CommandHandler {

	private final List<Command> commands;
	private final SlackHandler handler;
	private final SortedSet<String> allUsers;

	private boolean preexecution;

	public CommandHandler(String token, List<Command> commands) {
		this.commands = commands;
		this.allUsers = getAllStudentEmails(commands);
		this.handler = new SlackHandler(token);
	}

	public Set<String> getUninvitedUsers() {
		allUsers.removeAll(handler.getAllUserEmails());
		preexecution = true;
		return allUsers;
	}

	public void execute() throws IOException, SlackApiException {
		if (!preexecution || !allUsers.isEmpty()) {
			throw new IllegalStateException();
		}
		for (Command command : commands) {
			switch (command.command().toUpperCase()) {
			case "CREATECHANNEL":
				executeCreate(command);
				break;
			case "ARCHIVECHANNEL":
				executeArchive(command);
				break;
			case "UNARCHIVECHANNEL":
				executeUnarchive(command);
				break;
			case "ADDSTUDENT":
				executeAdd(command);
				break;
			case "REMOVESTUDENT":
				executeRemove(command);
				break;
			case "MOVESTUDENT":
				executeMove(command);
				break;
			}
		}
	}

	private void executeCreate(Command command) throws IOException, SlackApiException {
		String channelName = getChannelName(Integer.parseInt(command.year()), command.section());
		String response = handler.createChannel(channelName, command.args()[0].equalsIgnoreCase("private"));
		if (response != null) {
			System.out
					.println(String.format("Creating channel: %s failed with the message: %s", channelName, response));
		}
	}

	private void executeArchive(Command command) throws IOException, SlackApiException {
		String channelName = getChannelName(Integer.parseInt(command.year()), command.section());
		String response = handler.archiveChannel(channelName);
		if (response != null) {
			System.out
					.println(String.format("Archiving channel: %s failed with the message: %s", channelName, response));
		}
	}

	private void executeUnarchive(Command command) throws IOException, SlackApiException {
		String channelName = getChannelName(Integer.parseInt(command.year()), command.section());
		String response = handler.unarchiveChannel(channelName);
		if (response != null) {
			System.out.println(
					String.format("Unarchiving channel: %s failed with the message: %s", channelName, response));
		}
	}

	private void executeAdd(Command command) throws IOException, SlackApiException {
		String channelName = getChannelName(Integer.parseInt(command.year()), command.section());
		for (String email : command.args()) {
			String returnResponse = handler.addUserToChannel(email, channelName);
			if (returnResponse != null) {
				System.out.println(String.format("Adding user: %s to channel: %s failed with the message: %s", email,
						channelName, returnResponse));
			}
		}
	}

	private void executeRemove(Command command) throws IOException, SlackApiException {
		String channelName = getChannelName(Integer.parseInt(command.year()), command.section());
		for (String email : command.args()) {
			String returnResponse = handler.addUserToChannel(email, channelName);
			if (returnResponse != null) {
				System.out.println(String.format("Removing user: %s from channel: %s failed with the message: %s",
						email, channelName, returnResponse));
			}
		}
	}

	private void executeMove(Command command) throws IOException, SlackApiException {
		String channelName = getChannelName(Integer.parseInt(command.year()), command.section());
		for (String email : command.args()) {
			String returnResponse = handler.moveUserToChannel(email, channelName);
			if (returnResponse != null) {
				System.out.println(String.format("Moving user: %s to channel: %s failed with the message: %s", email,
						channelName, returnResponse));
			}
		}
	}

	private List<String> executeCommand(BiFunction<String, String, String> function, Command command) {
		String channelName = getChannelName(Integer.parseInt(command.year()), command.section());
		List<String> response = new ArrayList<>();
		for (String email : command.args()) {
			response.add(function.apply(email, channelName));
		}
		return response;
	}

	private static SortedSet<String> getAllStudentEmails(List<Command> commands) {
		SortedSet<String> allUsers = new TreeSet<>();
		for (Command command : commands) {
			if (command.command().equalsIgnoreCase("ADDSTUDENT") || command.command().equalsIgnoreCase("REMOVESTUDENT")
					|| command.command().equalsIgnoreCase("MOVESTUDENT")) {
				allUsers.addAll(List.of(command.args()));
			}

		}
		allUsers.remove("");
		return allUsers;
	}

	public static int getYear(String channelName) {
		String prefix = "year-of-";
		if (!channelName.startsWith(prefix)) {
			throw new IllegalArgumentException();
		}
		int start = prefix.length();
		int end = findNumericalSubstringLength(channelName, start);
		return Integer.parseInt(channelName.substring(start, end));

	}

	public static String getChannelName(int year, String section) {
		String channel = "class_of_" + year;
		if (!section.isEmpty()) {
			channel += "_" + section.toLowerCase();
		}
		return channel;
	}

	public static int findNumericalSubstringLength(String s, int start) {
		int i;
		for (i = start; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				break;
			}
		}
		return i - start;
	}

	public boolean specialEquals(String token) {
		return this.handler.specialEquals(new SlackHandler(token));
	}
}
