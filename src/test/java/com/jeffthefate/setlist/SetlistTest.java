package com.jeffthefate.setlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SetlistTest extends TestCase {
	
	private static final String DEV_KEY = "BXx60ptC4JAMBQLQ965H3g";
	private static final String DEV_SECRET = "0ivTqB1HKqQ6t7HQhIl0tTUNk8uRnv1nhDqyFXBw";
	private static final String DEV_ACCESS_TOKEN = "1265342035-6mYSoxlw8NuZSdWX0AS6cpIu3We2CbCev6rbKUQ";
	private static final String DEV_ACCESS_SECRET = "XqxxE4qLUK3wJ4LHlIbcSP1m6G4spZVmCDdu5RLuU";
	private static final String DEV_ACCOUNT = "dmbtriviatest";
	
	public SetlistTest(String testName) {
		super(testName);
	}
	
	private Configuration setupDevConfig() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey(DEV_KEY)
		  .setOAuthConsumerSecret(DEV_SECRET)
		  .setOAuthAccessToken(DEV_ACCESS_TOKEN)
		  .setOAuthAccessTokenSecret(DEV_ACCESS_SECRET)
		  .setUseSSL(true);
		return cb.build();
	}
	
	public static Test suite() {
		return new TestSuite(SetlistTest.class);
	}
	/*
	public void testUpdateStatus() {
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "", "", 0, 0, "", "", "", null, "");
		setlist.postTweet("[TEST] Test message 10", "", null, -1, false);
		setlist.postTweet("[ENCORE:] Test message 11", "TEST GAME MESSAGE 11", null, -1, true);
		setlist.postTweet("[Final:] Test message 12", "TEST GAME MESSAGE 12", null, -1, true);
		setlist.postTweet("Test message 13", "TEST GAME MESSAGE 13", null, -1, true);
	}

	public void testFullSetlist() {
		ArrayList<String> symbolList = new ArrayList<String>(0);
		symbolList.add("*");
		symbolList.add("+");
    	symbolList.add("~");
    	symbolList.add("^");
    	symbolList.add("§");
    	symbolList.add("¤");
    	symbolList.add("$");
    	symbolList.add("%");
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "D:\\setlist.jpg", "D:\\roboto.ttf", 21, 70,
				"", "", "", "", null, symbolList, "");
		setlist.runSetlistCheck("C:\\Users\\Jeff\\Desktop\\testOne.txt");
	}
	
	public void testMassageAnswer() {
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "D:\\setlist.jpg", "D:\\roboto.ttf", 21, 70,
				"", "", "", "", null, null, "");
		System.out.println(setlist.massageAnswer("Sister5||"));
		System.out.println(setlist.massageAnswer("SisterÄ"));
		System.out.println(setlist.massageAnswer("Sister@"));
		System.out.println(setlist.massageAnswer("Sister~"));
	}

	public void testFindWinners() {
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "", "", 0, 0, "", "", "", null, "");
		setlist.answers.put("jeffthefate", "stolen away on 55th & 3rd b");
		setlist.findWinners("Stolen Away On 55th & 3rd",
				"Current #DMB Song & Setlist: [Stolen Away On 55th & 3rd]");
	}

	public void testPostSetlistScores() {
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "D:\\setlist.jpg", "D:\\roboto.ttf", 21, 70,
				"", "", "", null, null, "");
		HashMap<String, Integer> winnerMap = new HashMap<String, Integer>(0);
		winnerMap.put("Copperpot5", 1);
		winnerMap.put("jeffthefate", 1);
		winnerMap.put("testersonman", 2);
		winnerMap.put("testy", 4);
		winnerMap.put("boydtinsley46", 7);
		setlist.postSetlistScoresText("[Current Scores]", winnerMap);
		winnerMap.put("Copperpot5", 3);
		winnerMap.put("jeffthefate", 4);
		winnerMap.put("testersonman", 5);
		winnerMap.put("suckerman", 1);
		winnerMap.put("testy", 5);
		winnerMap.put("boydtinsley45", 12);
		setlist.postSetlistScoresImage("[Final Scores]", "Top Scores",
				winnerMap);
	}
	*/
	public void testBanList() {
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "D:\\setlist.jpg", "D:\\roboto.ttf", 21, 70,
				"", "", "", "D:\\banlist.ser", null, null, "");
		setlist.addAnswer("jeffthefate", "JTR");
		setlist.addAnswer("Copperpot5", "JTR");
		setlist.banUser("jeffthefate");
		System.out.println(setlist.getBanList());
		System.out.println(setlist.findWinners("jtr", "Current Song [JTR]"));
		setlist.unbanUser("jeffthefate");
		System.out.println(setlist.getBanList());
		setlist.addAnswer("jeffthefate", "JTR");
		setlist.addAnswer("Copperpot5", "JTR");
		System.out.println(setlist.findWinners("jtr", "Current Song [JTR]"));
	}

}
