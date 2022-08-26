package com.newmark.SlackHelperBot.backendhandlers;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import model.Command;

public class CSVReader implements Iterable<Command>, Iterator<Command> {
	private final Iterator<CSVRecord> csvRecordIterator;

	public CSVReader(Reader reader) throws IOException {
		CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).build();
		CSVParser parser = format.parse(reader);
		this.csvRecordIterator = parser.iterator();
	}

	@Override
	public boolean hasNext() {
		return csvRecordIterator.hasNext();
	}

	@Override
	public Command next() {
		CSVRecord record = csvRecordIterator.next();
		String[] recordEntries = record.toList().toArray(new String[0]);
		if (recordEntries.length < 3) {
			throw new IllegalArgumentException();
		}
		return new Command(recordEntries);
	}

	@Override
	public Iterator<Command> iterator() {
		return this;
	}

}
