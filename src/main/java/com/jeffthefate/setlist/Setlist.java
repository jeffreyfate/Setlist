package com.jeffthefate.setlist;

/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import twitter4j.DirectMessage;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserMentionEntity;
import twitter4j.UserStreamListener;
import twitter4j.conf.Configuration;

import com.jeffthefate.Screenshot;
import com.jeffthefate.SetlistScreenshot;

public class Setlist implements UserStreamListener {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String TWEET_DATE_FORMAT = "yyyy-MM-dd";
    
    private static String lastSong = "";
    private static boolean hasEncore = false;
    private static boolean hasGuests = false;
    private static boolean hasSegue = false;
    private static boolean firstBreak = false;
    private static boolean secondBreak = false;
    
    private static Calendar cal;
    private static long endTime = -1;
    
    private static String setlistText = "";
    
    private static String currDateString = null;
    private static String tweetDateString = null;
    
    private static Screenshot screenshot;
    
    private String url;
    private long duration;
    
    private boolean isDev;
    
    private String setlistJpgFilename;
    private String fontFilename;
    private int fontSize;
    private int verticalOffset;
    private String setlistFilename;
    private String lastSongFilename;
    private String setlistDir;
    
    private Configuration setlistConfig;
    private Configuration gameConfig;
    
    private String currAccount;
    
    private static LinkedHashMap<String, String> answers = new LinkedHashMap<String, String>();
    
    private ArrayList<ArrayList<String>> nameList =
    		new ArrayList<ArrayList<String>>(0);
    
    private TwitterStream twitterStream;
    private boolean kill = false;
    
    private static ArrayList<String> locList = new ArrayList<String>();
    private static ArrayList<String> setList = new ArrayList<String>();
    private static ArrayList<String> noteList = new ArrayList<String>();
    private static TreeMap<Integer, String> noteMap =
    		new TreeMap<Integer, String>();
    
    private HashMap<String, Integer> usersMap = new HashMap<String, Integer>();
    private GameComparator gameComparator = new GameComparator(usersMap);
    private TreeMap<String, Integer> sortedUsersMap =
    		new TreeMap<String, Integer>(gameComparator);
    
    // java -jar /home/Setlist-One.jar 14400000 >> ...
    
    public Setlist(String url, long duration, boolean isDev,
    		Configuration setlistConfig, Configuration gameConfig,
    		String setlistJpgFilename, String fontFilename, int fontSize,
    		int verticalOffset, String setlistFilename, String lastSongFilename,
    		String setlistDir, ArrayList<ArrayList<String>> nameList,
    		String currAccount) {
    	this.url = url;
    	this.duration = duration;
    	this.isDev = isDev;
    	this.setlistConfig = setlistConfig;
    	this.gameConfig = gameConfig;
    	this.setlistJpgFilename = setlistJpgFilename;
    	this.fontFilename = fontFilename;
    	this.fontSize = fontSize;
    	this.verticalOffset = verticalOffset;
    	this.setlistFilename = setlistFilename;
    	this.lastSongFilename = lastSongFilename;
    	this.setlistDir = setlistDir;
    	this.nameList = nameList;
    	this.currAccount = currAccount;
    }
    
    public void startSetlist() {
    	watchTwitterStream();
    	endTime = System.currentTimeMillis() + duration;
    	do {
    		runSetlistCheck(url);
    		try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
    	} while (endTime >= System.currentTimeMillis() && !kill);
    	System.out.println("duration: " + duration);
    	if (duration > 0) {
    		String setlistMessage = "";
    		StringBuilder sb = new StringBuilder();
    		sb.append("[Final #DMB ");
    		sb.append(tweetDateString);
    		sb.append(" Setlist]");
    		setlistMessage = sb.toString();
    		System.out.println(sb.toString());
    		screenshot = new SetlistScreenshot(setlistJpgFilename, fontFilename,
    				setlistText, fontSize, verticalOffset);
    		String gameMessage = "";
    		postTweet(setlistMessage, gameMessage,
    				new File(screenshot.getOutputFilename()), -1, false);
    	}
    	twitterStream.cleanUp();
    	twitterStream.shutdown();
    }
    
    public List<String> getLocList() {
    	return locList;
    }
    
    public List<String> getSetList() {
    	return setList;
    }
    
    public List<String> getNoteList() {
    	return noteList;
    }
    
    public Map<Integer, String> getNoteMap() {
    	return noteMap;
    }
    
    private void runSetlistCheck(String url) {
    	cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(System.currentTimeMillis());
        System.out.println(cal.getTime().toString());
        System.out.println(Charset.defaultCharset().displayName());
        /*
        postNotification(getPushJsonString("Show begins @ 8:05 pm EDT", "Jun 16 2013\nDave Matthews Band\nComcast Center\nMansfield, MA\n\nShow begins @ 8:05 pm EDT\n",
                getExpireDateString()));
        */
        String html = null;
        if (url != null)
        	html = liveSetlist(url);
        else	
        	html = liveSetlist("https://whsec1.davematthewsband.com/backstage.asp");
        currDateString = getNewSetlistDateString(locList.get(0));
        tweetDateString = getTweetDateString(locList.get(0));
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
        setlistText = sb.toString();
        System.out.println(setlistText);
        // createScreenshot(setlistText);
        String setlistFile = setlistFilename +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSongFile = lastSongFilename +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSetlist = readStringFromFile(setlistFile);
        System.out.println("lastSetlist:");
        System.out.println(lastSetlist);
        String diff = StringUtils.difference(lastSetlist, setlistText);
        System.out.println("diff:");
        System.out.println(diff);
        boolean hasChange = !StringUtils.isBlank(diff);
        sb.setLength(0);
        if (hasChange) {
            writeStringToFile(setlistText, setlistFile);
            // -1 if failure or not a new setlist
            // 0 if a new setlist (latest)
            // 1 if there is a newer date available already
            int newDate = uploadLatest(setlistText);
            /*
            if (getLatestDate().after(convertStringToDate(DATE_FORMAT,
            		currDateString)))
            	return;
            	*/
            String lastSongFromFile = readStringFromFile(lastSongFile);
            if (newDate == 0 || (newDate == -1 &&
            		!lastSongFromFile.equals(lastSong))) {
            	if (!stripSpecialCharacters(lastSongFromFile).equals(
            			stripSpecialCharacters(lastSong))) {
            		System.out.println("POST NOTIFICATION AND TWEET: " +
            				lastSong);
            		String gameMessage = "";
            		if (!isDev) {
            			postNotification(getPushJsonString(lastSong,
            					setlistText, getExpireDateString()));
            		}
	                if (lastSong.toLowerCase().startsWith("show begins")) {
	                	sb.append("DMB ");
	                	sb.append(lastSong);
	                }
	                else {
	                	sb.append("Current #DMB Song & Setlist: [");
	                    sb.append(Setlist.lastSong);
	                    sb.append("]");
		                gameMessage = findWinners(lastSong);
	                }
	                screenshot = new SetlistScreenshot(
		    				setlistJpgFilename, fontFilename, setlistText,
		    				fontSize, verticalOffset);
	                postTweet(sb.toString(), gameMessage,
	                		new File(screenshot.getOutputFilename()), -1, true);
            	}
            	else {
            		System.out.println("POST NOTIFICATION: BLANK");
            		if (!isDev) {
            			postNotification(getPushJsonString("", setlistText,
            					getExpireDateString()));
            		}
            	}
                writeStringToFile(lastSong, lastSongFile);
            }
            else if (readStringFromFile(lastSongFile).equals(lastSong)) {
            	System.out.println("POST NOTIFICATION: BLANK");
            	if (!isDev) {
            		postNotification(getPushJsonString("", setlistText,
            				getExpireDateString()));
            		postTweet(StringUtils.strip(diff), "", null, -1, false);
            	}
            }
            System.out.println(html);
        }
        locList.clear();
        setList.clear();
        noteList.clear();
        noteMap.clear();
        /*
        sb.setLength(0);
        sb.append("Current song: [");
        sb.append("Testing");
        sb.append("] Get live updates on your Android: https://play.google.com/store/apps/details?id=com.jeffthefate.dmbquiz");
        authTweet(sb.toString(), new File(createScreenshot(readStringFromFile("/home/setlist2013-05-26T00:00:00.000Z.txt"))));
        if (args.length > 0)
        	newSetlist(args[0]);
        else	
        	newSetlist("https://whsec1.davematthewsband.com/backstage.asp?Month=5&year=2013&ShowID=1287526");
        	//newSetlist("https://whsec1.davematthewsband.com/backstage.asp?Month=5&year=2013&ShowID=1287462");
        //archiveSetlists();
        String setlistText;
        if (args.length > 0)
        	setlistText = latestSetlist(args[0]);
        else	
        	setlistText = latestSetlist("https://whsec1.davematthewsband.com/backstage.asp");
        //String setlistText = latestSetlist("http://jeffthefate.com/dmb-trivia-test");
        System.out.println(setlistText);
        currDateString = getSetlistDateString(setlistText);
        String setlistFile = SETLIST_FILENAME +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSongFile = LAST_SONG_FILENAME +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSetlist = readStringFromFile(setlistFile);
        System.out.println("lastSetlist:");
        System.out.println(lastSetlist);
        String diff = StringUtils.difference(lastSetlist, setlistText);
        System.out.println("diff:");
        System.out.println(diff);
        boolean hasChange = !StringUtils.isBlank(diff);
        StringBuilder sb = new StringBuilder();
        if (hasChange) {
            writeStringToFile(setlistText, setlistFile);
            // -1 if failure or not a new setlist
            // 0 if a new setlist (latest)
            // 1 if there is a newer date available already
            int newDate = uploadLatest(setlistText);
            if (newDate == 0 || (newDate == -1 &&
            		!readStringFromFile(lastSongFile).equals(lastSong))) {
                postNotification(getPushJsonString(lastSong, setlistText,
                        getExpireDateString()));
                writeStringToFile(lastSong, lastSongFile);
                sb.append("Current song: [");
                sb.append(lastSong);
                sb.append("] Get live updates on your Android: https://play.google.com/store/apps/details?id=com.jeffthefate.dmbquiz");
                authTweet(sb.toString(), new File(createScreenshot(setlistText)));
            }
            else if (readStringFromFile(lastSongFile).equals(lastSong)) {
            	postNotification(getPushJsonString("", setlistText,
                        getExpireDateString()));
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Current song: [");
        sb.append("Testing");
        sb.append("] Get live updates on your Android: https://play.google.com/store/apps/details?id=com.jeffthefate.dmbquiz");
        authTweet(sb.toString(), new File(createScreenshot(readStringFromFile("/home/setlist2013-05-26T00:00:00.000Z.txt"))));
        */
    }
    
    private String stripSpecialCharacters(String song) {
    	song = StringUtils.remove(song, "(");
    	song = StringUtils.remove(song, ")");
    	song = StringUtils.remove(song, "->");
    	song = StringUtils.remove(song, "*");
    	song = StringUtils.remove(song, "+");
    	song = StringUtils.remove(song, "~");
    	song = StringUtils.remove(song, "�");
    	song = StringUtils.remove(song, "Ä");
    	song = StringUtils.trim(song);
    	return song;
    }
    
    private Date getLatestDate() {
    	File file = new File(setlistDir);
    	String[] files = file.list();
    	Date latest = null;
        Date date = null;
        String curr = "";
        String dateString = "";
    	for (int i = 0; i < files.length; i++) {
    		curr = files[i];
    		dateString = curr.substring("setlist".length(),
    				curr.indexOf(".txt"));
    		date = convertStringToDate(DATE_FORMAT, dateString);
    		if (latest == null || date.after(latest))
    			latest = date;
    	}
    	return latest;
    }
    
    private Date convertStringToDate(String format, String dateString) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat(format);
    	Date date = null;
    	try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e2) {
            System.out.println("Failed to parse date from " + dateString);
            e2.printStackTrace();
        }
    	return date;
    }
    
    private HttpClient createSecureConnection() {
    	// SSL context for secure connections can be created either based on
        // system or application specific properties.
        SSLContext sslcontext = SSLContexts.createSystemDefault();
        // Use custom hostname verifier to customize SSL hostname verification.
        X509HostnameVerifier hostnameVerifier = new BrowserCompatHostnameVerifier();
    	Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", new SSLConnectionSocketFactory(sslcontext, hostnameVerifier))
                .build();

        PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager(
        		socketFactoryRegistry);
        
        return HttpClientBuilder.create().setConnectionManager(mgr).build();
    }
    
    private void archiveSetlists() {
        HttpPost postMethod = new HttpPost(
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept",
                "text/html, application/xhtml+xml, */*");
        postMethod.addHeader("Referer",
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept-Language", "en-US");
        postMethod.addHeader("User-Agent", "Mozilla/5.0 (compatible; " +
        		"MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        postMethod.addHeader("Content-Type",
                "application/x-www-form-urlencoded");
        postMethod.addHeader("Accept-Encoding", "gzip, deflate");
        postMethod.addHeader("Host", "whsec1.davematthewsband.com");
        postMethod.addHeader("Connection", "Keep-Alive");
        postMethod.addHeader("Cache-Control", "no-cache");
        postMethod.addHeader("Cookie", "MemberInfo=isInternational=&" +
        		"MemberID=&UsrCount=04723365306&ExpDate=&Username=; " +
        		"ASPSESSIONIDQQTDRTTC=PKEGDEFCJBLAIKFCLAHODBHN; __utma=" +
        		"10963442.556285711.1366154882.1366154882.1366154882.1; " +
        		"__utmb=10963442.2.10.1366154882; __utmc=10963442; " +
        		"__utmz=10963442.1366154882.1.1.utmcsr=warehouse.dmband.com" +
        		"|utmccn=(referral)|utmcmd=referral|utmcct=/; " +
        		"ASPSESSIONIDSSDRTSRA=HJBPPKFCJGEJKGNEMJJMAIPN");
        
        List<NameValuePair> nameValuePairs =
                new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("the_url", ""));
        nameValuePairs.add(new BasicNameValuePair("form_action", "login"));
        nameValuePairs.add(new BasicNameValuePair("Username", "fateman"));
        nameValuePairs.add(new BasicNameValuePair("Password", "nintendo"));
        nameValuePairs.add(new BasicNameValuePair("x", "0"));
        nameValuePairs.add(new BasicNameValuePair("y", "0"));
        try {
            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e1) {}
        HttpClient client = createSecureConnection();
        HttpResponse response = null;
        try {
            response = client.execute(postMethod);
        } catch (IOException e1) {}
        if (response == null || (response.getStatusLine().getStatusCode() !=
                200 && response.getStatusLine().getStatusCode() != 302))
            return;
        // https://whsec1.davematthewsband.com/backstage.asp?Month=7&year=2009&ShowID=1286649
        // https://whsec1.davematthewsband.com/backstage.asp?Month=9&year=2012&ShowID=1287166
        for (int i = 1992; i < 1993; i++) {
            HttpGet getMethod = new HttpGet(
                    "https://whsec1.davematthewsband.com/backstage.asp?year=" +
                    		i);
            StringBuilder sb = new StringBuilder();
            String html = null;
            try {
                response = client.execute(getMethod);
                html = EntityUtils.toString(response.getEntity(), "UTF-8");
                html = StringEscapeUtils.unescapeHtml4(html);
            } catch (ClientProtocolException e1) {
                System.out.println("Failed to connect to " +
                        getMethod.getURI().toASCIIString());
                e1.printStackTrace();
            } catch (IOException e1) {
                System.out.println("Failed to get setlist from " +
                        getMethod.getURI().toASCIIString());
                e1.printStackTrace();
            }
            Document doc = Jsoup.parse(html);
            Elements links;
            if (doc != null) {
                Element body = doc.body();
                links = body.getElementsByAttributeValue("id",
                        "itemHeaderSmall");
                String currUrl;
                for (Element link : links) {
                    currUrl = "https://whsec1.davematthewsband.com/" +
                    		link.attr("href");
                    System.out.println();
                    getMethod = new HttpGet(currUrl);
                    sb = new StringBuilder();
                    html = null;
                    try {
                        response = client.execute(getMethod);
                        html = EntityUtils.toString(response.getEntity(),
                        		"UTF-8");
                        html = StringEscapeUtils.unescapeHtml4(html);
                    } catch (ClientProtocolException e1) {
                        System.out.println("Failed to connect to " +
                                getMethod.getURI().toASCIIString());
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        System.out.println("Failed to get setlist from " +
                                getMethod.getURI().toASCIIString());
                        e1.printStackTrace();
                    }
                    doc = Jsoup.parse(html);
                    char badChar = 65533;
                    char apos = 39;
                    char endChar = 160;
                    StringBuilder locString = new StringBuilder();
                    String dateString = null;
                    StringBuilder setString = new StringBuilder();
                    int numTicketings = 0;
                    boolean br = false;
                    boolean b = false;
                    int slot = 0;
                    String setlistId = null;
                    System.out.println("nulling lastPlay");
                    String lastPlay = null;
                    boolean hasSetCloser = false;
                    hasEncore = false;
                    hasGuests = false;
                    hasSegue = false;
                    firstBreak = false;
                    secondBreak = false;
                    sb.setLength(0);
                    if (doc != null) {
                        body = doc.body();
                        Elements ticketings = body.getElementsByAttributeValue(
                        		"id", "ticketingColText");
                        for (Element ticketing : ticketings) {
                            for (Element single : ticketing.getAllElements()) {
                                if (single.tagName().equals("span")) {
                                	if (locString.length() > 0) {
                                		dateString = getNewSetlistDateString(
                                				locString.toString());
                                		setlistId = createLatest(dateString);
                                	}
                                    for (Node node : single.childNodes()) {
                                        if (!(node instanceof Comment)) {
                                            if (node instanceof TextNode) {
                                            	System.out.println(
                                            			"TextNode is blank: " +
                                            			StringUtils.isBlank(
                                        					((TextNode) node)
                                        						.text()));
                                            	if (lastPlay != null &&
                                            			!StringUtils.isBlank(
                                        					((TextNode) node)
                                        						.text())) {
                                            		uploadSong(lastPlay, ++slot,
                                            				setlistId,
                                            				slot == 1, false,
                                            				false);
                                            		System.out.println(
                                            				"TextNode nulling" +
                                            				" lastPlay");
                                            		System.out.println(
                                            				"TextNode: '" +
                                    						((TextNode) node)
                                    							.text() + "'");
                                            		lastPlay = null;
                                            	}
                                                sb.append(StringUtils.remove(
                                            		((TextNode) node).text(),
                                            		endChar));
                                            } else {
                                                if (node.nodeName()
                                                		.equals("div")) {
                                                    // End current string
                                                    if (setString.length() > 0)
                                                        setString.append("\n");
                                                    if (StringUtils
                                                    		.replaceChars(
                                                            StringUtils.strip(
                                                                sb.toString()),
                                                                badChar, apos)
                                                            .startsWith(
                                                        		"Encore") &&
                                                    		!hasEncore) {
                                                        hasEncore = true;
                                                        if (lastPlay != null &&
                                                        		!hasSetCloser) {
                                                        	uploadSong(lastPlay,
                                                    			++slot,
                                                    			setlistId,
                                                    			slot == 1, true,
                                                    			false);
                                                        	hasSetCloser = true;
                                                        	System.out.println(
                                                    			"div nulling " +
                                                    			"lastPlay");
                                                        	lastPlay = null;
                                                        }
                                                        if (!firstBreak) {
                                                            setString.append(
                                                        		"\n");
                                                            firstBreak = true;
                                                        }
                                                        if (sb.indexOf(":") ==
                                                        		-1) {
                                                        	sb.setLength(0);
                                                        	sb.append(
                                                        			"Encore:");
                                                        }
                                                    }
                                                    else {
                                                    	lastPlay = StringUtils
                                                			.replaceChars(
                                        					StringUtils.strip(
                                                                sb.toString()),
                                                                badChar, apos);
                                                    }
                                                    setString.append(StringUtils
                                                    		.replaceChars(
                                                            StringUtils.strip(
                                                                sb.toString()),
                                                                badChar, apos));
                                                    setString.trimToSize();
                                                    sb.setLength(0);
                                                }
                                                else if (node.nodeName().equals(
                                                		"br")) {
                                                    /*
                                                    if (!hasBreak && hasEncore) {
                                                        setString.append("\n");
                                                        hasBreak = true;
                                                    }
                                                    */
                                                    if (sb.length() > 0 &&
                                                        !StringUtils.isBlank(
                                                            sb.toString())) {
                                                        if (setString.length() >
                                                        		0)
                                                            setString.append(
                                                            		"\n");
                                                        setString.append(
                                                            StringUtils
                                                            	.replaceChars(
                                                                StringUtils.strip(
                                                                        sb.toString()),
                                                                        badChar, apos));
                                                        setString.trimToSize();
                                                        sb.setLength(0);
                                                    }
                                                    if (firstBreak &&
                                                    		!secondBreak &&
                                                    		hasEncore) {
                                                        setString.append("\n");
                                                        secondBreak = true;
                                                        if (lastPlay != null) {
                                                        	uploadSong(lastPlay,
                                                        			++slot,
                                                        			setlistId,
                                                        			slot == 1,
                                                        			false,
                                                        			true);
                                                        	System.out.println(
                                                    			"br nulling " +
                                                    			"lastPlay");
                                                        	lastPlay = null;
                                                        }
                                                    }
                                                    if (!firstBreak) {
                                                    	System.out.println(
                                                			"NOT firstBreak");
                                                    	System.out.println(
                                                			"lastPlay: " +
                                        					lastPlay);
                                                    	System.out.println(
                                                			"hasSetCloser: " +
                                        					hasSetCloser);
                                                        setString.append("\n");
                                                        firstBreak = true;
                                                        if (lastPlay != null &&
                                                        		!hasSetCloser) {
                                                        	uploadSong(lastPlay,
                                                    			++slot,
                                                    			setlistId,
                                                    			slot == 1, true,
                                                    			false);
                                                        	hasSetCloser = true;
                                                        	System.out.println(
                                                    			"!firstBreak " +
                                                    			"nulling lastPlay");
                                                        	lastPlay = null;
                                                        }
                                                    }
                                                }
                                                else if (node.nodeName().equals(
                                                		"img")) {
                                                    sb.append("->");
                                                    hasSegue = true;
                                                    if (!hasGuests) {
                                                        lastSong = StringUtils
                                                    		.chomp(setString
                                                				.toString())
                                            				.substring(
                                                                StringUtils.chomp(
                                                            		setString.toString())
                                                            			.lastIndexOf("\n")+1);
                                                    }
                                                }
                                                else if (node instanceof Element) {
                                                    sb.append(((Element) node)
                                                    		.text());
                                                    if (!StringUtils
                                                    		.replaceChars(
                                                            StringUtils.strip(
                                                                sb.toString()),
                                                                badChar, apos)
                                                            .equals("Encore:")
                                                            	&& !hasGuests) {
                                                        hasGuests = true;
                                                        lastSong = StringUtils
                                                    		.chomp(setString.toString())
                                                    			.substring(
                                                					StringUtils
                                                						.chomp(
                                            								setString.toString())
                                            								.lastIndexOf("\n")+1);
                                                    }
                                                    else if (
                                                		StringUtils.replaceChars(
                                                            StringUtils.strip(
                                                                    sb.toString()),
                                                                    badChar, apos)
                                                            .equals("Encore:")) {
                                                        hasEncore = true;
                                                        lastSong = StringUtils.strip(
                                                        		sb.toString());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!hasSegue && !hasGuests) {
                                        lastSong = StringUtils.strip(
                                        		setString.toString()).substring(
                                                StringUtils.strip(
                                            		setString.toString())
                                            		.lastIndexOf("\n")+1);
                                    }
                                    if (setString.length() > 0)
                                        setString.append("\n");
                                    setString.append(
                                        StringUtils.replaceChars(
                                            StringUtils.strip(
                                                    sb.toString()),
                                                    badChar, apos));
                                    setString.trimToSize();
                                }
                                else if (setString.length() == 0) {
                                    if (single.id().equals("ticketingColText"))
                                        numTicketings++;
                                    if (numTicketings == 2 &&
                                    		single.nodeName().equals("div")) {
                                        locString.append(single.ownText());
                                        locString.append("\n");
                                    }
                                    if (single.tagName().equals("br"))
                                        br = true;
                                    else if (single.tagName().equals("b"))
                                        b = true;
                                    if (br && b) {
                                        locString.append(single.ownText());
                                        locString.append("\n");
                                        br = false;
                                        b = false;
                                    }
                                }
                            }
                        }
                    }
                    uploadLatest(locString.append("\n").append(setString)
                    		.toString());
                }
            }
        }
    }
    
    private String latestSetlist(String url) {
        HttpPost postMethod = new HttpPost(
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept",
                "text/html, application/xhtml+xml, */*");
        postMethod.addHeader("Referer",
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept-Language", "en-US");
        postMethod.addHeader("User-Agent",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; " +
        		"Trident/5.0)");
        postMethod.addHeader("Content-Type",
                "application/x-www-form-urlencoded");
        postMethod.addHeader("Accept-Encoding", "gzip, deflate");
        postMethod.addHeader("Host", "whsec1.davematthewsband.com");
        postMethod.addHeader("Connection", "Keep-Alive");
        postMethod.addHeader("Cache-Control", "no-cache");
        postMethod.addHeader("Cookie",
                "MemberInfo=isInternational=&MemberID=&UsrCount=04723365306" +
        		"&ExpDate=&Username=; ASPSESSIONIDQQTDRTTC=" +
        		"PKEGDEFCJBLAIKFCLAHODBHN; __utma=10963442.556285711." +
        		"1366154882.1366154882.1366154882.1; __utmb=10963442.2.10." +
        		"1366154882; __utmc=10963442; __utmz=10963442.1366154882.1.1" +
        		".utmcsr=warehouse.dmband.com|utmccn=(referral)|utmcmd=" +
        		"referral|utmcct=/; ASPSESSIONIDSSDRTSRA=" +
        		"HJBPPKFCJGEJKGNEMJJMAIPN");
        
        List<NameValuePair> nameValuePairs =
                new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("the_url", ""));
        nameValuePairs.add(new BasicNameValuePair("form_action", "login"));
        nameValuePairs.add(new BasicNameValuePair("Username", "fateman"));
        nameValuePairs.add(new BasicNameValuePair("Password", "nintendo"));
        nameValuePairs.add(new BasicNameValuePair("x", "0"));
        nameValuePairs.add(new BasicNameValuePair("y", "0"));
        try {
            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e1) {}
        HttpResponse response = null;
        HttpClient client = createSecureConnection();
        try {
            response = client.execute(postMethod);
        } catch (IOException e1) {}
        if (response == null || (response.getStatusLine().getStatusCode() !=
                200 && response.getStatusLine().getStatusCode() != 302))
            return "Error";
        HttpGet getMethod = new HttpGet(url);
        StringBuilder sb = new StringBuilder();
        String html = null;
        if (!url.startsWith("https"))
        	client = HttpClientBuilder.create().build();
        try {
            response = client.execute(getMethod);
            html = EntityUtils.toString(response.getEntity(), "UTF-8");
            html = StringEscapeUtils.unescapeHtml4(html);
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    getMethod.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    getMethod.getURI().toASCIIString());
            e1.printStackTrace();
        }
        Document doc = Jsoup.parse(html);
        char badChar = 65533;
        char apos = 39;
        StringBuilder locString = new StringBuilder();
        StringBuilder setString = new StringBuilder();
        int numTicketings = 0;
        boolean br = false;
        boolean b = false;
        sb.setLength(0);
        if (doc != null) {
            Element body = doc.body();
            Elements ticketings = body.getElementsByAttributeValue("id",
                    "ticketingColText");
            for (Element ticketing : ticketings) {
                for (Element single : ticketing.getAllElements()) {
                    if (single.tagName().equals("span")) {
                        for (Node node : single.childNodes()) {
                            if (!(node instanceof Comment)) {
                                if (node instanceof TextNode)
                                    sb.append(((TextNode) node).text());
                                else {
                                    if (node.nodeName().equals("div")) {
                                        // End current string
                                        if (setString.length() > 0)
                                            setString.append("\n");
                                        if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .startsWith("Encore") &&
                                                	!hasEncore) {
                                            hasEncore = true;
                                            if (!firstBreak) {
                                                setString.append("\n");
                                                firstBreak = true;
                                            }
                                            if (sb.indexOf(":") == -1) {
                                            	sb.setLength(0);
                                            	sb.append("Encore:");
                                            }
                                        }
                                        setString.append(
                                            StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos));
                                        setString.trimToSize();
                                        sb.setLength(0);
                                    }
                                    else if (node.nodeName().equals("br")) {
                                        /*
                                        if (!hasBreak && hasEncore) {
                                            setString.append("\n");
                                            hasBreak = true;
                                        }
                                        */
                                        if (sb.length() > 0 &&
                                                !StringUtils.isBlank(
                                                        sb.toString())) {
                                            if (setString.length() > 0)
                                                setString.append("\n");
                                            setString.append(
                                                StringUtils.replaceChars(
                                                    StringUtils.strip(
                                                            sb.toString()),
                                                            badChar, apos));
                                            setString.trimToSize();
                                            sb.setLength(0);
                                        }
                                        if (firstBreak && !secondBreak &&
                                        		hasEncore) {
                                            setString.append("\n");
                                            secondBreak = true;
                                        }
                                        if (!firstBreak) {
                                            setString.append("\n");
                                            firstBreak = true;
                                        }
                                    }
                                    else if (node.nodeName().equals("img")) {
                                        sb.append("->");
                                        hasSegue = true;
                                        if (!hasGuests) {
                                            lastSong = StringUtils.chomp(
                                            		setString.toString())
                                            		.substring(
                                                    StringUtils.chomp(
                                                		setString.toString())
                                                		.lastIndexOf("\n")+1);
                                        }
                                    }
                                    else if (node instanceof Element) {
                                        sb.append(((Element) node).text());
                                        if (!StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:") &&
                                                	!hasGuests) {
                                            hasGuests = true;
                                            lastSong = StringUtils.chomp(
                                            		setString.toString())
                                        			.substring(
                                    					StringUtils.chomp(
                                							setString.toString())
                                							.lastIndexOf("\n")+1);
                                        }
                                        else if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:")) {
                                            hasEncore = true;
                                            lastSong = StringUtils.strip(
                                            		sb.toString());
                                        }
                                    }
                                }
                            }
                        }
                        if (!hasSegue && !hasGuests) {
                            lastSong = StringUtils.strip(setString.toString())
                            		.substring(
                        				StringUtils.strip(setString.toString())
                        					.lastIndexOf("\n")+1);
                        }
                        if (setString.length() > 0)
                            setString.append("\n");
                        setString.append(
                            StringUtils.replaceChars(
                                StringUtils.strip(
                                        sb.toString()),
                                        badChar, apos));
                        setString.trimToSize();
                    }
                    else if (setString.length() == 0) {
                        if (single.id().equals("ticketingColText"))
                            numTicketings++;
                        if (numTicketings == 2 && single.nodeName()
                        		.equals("div")) {
                            locString.append(single.ownText());
                            locString.append("\n");
                        }
                        if (single.tagName().equals("br"))
                            br = true;
                        else if (single.tagName().equals("b"))
                            b = true;
                        if (br && b) {
                            locString.append(single.ownText());
                            locString.append("\n");
                            br = false;
                            b = false;
                        }
                    }
                }
            }
        }
        System.out.println("lastSong: " + lastSong);
        return locString.append("\n").append(setString).toString();
    }
    
    private String newSetlist(String url) {
        HttpPost postMethod = new HttpPost(
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept",
                "text/html, application/xhtml+xml, */*");
        postMethod.addHeader("Referer",
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept-Language", "en-US");
        postMethod.addHeader("User-Agent",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; " +
    			"Trident/5.0)");
        postMethod.addHeader("Content-Type",
                "application/x-www-form-urlencoded");
        postMethod.addHeader("Accept-Encoding", "gzip, deflate");
        postMethod.addHeader("Host", "whsec1.davematthewsband.com");
        postMethod.addHeader("Connection", "Keep-Alive");
        postMethod.addHeader("Cache-Control", "no-cache");
        postMethod.addHeader("Cookie",
                "MemberInfo=isInternational=&MemberID=&UsrCount=04723365306&" +
        		"ExpDate=&Username=; ASPSESSIONIDQQTDRTTC=" +
        		"PKEGDEFCJBLAIKFCLAHODBHN; __utma=10963442.556285711." +
        		"1366154882.1366154882.1366154882.1; __utmb=10963442.2.10." +
        		"1366154882; __utmc=10963442; __utmz=10963442.1366154882.1.1." +
        		"utmcsr=warehouse.dmband.com|utmccn=(referral)|utmcmd=" +
        		"referral|utmcct=/; ASPSESSIONIDSSDRTSRA=" +
        		"HJBPPKFCJGEJKGNEMJJMAIPN");
        
        List<NameValuePair> nameValuePairs =
                new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("the_url", ""));
        nameValuePairs.add(new BasicNameValuePair("form_action", "login"));
        nameValuePairs.add(new BasicNameValuePair("Username", "fateman"));
        nameValuePairs.add(new BasicNameValuePair("Password", "nintendo"));
        nameValuePairs.add(new BasicNameValuePair("x", "0"));
        nameValuePairs.add(new BasicNameValuePair("y", "0"));
        try {
            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e1) {}
        HttpResponse response = null;
        HttpClient client = createSecureConnection();
        try {
            response = client.execute(postMethod);
        } catch (IOException e1) {}
        if (response == null || (response.getStatusLine().getStatusCode() !=
                200 && response.getStatusLine().getStatusCode() != 302))
            return "Error";
        HttpGet getMethod = new HttpGet(url);
        StringBuilder sb = new StringBuilder();
        String html = null;
        if (!url.startsWith("https"))
        	client = HttpClientBuilder.create().build();
        try {
            response = client.execute(getMethod);
            html = EntityUtils.toString(response.getEntity(), "UTF-8");
            html = StringEscapeUtils.unescapeHtml4(html);
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    getMethod.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    getMethod.getURI().toASCIIString());
            e1.printStackTrace();
        }
        Document doc = Jsoup.parse(html);
        char badChar = 65533;
        char apos = 39;
        char endChar = 160;
        StringBuilder locString = new StringBuilder();
        String dateString = null;
        StringBuilder setString = new StringBuilder();
        int numTicketings = 0;
        boolean br = false;
        boolean b = false;
        int slot = 0;
        String setlistId = null;
        String lastPlay = null;
        boolean hasSetCloser = false;
        hasEncore = false;
        hasGuests = false;
        hasSegue = false;
        firstBreak = false;
        secondBreak = false;
        String divStyle = "";
        String locStyle = "padding-bottom:12px;padding-left:3px;color:#3995aa;";
        String setStyle = "font-family:sans-serif;font-size:14;font-weight:" +
        		"normal;margin-top:15px;margin-left:15px;";
        sb.setLength(0);
        if (doc != null) {
            Element body = doc.body();
            Elements divs = body.getElementsByTag("div");
            for (Element div : divs) {
            	if (div.hasAttr("style")) {
            		divStyle = div.attr("style");
            		if (divStyle.equals(locStyle))
            			System.out.println("LOC: " + div.ownText());
        			else if (divStyle.equals(setStyle)) {
        				String divText = div.ownText();
        				System.out.println("SET: " + divText);
        				System.out.println("COUNT: " + StringUtils.countMatches(
        						divText, String.valueOf(endChar)));
        				String[] setAndNotes = divText.split(
        						"(([\\s]*)[" + String.valueOf(endChar) +
        						"]([\\s]*)){3}");
        				for (int i = 0; i < setAndNotes.length; i++) {
        					System.out.println(setAndNotes[i]);
        				}
        				sb.append(StringUtils.remove(divText, endChar));
        				System.out.println("SET: " + sb.toString());
        				String[] sections = sb.toString().split(
        						"-------- ENCORE --------");
        				for (int i = 0; i < sections.length; i++) {
        					System.out.println(sections[i]);
        				}
        				String[] songs = sections[0].split("\\d+[\\.]{1}");
        				for (int i = 0; i < songs.length; i++) {
        					System.out.println(songs[i]);
        				}
        			}
            	}
            }
            /*
            Elements ticketings = body.getElementsByAttributeValue("id",
                    "ticketingColText");
            for (Element ticketing : ticketings) {
                for (Element single : ticketing.getAllElements()) {
                    if (single.tagName().equals("span")) {
                    	if (locString.length() > 0) {
                    		dateString = getSetlistDateString(locString.toString());
                    		setlistId = createLatest(dateString);
                    	}
                        for (Node node : single.childNodes()) {
                            if (!(node instanceof Comment)) {
                                if (node instanceof TextNode) {
                                	System.out.println("TextNode is blank: " + StringUtils.isBlank(((TextNode) node).text()));
                                	if (lastPlay != null && !StringUtils.isBlank(((TextNode) node).text())) {
                                		uploadSong(lastPlay, ++slot, setlistId, slot == 1, false, false);
                                		System.out.println("TextNode nulling lastPlay");
                                		System.out.println("TextNode: '" + ((TextNode) node).text() + "'");
                                		lastPlay = null;
                                	}
                                    sb.append(StringUtils.remove(((TextNode) node).text(), endChar));
                                } else {
                                    if (node.nodeName().equals("div")) {
                                        // End current string
                                        if (setString.length() > 0)
                                            setString.append("\n");
                                        if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .startsWith("Encore") && !hasEncore) {
                                            hasEncore = true;
                                            if (lastPlay != null && !hasSetCloser) {
                                            	uploadSong(lastPlay, ++slot, setlistId, slot == 1, true, false);
                                            	hasSetCloser = true;
                                            	System.out.println("div nulling lastPlay");
                                            	lastPlay = null;
                                            }
                                            if (!firstBreak) {
                                                setString.append("\n");
                                                firstBreak = true;
                                            }
                                            if (sb.indexOf(":") == -1) {
                                            	sb.setLength(0);
                                            	sb.append("Encore:");
                                            }
                                        }
                                        else {
                                        	lastPlay = StringUtils.replaceChars(
                                					StringUtils.strip(
                                                            sb.toString()),
                                                            badChar, apos);
                                        }
                                        setString.append(
                                            StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos));
                                        setString.trimToSize();
                                        sb.setLength(0);
                                    }
                                    else if (node.nodeName().equals("br")) {
                                        if (sb.length() > 0 &&
                                                !StringUtils.isBlank(
                                                        sb.toString())) {
                                            if (setString.length() > 0)
                                                setString.append("\n");
                                            setString.append(
                                                StringUtils.replaceChars(
                                                    StringUtils.strip(
                                                            sb.toString()),
                                                            badChar, apos));
                                            setString.trimToSize();
                                            sb.setLength(0);
                                        }
                                        if (firstBreak && !secondBreak && hasEncore) {
                                            setString.append("\n");
                                            secondBreak = true;
                                            if (lastPlay != null) {
                                            	uploadSong(lastPlay, ++slot, setlistId, slot == 1, false, true);
                                            	System.out.println("br nulling lastPlay");
                                            	lastPlay = null;
                                            }
                                        }
                                        if (!firstBreak) {
                                        	System.out.println("NOT firstBreak");
                                        	System.out.println("lastPlay: " + lastPlay);
                                        	System.out.println("hasSetCloser: " + hasSetCloser);
                                            setString.append("\n");
                                            firstBreak = true;
                                            if (lastPlay != null && !hasSetCloser) {
                                            	uploadSong(lastPlay, ++slot, setlistId, slot == 1, true, false);
                                            	hasSetCloser = true;
                                            	System.out.println("!firstBreak nulling lastPlay");
                                            	lastPlay = null;
                                            }
                                        }
                                    }
                                    else if (node.nodeName().equals("img")) {
                                        sb.append("->");
                                        hasSegue = true;
                                        if (!hasGuests) {
                                            lastSong = StringUtils.chomp(setString.toString()).substring(
                                                    StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                        }
                                    }
                                    else if (node instanceof Element) {
                                        sb.append(((Element) node).text());
                                        if (!StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:") && !hasGuests) {
                                            hasGuests = true;
                                            lastSong = StringUtils.chomp(setString.toString()).substring(
                                                    StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                        }
                                        else if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:")) {
                                            hasEncore = true;
                                            lastSong = StringUtils.strip(sb.toString());
                                        }
                                    }
                                }
                            }
                        }
                        if (!hasSegue && !hasGuests) {
                            lastSong = StringUtils.strip(setString.toString()).substring(
                                    StringUtils.strip(setString.toString()).lastIndexOf("\n")+1);
                        }
                        if (setString.length() > 0)
                            setString.append("\n");
                        setString.append(
                            StringUtils.replaceChars(
                                StringUtils.strip(
                                        sb.toString()),
                                        badChar, apos));
                        setString.trimToSize();
                    }
                    else if (setString.length() == 0) {
                        if (single.id().equals("ticketingColText"))
                            numTicketings++;
                        if (numTicketings == 2 && single.nodeName().equals("div")) {
                            locString.append(single.ownText());
                            locString.append("\n");
                        }
                        if (single.tagName().equals("br"))
                            br = true;
                        else if (single.tagName().equals("b"))
                            b = true;
                        if (br && b) {
                            locString.append(single.ownText());
                            locString.append("\n");
                            br = false;
                            b = false;
                        }
                    }
                }
            }
            */
        }
        System.out.println("lastSong: " + lastSong);
        return locString.append("\n").append(setString).toString();
    }
    
    private Document getPageDocument(String url) {
    	if (url.startsWith("http")) {
	        HttpPost postMethod = new HttpPost(
	                "https://whsec1.davematthewsband.com/login.asp");
	        postMethod.addHeader("Accept",
	                "text/html, application/xhtml+xml, */*");
	        postMethod.addHeader("Referer",
	                "https://whsec1.davematthewsband.com/login.asp");
	        postMethod.addHeader("Accept-Language", "en-US");
	        postMethod.addHeader("User-Agent",
	                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; " +
	        		"WOW64; Trident/5.0)");
	        postMethod.addHeader("Content-Type",
	                "application/x-www-form-urlencoded");
	        postMethod.addHeader("Accept-Encoding", "gzip, deflate");
	        postMethod.addHeader("Host", "whsec1.davematthewsband.com");
	        postMethod.addHeader("Connection", "Keep-Alive");
	        postMethod.addHeader("Cache-Control", "no-cache");
	        postMethod.addHeader("Cookie",
	                "MemberInfo=isInternational=&MemberID=&UsrCount=" +
	        		"04723365306&ExpDate=&Username=; ASPSESSIONIDQQTDRTTC=" +
            		"PKEGDEFCJBLAIKFCLAHODBHN; __utma=10963442.556285711." +
	        		"1366154882.1366154882.1366154882.1; __utmb=10963442.2." +
            		"10.1366154882; __utmc=10963442; __utmz=10963442." +
	        		"1366154882.1.1.utmcsr=warehouse.dmband.com|utmccn=" +
            		"(referral)|utmcmd=referral|utmcct=/; " +
	        		"ASPSESSIONIDSSDRTSRA=HJBPPKFCJGEJKGNEMJJMAIPN");
	        
	        List<NameValuePair> nameValuePairs =
	                new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("the_url", ""));
	        nameValuePairs.add(new BasicNameValuePair("form_action", "login"));
	        nameValuePairs.add(new BasicNameValuePair("Username", "fateman"));
	        nameValuePairs.add(new BasicNameValuePair("Password", "nintendo"));
	        nameValuePairs.add(new BasicNameValuePair("x", "0"));
	        nameValuePairs.add(new BasicNameValuePair("y", "0"));
	        try {
	            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	        } catch (UnsupportedEncodingException e1) {}
	        HttpResponse response = null;
	        HttpClient client = createSecureConnection();
	        try {
	            response = client.execute(postMethod);
	        } catch (IOException e1) {}
	        if (response == null || (response.getStatusLine().getStatusCode() !=
	                200 && response.getStatusLine().getStatusCode() != 302))
	        	System.out.println("Failed to get response from to " +
	                    postMethod.getURI().toASCIIString());
	        HttpGet getMethod = new HttpGet(url);
	        String html = null;
	        if (!url.startsWith("https"))
	        	client = HttpClientBuilder.create().build();
	        try {
	            response = client.execute(getMethod);
	            html = EntityUtils.toString(response.getEntity(), "UTF-8");
	            html = StringEscapeUtils.unescapeHtml4(html);
	        } catch (ClientProtocolException e1) {
	            System.out.println("Failed to connect to " +
	                    getMethod.getURI().toASCIIString());
	            e1.printStackTrace();
	        } catch (IOException e1) {
	            System.out.println("Failed to get setlist from " +
	                    getMethod.getURI().toASCIIString());
	            e1.printStackTrace();
	        }
	        return Jsoup.parse(html);
    	}
    	else
    		return Jsoup.parse(StringEscapeUtils.unescapeHtml4(
    				readStringFromFile(url)));
    }
    
    public String liveSetlist(String url) {
    	Document doc = getPageDocument(url);
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testLive.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testOne.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testTwo.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testThree.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testFour.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testFive.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testSix.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testSeven.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testEight.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testNine.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testTen.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("/home/testEleven.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("/home/testLive.txt")));
        char badChar = 65533;
        char apos = 39;
        char endChar = 160;
        hasEncore = false;
        hasGuests = false;
        hasSegue = false;
        firstBreak = false;
        secondBreak = false;
        boolean hasGuest = false;
        boolean firstPartial = false;
        boolean lastPartial = false;
        String divStyle = "";
        String divTemp = "";
        int divStyleLocation = -1;
        String oldNote = "";
        String setlistStyle = "font-family:sans-serif;font-size:14;" +
        		"font-weight:normal;margin-top:15px;margin-left:15px;";
        String locStyle = "padding-bottom:12px;padding-left:3px;color:#3995aa;";
        String setStyle = "Color:#000000;Position:Absolute;Top:";
        String divText = "";
        TreeMap<Integer, String> songMap = new TreeMap<Integer, String>();
        int currentLoc = 0;
        String currSong = "";
        int breaks = 0;
        String fontText = "";
        boolean hasSong = false;
        if (doc != null) {
        	// Find nodes in the parent setlist node, for both types
            for (Node node : doc.body().getElementsByAttributeValue("style",
            		setlistStyle).first().childNodes()) {
            	// Location and parent setlist node
            	if (node.nodeName().equals("div")) {
            		divStyle = node.attr("style");
            		// Location node
            		if (divStyle.equals(locStyle)) {
            			for (Node locNode : node.childNodes()) {
                            if (!(locNode instanceof Comment)) {
                                if (locNode instanceof TextNode) {
                                	locList.add(StringUtils.trim(
                                			((TextNode)locNode).text()));
                                }
                            }
            			}
            		}
            		// If the song nodes are divs
            		else {
            			// All song divs
            			Elements divs = ((Element) node)
            					.getElementsByTag("div");
                        for (Element div : divs) {
                        	if (div.hasAttr("style")) {
                        		divStyle = div.attr("style");
                        		if (divStyle.contains("Top:")) {
	                        		divTemp = divStyle.substring(
	                        				divStyle.indexOf("Top:"));
	                        		divStyleLocation = Integer.parseInt(
	                        				divTemp.substring(4,
	                        						divTemp.indexOf(";")));
                        		}
                        		if (divStyle.startsWith(setStyle)) {
                    				String[] locations = divStyle.split(
                    						setStyle);
                    				currentLoc = Integer.parseInt(
                    						locations[1].split(";")[0]);
                    				divText = div.ownText();
                    				divText = StringUtils.remove(divText,
                    						endChar);
                    				String[] songs = divText.split(
                    						"\\d+[\\.]{1}");
                    				if (songs.length > 1) {
                    					currSong = StringUtils.replaceChars(
                                        		songs[1], badChar, apos);;
                    					Elements imgs =
                    							div.getElementsByTag("img");
                    					if (!imgs.isEmpty()) {
                    						currSong = currSong.concat(" ->");
                    						hasSegue = true;
                    					}
                            			songMap.put(currentLoc, currSong);
                    				}
                    				else if (divText.toLowerCase().contains(
                    						"encore")) {
                    					songMap.put(currentLoc, "Encore:");
                    				}
                    				else if (divText.toLowerCase().contains(
                    						"set break")) {
                    					songMap.put(currentLoc, "Set Break");
                    				}
                    			}
                    			else {
                    				boolean segue = false;
                    				divText = div.ownText();
                    				if (!StringUtils.isBlank(divText)) {
            	        				for (Node child : div.childNodes()) {
            	        					oldNote = noteMap.get(
            	        							divStyleLocation);
	                                		if (oldNote == null)
	                                			oldNote = "";
        	                                if (child instanceof TextNode) {
        	                                	String nodeText = StringUtils
    	                                			.remove(((TextNode)child)
    	                                					.text(), endChar);
        	                                	if (!StringUtils.isBlank(
        	                                			nodeText)) {
        	                                		if (segue) {
        	                                			System.out.println(
    	                                					"segue: " +
                                							divStyleLocation);
        	                                			if (divStyleLocation > -1)
        	                                				noteMap.put(
    	                                						divStyleLocation,
    	                                						oldNote.concat(
	                                								StringUtils.trim(
                                										nodeText)));
        	                                			noteList.set(
    	                                					noteList.size()-1,
    	                                					noteList.get(
	                                							noteList.size()-1)
	                                								.concat(
                                										StringUtils.trim(nodeText)));
        	                                		}
        	                                		else {
        	                                			String noteText =
    	                                					StringUtils.trim(
	                                							nodeText);
        	                                			if (noteText
        	                                					.toLowerCase()
        	                                					.contains(
    	                                							"show notes")) {
        	                                				System.out.println(
    	                                						"show notes: " +
                                								divStyleLocation);
        	                                				if (divStyleLocation > -1)
        	                                					noteMap.put(
    	                                							divStyleLocation,
    	                                							oldNote.concat("Notes:"));
        	                                				System.out.println(
        	                                						"Notes:");
        	                                				noteList.add(0,
    	                                						"Notes:");
        	                                				breaks = 0;
        	                                			}
        	                                			else {
        	                                				if (hasGuest) {
        	                                					System.out.println(
    	                                							"hasGuest: " +
                                									divStyleLocation);
        	                                					if (divStyleLocation > -1)
        	                                						noteMap.put(
    	                                								divStyleLocation,
    	                                								oldNote.concat(
	                                										StringUtils.trim(nodeText)));
        	                                					noteList.set(
    	                                							noteList.size()-1,
    	                                							noteList.get(
	                                									noteList.size()-1).concat(
                                											StringUtils.trim(nodeText)));
        	                                				}
        	                                				else if (firstPartial ||
        	                                						lastPartial) {
        	                                					System.out.println(
    	                                							"partial: " +
                                									divStyleLocation);
        	                                					if (divStyleLocation > -1)
        	                                						noteMap.put(
    	                                								divStyleLocation,
    	                                								oldNote.concat(
	                                										StringUtils.trim(nodeText)));
        	                                					noteList.set(
    	                                							noteList.size()-1,
    	                                							noteList.get(
	                                									noteList.size()-1).concat(
                                											StringUtils.trim(nodeText)));
        	                                				}
        	                                				else {
        	                                					System.out.println(
    	                                							"other: " +
                                									divStyleLocation);
        	                                					if (divStyleLocation > -1)
        	                                						noteMap.put(
    	                                								divStyleLocation,
    	                                								oldNote.concat(
	                                										StringUtils.trim(nodeText)));
        	                                					noteList.add(
    	                                							StringUtils.trim(
	                                									nodeText));
        	                                					System.out.println(
        	                                							StringUtils.trim(
        	                                									nodeText));
        	                                				}
        	                                			}
        	                                		}
        	                                		segue = false;
        	                                		hasGuest = false;
        	                                	}
        	                                }
        	                                else if (child.nodeName().equals("img")) {
        	                                	System.out.println("img: " +
        	                                			divStyleLocation);
        	                                	if (divStyleLocation > -1)
        	                                		noteMap.put(divStyleLocation,
    	                                				oldNote.concat("\n")
    	                                				.concat("-> "));
        	                                	noteList.add("-> ");
        	                                	System.out.println("-> ");
        	                                	segue = true;
        	                                }
        	                                else if (child.nodeName().equals(
        	                                		"font")) {
        	                        			List<Node> children =
        	                        					child.childNodes();
        	                        			if (!children.isEmpty()) {
        	                        				Node leaf = children.get(0);
        	                        				if (leaf instanceof TextNode) {
        	                        					fontText = ((TextNode) leaf)
        	                        							.text();
        	                        					if (fontText.contains("(")) {
        	                        						firstPartial = true;
        	                        						System.out.println(
    	                        								"partial: " +
                        										divStyleLocation);
        	                        						if (divStyleLocation > -1)
        	                        							noteMap.put(
    	                        									divStyleLocation,
    	                        									oldNote.concat("\n")
    	                        										.concat(
	                        												StringUtils.trim(fontText)));
        	                        						noteList.add(
    	                        								fontText);
        	                        						System.out.println(fontText);
        	                        					} else if (fontText.contains(")")) {
        	                        						lastPartial = true;
        	                        						System.out.println(
    	                        								"partial: " +
                        										divStyleLocation);
        	                        						if (divStyleLocation > -1)
        	                        							noteMap.put(
    	                        									divStyleLocation,
    	                        									oldNote.concat(
	                        											StringUtils.trim(fontText)
	                        												.concat(" ")));
        	                        						noteList.set(
    	                        								noteList.size()-1,
    	                        								noteList.get(
	                        										noteList.size()-1).concat(
                        												StringUtils.trim(fontText)
                        													.concat(" ")));
        	                        					} else {
        	                        						hasGuest = true;
        	                        						System.out.println(
    	                        								"guest: " +
                        										divStyleLocation);
        	                        						if (divStyleLocation > -1)
        	                        							noteMap.put(
    	                        									divStyleLocation,
    	                        									oldNote.concat(
	                        											StringUtils.trim(fontText)
	                        												.concat(" ")));
        	                        						noteList.add(
    	                        								fontText.concat(" "));
        	                        						System.out.println(
        	                        								fontText.concat(" "));
        	                        					}
        	                        				}
        	                        			}
        	                        		}
            	            			}
                    				}
                    			}
                        	}
                        }
            		}
            	}
            	else if (node instanceof TextNode) {
                	// Get the song here
        			divText = ((TextNode)node).text();
        			divText = StringUtils.remove(divText, endChar);
        			// Split the song from the number
    				String[] songs = divText.split("\\d+[\\.]{1}");
    				// If a song is found
    				if (songs.length > 1) {
    					hasSong = true;
    					// Add the song
    					currSong = StringUtils.replaceChars(
                        		songs[1], badChar, apos);
    					setList.add(currSong);
    					System.out.println(currSong);
    					lastSong = currSong;
    					// Reset break tracking
    					breaks = 0;
    				}
    				else {
    					// No Song
    					if (!StringUtils.isBlank(divText)) {
    						// Look for encore
    						if (divText.toLowerCase().contains("encore")) {
    							currSong = "Encore:";
    	    					setList.add(currSong);
    	    					System.out.println(currSong);
    	    					lastSong = currSong;
    	    					breaks = 0;
    						}
    						else if (divText.toLowerCase().contains(
    								"set break")) {
    							currSong = "Set Break";
    							setList.add(currSong);
    							System.out.println(currSong);
    							lastSong = currSong;
    							breaks = 0;
    						}
    						// We're in the show notes
    						else {
	    						String nodeText = StringUtils.remove(divText,
	    								endChar);
	    						// Create the notes
	                        	if (!StringUtils.isBlank(nodeText)) {
	                        		if (noteList.isEmpty()) {
	                					noteList.add("Notes:");
	                					System.out.println("Notes:");
	                        		}
	                        		// If a img tag is found within the notes,
	                        		// a -> is added so this node text should
	                        		// be appended to that last note item in
	                        		// the list
	                        		if (hasSegue)
	                        			noteList.set(noteList.size()-1,
                        					noteList.get(noteList.size()-1)
                        						.concat(StringUtils.trim(
                        								nodeText)));
	                        		else {
	                        			// If a guest has been found via the
	                        			// font tag, the symbol is added to the
	                        			// notes list, so this text now needs to
	                        			// be appended to that symbol
                        				if (hasGuest)
                        					noteList.set(noteList.size()-1,
                    							noteList.get(noteList.size()-1)
                    								.concat(StringUtils.trim(
                    										nodeText)));
                        				// Everything else is just added as a
                        				// new item in the list
                        				// Sometimes there is a double break
                        				// between notes, so reset breaks value
                        				else {
                        					noteList.add(StringUtils.trim(
                        							nodeText));
                        					System.out.println(StringUtils.trim(
                        							nodeText));
                        					breaks = 0;
                        				}
                        			}
	                        		// Has guest gets reset for the next item
	                        		// in the show notes
	                        		hasGuest = false;
	                        	}
    						}
    					}
    				}
                }
            	else if (node instanceof Element) {
            		System.out.println("firstBreak: " + firstBreak);
    				System.out.println("hasEncore: " + hasEncore);
    				System.out.println("secondBreak: " + secondBreak);
            		// Found a segue image
            		if (node.nodeName().equals("img")) {
            			if (hasSong) {
            				System.out.println("Adding ->");
            				currSong = setList.get(setList.size()-1).concat(
            						" ->");
            				setList.set(setList.size()-1, currSong);
            				lastSong = currSong;
            			}
            			else {
            				if (noteList.isEmpty()) {
            					noteList.add("Notes:");
            					System.out.println("Notes:");
            				}
            				noteList.add("-> ");
            				System.out.println("-> ");
            				hasSegue = true;
            			}
            			// If either still in main set
            			// OR
            			// In encore with second break
            			// This indicates the img is in the show notes
            			/*
            			if ((firstBreak && !hasEncore) || secondBreak) {
            				if (noteList.isEmpty()) {
            					noteList.add("Notes:");
            					System.out.println("Notes:");
            				}
                            noteList.add("-> ");
                            System.out.println("-> ");
                            hasSegue = true;
            			}
            			// If img tag is in the set
            			else {
            				System.out.println("Adding ->");
		            		currSong = setList.get(setList.size()-1).concat(
		            				" ->");
		            		setList.set(setList.size()-1, currSong);
		            		lastSong = currSong;
            			}
            			*/
            			breaks = 0;
            		}
            		// Found a guest symbol
            		else if (node.nodeName().equals("font")) {
            			List<Node> children = node.childNodes();
            			if (!children.isEmpty()) {
            				Node child = children.get(0);
            				if (child instanceof TextNode) {
            					hasGuest = true;
            					noteList.add(((TextNode) child).text().concat(
            							" "));
            					System.out.println(((TextNode) child).text().concat(
            							" "));
            				}
            			}
            			breaks = 0;
            		}
            		// Everything else in the show notes
            		else {
            			// br tags indicate where we are in the notes
            			if (node.nodeName().equals("br")) {
            				hasSong = false;
            				// Increment the break tag count
            				breaks++;
        					// This is the third double br because we have
        					// a first and are in encore
        					// The secondBreak is the last double br in the
        					// set text
        					if (firstBreak && hasEncore && !secondBreak &&
        							breaks > 1)
        						secondBreak = true;
        					// This is the second double br, but we aren't
        					// in the encore yet, so now we are
        					else if (firstBreak && !hasEncore && breaks > 1)
        						hasEncore = true;
        					// This is the first double br, so we indicate
        					// that this is the first
        					else if (!firstBreak && breaks > 1)
    	    					firstBreak = true;
            			}
            		}
            	}
            	//System.out.println(noteMap.toString());
            	//System.out.println(noteList.toString());
            }
            /*
            Elements divs = body.getElementsByTag("div");
            int currentLoc = 0;
            String currSong = "";
            for (Element div : divs) {
            	if (div.hasAttr("style")) {
            		divStyle = div.attr("style");
            		if (divStyle.equals(locStyle)) {
            			for (Node node : div.childNodes()) {
                            if (!(node instanceof Comment)) {
                                if (node instanceof TextNode) {
                                	locList.add(StringUtils.trim(((TextNode)node).text()));
                                }
                            }
            			}
            		}
        			else if (divStyle.startsWith(setStyle)) {
        				String[] locations = divStyle.split(setStyle);
        				currentLoc = Integer.parseInt(locations[1].split(";")[0]);
        				String divText = div.ownText();
        				divText = StringUtils.remove(divText, endChar);
        				String[] songs = divText.split("\\d+[\\.]{1}");
        				if (songs.length > 1) {
        					currSong = songs[1];
        					Elements imgs = div.getElementsByTag("img");
        					if (!imgs.isEmpty()) {
        						currSong = currSong.concat(" ->");
        						hasSegue = true;
        					}
                			songMap.put(currentLoc, currSong);
        				}
        			}
        			else {
        				boolean segue = false;
        				String divText = div.ownText();
        				if (!StringUtils.isBlank(divText)) {
	        				for (Node node : div.childNodes()) {
	                            if (!(node instanceof Comment)) {
	                                if (node instanceof TextNode) {
	                                	String nodeText = StringUtils.remove(((TextNode)node).text(), endChar);
	                                	if (!StringUtils.isBlank(nodeText)) {
	                                		if (segue)
	                                			noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(nodeText)));
	                                		else
	                                			noteList.add(StringUtils.trim(nodeText));
	                                		segue = false;
	                                	}
	                                }
	                                else if (node.nodeName().equals("img")) {
	                                	noteList.add("-> ");
	                                	segue = true;
	                                }
	                            }
	            			}
        				}
        			}
            	}
            }
            */
            for (Entry<Integer, String> song : songMap.entrySet()) {
            	currSong = song.getValue();
            	setList.add(currSong);
            	System.out.println(currSong);
            	lastSong = currSong;
            }
            int segueIndex = -1;
            int partialIndex = -1;
            for (int i = 0; i < noteList.size(); i++) {
            	if (noteList.get(i).contains("->"))
            		segueIndex = i;
            	if (noteList.get(i).startsWith("("))
            		partialIndex = i;
            }
            if (segueIndex >=0) {
            	noteList.add(noteList.remove(segueIndex));
            	if (partialIndex >= 0) {
            		String partial = noteList.remove(partialIndex);
            		noteList.add(noteList.size()-1, partial);
            	}
            }
            else if (partialIndex >= 0) {
        		String partial = noteList.remove(partialIndex);
        		noteList.add(partial);
        	}
            /*
            Elements ticketings = body.getElementsByAttributeValue("id",
                    "ticketingColText");
            for (Element ticketing : ticketings) {
                for (Element single : ticketing.getAllElements()) {
                    if (single.tagName().equals("span")) {
                    	if (locString.length() > 0) {
                    		dateString = getSetlistDateString(locString.toString());
                    		setlistId = createLatest(dateString);
                    	}
                        for (Node node : single.childNodes()) {
                            if (!(node instanceof Comment)) {
                                if (node instanceof TextNode) {
                                	System.out.println("TextNode is blank: " + StringUtils.isBlank(((TextNode) node).text()));
                                	if (lastPlay != null && !StringUtils.isBlank(((TextNode) node).text())) {
                                		uploadSong(lastPlay, ++slot, setlistId, slot == 1, false, false);
                                		System.out.println("TextNode nulling lastPlay");
                                		System.out.println("TextNode: '" + ((TextNode) node).text() + "'");
                                		lastPlay = null;
                                	}
                                    sb.append(StringUtils.remove(((TextNode) node).text(), endChar));
                                } else {
                                    if (node.nodeName().equals("div")) {
                                        // End current string
                                        if (setString.length() > 0)
                                            setString.append("\n");
                                        if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .startsWith("Encore") && !hasEncore) {
                                            hasEncore = true;
                                            if (lastPlay != null && !hasSetCloser) {
                                            	uploadSong(lastPlay, ++slot, setlistId, slot == 1, true, false);
                                            	hasSetCloser = true;
                                            	System.out.println("div nulling lastPlay");
                                            	lastPlay = null;
                                            }
                                            if (!firstBreak) {
                                                setString.append("\n");
                                                firstBreak = true;
                                            }
                                            if (sb.indexOf(":") == -1) {
                                            	sb.setLength(0);
                                            	sb.append("Encore:");
                                            }
                                        }
                                        else {
                                        	lastPlay = StringUtils.replaceChars(
                                					StringUtils.strip(
                                                            sb.toString()),
                                                            badChar, apos);
                                        }
                                        setString.append(
                                            StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos));
                                        setString.trimToSize();
                                        sb.setLength(0);
                                    }
                                    else if (node.nodeName().equals("br")) {
                                        if (sb.length() > 0 &&
                                                !StringUtils.isBlank(
                                                        sb.toString())) {
                                            if (setString.length() > 0)
                                                setString.append("\n");
                                            setString.append(
                                                StringUtils.replaceChars(
                                                    StringUtils.strip(
                                                            sb.toString()),
                                                            badChar, apos));
                                            setString.trimToSize();
                                            sb.setLength(0);
                                        }
                                        if (firstBreak && !secondBreak && hasEncore) {
                                            setString.append("\n");
                                            secondBreak = true;
                                            if (lastPlay != null) {
                                            	uploadSong(lastPlay, ++slot, setlistId, slot == 1, false, true);
                                            	System.out.println("br nulling lastPlay");
                                            	lastPlay = null;
                                            }
                                        }
                                        if (!firstBreak) {
                                        	System.out.println("NOT firstBreak");
                                        	System.out.println("lastPlay: " + lastPlay);
                                        	System.out.println("hasSetCloser: " + hasSetCloser);
                                            setString.append("\n");
                                            firstBreak = true;
                                            if (lastPlay != null && !hasSetCloser) {
                                            	uploadSong(lastPlay, ++slot, setlistId, slot == 1, true, false);
                                            	hasSetCloser = true;
                                            	System.out.println("!firstBreak nulling lastPlay");
                                            	lastPlay = null;
                                            }
                                        }
                                    }
                                    else if (node.nodeName().equals("img")) {
                                        sb.append("->");
                                        hasSegue = true;
                                        if (!hasGuests) {
                                            lastSong = StringUtils.chomp(setString.toString()).substring(
                                                    StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                        }
                                    }
                                    else if (node instanceof Element) {
                                        sb.append(((Element) node).text());
                                        if (!StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:") && !hasGuests) {
                                            hasGuests = true;
                                            lastSong = StringUtils.chomp(setString.toString()).substring(
                                                    StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                        }
                                        else if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:")) {
                                            hasEncore = true;
                                            lastSong = StringUtils.strip(sb.toString());
                                        }
                                    }
                                }
                            }
                        }
                        if (!hasSegue && !hasGuests) {
                            lastSong = StringUtils.strip(setString.toString()).substring(
                                    StringUtils.strip(setString.toString()).lastIndexOf("\n")+1);
                        }
                        if (setString.length() > 0)
                            setString.append("\n");
                        setString.append(
                            StringUtils.replaceChars(
                                StringUtils.strip(
                                        sb.toString()),
                                        badChar, apos));
                        setString.trimToSize();
                    }
                    else if (setString.length() == 0) {
                        if (single.id().equals("ticketingColText"))
                            numTicketings++;
                        if (numTicketings == 2 && single.nodeName().equals("div")) {
                            locString.append(single.ownText());
                            locString.append("\n");
                        }
                        if (single.tagName().equals("br"))
                            br = true;
                        else if (single.tagName().equals("b"))
                            b = true;
                        if (br && b) {
                            locString.append(single.ownText());
                            locString.append("\n");
                            br = false;
                            b = false;
                        }
                    }
                }
            }
            */
        }
        
        return doc.body().toString();
    }
    /*
    private static String getSetlistDateString(String latestSetlist) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = null;
        String subString = latestSetlist.substring(0,
                latestSetlist.indexOf("\n"));
        String dateString = null;
        try {
            date = dateFormat.parse(subString);
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e2) {
            System.out.println("Failed to parse date from " + subString);
            e2.printStackTrace();
        }
        return dateString;
    }
    */
    private String getNewSetlistDateString(String dateLine) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        Date date = null;
        String dateString = null;
        try {
            date = dateFormat.parse(dateLine);
            dateFormat = new SimpleDateFormat(DATE_FORMAT);
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e2) {
            System.out.println("Failed to parse date from " + dateLine);
            e2.printStackTrace();
        }
        return dateString;
    }
    
    private String getTweetDateString(String dateLine) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        Date date = null;
        String dateString = null;
        try {
            date = dateFormat.parse(dateLine);
            dateFormat = new SimpleDateFormat(TWEET_DATE_FORMAT);
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e) {
            System.out.println("Failed to parse date from " + dateLine);
            e.printStackTrace();
        }
        return dateString;
    }
    
    private String getExpireDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        date.setTime(System.currentTimeMillis() + 300000); // 5 minutes
        return dateFormat.format(date.getTime());
    }
    /*
    private static int getQuestionCount() {
    	DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Question?";
        url += ("count=1&limit=0&where=%7B%22%24or%22%3A%5B%7B%22trivia%22%3A" +
        		"%7B%22%24exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3Afalse" +
        		"%7D%5D%7D");
        HttpGet httpGet = new HttpGet(url);
        System.out.println(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET questions failed!");
                return -1;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        System.out.println("COUNT: " + responseString);
        return getQuestionCountFromResponse(responseString);
    }
    
    private static void markAsTrivia(String objectId, boolean isTrivia) {
    	String setTrivia = null;
    	if (isTrivia)
    		setTrivia = "{\"trivia\":true}";
    	else
    		setTrivia = "{\"trivia\":false}";
    	DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = new StringEntity(setTrivia,
        		ContentType.APPLICATION_JSON);
        HttpResponse response = null;
        String url = "https://api.parse.com/1/classes/Question/" + objectId;
        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(entity);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT question " + objectId + " failed!");
                return;
            }
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
            		httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
            		httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
    }
    
    private static void markAllAsTrivia(boolean isTrivia) {
    	Map<String, String> questionMap;
    	do {
    		questionMap = getQuestion(true, 1, 0);
    		if (!questionMap.isEmpty())
    			markAsTrivia(questionMap.get("objectId"), isTrivia);
    	} while (!questionMap.isEmpty());
    }
    
    // Need to get a random question not recently asked
    private static Map<String, String> getQuestion(boolean isTrivia,
    		int limit, int skip) {
    	System.out.println("isTrivia: " + isTrivia);
    	System.out.println("limit: " + limit);
    	System.out.println("skip: " + skip);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Question?";
    	if (skip >= 0)
    		url += ("skip=" + skip);
		url += ("&limit=" + limit);
		if (isTrivia)
			url += "&where=%7B%22trivia%22%3Atrue%7D";
		else
			url += ("&where=%7B%22%24or%22%3A%5B%7B%22trivia%22%3A%7B%22" +
					"%24exists%22%3Afalse%7D%7D%2C%7B%22trivia%22%3Afalse" +
					"%7D%5D%7D");
		
		if (askedQuestions != null && !askedQuestions.isEmpty()) {
			url += "&where=%7B%22objectId%22:%7B%22$nin%22:%5B";
			for (String askedQuestion : askedQuestions) {
				url += "%22";
				url += askedQuestion;
				url += "%22";
				url += "%2C";
			}
			if (url.endsWith("%2C"))
				url = StringUtils.removeEnd(url, "%2C");
			url += "%5D%7D%7D";
		}
		
        HttpGet httpGet = new HttpGet(url);
        System.out.println(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET questions failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return getQuestionInfoFromResponse(responseString);
    }
    */
    private String getSetlist(String latestSetlist) {
        String dateString = getNewSetlistDateString(latestSetlist);
        System.out.println("getSetlist dateString: " + dateString);
        if (dateString == null)
            return null;
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Setlist?";
        try {
            url += URLEncoder.encode(
            		"where={\"setDate\":{\"__type\":\"Date\",\"iso\":\"" +
    				dateString + "\"}}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + dateString + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        System.out.println("getSetlist responseString: " + responseString);
        return responseString;
    }
    
    private String postSetlist(String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        String objectId = null;
        HttpPost httpPost = new HttpPost(
        		"https://api.parse.com/1/classes/Setlist");
        httpPost.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println("POST of setlist failed!");
                System.out.println(json);
            } 
            else {
            	HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                 String responseString = EntityUtils.toString(
	                		 response.getEntity());
	                 objectId = getSimpleObjectIdFromResponse(responseString);
	            }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private boolean putSetlist(String objectId, String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Setlist/"
        		+ objectId);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private boolean addPlay(String setlistId, String playId) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Setlist/"
        		+ setlistId);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        String json = null;
        try {
        	json = getAddPlayJsonString(playId);
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Add play " + playId + " to " + setlistId +
                		" failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private String getSong(String latestSong) {
    	System.out.println("getSong: " + latestSong);
    	if (latestSong == null)
    		return null;
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Song?";
        try {
            url += URLEncoder.encode("where={\"title\":\"" + latestSong + "\"}",
            		"US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + latestSong + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return responseString;
    }
    
    private boolean getPlay(String setlistId, Integer slot) {
    	if (setlistId == null)
    		return true;
    	// Check if this setlist has this many slots already
    	// Get relations (plays) to this setlist
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Play?";
        try {
        	url += URLEncoder.encode("where={\"$relatedTo\":{\"object\":" +
        			"{\"__type\":\"Pointer\",\"className\":\"Setlist\"," +
        			"\"objectId\":\"" + setlistId + "\"},\"key\":\"plays\"}}",
        			"US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + setlistId + " : " + slot +
                		" play failed!");
                return true;
            }
            entity = response.getEntity();
            if (entity != null) {
                 responseString = EntityUtils.toString(response.getEntity());
                 System.out.println("getPlay responseString: " +
                		 responseString);
                 int tempSlot = getLargestSlotFromResponse(responseString);
                 System.out.println("getPlay slot: " + slot);
                 System.out.println("getPlay tempSlot: " + tempSlot);
                 if (slot > tempSlot)
                	 return false;
            }
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private boolean postNotification(String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        HttpPost httpPost = new HttpPost("https://api.parse.com/1/push");
        httpPost.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("POST of notification failed!");
                System.out.println(json);
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private String postSong(String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        String objectId = null;
        HttpPost httpPost = new HttpPost(
        		"https://api.parse.com/1/classes/Song");
        httpPost.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println("POST of song failed!");
                System.out.println(json);
            }
            else {
            	HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                 String responseString = EntityUtils.toString(
	                		 response.getEntity());
	                 objectId = getSimpleObjectIdFromResponse(responseString);
	            }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private boolean putSong(String objectId, String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        System.out.println(objectId);
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Song/" +
        		objectId);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private String postPlay(String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        String objectId = null;
        HttpPost httpPost = new HttpPost(
        		"https://api.parse.com/1/classes/Play");
        httpPost.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println("POST of song failed!");
                System.out.println(json);
            }
            else {
            	HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                 String responseString = EntityUtils.toString(
	                		 response.getEntity());
	                 objectId = getSimpleObjectIdFromCreate(responseString);
	            }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private boolean putPlay(String objectId, String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Play/" +
        		objectId);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private boolean putSetSong(String objectId, String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Song/" +
        		objectId);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private String getSetlistJsonString(String latestSetlist) {
        currDateString = getNewSetlistDateString(latestSetlist);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dateNode = factory.objectNode();
        dateNode.put("__type", "Date");
        dateNode.put("iso", currDateString);
        rootNode.put("set", latestSetlist);
        rootNode.put("setDate", dateNode);
        return rootNode.toString();
    }
    
    private String getNewSetlistJsonString(String dateString) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dateNode = factory.objectNode();
        dateNode.put("__type", "Date");
        dateNode.put("iso", dateString);
        rootNode.put("setDate", dateNode);
        return rootNode.toString();
    }
    
    private String getPushJsonString(String latestSong, String setlist,
            String expireDateString) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dataNode = factory.objectNode();
        ObjectNode whereNode = factory.objectNode();
        whereNode.put("deviceType", "android");
        //whereNode.put("appVersion", "2.0.2");
        dataNode.put("action", "com.jeffthefate.dmb.ACTION_NEW_SONG");
        dataNode.put("song", latestSong);
        dataNode.put("setlist", setlist);
        dataNode.put("timestamp", Long.toString(System.currentTimeMillis()));
        rootNode.put("where", whereNode);
        rootNode.put("expiration_time", expireDateString);
        rootNode.put("data", dataNode);
        return rootNode.toString();
    }
    
    private String getSongJsonString(String latestSong) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        rootNode.put("title", latestSong);
        return rootNode.toString();
    }
    /*
     {
	  "__type": "Pointer",
	  "className": "GameScore",
	  "objectId": "Ed1nuqPvc"
	}
     */
    private String getPlayJsonString(String showId, Integer slot,
    		String songId, boolean isOpener, boolean isSetCloser,
    		boolean isEncoreCloser) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode showNode = factory.objectNode();
        ObjectNode songNode = factory.objectNode();
        showNode.put("__type", "Pointer");
        showNode.put("className", "Setlist");
        showNode.put("objectId", showId);
        songNode.put("__type", "Pointer");
        songNode.put("className", "Song");
        songNode.put("objectId", songId);
        rootNode.put("opener", isOpener);
        rootNode.put("setCloser", isSetCloser);
        rootNode.put("encoreCloser", isEncoreCloser);
        rootNode.put("show", showNode);
        rootNode.put("slot", slot);
        rootNode.put("song", songNode);
        return rootNode.toString();
    }
    
    private String getAddPlayJsonString(String playId) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode playNode = factory.objectNode();
        ArrayNode playArray = factory.arrayNode();
        ObjectNode playsNode = factory.objectNode();
        playNode.put("__type", "Pointer");
        playNode.put("className", "Play");
        playNode.put("objectId", playId);
        playArray.add(playNode);
        playsNode.put("__op", "AddRelation");
        playsNode.put("objects", playArray);
        rootNode.put("plays", playsNode);
        return rootNode.toString();
    }
    
    private String getSetSongJsonString(String playId) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode addRelationNode = factory.objectNode();
        ArrayNode relationArray = factory.arrayNode();
        ObjectNode relationNode = factory.objectNode();
        // {"setlist":{"__op":"AddRelation","objects":[{"__type":"Pointer","className":"Song","objectId":"Vx4nudeWn"}]}}
        relationNode.put("__type", "Pointer");
        relationNode.put("className", "Play");
        relationNode.put("objectId", playId);
        relationArray.add(relationNode);
        addRelationNode.put("__op", "AddRelation");
        addRelationNode.put("objects", relationArray);
        rootNode.put("setlist", addRelationNode);
        return rootNode.toString();
    }
    /*
    private static int getQuestionCountFromResponse(String responseString) {
    	JsonFactory f = new JsonFactory();
        JsonParser jp;
        int count = -1;
        String fieldname;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("count".equals(fieldname)) {
                        jp.nextToken();
                        count = Integer.parseInt(jp.getText().trim());
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return count;
    }
    
    private static Map<String, String> getQuestionInfoFromResponse(
    		String responseString) {
    	JsonFactory f = new JsonFactory();
        JsonParser jp;
        Map<String, String> questionMap = new HashMap<String, String>();
        String fieldname;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("answer".equals(fieldname)) {
                        jp.nextToken();
                        questionMap.put(fieldname, jp.getText());
                    }
                    else if ("question".equals(fieldname)) {
                    	questionMap.put(fieldname, jp.getText());
                    }
                    else if ("objectId".equals(fieldname)) {
                    	questionMap.put(fieldname, jp.getText());
                    }
                    else if ("score".equals(fieldname)) {
                    	questionMap.put(fieldname, jp.getText());
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return questionMap;
    }
    */
    private String getObjectIdFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("set".equals(fieldname)) {
                        jp.nextToken();
                        jp.getText(); // setlist
                    }
                    else if ("setDate".equals(fieldname)) {
                        jp.nextToken();
                        jp.nextToken();
                        jp.nextToken();
                        jp.nextToken();
                        jp.getText(); // date string
                    }
                    else if ("title".equals(fieldname)) {
                    	jp.nextToken();
                    	jp.getText(); // song title
                    }
                    else if ("objectId".equals(fieldname)) {
                        objectId = jp.getText();
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private String getSimpleObjectIdFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("objectId".equals(fieldname)) {
                    	jp.nextToken();
                        objectId = jp.getText();
                        jp.close();
                        return objectId;
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private String getSimpleObjectIdFromCreate(String createString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(createString);
            while (jp.nextToken() != null) {
                fieldname = jp.getCurrentName();
                if ("objectId".equals(fieldname)) {
                	jp.nextToken();
                    objectId = jp.getText();
                    jp.close();
                    return objectId;
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + createString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + createString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private int getLargestSlotFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        int slot = -1;
        int tempSlot = -1;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("slot".equals(fieldname)) {
                    	System.out.println("slot fieldname");
                        tempSlot = jp.getIntValue();
                        System.out.println("tempSlot: " + tempSlot);
                        slot = tempSlot > slot ? tempSlot : slot;
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return slot;
    }
    
    private String getEncoreCloserFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("objectId".equals(fieldname)) {
                    	jp.nextToken();
                    	objectId = jp.getText();
                    } else if ("encoreCloser".equals(fieldname)) {
                        jp.nextToken();
                        if (jp.getBooleanValue())
                        	return objectId;
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private int uploadLatest(String latestSetlist) {
        String getResponse = getSetlist(latestSetlist);
        if (getResponse == null) {
            System.out.println("Fetch setlist from Parse failed!");
            System.out.println(latestSetlist);
            return -1;
        }
        String objectId = getObjectIdFromResponse(getResponse);
        if (objectId == null) {
        	if (!isDev)
        		postSetlist(getSetlistJsonString(latestSetlist));
            File dir = new File("/home/");
            String[] files = dir.list(new FilenameFilter() {
            	public boolean accept(File dir, String filename) {
            		return filename.endsWith(".txt");
        		}
        	});
            String dateString = getNewSetlistDateString(latestSetlist);
            Date newDate = convertStringToDate(DATE_FORMAT, dateString);
            for (int i = 0; i < files.length; i++) {
            	if (files[i].startsWith("setlist")) {
            		if (convertStringToDate(DATE_FORMAT,
            				files[i].substring(7)).after(newDate)) {
            			System.out.println("older setlist file found!");
            			return 1;
            		}
            	}
            }
            return 0;
        }
        else {
            putSetlist(objectId, getSetlistJsonString(latestSetlist));
            return -1;
        }
    }
    
    private String createLatest(String dateString) {
    	System.out.println("createLatest: " + dateString);
    	if (dateString == null)
            return null;
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Setlist?";
        try {
            url += URLEncoder.encode(
            		"where={\"setDate\":{\"__type\":\"Date\",\"iso\":\"" +
    				dateString + "\"}}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + dateString + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        if (responseString == null) {
            System.out.println("Fetch setlist from Parse failed!");
            System.out.println(dateString);
        }
        String objectId = getObjectIdFromResponse(responseString);
        if (objectId == null)
            objectId = postSetlist(getNewSetlistJsonString(dateString));
        return objectId;
    }
    
    private boolean uploadPlay(String songId, Integer slot,
    		String setlistId, boolean isOpener, boolean isSetCloser,
    		boolean isEncoreCloser) {
    	// Check if set has this many plays
        boolean hasPlay = getPlay(setlistId, slot);
        if (!hasPlay) {
        	// Check if there is already an encore closer
        	// If so, change that play to false, make this one true
        	if (isEncoreCloser)
        		resetEncoreCloser(setlistId);
            String playId = postPlay(getPlayJsonString(setlistId, slot, songId,
            		isOpener, isSetCloser, isEncoreCloser));
            addPlay(setlistId, playId);
            return true;
        }
        return false;
    }
    
    private void resetEncoreCloser(String setlistId) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String closerId = null;
        String url = "https://api.parse.com/1/classes/Play?";
        try {
        	url += URLEncoder.encode("where={\"$relatedTo\":{\"object\":" +
        			"{\"__type\":\"Pointer\",\"className\":\"Setlist\"," +
        			"\"objectId\":\"" + setlistId + "\"},\"key\":\"plays\"}}",
        			"US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + setlistId + " plays failed!");
                return;
            }
            entity = response.getEntity();
            if (entity != null) {
                 responseString = EntityUtils.toString(response.getEntity());
                 closerId = getEncoreCloserFromResponse(responseString);
                 if (closerId == null)
                	 return;
            }
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        String json = "{\"encoreCloser\":false}";
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Play/" +
        		closerId);
        httpPut.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + closerId + " failed!");
                System.out.println(json);
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
    }
    
    private boolean uploadSong(String latestSong, Integer slot,
    		String setlistId, boolean isOpener, boolean isSetCloser,
    		boolean isEncoreCloser) {
    	// Check if song exists
        String getResponse = getSong(latestSong);
        if (getResponse == null) {
            System.out.println("Fetch setlist from Parse failed!");
            System.out.println(latestSong);
            return false;
        }
        // Get the song id
        String objectId = getSimpleObjectIdFromResponse(getResponse);
        if (objectId == null) {
        	// Song doesn't exist, so add song and get new objectId
            objectId = postSong(getSongJsonString(latestSong));
            if (objectId == null)
            	return false;
        }
    	// Song exists, add play
        uploadPlay(objectId, slot, setlistId, isOpener, isSetCloser,
        		isEncoreCloser);
        return true;
    }
    
    private void writeBufferToFile(byte[] buffer, String filename) {
        BufferedOutputStream bufStream = null;
        try {
            bufStream = new BufferedOutputStream(
                    new FileOutputStream(filename, false), buffer.length);
            bufStream.write(buffer);
            bufStream.flush();
            bufStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("BufferedOutputStream failed for: " + filename);
            e.printStackTrace();
        }
    }
    
    private byte[] readBufferFromFile(String filename) {
        File file = new File(filename);
        byte[] buffer = new byte[(int)file.length()];
        BufferedInputStream bufStream = null;
        try {
            bufStream = new BufferedInputStream(new FileInputStream(file),
                    buffer.length);
            bufStream.read(buffer);
            bufStream.close();
        } catch (FileNotFoundException e) {
            System.out.println(filename + " not found!");
        } catch (IOException e) {
            System.out.println(filename + " IO Exception!");
        } catch (IllegalArgumentException e) {
            System.out.println("File stream is <= 0");
            return new byte[0];
        }
        return buffer;
    }
    
    private void writeStringToFile(String output, String filename) {
        if (output != null)
            writeBufferToFile(output.getBytes(), filename);
    }
    
    private String readStringFromFile(String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append(bytesToString(readBufferFromFile(filename)));
        return sb.toString();
    }
    
    private void appendListToFile(List<String> output, String filename) {
    	String old = readStringFromFile(filename);
    	StringBuilder sb = new StringBuilder();
    	sb.append(old);
    	for (String line : output) {
    		sb.append(line);
    		sb.append("\n");
    	}
    	writeStringToFile(sb.toString(), filename);
    }
    
    private void appendStringToFile(String output, String filename) {
    	String old = readStringFromFile(filename);
    	StringBuilder sb = new StringBuilder();
    	sb.append(old);
    	sb.append(output);
    	sb.append("\n");
    	writeStringToFile(sb.toString(), filename);
    }
    
    private List<String> getListFromFile(String filename) {
    	String content = readStringFromFile(filename);
    	String[] questionIds = content.split("\n");
    	if (questionIds.length < 1)
    		return null;
    	List<String> questionList = new ArrayList<String>(0);
    	questionList.clear();
    	for (int i = 0; i < questionIds.length; i++) {
    		if (!StringUtils.isEmpty(questionIds[i]))
    			questionList.add(questionIds[i]);
    	}
    	return questionList;
    }
    
    private Map<String, Integer> getMapFromFile(String filename) {
    	String content = readStringFromFile(filename);
    	String[] scores = content.split("\n");
    	String[] user;
    	Map<String, Integer> scoreMap = new HashMap<String, Integer>();
    	for (int i = 0; i < scores.length; i++) {
    		if (!StringUtils.isEmpty(scores[i])) {
    			user = scores[i].split(" | ");
    			try {
    				scoreMap.put(user[0], Integer.parseInt(user[1]));
    			} catch (NumberFormatException e) {}
    		}
    	}
    	return scoreMap;
    }
    
    private CharBuffer bytesToString(byte[] input) {
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        ByteBuffer srcBuffer = ByteBuffer.wrap(input);
        CharBuffer charBuffer = null;
        try {
            charBuffer = decoder.decode(srcBuffer);
        } catch (CharacterCodingException e) {}
        return charBuffer;
    }
    
    private boolean needsQuoting(String s) {
        int len = s.length();
        if (len == 0) // empty string have to be quoted
            return true;
        for (int i = 0; i < len; i++) {
            switch (s.charAt(i)) {
            case ' ': case '\t': case '\\': case '"':
                return true;
            }
        }
        return false;
    }

    private String winQuote(String s) {
        if (! needsQuoting(s))
            return s;
        s = s.replaceAll("([\\\\]*)\"", "$1$1\\\\\"");
        s = s.replaceAll("([\\\\]*)\\z", "$1$1");
        return "\"" + s + "\"";
    }
    
    private Status postTweet(String setlistMessage, String gameMessage,
    		File file, long replyTo, boolean postGame) {
    	System.out.println("Tweet text: " + setlistMessage);
    	System.out.println("Tweet length: " + setlistMessage.length());
    	boolean failed = false;
        Twitter setlistTwitter = new TwitterFactory(setlistConfig).getInstance();
        Twitter gameTwitter = new TwitterFactory(gameConfig).getInstance();
		StatusUpdate statusUpdate = new StatusUpdate(setlistMessage);
		Status status = null;
		if (file != null)
			statusUpdate.media(file);
		statusUpdate.setInReplyToStatusId(replyTo);
		int tries = 0;
		do {
			tries++;
	    	try {
				status = setlistTwitter.updateStatus(statusUpdate);
				failed = false;
	    	} catch (TwitterException te) {
	    		te.printStackTrace();
	    		System.out.println("Failed to get timeline: " +
	    				te.getMessage());
	    		failed = true;
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
	    	}
		} while (failed && tries < 10);
		if (!failed && postGame && !setlistMessage.toLowerCase(
				Locale.getDefault()).contains("[Encore:]".toLowerCase(
						Locale.getDefault()))) {
			if (!setlistMessage.toLowerCase(Locale.getDefault()).contains(
						"[Final".toLowerCase(Locale.getDefault()))) {
				statusUpdate = new StatusUpdate(
						setlistMessage.concat(gameMessage));
			}
			else if (!gameMessage.isEmpty()) {
				statusUpdate = new StatusUpdate(gameMessage);
			}
			else {
				return null;
			}
			status = null;
			statusUpdate.setInReplyToStatusId(replyTo);
			tries = 0;
			do {
				tries++;
		    	try {
					status = gameTwitter.updateStatus(statusUpdate);
					failed = false;
		    	} catch (TwitterException te) {
		    		te.printStackTrace();
		    		System.out.println("Failed to get timeline: " +
		    				te.getMessage());
		    		failed = true;
		    		try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
		    	}
			} while (failed && tries < 10);
		}
		return status;
    }
    /*
    private static void getAccessToken() {
    	// The factory instance is re-useable and thread safe.
        Twitter twitter = TwitterFactory.getSingleton();
        twitter.setOAuthConsumer("z9rtG1MwLm1EHjIoN2kYAw",
        		"n5eF6tVtORPTFVSauSA8IaIVY1jORuUVbwRPHKbXWyg");
        RequestToken requestToken = null;
		try {
			requestToken = twitter.getOAuthRequestToken();
		} catch (TwitterException e) {
			e.printStackTrace();
			return;
		}
        AccessToken accessToken = null;
        BufferedReader br = new BufferedReader(
        		new InputStreamReader(System.in));
        while (null == accessToken) {
          System.out.println(
        		  "Open the following URL and grant access to your account:");
          System.out.println(requestToken.getAuthorizationURL());
          System.out.print(
        		  "Enter the PIN(if aviailable) or just hit enter.[PIN]:");
          String pin = null;
		try {
			pin = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
          try{
             if(pin.length() > 0){
               accessToken = twitter.getOAuthAccessToken(requestToken, pin);
             }else{
               accessToken = twitter.getOAuthAccessToken();
             }
          } catch (TwitterException te) {
            if(401 == te.getStatusCode()){
              System.out.println("Unable to get the access token.");
            }else{
              te.printStackTrace();
            }
          }
        }
        System.out.println(accessToken.getToken());
        System.out.println(accessToken.getTokenSecret());
    }
    
    private interface WatchStreamListener {
    	public void onCorrectUser(String correctAnswer,
    			List<String> screenNames);
		public void onFinalLeaders();
    }
    
    private static String response;
    private static String tempAnswer;
    private static int diffCount;
    private static List<String> winners = new ArrayList<String>(0);
	private static String currAnswer;
	private static long startTime;
    
    private static void watchTwitterStream(final String answer,
    		final String questionId) {
    	System.out.println("watchTwitterStream");
    	winners.clear();
    	winners = new ArrayList<String>(0);
    	System.out.println("ANSWER: " + answer);
    	currAnswer = answer;
    	questionWait = WAIT_FOR_QUESTION;
    	startTime = System.currentTimeMillis();
    	if (twitterStream == null) {
		    twitterStream = new TwitterStreamFactory(
		    		setupTweet()).getInstance();
		    twitterStream.addListener(listener);
		    // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
		    twitterStream.user();
    	}
    }
    
    private static int questionWait = WAIT_FOR_QUESTION;
    
    private static UserStreamListener listener = new UserStreamListener() {
		@Override
		public void onBlock(User arg0, User arg1) {}
		@Override
		public void onDeletionNotice(long arg0, long arg1) {}
		@Override
		public void onDirectMessage(DirectMessage arg0) {}
		@Override
		public void onFavorite(User arg0, User arg1, Status arg2) {}
		@Override
		public void onFollow(User arg0, User arg1) {}
		@Override
		public void onFriendList(long[] arg0) {}
		@Override
		public void onUnblock(User arg0, User arg1) {}
		@Override
		public void onUnfavorite(User arg0, User arg1, Status arg2) {}
		@Override
		public void onUserListCreation(User arg0, UserList arg1) {}
		@Override
		public void onUserListDeletion(User arg0, UserList arg1) {}
		@Override
		public void onUserListMemberAddition(User arg0, User arg1,
				UserList arg2) {}
		@Override
		public void onUserListMemberDeletion(User arg0, User arg1,
				UserList arg2) {}
		@Override
		public void onUserListSubscription(User arg0, User arg1,
				UserList arg2) {}
		@Override
		public void onUserListUnsubscription(User arg0, User arg1,
				UserList arg2) {}
		@Override
		public void onUserListUpdate(User arg0, UserList arg1) {}
		@Override
		public void onUserProfileUpdate(User arg0) {}
		@Override
		public void onDeletionNotice(StatusDeletionNotice arg0) {}
		@Override
		public void onScrubGeo(long arg0, long arg1) {}
		@Override
		public void onStallWarning(StallWarning arg0) {}
		@Override
		public void onStatus(Status status) {
			// Get the diff characters between the answer and the response
			System.out.println("RAW RESPONSE: " + status.getText());
			response = status.getText().toLowerCase(
					Locale.getDefault()).replace(isDev ?
							DEV_ACCOUNT : PROD_ACCOUNT, "").
					replaceAll("[.,'`\":;/?\\-!@#]", "").trim();
			System.out.println("MASSAGED RESPONSE: " + response);
			tempAnswer = currAnswer.toLowerCase(Locale.getDefault()).
					replaceAll("[.,'`\":;/?\\-!@#]", "").trim();
			System.out.println("MASSAGED ANSWER: " + tempAnswer);
			System.out.println("questionWait: " + questionWait);
			System.out.println("WAIT_FOR_ANSWER: " + WAIT_FOR_ANSWER);
			if (checkAnswer(tempAnswer, response,
					status.getUser().getScreenName()) &&
					questionWait > WAIT_FOR_ANSWER)
				questionWait = WAIT_FOR_ANSWER;
			System.out.println("questionWait: " + questionWait);
			System.out.println("time left: " + (System.currentTimeMillis() -
					startTime));
			checkedNameMap = false;
			checkedAcronymMap = false;
			if (winners.size() == 3 ||
					System.currentTimeMillis() - startTime >= questionWait) {
				try {
					Thread.sleep(WAIT_FOR_ANSWER);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				watchStreamListener.onCorrectUser(currAnswer, winners);
				System.out.println("checking if asking a new question");
			    if (totalQuestions < reqQuestions) {
			    	if (totalQuestions % LEADERS_EVERY == 0) {
			    		screenshot = new TriviaScreenshot(SETLIST_JPG_FILENAME,
			    				ROBOTO_FONT_FILENAME, LEADERS_TITLE,
			    				generateLeaderboard(), TRIVIA_MAIN_FONT_SIZE,
			    				TRIVIA_DATE_FONT_SIZE, LEADERS_LIMIT);
			    		postTweet("[#DMB Trivia] Current Top 10",
			    				new File(screenshot.getOutputFilename()), -1);
			    	}
			    	if (totalQuestions == BONUS_ROUND) {
			    		postTweet("[BONUS ROUND] " + BONUS_SCORE +
			    				" pts added to the value of each question",
			    				null, -1);
			    		try {
							Thread.sleep(PRE_ROUND_TIME);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
			    	}
			    	else if (totalQuestions == ROUND_TWO) {
			    		postTweet("[Starting ROUND 2] " + PLUS_SCORE +
			    				" pts added to the value of each question",
			    				null, -1);
			    		try {
							Thread.sleep(PRE_ROUND_TIME);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
			    	}
					askQuestion();
			    }
			    else {
			    	watchStreamListener.onFinalLeaders();
			    	// When finished, close the stream
				    twitterStream.cleanUp();
				    twitterStream.shutdown();
			    }
			}
		}
		@Override
		public void onTrackLimitationNotice(int arg0) {}
		@Override
		public void onException(Exception e) {
			e.printStackTrace();
		};
    };
    
    private static boolean checkedNameMap = false;
    private static boolean checkedAcronymMap = false;
    
    private static boolean checkAnswer(String answer, String response,
    		String screenName) {
    	int buffer = 4;
    	switch (answer.length()) {
    	case 15:
    	case 14:
    	case 13:
    	case 12:
    	case 11:
    	case 10:
    		buffer = 3;
    		break;
    	case 9:
    	case 8:
    	case 7:
    	case 6:
    	case 5:
    		buffer = 2;
    		break;
    	case 4:
    	case 3:
    		buffer = 1;
    		break;
    	case 2:
    	case 1:
    		buffer = 0;
    		break;
    	}
    	diffCount = StringUtils.getLevenshteinDistance(response, answer,
    			buffer);
    	boolean isCorrect = false;
    	// If more than 4, wrong
    	switch(diffCount) {
    	case -1:
    		if (answer.matches(".*\\d.*")) {
    			Pattern p = Pattern.compile("-?\\d+");
    			Matcher m = p.matcher(answer);
    			while (m.find()) {
    				answer = answer.replace(m.group(),
    						EnglishNumberToWords.convert(
    								Long.parseLong(m.group())));
    			}
    			checkAnswer(answer, response, screenName);
    		}
    		else if (response.matches(".*\\d.*")) {
    			Pattern p = Pattern.compile("-?\\d+");
    			Matcher m = p.matcher(response);
    			while (m.find()) {
    				response = response.replace(m.group(),
    						EnglishNumberToWords.convert(
    								Long.parseLong(m.group())));
    			}
    			checkAnswer(answer, response, screenName);
    		}
    		if (nameMap.containsKey(answer) && !checkedNameMap) {
    			checkedNameMap = true;
    			checkAnswer((String) nameMap.get(answer), response, screenName);
    		}
    		else if (nameMap.containsValue(answer) && !checkedNameMap) {
    			checkedNameMap = true;
    			checkAnswer((String) nameMap.getKey(answer), response,
						screenName);
    		}
    		if (acronymMap.containsValue(answer) && !checkedAcronymMap) {
    			checkedAcronymMap = true;
    			checkAnswer((String) acronymMap.getKey(answer), response,
    					screenName);
    		}
    		break;
    	default:
    		isCorrect = true;
    		if (!winners.contains(screenName) && winners.size() < 3)
    			winners.add(screenName);
    		Integer userScore = scoreMap.get(screenName);
    		if (userScore == null)
    			scoreMap.put(screenName, currScore);
    		else
    			scoreMap.put(screenName, userScore+currScore);
    		break;
    	}
    	return isCorrect;
    }
    */
    
    private static final String CORRECT_ANSWERS_TEXT = "\nCorrect guesses:";
    
    /*
     * Current #DMB Song & Setlist: [Rooftop ->]
     * Correct guesses: 
	 *	#1 - @Copperpot5 (Streak?) 
	 *	#2 - @jeffthefate
	 *	#3 -
     */
    private String findWinners(String lastSong) {
    	List<String> winners = new ArrayList<String>(0);
    	
    	lastSong = massageAnswer(lastSong);
    	
    	for (Entry<String, String> answer : answers.entrySet()) {
    		System.out.println("Checking " + answer.getValue());
    		boolean isCorrect = checkAnswer(lastSong, answer.getValue());
    		if (!isCorrect) {
    			System.out.println("Checking again " + answer.getValue());
    			isCorrect = checkAnswer(lastSong, 
    					reCheck(lastSong, answer.getValue()));
    		}
    		System.out.println("isCorrect: " + isCorrect);
    		if (isCorrect) {
    			winners.add(answer.getKey());
    			if (usersMap.containsKey(answer.getKey())) {
    				usersMap.put(answer.getKey(),
    						usersMap.get(answer.getKey())+1);
    			}
    			else {
    				usersMap.put(answer.getKey(), 1);
    			}
    		}
    	}
    	
    	StringBuilder sb = new StringBuilder();
        if (!winners.isEmpty() && (sb.length() + CORRECT_ANSWERS_TEXT.length()-1
        		+ winners.get(0).length() + 7) <= 140) {
	        sb.append(CORRECT_ANSWERS_TEXT);
	        int count = 0;
	    	for (String winner : winners) {
	    		if ((sb.length() + winner.length() + 10 +
	    				usersMap.get(winner).toString().length()) >
	    					140)
	    			break;
	    		count++;
	    		sb.append("\n#");
	    		sb.append(count);
	    		sb.append(" - @");
	    		sb.append(winner);
	    		sb.append(" (");
	    		sb.append(usersMap.get(winner).toString());
	    		sb.append(")");
	    	}
	    	/*
	    	if (sb.substring(sb.length()-1).equals(" "))
	    		sb.setLength(sb.length()-1);
	    	sb.append("]");
	    	*/
        }
        if (winners.isEmpty())
        	sb.append("\nNot Guessed");
    	answers.clear();
    	answers = new LinkedHashMap<String, String>();
    	return sb.toString();
    }
    
    private void watchTwitterStream() {
    	answers.clear();
    	answers = new LinkedHashMap<String, String>();
    	twitterStream = new TwitterStreamFactory(gameConfig)
	    		.getInstance();
	    twitterStream.addListener(this);
	    // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
	    twitterStream.user();
	    /*
	    FilterQuery filterQuery = new FilterQuery();
	    filterQuery.track(new String[]{"#hashtag"});
	    twitterStream.filter(filterQuery);
	    */
    }
    
    private void processTweet(Status status) {
		System.out.println("CURR TIME: " + new Date(System.currentTimeMillis()).toString());
		System.out.println("TWEET TIME: " + status.getCreatedAt().toString());
		System.out.println("RAW RESPONSE: " + status.getText());
		String userName = status.getUser().getScreenName();
		String inReplyTo = status.getInReplyToScreenName();
		System.out.println("user: " + userName);
		System.out.println("inReplyTo: " + inReplyTo);
		for (UserMentionEntity mention : status.getUserMentionEntities()) {
			System.out.println("mention: " + mention.getScreenName());
		}

		if (inReplyTo.equalsIgnoreCase(currAccount)) {
			String massaged = massageResponse(status.getText());
			System.out.println("Adding " + userName + " : " + massaged);
			answers.put(userName, massaged);
		}
	}
    
    public void onStatus(Status status) {
		processTweet(status);
	}

	public void onDeletionNotice(StatusDeletionNotice arg0) {}
	public void onScrubGeo(long arg0, long arg1) {}
	public void onStallWarning(StallWarning arg0) {}
	public void onTrackLimitationNotice(int arg0) {}
	public void onException(Exception arg0) {}
	public void onBlock(User arg0, User arg1) {}
	public void onDeletionNotice(long arg0, long arg1) {}
	public void onDirectMessage(DirectMessage dm) {
		// TODO Get user, if from @ChesterCopperpot or @jeffthefate, process further
		System.out.println("DM: " + dm.getSenderScreenName() + " : " + dm.getText());
		if ((dm.getSenderScreenName().equalsIgnoreCase("copperpot5") ||
				dm.getSenderScreenName().equalsIgnoreCase("jeffthefate")) &&
				dm.getText().toLowerCase(Locale.getDefault()).contains("end setlist")) {
			kill = true;
		}
	}
	public void onFavorite(User arg0, User arg1, Status arg2) {}
	public void onFollow(User arg0, User arg1) {}
	public void onFriendList(long[] arg0) {}
	public void onUnblock(User arg0, User arg1) {}
	public void onUnfavorite(User arg0, User arg1, Status arg2) {}
	public void onUserListCreation(User arg0, UserList arg1) {}
	public void onUserListDeletion(User arg0, UserList arg1) {}
	public void onUserListMemberAddition(User arg0, User arg1, UserList arg2) {}
	public void onUserListMemberDeletion(User arg0, User arg1, UserList arg2) {}
	public void onUserListSubscription(User arg0, User arg1, UserList arg2) {}
	public void onUserListUnsubscription(User arg0, User arg1, UserList arg2) {}
	public void onUserListUpdate(User arg0, UserList arg1) {}
	public void onUserProfileUpdate(User arg0) {}
	
	private void postSetlistScores() {
		System.out.println("usersMap size: " + usersMap.size());
		System.out.println(usersMap);
		if (!usersMap.isEmpty()) {
			sortedUsersMap.putAll(usersMap);
			StringBuilder sb = new StringBuilder();
			sb.append("[Final Results]");
			String winner = "";
			int count = 0;
			for (Entry<String, Integer> user : sortedUsersMap.entrySet()) {
				winner = user.getKey();
				if ((sb.length() + winner.length() + 10 +
	    				user.getValue().toString().length()) >
	    					140)
	    			break;
	    		count++;
	    		sb.append("\n#");
	    		sb.append(count);
	    		sb.append(" - @");
	    		sb.append(winner);
	    		sb.append(" (");
	    		sb.append(user.getValue().toString());
	    		sb.append(")");
			}
			// TODO postTweet
		}
	}
	
	private boolean checkAnswer(String answer, String response) {
		if (answer == null || response == null)
			return false;
    	int buffer = 4;
    	switch (answer.length()) {
    	case 15:
    	case 14:
    	case 13:
    	case 12:
    	case 11:
    	case 10:
    		buffer = 3;
    		break;
    	case 9:
    	case 8:
    	case 7:
    	case 6:
    	case 5:
    		buffer = 2;
    		break;
    	case 4:
    	case 3:
    		buffer = 1;
    		break;
    	case 2:
    	case 1:
    		buffer = 0;
    		break;
    	}
    	if (answer.matches("^[0-9]+$"))
    		buffer = 0;
    	int diffCount = StringUtils.getLevenshteinDistance(response, answer,
    			buffer);
    	
    	switch(diffCount) {
    	case -1:
    		return false;
    	default:
    		return true;
    	}
    }
	
	private String reCheck(String answer, String response) {
		Pattern p = Pattern.compile("-?\\d+");
    	Matcher m;
    	System.out.println("Recheck: " + answer + " : " + response);
		// If answer is numerals, convert them to words and check again
		if (answer.matches(".*\\d.*")) {
			System.out.println("Answer " + answer + " matches as numeral");
			m = p.matcher(answer);
			while (m.find()) {
				answer = answer.replace(m.group(),
						EnglishNumberToWords.convert(
								Long.parseLong(m.group())));
			}
			return answer;
		}
		// If response is numerals, convert them to words and check again
		else if (response.matches(".*\\d.*")) {
			System.out.println("Response " + response + " matches as numeral");
			m = p.matcher(response);
			while (m.find()) {
				response = response.replace(m.group(),
						EnglishNumberToWords.convert(
								Long.parseLong(m.group())));
			}
			return response;
		}
		boolean answerMatches = false;
		boolean responseMatches = false;
		for (ArrayList<String> list : nameList) {
			for (String name : list) {
				if (answer.contains(name))
					answerMatches = true;
				if (response.contains(name))
					responseMatches = true;
			}
			if (answerMatches && responseMatches)
				return answer;
			answerMatches = false;
			responseMatches = false;
		}
		return null;
	}
	
	private String massageResponse(String text) {
		return text.toLowerCase(Locale.getDefault()).replaceFirst(
						"(?<=^|(?<=[^a-zA-Z0-9-_\\.]))@([A-Za-z]+[A-Za-z0-9]+)",
						"").
				replaceAll("[.,'`\":;/?\\-!@#Ä~+*]", "").trim();
	}
	
	public String massageAnswer(String text) {
		String answer = text.replaceAll("[.,'`\":;/?\\-!@#Ä~+*]", "").
				toLowerCase(Locale.getDefault()).trim();
		return answer.replaceAll("(5\\|\\|)+", "");
	}
	
	public static class GameComparator implements Comparator<String> {

	    Map<String, Integer> base;
	    public GameComparator(Map<String, Integer> base) {
	        this.base = base;
	    }
   
	    public int compare(String a, String b) {
	        if (base.get(a) <= base.get(b)) {
	            return 1;
	        } else {
	            return -1;
	        } // returning 0 would merge keys
	    }
	}
    
}

