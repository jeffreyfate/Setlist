package com.jeffthefate.setlist;

/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */

import com.jeffthefate.Screenshot;
import com.jeffthefate.SetlistScreenshot;
import com.jeffthefate.TriviaScreenshot;
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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import twitter4j.*;
import twitter4j.conf.Configuration;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.TimeZone;

public class Setlist /*implements UserStreamListener*/ {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String TWEET_DATE_FORMAT = "MM/dd/yy";
    
    public final String FINAL_SCORES = "[Final Scores]";
    public final String CURRENT_SCORES = "[Current Scores]";
    
    private static String lastSong = "";
    private static boolean hasEncore = false;
    private static boolean hasGuests = false;
    private static boolean hasSegue = false;
    private static boolean firstBreak = false;
    private static boolean secondBreak = false;
    
    private static String setlistText = "";
    
    private static String currDateString = null;
    
    private static Screenshot screenshot;
    
    private String url;
    private int durationHours = 5;
    private long duration = durationHours * 60 * 60 * 1000;
    
    private boolean isDev;
    
    private String setlistJpgFilename;
    private String fontFilename;
    private int fontSize;
    private int verticalOffset;
    private String setlistFilename;
    private String lastSongFilename;
    private String setlistDir;
    private String banFile;
    
    private Configuration setlistConfig;
    private Configuration gameConfig;
    
    private String currAccount;
    
    public LinkedHashMap<String, String> answers = new LinkedHashMap<String, String>();
    
    private ArrayList<ArrayList<String>> nameList =
    		new ArrayList<ArrayList<String>>(0);
    
    //private TwitterStream twitterStream;
    private boolean kill = false;
    
    private static ArrayList<String> locList = new ArrayList<String>();
    private static ArrayList<String> setList = new ArrayList<String>();
    private static ArrayList<String> noteList = new ArrayList<String>();
    private static TreeMap<Integer, String> noteMap =
    		new TreeMap<Integer, String>();
    
    private HashMap<String, Integer> usersMap = new HashMap<String, Integer>();
    
    private String noteSong = "";
    
    private ArrayList<String> symbolList = new ArrayList<String>(0);
    private HashMap<String, String> replacementMap =
    		new HashMap<String, String>(0);
    
    private String finalTweetText = null;
    
    private boolean inSetlist = false;
    
    private String venueId = null;
    private String venueName = null;
    private String venueCity = null;
    
    private static Logger logger = Logger.getLogger(Setlist.class);
    
    // java -jar /home/Setlist-One.jar 14400000 >> ...
    
    public Setlist(String url, boolean isDev,
    		Configuration setlistConfig, Configuration gameConfig,
    		String setlistJpgFilename, String fontFilename, int fontSize,
    		int verticalOffset, String setlistFilename, String lastSongFilename,
    		String setlistDir, String banFile,
    		ArrayList<ArrayList<String>> nameList,
    		ArrayList<String> symbolList, String currAccount) {
    	this.url = url;
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
    	this.banFile = banFile;
    	this.nameList = nameList;
    	this.symbolList = symbolList;
    	this.currAccount = currAccount;
    }
    
    public void startSetlist() {
    	logger.info("Starting setlist...");
    	long endTime = System.currentTimeMillis() + duration;
    	inSetlist = true;
    	do {
    		runSetlistCheck(url);
    		try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
                logger.error("Setlist check wait interrupted!");
                e.printStackTrace();
            }
    	} while (endTime >= System.currentTimeMillis() && !kill);
    	logger.debug("duration: " + duration);
    	if (duration > 0) {
    		screenshot = new SetlistScreenshot(setlistJpgFilename, fontFilename,
    				setlistText, fontSize, verticalOffset);
    		updateStatus(setlistConfig, finalTweetText,
    				new File(screenshot.getOutputFilename()), -1);
    		postSetlistScoresImage(FINAL_SCORES);
    	}
    	inSetlist = false;
    }
    
    private void setVenueId(String venueId) {
    	this.venueId = venueId;
    }
    
    private String getVenueId() {
    	return venueId;
    }
    
    private void setVenueName(String venueName) {
    	this.venueName = venueName;
    }
    
    private String getVenueName() {
    	return venueName;
    }
    
    private void setVenueCity(String venueCity) {
    	this.venueCity = venueCity;
    }
    
    private String getVenueCity() {
    	return venueCity;
    }
    
    public void setKill(boolean kill) {
    	this.kill = kill;
    }
    
    public int getDurationHours() {
    	return durationHours;
    }
    
    public void setDuration(int hours) {
    	if (hours <= 10) {
    		durationHours = hours;
    		duration = hours * 60 * 60 * 1000;
    	}
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
    
    public boolean checkForNotes(String currSong, List<String> noteList) {
    	String noteChar;
    	for (String note : noteList) {
    		noteChar = note.substring(0, 1);
    		if (currSong.contains(noteChar)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public void runSetlistCheck(String url) {
    	Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(System.currentTimeMillis());
        logger.info(cal.getTime().toString());
        logger.info(Charset.defaultCharset().displayName());
        String html;
        if (url != null) {
        	html = liveSetlist(url);
        }
        else {
        	html = liveSetlist("https://whsec1.davematthewsband.com/backstage.asp");
        }
        currDateString = getNewSetlistDateString(locList.get(0));
        StringBuilder sb = new StringBuilder();
        if (locList.size() < 4) {
        	locList.add(1, "Dave Matthews Band");
        }
		sb.append("[Final] Dave Matthews Band Setlist for ");
		sb.append(getTweetDateString(locList.get(0)));
		sb.append(" - ");
		sb.append(locList.get(locList.size()-2));
		finalTweetText = sb.toString();
		logger.info(sb.toString());
		sb = new StringBuilder();
        for (String loc : locList) {
        	sb.append(loc);
        	sb.append("\n");
        }
        if (setList.size() >= 2 && setList.get(setList.size()-1).equals(
        		setList.get(setList.size()-2))) {
        	logger.info("Removed " + setList.remove(setList.size()-1));
        }
        // Replace note symbols
        logger.info("Old symbols:");
        logger.info(setList);
		String noteChar = "";
		ArrayList<String> newSymbols = new ArrayList<String>(setList);
		for (int i = 0; i < newSymbols.size(); i++) {
			if (newSymbols.get(i).contains("5||")) {
				noteChar = "5||";
			}
			else {
				noteChar = StringUtils.strip(newSymbols.get(i).replaceAll(
						"[A-Za-z0-9,'’()&:.\\->@]+", ""));
			}
			if (!StringUtils.isBlank(noteChar)) {
				// There is a note for this song
				if (!replacementMap.containsKey(noteChar)) {
					replacementMap.put(noteChar,
							symbolList.get(replacementMap.size()));
					logger.info(replacementMap);
				}
				newSymbols.set(i, newSymbols.get(i).replace(noteChar,
						replacementMap.get(noteChar)));
			}
		}
		logger.info("New symbols:");
        logger.info(newSymbols);
		// TODO Replace in notes
        // We need a spacer new line in these scenarios:
        //     Between location block and set list
        //     Between first set and set break
        //     Between set break and second set
        //     Between second set and encore
        //     Between last song and notes, if any
        boolean setBreakLast = false;
        for (String set : setList) {
        	if (setBreakLast) {
        		sb.append("\n");
        		setBreakLast = false;
        	}
        	sb.append("\n");
        	if (set.toLowerCase().equals("encore:")) {
    			sb.append("\n");
    			sb.append(set);
        	}
        	else if (set.toLowerCase().equals("set break")) {
    			sb.append("\n");
    			sb.append(set);
    			setBreakLast = true;
        	}
        	else {
        		sb.append(set);
        	}
        }
        if (sb.substring(sb.length()-4, sb.length()).equals("\n\n")) {
        	sb.delete(sb.length()-2, sb.length());
        }
        if (!noteMap.isEmpty()) {
        	sb.append("\n");
        	for (Entry<Integer, String> note : noteMap.entrySet()) {
        		sb.append("\n");
            	sb.append(note.getValue());
        	}
        }
        else if (!noteList.isEmpty()) {
    		sb.append("\n");
        	for (String note : noteList) {
        		sb.append("\n");
            	sb.append(note);
        	}
        }
        setlistText = sb.toString();
        logger.info(setlistText);
        String setlistFile = setlistFilename +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSongFile = lastSongFilename +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSetlist = readStringFromFile(setlistFile);
        logger.info("lastSetlist:");
        logger.info(lastSetlist);
        String diff = StringUtils.difference(lastSetlist, setlistText);
        logger.info("diff:");
        logger.info(diff);
        boolean hasChange = !StringUtils.isBlank(diff);
        sb.setLength(0);
        if (hasChange) {
            writeStringToFile(setlistText, setlistFile);
            // -1 if failure or not a new setlist
            // 0 if a new setlist (latest)
            // 1 if there is a newer date available already
            int newDate = uploadLatest(setlistText);
            getVenueFromResponse(getResponse("Venue", getVenueId()));
            String lastSongFromFile = readStringFromFile(lastSongFile);
            if (newDate == 0 || (newDate == -1 &&
            		!lastSongFromFile.equals(lastSong))) {
            	if (!stripSpecialCharacters(lastSongFromFile).equals(
            			stripSpecialCharacters(lastSong))) {
            		logger.info("POST NOTIFICATION AND TWEET: " +
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
	                    sb.append(lastSong);
	                    sb.append("]");
	                    if (!lastSong.toLowerCase().contains("encore:") &&
	                    		!lastSong.toLowerCase().contains("set break")) {
	                    	gameMessage = findWinners(lastSong, sb.toString());
                			noteSong = lastSong;
	                    }
	                }
	                screenshot = new SetlistScreenshot(
		    				setlistJpgFilename, fontFilename, setlistText,
		    				fontSize, verticalOffset);
	                postTweet(sb.toString(), gameMessage,
	                		new File(screenshot.getOutputFilename()), -1, true);
            	}
            	else {
            		logger.info("POST NOTIFICATION: BLANK");
            		if (!isDev) {
            			postNotification(getPushJsonString("", setlistText,
            					getExpireDateString()));
            		}
            	}
                writeStringToFile(lastSong, lastSongFile);
            }
            else if (readStringFromFile(lastSongFile).equals(lastSong)) {
            	logger.info("POST NOTIFICATION: BLANK");
            	if (!isDev) {
            		postNotification(getPushJsonString("", setlistText,
            				getExpireDateString()));
            		String updateText = StringUtils.strip(diff);
            		if (updateText.length() <= 140) {
            			updateStatus(setlistConfig, StringUtils.strip(diff),
            					null, -1);
            		}
        			String noteUpdate = "";
        			for (String setSong : setList) {
        				if (setSong.contains(noteSong)) {
        					if (setSong.contains("5||")) {
        						noteChar = "5||";
        					}
        					else {
        						noteChar = setSong.replaceAll(
        								"[A-Za-z0-9,'()&:.]+", "");
        					}
        					if (!StringUtils.isBlank(noteChar)) {
        						for (String note : noteList) {
        							if (note.startsWith(noteChar) &&
        									diff.contains(note)) {
        								if (!StringUtils.isBlank(
        										noteUpdate)) {
        									noteUpdate =
        											noteUpdate.concat("\n");
        								}
        								noteUpdate = noteUpdate.concat(note);
        							}
        						}
        					}
        				}
        			}
        			logger.info("noteUpdate: " + noteUpdate);
            	}
            }
            logger.info(html);
        }
        locList.clear();
        setList.clear();
        noteList.clear();
        noteMap.clear();
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
        Date date;
        String curr;
        String dateString;
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
            logger.info("Failed to parse date from " + dateString);
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
            StringBuilder sb;
            String html = null;
            try {
                response = client.execute(getMethod);
                html = EntityUtils.toString(response.getEntity(), "UTF-8");
                html = StringEscapeUtils.unescapeHtml4(html);
            } catch (ClientProtocolException e1) {
                logger.info("Failed to connect to " +
                        getMethod.getURI().toASCIIString());
                e1.printStackTrace();
            } catch (IOException e1) {
                logger.info("Failed to get setlist from " +
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
                    getMethod = new HttpGet(currUrl);
                    sb = new StringBuilder();
                    html = null;
                    try {
                        response = client.execute(getMethod);
                        html = EntityUtils.toString(response.getEntity(),
                        		"UTF-8");
                        html = StringEscapeUtils.unescapeHtml4(html);
                    } catch (ClientProtocolException e1) {
                        logger.info("Failed to connect to " +
                                getMethod.getURI().toASCIIString());
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        logger.info("Failed to get setlist from " +
                                getMethod.getURI().toASCIIString());
                        e1.printStackTrace();
                    }
                    doc = Jsoup.parse(html);
                    char badChar = 65533;
                    char apos = 39;
                    char endChar = 160;
                    StringBuilder locString = new StringBuilder();
                    String dateString;
                    StringBuilder setString = new StringBuilder();
                    int numTicketings = 0;
                    boolean br = false;
                    boolean b = false;
                    int slot = 0;
                    String setlistId = null;
                    logger.info("nulling lastPlay");
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
                                            	logger.info(
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
                                            		logger.info(
                                            				"TextNode nulling" +
                                            				" lastPlay");
                                            		logger.info(
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
                                                        	logger.info(
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
                                                        	logger.info(
                                                    			"br nulling " +
                                                    			"lastPlay");
                                                        	lastPlay = null;
                                                        }
                                                    }
                                                    if (!firstBreak) {
                                                    	logger.info(
                                                			"NOT firstBreak");
                                                    	logger.info(
                                                			"lastPlay: " +
                                        					lastPlay);
                                                    	logger.info(
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
                                                        	logger.info(
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
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported encoding:");
            logger.error(nameValuePairs);
            e.printStackTrace();
        }
        HttpResponse response = null;
        HttpClient client = createSecureConnection();
        try {
            response = client.execute(postMethod);
        } catch (IOException e) {
            logger.error("Unable to connect to " +
                    postMethod.getURI().toASCIIString());
            e.printStackTrace();
        }
        if (response == null || (response.getStatusLine().getStatusCode() !=
                200 && response.getStatusLine().getStatusCode() != 302)) {
            return "Error";
        }
        HttpGet getMethod = new HttpGet(url);
        StringBuilder sb = new StringBuilder();
        String html = null;
        if (!url.startsWith("https")) {
            client = HttpClientBuilder.create().build();
        }
        try {
            response = client.execute(getMethod);
            html = EntityUtils.toString(response.getEntity(), "UTF-8");
            html = StringEscapeUtils.unescapeHtml4(html);
        } catch (ClientProtocolException e) {
            logger.info("Failed to connect to " +
                    getMethod.getURI().toASCIIString());
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("Failed to get setlist from " +
                    getMethod.getURI().toASCIIString());
            e.printStackTrace();
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
        logger.info("lastSong: " + lastSong);
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
	        } catch (UnsupportedEncodingException e) {
                logger.error("Unsupported encoding for " + nameValuePairs);
                e.printStackTrace();
            }
	        HttpResponse response = null;
	        HttpClient client = createSecureConnection();
	        try {
	            response = client.execute(postMethod);
	        } catch (IOException e) {
                logger.error("Unable to connect to " +
                        postMethod.getURI().toASCIIString());
                e.printStackTrace();
            }
	        if (response == null || (response.getStatusLine().getStatusCode() !=
	                200 && response.getStatusLine().getStatusCode() != 302))
	        	logger.info("Failed to get response from to " +
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
	            logger.info("Failed to connect to " +
	                    getMethod.getURI().toASCIIString());
	            e1.printStackTrace();
	        } catch (IOException e1) {
	            logger.info("Failed to get setlist from " +
	                    getMethod.getURI().toASCIIString());
	            e1.printStackTrace();
	        }
	        return Jsoup.parse(html);
    	}
    	else {
    		return Jsoup.parse(StringEscapeUtils.unescapeHtml4(
    				readStringFromFile(url)));	
    	}
    }

    public String liveSetlist(String url) {
    	Document doc = getPageDocument(url);
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
        String divText;
        TreeMap<Integer, String> songMap = new TreeMap<Integer, String>();
        int currentLoc;
        String currSong;
        int breaks = 0;
        String fontText;
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
        	                                			logger.info(
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
        	                                				logger.info(
    	                                						"show notes: " +
                                								divStyleLocation);
        	                                				if (divStyleLocation > -1)
        	                                					noteMap.put(
    	                                							divStyleLocation,
    	                                							oldNote.concat("Notes:"));
        	                                				logger.info(
        	                                						"Notes:");
        	                                				noteList.add(0,
    	                                						"Notes:");
        	                                				breaks = 0;
        	                                			}
        	                                			else {
        	                                				if (hasGuest) {
        	                                					logger.info(
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
        	                                					logger.info(
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
        	                                					logger.info(
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
        	                                					logger.info(
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
        	                                	logger.info("img: " +
        	                                			divStyleLocation);
        	                                	if (divStyleLocation > -1)
        	                                		noteMap.put(divStyleLocation,
    	                                				oldNote.concat("\n")
    	                                				.concat("-> "));
        	                                	noteList.add("-> ");
        	                                	logger.info("-> ");
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
        	                        						logger.info(
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
        	                        						logger.info(fontText);
        	                        					} else if (fontText.contains(")")) {
        	                        						lastPartial = true;
        	                        						logger.info(
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
        	                        						logger.info(
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
        	                        						logger.info(
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
    					logger.info(currSong);
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
    	    					logger.info(currSong);
    	    					lastSong = currSong;
    	    					breaks = 0;
    						}
    						else if (divText.toLowerCase().contains(
    								"set break")) {
    							currSong = "Set Break";
    							setList.add(currSong);
    							logger.info(currSong);
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
	                					logger.info("Notes:");
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
                        					logger.info(StringUtils.trim(
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
            		logger.info("firstBreak: " + firstBreak);
    				logger.info("hasEncore: " + hasEncore);
    				logger.info("secondBreak: " + secondBreak);
            		// Found a segue image
            		if (node.nodeName().equals("img")) {
            			if (hasSong) {
            				logger.info("Adding ->");
            				currSong = setList.get(setList.size()-1).concat(
            						" ->");
            				setList.set(setList.size()-1, currSong);
            				lastSong = currSong;
            			}
            			else {
            				if (noteList.isEmpty()) {
            					noteList.add("Notes:");
            					logger.info("Notes:");
            				}
            				noteList.add("-> ");
            				logger.info("-> ");
            				hasSegue = true;
            			}
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
            					logger.info(((TextNode) child).text().concat(
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
            }
            for (Entry<Integer, String> song : songMap.entrySet()) {
            	currSong = song.getValue();
            	setList.add(currSong);
            	logger.info(currSong);
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
        }
        return doc.body().toString();
    }

    private String getNewSetlistDateString(String dateLine) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        Date date;
        String dateString = null;
        try {
            date = dateFormat.parse(dateLine);
            dateFormat = new SimpleDateFormat(DATE_FORMAT);
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e) {
            logger.info("Failed to parse date from " + dateLine);
            e.printStackTrace();
        }
        return dateString;
    }
    
    private String getShortSetlistString(String dateLine) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        Date date;
        String dateString = null;
        try {
            date = dateFormat.parse(dateLine);
            dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e) {
            logger.info("Failed to parse date from " + dateLine);
            e.printStackTrace();
        }
        return dateString;
    }
    
    private String getTweetDateString(String dateLine) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        Date date;
        String dateString = null;
        try {
            date = dateFormat.parse(dateLine);
            dateFormat = new SimpleDateFormat(TWEET_DATE_FORMAT);
            dateString = dateFormat.format(date.getTime());
        } catch (Exception e) {
            logger.info("Failed to parse date from " + dateLine);
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

    private String getSetlist(String latestSetlist) {
        String dateString = getNewSetlistDateString(latestSetlist);
        logger.info("getSetlist dateString: " + dateString);
        if (dateString == null) {
            return null;
        }
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity;
        HttpResponse response;
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
                logger.info("GET of " + dateString + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        logger.info("getSetlist responseString: " + responseString);
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
                logger.info("POST of setlist failed!");
                logger.info(json);
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
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private boolean putSetlist(String objectId, String json) {
    	logger.info("putSetlist: " + objectId + " : " + json);
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
                logger.info("PUT to " + objectId + " failed!");
                logger.info(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("Add play " + playId + " to " + setlistId +
                		" failed!");
                logger.info(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private String getSong(String latestSong) {
    	logger.info("getSong: " + latestSong);
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
                logger.info("GET of " + latestSong + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("GET of " + setlistId + " : " + slot +
                		" play failed!");
                return true;
            }
            entity = response.getEntity();
            if (entity != null) {
                 responseString = EntityUtils.toString(response.getEntity());
                 logger.info("getPlay responseString: " +
                		 responseString);
                 int tempSlot = getLargestSlotFromResponse(responseString);
                 logger.info("getPlay slot: " + slot);
                 logger.info("getPlay tempSlot: " + tempSlot);
                 if (slot > tempSlot)
                	 return false;
            }
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("POST of notification failed!");
                logger.info(json);
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("POST of song failed!");
                logger.info(json);
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
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private boolean putSong(String objectId, String json) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        logger.info(objectId);
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
                logger.info("PUT to " + objectId + " failed!");
                logger.info(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("POST of song failed!");
                logger.info(json);
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
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("PUT to " + objectId + " failed!");
                logger.info(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("PUT to " + objectId + " failed!");
                logger.info(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private String getSetlistJsonString(String latestSetlist, String venueId) {
        currDateString = getNewSetlistDateString(latestSetlist);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dateNode = factory.objectNode();
        dateNode.put("__type", "Date");
        dateNode.put("iso", currDateString);
        ObjectNode venueNode = factory.objectNode();
        venueNode.put("__type", "Pointer");
        venueNode.put("className", "Venue");
        venueNode.put("objectId", venueId);
        rootNode.put("set", latestSetlist);
        rootNode.put("setDate", dateNode);
        if (venueId != null) {
        	rootNode.put("venue", venueNode);
        }
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
        dataNode.put("shortDate", getShortSetlistString(locList.get(0)));
        dataNode.put("venueName", getVenueName());
        dataNode.put("venueCity", getVenueCity());
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
        relationNode.put("__type", "Pointer");
        relationNode.put("className", "Play");
        relationNode.put("objectId", playId);
        relationArray.add(relationNode);
        addRelationNode.put("__op", "AddRelation");
        addRelationNode.put("objects", relationArray);
        rootNode.put("setlist", addRelationNode);
        return rootNode.toString();
    }

    private String getVenueIdFromResponse(String responseString) {
    	logger.info("getVenueIdFromResponse: " + responseString);
    	if (responseString == null) {
    		return null;
    	}
		JsonFactory f = new JsonFactory();
		JsonParser jp;
		String fieldName;
		String venue = null;
		try {
		    jp = f.createJsonParser(responseString);
		    JsonToken token;
			if (jp.nextToken() == JsonToken.START_OBJECT) {
				if (jp.nextToken() == JsonToken.FIELD_NAME &&
						"results".equals(jp.getCurrentName())) {
					if (jp.nextToken() == JsonToken.START_ARRAY) {
						while ((token = jp.nextToken()) !=
								JsonToken.END_ARRAY) {
							if (token == JsonToken.FIELD_NAME) {
								fieldName = jp.getCurrentName();
								if ("venue".equals(fieldName)) {
									while ((token = jp.nextToken()) !=
											JsonToken.END_OBJECT) {
										if (token == JsonToken.FIELD_NAME &&
												jp.getCurrentName().equals("objectId")) {
											jp.nextToken();
											venue = jp.getText();
										}
									}
								}
							}
						}
					}
				}
			}
		    jp.close();
		} catch (JsonParseException e) {
		    logger.info("Failed to parse " + responseString);
		    e.printStackTrace();
		} catch (IOException e) {
		    logger.info("Failed to parse " + responseString);
		    e.printStackTrace();
		}
		return venue;
	}
    
    private static String getVenueIdFromVenue(String responseString) {
    	logger.info("getVenueIdFromVenue: " + responseString);
    	if (responseString == null) {
    		return null;
    	}
		JsonFactory f = new JsonFactory();
		JsonParser jp;
		String fieldName;
		String venue = null;
		try {
		    jp = f.createJsonParser(responseString);
		    JsonToken token;
			if (jp.nextToken() == JsonToken.START_OBJECT) {
				while ((token = jp.nextToken()) !=
						JsonToken.END_OBJECT) {
					if (token == JsonToken.FIELD_NAME) {
						fieldName = jp.getCurrentName();
						if ("objectId".equals(fieldName)) {
							jp.nextToken();
							venue = jp.getText();
							break;
						}
					}
				}
			}
		    jp.close();
		} catch (JsonParseException e) {
		    logger.info("Failed to parse " + responseString);
		    e.printStackTrace();
		} catch (IOException e) {
		    logger.info("Failed to parse " + responseString);
		    e.printStackTrace();
		}
		return venue;
	}
    
    private void getVenueFromResponse(String response) {
    	if (response == null) {
    		return;
    	}
		JsonFactory f = new JsonFactory();
		JsonParser jp;
		String fieldName;
		try {
		    jp = f.createJsonParser(response);
		    JsonToken token;
			if (jp.nextToken() == JsonToken.START_OBJECT) {
				while ((token = jp.nextToken()) !=
						JsonToken.END_OBJECT) {
					if (token == JsonToken.FIELD_NAME) {
						fieldName = jp.getCurrentName();
						if ("name".equals(fieldName)) {
							jp.nextToken();
							setVenueName(jp.getText());
						}
						else if ("city".equals(fieldName)) {
							jp.nextToken();
							setVenueCity(jp.getText());
						}
					}
				}
			}
		    jp.close();
		} catch (JsonParseException e) {
		    logger.info("Failed to parse " + response);
		    e.printStackTrace();
		} catch (IOException e) {
		    logger.info("Failed to parse " + response);
		    e.printStackTrace();
		}
    }
    
    public String getObjectIdFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        JsonToken token;
        boolean insideObject = false;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
            	jp.nextToken();
            	jp.nextToken();
                while ((token = jp.nextToken()) != null) {
                	if (token == JsonToken.START_OBJECT) {
                		insideObject = true;
                	}
                	else if (token == JsonToken.END_OBJECT) {
                		insideObject = false;
                	}
                	if (token == JsonToken.FIELD_NAME) {
	                    fieldname = jp.getCurrentName();
	                    if ("objectId".equals(fieldname) && !insideObject) {
	                    	jp.nextToken();
	                        objectId = jp.getText();
	                        break;
	                    }
                	}
                }
            }
            jp.close();
        } catch (JsonParseException e) {
            logger.info("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("Failed to parse " + responseString);
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
            jp.close();
        } catch (JsonParseException e) {
            logger.info("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("Failed to parse " + responseString);
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
            jp.close();
        } catch (JsonParseException e) {
            logger.info("Failed to parse " + createString);
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("Failed to parse " + createString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private int getLargestSlotFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        int slot = -1;
        int tempSlot;
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
                    	logger.info("slot fieldname");
                        tempSlot = jp.getIntValue();
                        logger.info("tempSlot: " + tempSlot);
                        slot = tempSlot > slot ? tempSlot : slot;
                    }
                }
            }
            jp.close();
        } catch (JsonParseException e) {
            logger.info("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("Failed to parse " + responseString);
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
            jp.close();
        } catch (JsonParseException e) {
            logger.info("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private String getVenueJson() {
    	String venueName = locList.get(2);
		String venueCity = locList.get(3);
		JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
		ObjectNode rootNode = jsonNodeFactory.objectNode();
		rootNode.put("name", venueName);
		rootNode.put("city", venueCity);
		return rootNode.toString();
    }
    
    private static String getResponse(String className, String objectId) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity;
        HttpResponse response;
        String url = "https://api.parse.com/1/classes/";
        url += className;
        url += "/";
        url += objectId;
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
            	logger.info("GET to " + className + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null) {
                 return EntityUtils.toString(response.getEntity());
            }
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return null;
    }
    
    private static String getResponse(String className, int limit,
    		String where) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity;
        HttpResponse response;
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.parse.com/1/classes/");
        sb.append(className);
        sb.append("?limit=");
        sb.append(Integer.toString(limit));
        sb.append("&order=setDate");
        if (where != null) {
        	sb.append("&where=");
        	try {
				sb.append(URLEncoder.encode(where, "UTF-8").replace("+", "%20")
						.replace("-", "%2D"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
        }
        String url = sb.toString();
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
            	logger.info("GET to " + className + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null) {
                 return EntityUtils.toString(response.getEntity());
            }
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return null;
    }
    
    private static String postObject(String className, String postString) {
    	HttpClient httpclient = HttpClientBuilder.create().build();
    	HttpEntity entity;
        HttpResponse response;
        String url = "https://api.parse.com/1/classes/";
        url += className;
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("X-Parse-Application-Id",
        		"ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key",
        		"1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.setEntity(new StringEntity(postString,
        		ContentType.APPLICATION_JSON));
        try {
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                logger.info("POST to " + className + " failed!");
            }
            else {
            	entity = response.getEntity();
                if (entity != null) {
                     return EntityUtils.toString(entity);
                }
            }
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
            		httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
            		httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return null;
    }
    
    private int uploadLatest(String latestSetlist) {
        String getResponse = getSetlist(latestSetlist);
        if (getResponse == null) {
            logger.info("Fetch setlist from Parse failed!");
            logger.info(latestSetlist);
            return -1;
        }
        String venueJson = getVenueJson();
        String objectId = getObjectIdFromResponse(getResponse);
        if (objectId == null) {
        	String venueId = getVenueIdFromVenue(getResponse("Venue", 1,
        			venueJson));
        	if (venueId == null) {
				venueId = getVenueIdFromVenue(postObject("Venue",
						venueJson));
        	}
        	if (!isDev) {
        		postSetlist(getSetlistJsonString(latestSetlist, venueId));
        	}
            File dir = new File("/home/");
            String[] files = dir.list(new FilenameFilter() {
            	public boolean accept(File dir, String filename) {
            		return filename.endsWith(".txt");
        		}
        	});
            String dateString = getNewSetlistDateString(latestSetlist);
            Date newDate = convertStringToDate(DATE_FORMAT, dateString);
            for (String file : files) {
            	if (file.startsWith("setlist")) {
            		if (convertStringToDate(DATE_FORMAT,
            				file.substring(7)).after(newDate)) {
            			logger.info("newer setlist file found!");
            			return 1;
            		}
            	}
            }
            setVenueId(venueId);
            return 0;
        }
        else {
        	String venueId = getVenueIdFromResponse(getResponse);
        	logger.info("VenueId: " + venueId);
        	if (venueId == null) {
        		venueId = getVenueIdFromVenue(getResponse("Venue", 1,
        				venueJson));
        		logger.info("VenueId: " + venueId);
        		if (venueId == null) {
    				venueId = getVenueIdFromVenue(postObject("Venue",
    						venueJson));
    				logger.info("VenueId: " + venueId);
            	}
        	}
            putSetlist(objectId, getSetlistJsonString(latestSetlist, venueId));
            setVenueId(venueId);
            return -1;
        }
    }
    
    private String createLatest(String dateString) {
    	logger.info("createLatest: " + dateString);
    	if (dateString == null)
            return null;
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpEntity entity;
        HttpResponse response;
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
                logger.info("GET of " + dateString + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        if (responseString == null) {
            logger.info("Fetch setlist from Parse failed!");
            logger.info(dateString);
        }
        String objectId = getObjectIdFromResponse(responseString);
        if (objectId == null) {
            objectId = postSetlist(getNewSetlistJsonString(dateString));
        }
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
        HttpEntity entity;
        HttpResponse response;
        String responseString;
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
                logger.info("GET of " + setlistId + " plays failed!");
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
            logger.info("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
                logger.info("PUT to " + closerId + " failed!");
                logger.info(json);
            }  
        } catch (UnsupportedEncodingException e) {
            logger.info("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            logger.info("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.info("Failed to get setlist from " +
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
            logger.info("Fetch setlist from Parse failed!");
            logger.info(latestSong);
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
        BufferedOutputStream bufStream;
        try {
            bufStream = new BufferedOutputStream(
                    new FileOutputStream(filename, false), buffer.length);
            bufStream.write(buffer);
            bufStream.flush();
            bufStream.close();
        } catch (FileNotFoundException e) {
            logger.info("File not found: " + filename);
            e.printStackTrace();
        } catch (IOException e) {
            logger.info("BufferedOutputStream failed for: " + filename);
            e.printStackTrace();
        }
    }
    
    private byte[] readBufferFromFile(String filename) {
        File file = new File(filename);
        byte[] buffer = new byte[(int)file.length()];
        BufferedInputStream bufStream;
        try {
            bufStream = new BufferedInputStream(new FileInputStream(file),
                    buffer.length);
            bufStream.read(buffer);
            bufStream.close();
        } catch (FileNotFoundException e) {
            logger.info(filename + " not found!");
        } catch (IOException e) {
            logger.info(filename + " IO Exception!");
        } catch (IllegalArgumentException e) {
            logger.info("File stream is <= 0");
            return new byte[0];
        }
        return buffer;
    }
    
    private void writeStringToFile(String output, String filename) {
        if (output != null) {
            writeBufferToFile(output.getBytes(), filename);
        }
    }
    
    private String readStringFromFile(String filename) {
        return bytesToString(readBufferFromFile(filename)).toString();
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
    
    public Status postTweet(String setlistMessage, String gameMessage,
    		File file, long replyTo, boolean postGame) {
    	logger.info("Tweet text: " + setlistMessage);
    	logger.info("Tweet length: " + setlistMessage.length());
    	Status status = updateStatus(setlistConfig, setlistMessage, file,
    			replyTo);
    	if (status == null) {
    		return status;
    	}
    	if (postGame && !setlistMessage.toLowerCase(
				Locale.getDefault()).contains("[Encore:]".toLowerCase(
						Locale.getDefault())) && !setlistMessage.toLowerCase(
								Locale.getDefault()).contains(
										"[Set Break]".toLowerCase(
												Locale.getDefault()))) {
    		if (!setlistMessage.toLowerCase(Locale.getDefault()).contains(
					"[Final".toLowerCase(Locale.getDefault()))) {
    			status = updateStatus(gameConfig,
    					setlistMessage.concat(gameMessage), null, -1);
			}
			else {
				return null;
			}
    	}
		return status;
    }
    
    private Status updateStatus(Configuration twitterConfig, String message,
    		File file, long replyTo) {
    	Twitter twitter = new TwitterFactory(twitterConfig).getInstance();
    	StatusUpdate statusUpdate = new StatusUpdate(message);
    	if (file != null)
			statusUpdate.media(file);
		statusUpdate.setInReplyToStatusId(replyTo);
    	Status status = null;
    	int tries = 0;
		do {
			tries++;
	    	try {
				status = twitter.updateStatus(statusUpdate);
	    	} catch (TwitterException te) {
	    		te.printStackTrace();
	    		logger.info("Failed to get timeline: " +
	    				te.getMessage());
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
	    	}
		} while (status == null && tries < 10);
		return status;
    }

    private static final String CORRECT_ANSWERS_TEXT = "\nCorrect guesses:";
    
    /*
     * Current #DMB Song & Setlist: [Rooftop ->]
     * Correct guesses: 
	 *	#1 - @Copperpot5 (Streak?) 
	 *	#2 - @jeffthefate
	 *	#3 -
     */
    public String findWinners(String lastSong, String songMessage) {
    	List<String> winners = new ArrayList<String>(0);
    	
    	lastSong = massageAnswer(lastSong);
    	
    	boolean answerMatches;
		boolean responseMatches;
		boolean isCorrect;
		
		ArrayList<String> banList = getBanList();
		
    	for (Entry<String, String> answer : answers.entrySet()) {
    		if (banList.contains(answer.getKey().toLowerCase(
    				Locale.getDefault()))) {
    			continue;
    		}
    		logger.info("Checking " + answer.getValue());
    		isCorrect = false;
    		if (checkAnswer(lastSong, answer.getValue())) {
    			isCorrect = true;
    		}
    		else if (answer.getValue().contains(lastSong)) {
    			isCorrect = true;
    		}
    		else {
	    		for (ArrayList<String> list : nameList) {
	    			answerMatches = false;
	    			responseMatches = false;
	    			for (String name : list) {
	    				if (lastSong.contains(name) ||
	    						checkAnswer(name, lastSong)) {
	    					answerMatches = true;
	    				}
	    				if (answer.getValue().contains(name) ||
	    						checkAnswer(name, answer.getValue())) {
	    					responseMatches = true;
	    				}
	    			}
	    			if (answerMatches && responseMatches) {
	    				isCorrect = true;
	        			break;
	    			}
	    		}
    		}
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
        if (!winners.isEmpty() && (songMessage.length() +
        		CORRECT_ANSWERS_TEXT.length()-1 + winners.get(0).length() + 10 +
        		usersMap.get(winners.get(0)).toString().length())
        			<= 140) {
	        sb.append(CORRECT_ANSWERS_TEXT);
	        int count = 0;
	    	for (String winner : winners) {
	    		if (banList.contains(winner.toLowerCase(
	    				Locale.getDefault()))) {
	    			continue;
	    		}
	    		count++;
	    		logger.info("Setlist game tweet length: " + sb.length());
	    		if ((songMessage.length() + sb.length()-1 + 3 +
	    				Integer.toString(count).length() + 4 + winner.length() +
	    				2 + usersMap.get(winner).toString().length() + 1) >
	    					140) {
	    			break;
	    		}
	    		sb.append("\n#");
	    		sb.append(count);
	    		sb.append(" - @");
	    		sb.append(winner);
	    		sb.append(" (");
	    		sb.append(usersMap.get(winner).toString());
	    		sb.append(")");
	    	}
        }
        if (winners.isEmpty()) {
        	sb.append("\nNot Guessed");
        }
    	answers.clear();
    	answers = new LinkedHashMap<String, String>();
    	if (sb.length() > 140) {
    		return (String) sb.toString().subSequence(0, 140);
    	}
    	else {
    		return sb.toString();
    	}
    }

    public void addAnswer(String userName, String message) {
    	answers.put(userName, massageResponse(message));
    }
    
    public void processTweet(Status status) {
    	logger.info("inSetlist: " + inSetlist);
    	if (!inSetlist) {
    		return;
    	}
		logger.info("CURR TIME: " + new Date(System.currentTimeMillis()).toString());
		logger.info("TWEET TIME: " + status.getCreatedAt().toString());
		logger.info("RAW RESPONSE: " + status.getText());
		String userName = status.getUser().getScreenName();
		String inReplyTo = status.getInReplyToScreenName();
		logger.info("user: " + userName);
		logger.info("inReplyTo: " + inReplyTo);
		for (UserMentionEntity mention : status.getUserMentionEntities()) {
			logger.info("mention: " + mention.getScreenName());
		}

		if (inReplyTo.equalsIgnoreCase(currAccount)) {
			String massaged = massageResponse(status.getText());
			logger.info("Adding " + userName + " : " + massaged);
			answers.put(userName, massaged);
		}
	}
    
    private void sendDirectMessage(Configuration tweetConfig,
			String screenName, String message) {
		Twitter twitter = new TwitterFactory(tweetConfig).getInstance();
		try {
			twitter.sendDirectMessage(screenName, message);
		} catch (TwitterException e) {
			logger.info("Unable to send direct message!");
			e.printStackTrace();
		}
	}

    public void banUser(String user) {
    	ArrayList<String> banList = getBanList();
    	if (!banList.contains(user)) {
    		banList.add(user);
    	}
    	if (!saveBanList(banList)) {
			sendDirectMessage(gameConfig, "Copperpot5",
					"Failed banning user!");
		}
    }
    
    public void unbanUser(String user) {
    	ArrayList<String> banList = getBanList();
		for (int i = 0; i < banList.size(); i++) {
			if (user.equalsIgnoreCase(banList.get(i))) {
				banList.remove(i);
			}
		}
		if (!saveBanList(banList)) {
			sendDirectMessage(gameConfig, "Copperpot5",
					"Failed banning user!");
		}
    }

	public void postSetlistScoresImage(String tweetMessage) {
		logger.info("winnerMap size: " + usersMap.size());
		logger.info(usersMap);
		if (!usersMap.isEmpty()) {
			GameComparator gameComparator = new GameComparator(usersMap);
		    TreeMap<String, Integer> sortedUsersMap =
		    		new TreeMap<String, Integer>(gameComparator);
			sortedUsersMap.putAll(usersMap);
			ArrayList<String> banList = getBanList();
			for (String user : usersMap.keySet()) {
				if (banList.contains(user.toLowerCase(Locale.getDefault()))) {
					sortedUsersMap.remove(user);
				}
			}
			Screenshot gameScreenshot = new TriviaScreenshot(setlistJpgFilename,
					fontFilename, "Top Scores", sortedUsersMap, 60, 30,
					10, verticalOffset);
			updateStatus(gameConfig, tweetMessage,
					new File(gameScreenshot.getOutputFilename()), -1);
		}
	}
	
	public void postSetlistScoresText(String message) {
		logger.info("winnerMap size: " + usersMap.size());
		logger.info(usersMap);
		if (!usersMap.isEmpty()) {
			GameComparator gameComparator = new GameComparator(usersMap);
		    TreeMap<String, Integer> sortedUsersMap =
		    		new TreeMap<String, Integer>(gameComparator);
			sortedUsersMap.putAll(usersMap);
			ArrayList<String> banList = getBanList();
			for (String user : usersMap.keySet()) {
				if (banList.contains(user.toLowerCase(Locale.getDefault()))) {
					sortedUsersMap.remove(user);
				}
			}
			StringBuilder sb = new StringBuilder();
			sb.append(message);
			String winner;
			Integer score;
			int count = 0;
			for (Entry<String, Integer> user : sortedUsersMap.entrySet()) {
				winner = user.getKey();
				score = user.getValue();
				count++;
				if ((sb.length() + 2 + Integer.toString(count).length() + 4 +
						winner.length() + 2 + score.toString().length() + 1) >
	    					140) {
	    			break;
				}
	    		sb.append("\n#");
	    		sb.append(count);
	    		sb.append(" - @");
	    		sb.append(winner);
	    		sb.append(" (");
	    		sb.append(score.toString());
	    		sb.append(")");
			}
			updateStatus(gameConfig, sb.toString(), null, -1);
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
	        if (base.get(a) < base.get(b)) {
	            return 1;
	        } else if (base.get(a) == base.get(b)) {
	        	logger.info(a + " : " + b);
	        	logger.info(a.compareToIgnoreCase(b));
	        	return a.compareToIgnoreCase(b);
	        } else {
	            return -1;
	        }
	    }
	}
    
	public ArrayList<String> getBanList() {
		ArrayList<String> banList;
		try {
			FileInputStream fileInputStream = new FileInputStream(banFile);
		    ObjectInputStream objectInputStream = new ObjectInputStream(
		    		fileInputStream);
		    banList = (ArrayList<String>) objectInputStream.readObject();
		    objectInputStream.close();
		    fileInputStream.close();
		} catch (FileNotFoundException e) {
			return new ArrayList<String>(0);
		} catch (Exception e) {
		    e.printStackTrace();
		    return null;
	    }
		return banList;
	}
	
	private boolean saveBanList(ArrayList<String> banList) {
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(banFile);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(
					fileOutputStream);
			objectOutputStream.writeObject(banList);
			objectOutputStream.close();
			fileOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}

