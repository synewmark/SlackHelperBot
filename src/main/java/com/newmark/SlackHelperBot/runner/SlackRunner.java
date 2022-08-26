package com.newmark.SlackHelperBot.runner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.newmark.SlackHelperBot.backendhandlers.CSVReader;
import com.newmark.SlackHelperBot.backendhandlers.CommandHandler;
import com.newmark.SlackHelperBot.backendhandlers.SlackHandler;
import com.slack.api.methods.SlackApiException;

import model.Command;

public class SlackRunner {
	private final CommandHandler commandHandler;

	public SlackRunner(String token, File commandLocation) throws IOException {
		List<Command> commands = getAllCommands(commandLocation);
		commandHandler = new CommandHandler(token, commands);
	}

	public void execute() throws IOException {

		try {
			Set<String> uninvitedUsers = commandHandler.getUninvitedUsers();
			if (!uninvitedUsers.isEmpty()) {
				printListCommaSeperated(uninvitedUsers);
				return;
			}
			commandHandler.execute();
		} catch (SlackApiException e) {
			throw new IOException(e);
		}
	}

	private static List<Command> getAllCommands(File file) throws IOException {
		List<Command> list = new ArrayList<>();
		for (Command command : new CSVReader(new FileReader(file))) {
			list.add(command);
		}
		return list;
	}

	private static void printListCommaSeperated(Collection<String> list) {
		final String delimiter = ", ";
		boolean first = true;
		System.out.println("Uninvited users:");
		System.out.println();
		for (String s : list) {
			if (!first) {
				System.out.print(delimiter);
			}
			first = false;
			System.out.print(s);
		}
		System.out.println();
		System.out.println();
		System.out.println("Please invite these users and try again");
	}

	public boolean specialEquals(String token) {
		return commandHandler.specialEquals(token);
	}

	public SlackHandler getSlackHandler() {
		return commandHandler.getSlackHandler();
	}
}
