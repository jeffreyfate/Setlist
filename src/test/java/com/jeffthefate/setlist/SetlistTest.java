package com.jeffthefate.setlist;

import com.jeffthefate.utils.GameUtil;
import junit.framework.TestCase;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
import java.util.ArrayList;

public class SetlistTest extends TestCase {
	
	private static final String DEV_KEY = "BXx60ptC4JAMBQLQ965H3g";
	private static final String DEV_SECRET = "0ivTqB1HKqQ6t7HQhIl0tTUNk8uRnv1nhDqyFXBw";
	private static final String DEV_ACCESS_TOKEN = "1265342035-6mYSoxlw8NuZSdWX0AS6cpIu3We2CbCev6rbKUQ";
	private static final String DEV_ACCESS_SECRET = "XqxxE4qLUK3wJ4LHlIbcSP1m6G4spZVmCDdu5RLuU";
	private static final String DEV_ACCOUNT = "dmbtriviatest";

    private Setlist setlist;
    private GameUtil gameUtil;

    public void setUp() throws Exception {
        super.setUp();
        gameUtil = GameUtil.instance();
        setlist = new Setlist("", true, setupDevConfig(), setupDevConfig(),
                new File("src/test/resources/setlist.jpg").getAbsolutePath(),
                new File("src/test/resources/roboto.ttf").getAbsolutePath(), 21,
                70, "", "", "", "D:\\banlist.ser",
                gameUtil.generateSongMatchList(), null, "",
                "6pJz1oVHAwZ7tfOuvHfQCRz6AVKZzg1itFVfzx2q",
                "uNZMDvDSahtRxZVRwpUVwzAG9JdLzx4cbYnhYPi7",
                "target/" + getName() + "Setlist",
                "target/" + getName() + "Scores");
    }
	
	private Configuration setupDevConfig() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey(DEV_KEY)
		  .setOAuthConsumerSecret(DEV_SECRET)
		  .setOAuthAccessToken(DEV_ACCESS_TOKEN)
		  .setOAuthAccessTokenSecret(DEV_ACCESS_SECRET);
		return cb.build();
	}

    private void addAnswers() {
        setlist.addAnswer("jeffthefate", gameUtil.massageResponse("IBYU"));
        setlist.addAnswer("testuser1", gameUtil.massageResponse("ill back u " +
                "up"));
        setlist.addAnswer("testuser2", gameUtil.massageResponse("i'll back " +
                "you up is my guess"));
        setlist.addAnswer("testuser3", gameUtil.massageResponse("smooth " +
                "rider"));
    }

    private void addLotsAnswers() {
        setlist.addAnswer("jeffthefate", gameUtil.massageResponse("IBYU"));
        setlist.addAnswer("testuser1", gameUtil.massageResponse("ill back u " +
                "up"));
        setlist.addAnswer("testuser2", gameUtil.massageResponse("i'll back " +
                "you up is my guess"));
        setlist.addAnswer("testuser3", gameUtil.massageResponse("smooth " +
                "rider"));
        setlist.addAnswer("testuser4", gameUtil.massageResponse("ill back u " +
                "up"));
        setlist.addAnswer("testuser5", gameUtil.massageResponse("ill back u " +
                "up"));
        setlist.addAnswer("testuser6", gameUtil.massageResponse("ill back u " +
                "up"));
    }

    private void addIncorrectAnswers() {
        setlist.addAnswer("jeffthefate", gameUtil.massageResponse("blue " +
                "water"));
        setlist.addAnswer("testuser1", gameUtil.massageResponse("ants"));
        setlist.addAnswer("testuser2", gameUtil.massageResponse("i did it"));
        setlist.addAnswer("testuser3", gameUtil.massageResponse("smooth " +
                "rider"));
    }
	/*
	public void testUpdateStatus() {
		Setlist setlist = new Setlist("", 0, true, setupDevConfig(),
				setupDevConfig(), "", "", 0, 0, "", "", "", null, "");
		setlist.tweetSong("[TEST] Test message 10", "", null, -1, false);
		setlist.tweetSong("[ENCORE:] Test message 11", "TEST GAME MESSAGE 11", null, -1, true);
		setlist.tweetSong("[Final:] Test message 12", "TEST GAME MESSAGE 12", null, -1, true);
		setlist.tweetSong("Test message 13", "TEST GAME MESSAGE 13", null, -1, true);
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
		setlist.createWinnersMessage("Stolen Away On 55th & 3rd",
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
	
	public void testBanList() {
		Setlist setlist = new Setlist("", true, setupDevConfig(),
				setupDevConfig(), "D:\\setlist.jpg", "D:\\roboto.ttf", 21, 70,
				"", "", "", "D:\\banlist.ser", null, null, "");
		setlist.addAnswer("jeffthefate", "JTR");
		setlist.addAnswer("Copperpot5", "JTR");
		setlist.banUser("jeffthefate");
		System.out.println(setlist.getBanList());
		System.out.println(setlist.createWinnersMessage("jtr", "Current Song [JTR]"));
		setlist.unbanUser("jeffthefate");
		System.out.println(setlist.getBanList());
		setlist.addAnswer("jeffthefate", "JTR");
		setlist.addAnswer("Copperpot5", "JTR");
		System.out.println(setlist.createWinnersMessage("jtr", "Current Song [JTR]"));
	}

	public void testObjectIdFromResponse() {
		Setlist setlist = new Setlist("", true, setupDevConfig(),
				setupDevConfig(), "D:\\setlist.jpg", "D:\\roboto.ttf", 21, 70,
				"", "", "", "D:\\banlist.ser", null, null, "",
                "6pJz1oVHAwZ7tfOuvHfQCRz6AVKZzg1itFVfzx2q",
                "uNZMDvDSahtRxZVRwpUVwzAG9JdLzx4cbYnhYPi7");
		String objectId = setlist.getObjectIdFromResponse("{\"results\":[{\"set\":\"Jun 14 2014\\nDave Matthews Band\\nSusquehanna Bank Center\\nCamden, NJ\\n\\nShow begins @ 7:00 pm EDT\",\"setDate\":{\"__type\":\"Date\",\"iso\":\"2014-06-14T00:00:00.000Z\"},\"venue\":{\"__type\":\"Pointer\",\"className\":\"Venue\",\"objectId\":\"sRnwmhzwPP\"},\"plays\":{\"__type\":\"Relation\",\"className\":\"Play\"},\"createdAt\":\"2014-06-14T22:30:31.951Z\",\"updatedAt\":\"2014-06-14T22:30:31.951Z\",\"objectId\":\"UCnjunxzHy\"}]}");
		System.out.println(objectId);
	}
	*/
    public void testLiveSetlist() {
        setlist.liveSetlist("src/test/resources/test.txt");
        ArrayList<String> setList = new ArrayList<String>();
        setList.add("Beach Ball*");
        setList.add("Bartender+");
        setList.add("Slip Slidin Away~");
        setList.add("Improv");
        setList.add("Two Step");
        setList.add("Snow Outside");
        setList.add("Tripping Billies");
        setList.add("Ill Back You UpÄ");
        setList.add("What Would You Say");
        setList.add("Set Break");
        setList.add("Save Me");
        setList.add("Seven");
        setList.add("Crush");
        setList.add("Belly Belly Nice");
        setList.add("Proudest Monkey ->");
        setList.add("Satellite");
        setList.add("Drive In Drive Out");
        setList.add("If Only");
        setList.add("The Song That Jane Likes");
        setList.add("So Right");
        setList.add("Dancing Nancies ->");
        setList.add("Warehouse");
        assertEquals("Set lists are not equal!", setList, setlist.getSetList());
        ArrayList<String> noteList = new ArrayList<String>();
        noteList.add("Notes:");
        noteList.add("Ä Carter, Dave, Stefan and Tim");
        noteList.add("+ Dave And Tim");
        noteList.add("~ Carter, Dave, Rashwn, Stefan and Tim");
        noteList.add("* Dave Solo");
        noteList.add("-> indicates a segue into next song");
        assertEquals("Note lists are not equal!", noteList,
                setlist.getNoteList());
    }

    public void testSortUsersMap() {
        addLotsAnswers();
        setlist.findWinners("Ill Back You UpÄ");
        ArrayList<String> sorted = new ArrayList<String>(0);
        sorted.add("jeffthefate");
        sorted.add("testuser1");
        sorted.add("testuser2");
        sorted.add("testuser4");
        sorted.add("testuser5");
        sorted.add("testuser6");
        assertEquals("Sorted maps not equal!", sorted,
                new ArrayList<String>(setlist.sortUsersMap().keySet()));
    }

    public void testSortUsersMapEmpty() {
        setlist.findWinners("Ill Back You UpÄ");
        ArrayList<String> sorted = new ArrayList<String>(0);
        assertEquals("Sorted maps not equal!", sorted,
                new ArrayList<String>(setlist.sortUsersMap().keySet()));
    }

    public void testCreatePlayersMessage() {
        addAnswers();
        String message = setlist.createPlayersMessage(
                setlist.findWinners("Ill Back You UpÄ"), setlist.getUsersMap(),
                "Current #DMB Song & Setlist: [Ill Back You UpÄ]",
                "\nCorrect guesses:");
        assertEquals("Players message not correct!", "\nCorrect guesses:\n" +
                "#1 - @jeffthefate (1)\n#2 - @testuser1 (1)\n" +
                "#3 - @testuser2 (1)", message);
    }

    public void testCreatePlayersMessageLong() {
        addLotsAnswers();
        String message = setlist.createPlayersMessage(
                setlist.findWinners("Ill Back You UpÄ"), setlist.getUsersMap(),
                "Current #DMB Song & Setlist: [Ill Back You UpÄ]",
                "\nCorrect guesses:");
        assertEquals("Players message not correct!", "\nCorrect guesses:\n" +
                "#1 - @jeffthefate (1)\n#2 - @testuser1 (1)\n" +
                "#3 - @testuser2 (1)", message);
    }

    public void testFindWinnersCorrect() {
        addAnswers();
        ArrayList<String> winners = new ArrayList<String>(0);
        winners.add("jeffthefate");
        winners.add("testuser1");
        winners.add("testuser2");
        assertEquals("Winner lists don't match!", winners,
                setlist.findWinners("Ill Back You UpÄ"));
    }

    public void testFindWinnersIncorrect() {
        addIncorrectAnswers();
        ArrayList<String> winners = new ArrayList<String>(0);
        assertEquals("Winner lists don't match!", winners,
                setlist.findWinners("Ill Back You UpÄ"));
    }
}
