/**
* Title:        Performance Data Analyzer (PDA)
* Copyright:    Copyright (c) 2006-2013 by Nicolas Michael
* Website:      http://pda.nmichael.de/
* License:      GNU General Public License v2
*
* @author Nicolas Michael
* @version 2
*/

package de.nmichael.pda.parser;

import de.nmichael.pda.data.*;
import de.nmichael.pda.data.TimeStamp.Fields;
import de.nmichael.pda.util.Util;

import java.util.regex.*;
import java.util.*;
import java.io.*;

public class Csv extends Parser {

	// handles a format as:
	// header;header;header;header;header;...
	// 2010-03-10 06:59:42;data;data;data;data;...

	private static final String PARAM_DELIMITER = "CSV Delimiter";
	private String delimiter = ";";
	private String myDelimiter = ";";
	private ArrayList<DataSeries> series;

	// @Override
	public boolean canHandle(String filename) {
		return super.canHandle(filename, "csv");
	}

	public Csv() {
		super("csv");
		setParameter(PARAM_DELIMITER, delimiter);
		setSupportedFileFormat(new FileFormatDescription(FileFormatDescription.PRODUCT_GENERIC, null, "Generic CSV",
				null, "generic CSV format",
				"header 'header;header;header;...' and lines '2010-03-10 06:59:42;data;data;...'"));
		getCurrentTimeStamp().addTimeStampPattern("Unix TS", Pattern.compile("(\\d+)"),
				new Fields[] { Fields.unixsec });
	}

	private static String getDelimiter(String s, String[] delims) {
		for (String d : delims) {
			if (s.indexOf(d) >= 0) {
				return d;
			}
		}
		return delims[0];
	}

	// @Override
	public void createAllSeries() {
		String category = Util.getNameOfFile(getFilename());
		String subcategory = "";
		category = category.replaceAll(":", "_");
		if (category.endsWith(".csv")) {
			category = category.substring(0, category.length() - 4);
		}
		int pos = category.indexOf(".");
		if (pos > 0 && pos + 1 < category.length()) {
			subcategory = category.substring(pos + 1);
			category = category.substring(0, pos);
		} else {
			pos = category.indexOf("_");
			if (pos > 0 && pos + 1 < category.length()) {
				subcategory = category.substring(pos + 1);
				category = category.substring(0, pos);
			}
		}

		series = new ArrayList<DataSeries>();
		String s = readLine();
		if (s != null) {
			s = s.trim();
			myDelimiter = getDelimiter(s, new String[] { delimiter, ";", ",", "|" });
			StringTokenizer tok = new StringTokenizer(s, delimiter);
			int i = 0;
			while (tok.hasMoreTokens()) {
				String cn = tok.nextToken();
				if (i >= 1) {
					cn = cn.replaceAll(":", "_");
					series.add(series().addSeries(category, subcategory, cn));
				}
				i++;
			}
		}
	}

	// @Override
	public void parse() {
		try {
			String s;
			while ((s = readLine()) != null) {
				s = s.trim();
				if (s.length() > 0) {
					StringTokenizer tok = new StringTokenizer(s, myDelimiter);
					if (tok.hasMoreTokens()) {
						getCurrentTimeStamp().getTimeStampFromLine(tok.nextToken(), null, 1, true);
						long t = getCurrentTimeStamp().getTimeStamp();
						int i = 0;
						while (tok.hasMoreTokens()) {
							String val = tok.nextToken();
							if (val.length() > 0) {
								try {
									DataSeries ser = series.get(i);
									ser.addSampleIfNeeded(t, Double.parseDouble(val));
								} catch (NumberFormatException ee) {
									// nothing to do (ignore this sample!
								}
							}
							i++;
						}
					}
				}
			}
			series().setPreferredScaleIndividual();
		} catch (Exception e) {
			logError(e.toString());
		}
	}

	// @Override
	public void setParameter(String name, String value) {
		super.setParameter(name, value);
		if (PARAM_DELIMITER.equals(name)) {
			try {
				delimiter = (value != null && value.length() > 0 ? value : ";");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
