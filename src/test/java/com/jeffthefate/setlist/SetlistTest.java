package com.jeffthefate.setlist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		Setlist setlist = new Setlist("", 0, true, null, null, "", "", 0, 0, "", "", "", null, "");
		setlist.liveSetlist("C:\\Users\\Jeff\\Desktop\\testSeventeen.txt");
		List<String> locList = setlist.getLocList();
		List<String> setList = setlist.getSetList();
		List<String> noteList = setlist.getNoteList();
		Map<Integer, String> noteMap = setlist.getNoteMap();
		StringBuilder sb = new StringBuilder();
		if (locList.size() < 4)
        	locList.add(1, "Dave Matthews Band");
        for (String loc : locList) {
        	sb.append(loc);
        	sb.append("\n");
        }
        sb.append("\n");
        for (String set : setList) {
        	if (set.toLowerCase().equals("encore:")) {
    			sb.append("\n");
    			sb.append(set);
    			sb.append("\n");
        	}
        	else if (set.toLowerCase().equals("set break")) {
    			sb.append("\n");
    			sb.append(set);
    			sb.append("\n");
    			sb.append("\n");
        	}
        	else {
        		sb.append(set);
        		sb.append("\n");
        	}
        }
        if (!noteMap.isEmpty()) {
        	for (Entry<Integer, String> note : noteMap.entrySet()) {
        		sb.append("\n");
            	sb.append(note.getValue());
        	}
        }
        else if (!noteList.isEmpty()) {
        	for (String note : noteList) {
        		sb.append("\n");
            	sb.append(note);
        	}
        }
        System.out.println(sb.toString());
        System.out.println(setList);
	}
	
	public void testMassageAnswer() {
		Setlist setlist = new Setlist("", 0, true, null, null, "", "", 0, 0, "", "", "", null, "");
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
	*/
	public void testPostSetlistScores() {
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "D:\\setlist.jpg", "D:\\roboto.ttf", 21, 70,
				"", "", "", null, null, "");
		HashMap<String, Integer> winnerMap = new HashMap<String, Integer>(0);
		/*
		winnerMap.put("Copperpot5", 1);
		winnerMap.put("jeffthefate", 1);
		winnerMap.put("testersonman", 2);
		winnerMap.put("testy", 4);
		winnerMap.put("boydtinsley46", 7);
		setlist.postSetlistScoresText("[Current Scores]", winnerMap);
		*/
		winnerMap.put("Copperpot5", 3);
		winnerMap.put("jeffthefate", 4);
		winnerMap.put("testersonman", 5);
		winnerMap.put("suckerman", 1);
		winnerMap.put("testy", 5);
		winnerMap.put("boydtinsley45", 12);
		setlist.postSetlistScoresImage("[Final Scores]", "Top Scores",
				winnerMap);
	}
}
