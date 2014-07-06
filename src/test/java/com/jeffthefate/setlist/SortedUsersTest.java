package com.jeffthefate.setlist;

import com.jeffthefate.utils.GameComparator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class SortedUsersTest extends TestCase {
	
	public SortedUsersTest(String testName) {
		super(testName);
	}
	
	public static Test suite() {
		return new TestSuite(SortedUsersTest.class);
	}
	
	public void testSortedUsers() {
		HashMap<Object, Object> usersMap = new HashMap<>(0);
		TreeMap<Object, Object> sortedUsersMap =
	    		new TreeMap<>(new GameComparator(usersMap));
		usersMap.put("jeffthefate", 5);
		usersMap.put("copperpot5", 2);
		usersMap.put("tjapple", 3);
		usersMap.put("dmbsly", 1);
		usersMap.put("duh", 0);
		sortedUsersMap.putAll(usersMap);
		int last = 100000;
		for (Entry<Object, Object> user : sortedUsersMap.entrySet()) {
			assertTrue("Not sorted correctly!", (Integer) user.getValue() <
                    last);
            last = (Integer) user.getValue();
		}
	}

}
