package com.jeffthefate.setlist;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SetlistTest extends TestCase {
	
	public SetlistTest(String testName) {
		super(testName);
	}
	
	public static Test suite() {
		return new TestSuite(SetlistTest.class);
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

}
