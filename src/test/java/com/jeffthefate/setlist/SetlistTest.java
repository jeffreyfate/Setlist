package com.jeffthefate.setlist;

import com.jeffthefate.utils.CredentialUtil;
import com.jeffthefate.utils.GameUtil;
import com.jeffthefate.utils.Parse;
import junit.framework.TestCase;
import twitter4j.conf.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class SetlistTest extends TestCase {

    private Setlist setlist;
    private GameUtil gameUtil;

    public void setUp() throws Exception {
        super.setUp();
        gameUtil = GameUtil.instance();
        CredentialUtil credentialUtil = CredentialUtil.instance();
        Parse parse = credentialUtil.getCredentialedParse(true,
                "D:\\parseCreds");
        Configuration configuration = credentialUtil.getCredentialedTwitter(
                parse, false);
        setlist = new Setlist("", true, configuration, configuration,
                new File("src/test/resources/setlist.jpg").getAbsolutePath(),
                new File("src/test/resources/roboto.ttf").getAbsolutePath(), 35,
                140, 20, "Game Title", 40, 20, 10, 200, 100, "", "", "",
                "D:\\banlist.ser", "D:\\scores.ser",
                gameUtil.generateSongMatchList(), gameUtil.generateSymbolList(),
                "", parse, "target/" + getName() + "Setlist",
                "target/" + getName() + "Scores");
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

    public void testStripSpecialCharacters() {
        String testSong = "(Don't Drink The Water)*+5||Ä�~->";
        String result = setlist.stripSpecialCharacters(testSong);
        assertEquals("Stripped song not the same!", "Don't Drink The Water",
                result);
        testSong = "Fool In The Rain->+%";
        result = setlist.stripSpecialCharacters(testSong);
        assertEquals("Stripped song not the same!", "Fool In The Rain%",
                result);
    }

    public void testConvertStringToDate() {
        Date date = new Date(1404086400000l);
        Date newDate = gameUtil.convertStringToDate("MM/dd/yy", "6/30/14");
        assertEquals("Dates don't match!", date, newDate);
        assertNull("Date returned not null!", gameUtil.convertStringToDate(null,
                null));
    }

    public void testLiveSetlist() {
        setlist.liveSetlist("src/test/resources/test.txt");
        ArrayList<String> setList = new ArrayList<>();
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
        ArrayList<String> noteList = new ArrayList<>();
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
        ArrayList<String> sorted = new ArrayList<>(0);
        sorted.add("jeffthefate");
        sorted.add("testuser1");
        sorted.add("testuser2");
        sorted.add("testuser4");
        sorted.add("testuser5");
        sorted.add("testuser6");
        assertEquals("Sorted maps not equal!", sorted,
                new ArrayList<>(setlist.sortUsersMap().keySet()));
    }

    public void testCreateWinnersMessage() {
        addLotsAnswers();
        String message = setlist.createWinnersMessage("Ill Back You UpÄ",
                "Current #DMB Song & Setlist: [Ill Back You UpÄ]");
        assertEquals("Winners message not correct!", "\n" +
                "Correct guesses:\n#1 - @jeffthefate (1)\n" +
                "#2 - @testuser1 (1)\n#3 - @testuser2 (1)", message);
    }

    public void testSortUsersMapEmpty() {
        setlist.findWinners("Ill Back You UpÄ");
        ArrayList<String> sorted = new ArrayList<>(0);
        assertEquals("Sorted maps not equal!", sorted,
                new ArrayList<>(setlist.sortUsersMap().keySet()));
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
        ArrayList<String> winners = new ArrayList<>(0);
        winners.add("jeffthefate");
        winners.add("testuser1");
        winners.add("testuser2");
        assertEquals("Winner lists don't match!", winners,
                setlist.findWinners("Ill Back You UpÄ"));
    }

    public void testFindWinnersIncorrect() {
        addIncorrectAnswers();
        ArrayList<String> winners = new ArrayList<>(0);
        assertEquals("Winner lists don't match!", winners,
                setlist.findWinners("Ill Back You UpÄ"));
    }

}
