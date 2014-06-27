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
import com.jeffthefate.utils.*;
import com.jeffthefate.utils.json.JsonUtil;
import com.jeffthefate.utils.json.Play;
import com.jeffthefate.utils.json.Song;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
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
import twitter4j.Status;
import twitter4j.conf.Configuration;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class Setlist {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String TWEET_DATE_FORMAT = "MM/dd/yy";
    
    public final String FINAL_SCORES = "[Final Scores]";
    public final String CURRENT_SCORES = "[Current Scores]";
    private static final String CORRECT_ANSWERS_TEXT = "\nCorrect guesses:";
    
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
    private String appId;
    private String restKey;
    
    public LinkedHashMap<String, String> answers = new LinkedHashMap<String, String>();
    
    private ArrayList<ArrayList<String>> nameList =
    		new ArrayList<ArrayList<String>>(0);

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

    private FileUtil fileUtil = FileUtil.instance();
    private GameUtil gameUtil = GameUtil.instance();
    private TwitterUtil twitterUtil = TwitterUtil.instance();
    private JsonUtil jsonUtil = JsonUtil.instance();
    
    private static Logger logger = Logger.getLogger(Setlist.class);

    public void setUrl(String url) {
        this.url = url;
    }

    private String setlistImageName;
    private String scoresImageName;

    // TODO Pass in the app ID and REST API key?
    public Setlist(String url, boolean isDev,
    		Configuration setlistConfig, Configuration gameConfig,
    		String setlistJpgFilename, String fontFilename, int fontSize,
    		int verticalOffset, String setlistFilename, String lastSongFilename,
    		String setlistDir, String banFile,
    		ArrayList<ArrayList<String>> nameList,
    		ArrayList<String> symbolList, String currAccount, String appId,
            String restKey, String setlistImageName, String scoresImageName) {
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
        this.appId = appId;
        this.restKey = restKey;
        this.setlistImageName = setlistImageName;
        this.scoresImageName = scoresImageName;
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

    public HashMap<String, Integer> getUsersMap() { return usersMap; }

    /**************************************************************************/
    /*                                Startup                                 */
    /**************************************************************************/
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
    				setlistText, fontSize, verticalOffset, setlistImageName);
            twitterUtil.updateStatus(setlistConfig, finalTweetText,
                    new File(screenshot.getOutputFilename()), -1);
    		postSetlistScoresImage(FINAL_SCORES);
    	}
    	inSetlist = false;
    }

    /**************************************************************************/
    /*                              DOM Fetching                              */
    /**************************************************************************/
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

    public Document getPageDocument(String url) {
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
                    fileUtil.readStringFromFile(url)));
        }
    }

    /**************************************************************************/
    /*                           Setlist Crunching                            */
    /**************************************************************************/
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
        String divStyle;
        String divTemp;
        int divStyleLocation = -1;
        String oldNote;
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
        String lastSetlist = fileUtil.readStringFromFile(setlistFile);
        logger.info("lastSetlist:");
        logger.info(lastSetlist);
        String diff = StringUtils.difference(lastSetlist, setlistText);
        logger.info("diff:");
        logger.info(diff);
        boolean hasChange = !StringUtils.isBlank(diff);
        sb.setLength(0);
        if (hasChange) {
            fileUtil.writeStringToFile(setlistText, setlistFile);
            // -1 if failure or not a new setlist
            // 0 if a new setlist (latest)
            // 1 if there is a newer date available already
            int newDate = uploadLatest(setlistText);
            getVenueFromResponse(getResponse("Venue", getVenueId()));
            String lastSongFromFile = fileUtil.readStringFromFile(lastSongFile);
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
	                    		!lastSong.toLowerCase().contains("set break")
                                && !lastSong.toLowerCase().contains("improv")) {
	                    	gameMessage = createWinnersMessage(lastSong, sb.toString());
                			noteSong = lastSong;
	                    }
	                }
	                screenshot = new SetlistScreenshot(
		    				setlistJpgFilename, fontFilename, setlistText,
		    				fontSize, verticalOffset, setlistImageName);
	                tweetSong(sb.toString(), gameMessage,
                            new File(screenshot.getOutputFilename()), -1, true);
            	}
            	else {
            		logger.info("POST NOTIFICATION: BLANK");
            		if (!isDev) {
            			postNotification(getPushJsonString("", setlistText,
            					getExpireDateString()));
            		}
            	}
                fileUtil.writeStringToFile(lastSong, lastSongFile);
            }
            else if (fileUtil.readStringFromFile(lastSongFile)
                    .equals(lastSong)) {
            	logger.info("POST NOTIFICATION: BLANK");
            	if (!isDev) {
            		postNotification(getPushJsonString("", setlistText,
            				getExpireDateString()));
            		String updateText = StringUtils.strip(diff);
            		if (updateText.length() <= 140) {
                        twitterUtil.updateStatus(setlistConfig,
                                StringUtils.strip(diff),
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
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response;
        try {
            response = parse.get("Song", URLEncoder.encode("where={\"setDate\":"
                    + "{\"__type\":\"Date\",\"iso\":\"" + dateString + "\"}}",
                    "US-ASCII"));
            return response;
        } catch (UnsupportedEncodingException e) {
            logger.error("Bad data!");
            return null;
        }
    }
    
    private String postSetlist(String json) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.post("Setlist", json);
        return jsonUtil.getCreated(response).getObjectId();
    }
    
    private boolean putSetlist(String objectId, String json) {
    	logger.info("putSetlist: " + objectId + " : " + json);
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.put("Setlist", objectId, json);
        return response != null;
    }
    
    private boolean addPlay(String setlistId, String playId) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.put("Setlist", setlistId,
                getAddPlayJsonString(playId));
        return response != null;
    }
    
    private String getSong(String latestSong) {
    	logger.info("getSong: " + latestSong);
    	if (latestSong == null) {
            return null;
        }
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response;
        try {
            response = parse.get("Song", URLEncoder.encode("where={\"title\":\""
                    + latestSong + "\"}", "US-ASCII"));
            return response;
        } catch (UnsupportedEncodingException e) {
            logger.error("Bad data!");
            return null;
        }
    }
    
    private boolean getPlay(String setlistId, Integer slot) {
        if (setlistId == null) {
            return true;
        }
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response;
        try {
            response = parse.get("Play", URLEncoder.encode(
                    "where={\"$relatedTo\":{\"object\":{\"__type\":\"Pointer" +
                    "\",\"className\":\"Setlist\",\"objectId\":\"" + setlistId +
                    "\"},\"key\":\"plays\"}}", "US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            logger.error("Bad data!");
            e.printStackTrace();
            return true;
        }
        int tempSlot = getLargestSlotFromResponse(response);
        logger.info("getPlay slot: " + slot);
        logger.info("getPlay tempSlot: " + tempSlot);
        if (slot > tempSlot) {
            return false;
        }
        return true;
    }
    
    private boolean postNotification(String json) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.postPush(json);
        return response != null;
    }
    
    private String postSong(String json) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        return parse.post("Song", json);
    }
    
    private boolean putSong(String objectId, String json) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.put("Song", objectId, json);
        return response != null;
    }
    
    private String postPlay(String json) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.post("Play", json);
        return getSimpleObjectIdFromCreate(response);
    }
    
    private boolean putPlay(String objectId, String json) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.put("Play", objectId, json);
        return response != null;
    }
    
    private boolean putSetSong(String objectId, String json) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        String response = parse.put("Song", objectId, json);
        return response != null;
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
        String fieldName;
        String objectId = null;
        JsonToken token;
        boolean insideObject = false;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldName = jp.getCurrentName();
            if ("results".equals(fieldName)) { // contains an object
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
	                    fieldName = jp.getCurrentName();
	                    if ("objectId".equals(fieldName) && !insideObject) {
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
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        return parse.getObject(className, objectId);
    }
    
    private static String getResponse(String className, int limit,
    		String where) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        StringBuilder sb = new StringBuilder();
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
        return parse.get(className, sb.toString());
    }
    
    private static String postObject(String className, String postString) {
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        return parse.post(className, postString);
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
    	if (dateString == null) {
            return null;
        }
        String responseString = null;
        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            responseString = parse.get("Setlist", URLEncoder.encode(
                    "where={\"setDate\":{\"__type\":\"Date\",\"iso\":\"" +
                            dateString + "\"}}", "US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to encode json!");
            e.printStackTrace();
        }
        List<com.jeffthefate.utils.json.Setlist> setlists = jsonUtil
                .getSetlistResults(responseString).getResults();
        for (com.jeffthefate.utils.json.Setlist setlist : setlists) {
            if (setlist.getSetDate().getIso().equals(dateString)) {
                return setlist.getObjectId();
            }
        }
        return postSetlist(getNewSetlistJsonString(dateString));
    }
    
    private boolean uploadPlay(String songId, Integer slot,
    		String setlistId, boolean isOpener, boolean isSetCloser,
    		boolean isEncoreCloser) {
    	// Check if set has this many plays
        boolean hasPlay = getPlay(setlistId, slot);
        if (!hasPlay) {
        	// Check if there is already an encore closer
        	// If so, change that play to false, make this one true
        	if (isEncoreCloser) {
                resetEncoreCloser(setlistId);
            }
            String playId = postPlay(getPlayJsonString(setlistId, slot, songId,
            		isOpener, isSetCloser, isEncoreCloser));
            addPlay(setlistId, playId);
            return true;
        }
        return false;
    }
    
    private String resetEncoreCloser(String setlistId) {
        String responseString;
        String closerId = null;

        Parse parse = new Parse("ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R",
                "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            responseString = parse.get("Play", URLEncoder.encode(
                    "where={\"$relatedTo\":{\"object\":{\"__type\":\"Pointer\","
                    + "\"className\":\"Setlist\"," + "\"objectId\":\"" +
                    setlistId + "\"},\"key\":\"plays\"}}", "US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to encode GET query!");
            e.printStackTrace();
            return null;
        }
        List<Play> plays = jsonUtil.getPlayResults(responseString).getResults();
        for (Play play : plays) {
            if (play.isEncoreCloser()) {
                closerId = play.getObjectId();
                break;
            }
        }
        if (closerId == null) {
            return null;
        }
        String json = "{\"encoreCloser\":false}";
        return parse.put("Play", closerId, json);
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
        // TODO What happens if there is more than one?
        String objectId;
        List<Song> songResults = jsonUtil.getSongResults(getResponse)
                .getResults();
        if (songResults.isEmpty()) {
            objectId = jsonUtil.getSong(postSong(getSongJsonString(
                    latestSong))).getObjectId();
            if (objectId == null) {
                return false;
            }
            else {
                // Song exists, add play
                uploadPlay(objectId, slot, setlistId, isOpener, isSetCloser,
                        isEncoreCloser);
            }
        }
        return true;
    }
    
    public Status tweetSong(String setlistMessage, String gameMessage,
                            File file, long replyTo, boolean postGame) {
    	logger.info("Tweet text: " + setlistMessage);
    	logger.info("Tweet length: " + setlistMessage.length());
    	Status status = twitterUtil.updateStatus(setlistConfig, setlistMessage, file,
                replyTo);
    	if (status == null) {
    		return status;
    	}
    	if (postGame && !setlistMessage.toLowerCase(
				Locale.getDefault()).contains("[Encore:]".toLowerCase(
						Locale.getDefault())) &&
                !setlistMessage.toLowerCase(Locale.getDefault()).contains(
                        "[Set Break]".toLowerCase(Locale.getDefault())) &&
                !setlistMessage.toLowerCase(Locale.getDefault()).contains(
                        "improv".toLowerCase(Locale.getDefault()))) {
    		if (!setlistMessage.toLowerCase(Locale.getDefault()).contains(
					"[Final".toLowerCase(Locale.getDefault()))) {
    			status = twitterUtil.updateStatus(gameConfig,
                        setlistMessage.concat(gameMessage), null, -1);
			}
			else {
				return null;
			}
    	}
		return status;
    }
    
    /*
     * Current #DMB Song & Setlist: [Rooftop ->]
     * Correct guesses: 
	 *	#1 - @Copperpot5 (Streak?) 
	 *	#2 - @jeffthefate
	 *	#3 -
     */
    public String createWinnersMessage(String lastSong, String songMessage) {
    	List<String> winners = findWinners(lastSong);
    	answers.clear();
    	answers = new LinkedHashMap<String, String>();
        return createPlayersMessage(winners, usersMap, songMessage,
                CORRECT_ANSWERS_TEXT);
    }

    public void addAnswer(String userName, String message) {
        logger.info("Adding " + userName + " : " + message);
    	answers.put(userName, gameUtil.massageResponse(message));
    }
    
    public void processTweet(Status status) {
    	logger.info("inSetlist: " + inSetlist);
    	if (!inSetlist) {
    		return;
    	}

		String userName = status.getUser().getScreenName();

		if (status.getInReplyToScreenName().equalsIgnoreCase(currAccount)) {
            addAnswer(userName, status.getText());
		}
	}

    public void banUser(String user) {
    	List<String> banList = fileUtil.readFromFile(banFile);
    	if (!banList.contains(user)) {
    		banList.add(user);
    	}
    	if (!fileUtil.saveToFile(banFile, banList)) {
			twitterUtil.sendDirectMessage(gameConfig, "Copperpot5",
                    "Failed banning user!");
		}
    }
    
    public void unbanUser(String user) {
    	List<String> banList = fileUtil.readFromFile(banFile);
		for (int i = 0; i < banList.size(); i++) {
			if (user.equalsIgnoreCase(banList.get(i))) {
				banList.remove(i);
			}
		}
		if (!fileUtil.saveToFile(banFile, banList)) {
            twitterUtil.sendDirectMessage(gameConfig, "Copperpot5",
                    "Failed banning user!");
		}
    }

	public void postSetlistScoresImage(String tweetMessage) {
		if (!usersMap.isEmpty()) {
            TreeMap<String, Integer> sortedUsersMap = sortUsersMap();
			TriviaScreenshot gameScreenshot = new TriviaScreenshot(
                    setlistJpgFilename, fontFilename, "Top Scores",
                    sortedUsersMap, 60, 30, 10, verticalOffset, scoresImageName);
            twitterUtil.updateStatus(gameConfig, tweetMessage,
                    new File(gameScreenshot.getOutputFilename()), -1);
		}
	}
	
	public void postSetlistScoresText(String tweetMessage) {
		if (!usersMap.isEmpty()) {
            List<String> winners = new ArrayList<String>(usersMap.keySet());
            String message = createPlayersMessage(winners, usersMap,
                    tweetMessage, "");
            twitterUtil.updateStatus(gameConfig, message, null, -1);
		}
	}

    public TreeMap<String, Integer> sortUsersMap() {
        GameComparator gameComparator = new GameComparator(usersMap);
        TreeMap<String, Integer> sortedUsersMap =
                new TreeMap<String, Integer>(gameComparator);
        sortedUsersMap.putAll(usersMap);
        List<String> banList = fileUtil.readFromFile(banFile);
        for (String user : usersMap.keySet()) {
            if (banList.contains(user.toLowerCase(Locale.getDefault()))) {
                sortedUsersMap.remove(user);
            }
        }
        return sortedUsersMap;
    }

    public String createPlayersMessage(List<String> winners,
            HashMap<String, Integer> usersMap, String songMessage,
            String correctText) {
        String message = "";
        if (!winners.isEmpty() && (songMessage.length() +
                correctText.length()-1 + winners.get(0).length() + 10 +
                usersMap.get(winners.get(0)).toString().length())
                <= 140) {
            StringBuilder sb = new StringBuilder();
            sb.append(correctText);
            int count = 0;
            for (String winner : winners) {
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
            message = sb.toString();
        }
        if (winners.isEmpty()) {
            message = "\nNot Guessed";
        }
        if (message.length() > 140) {
            message = message.substring(0, 140);
        }
        return message;
    }

    public List<String> findWinners(String lastSong) {
        List<String> winners = new ArrayList<String>(0);

        lastSong = gameUtil.massageAnswer(lastSong);

        boolean answerMatches;
        boolean responseMatches;
        boolean isCorrect;

        List<String> banList = fileUtil.readFromFile(banFile);

        for (Entry<String, String> answer : answers.entrySet()) {
            if (banList.contains(answer.getKey().toLowerCase(
                    Locale.getDefault()))) {
                continue;
            }
            logger.info("Checking " + answer.getValue());
            isCorrect = false;
            if (gameUtil.checkAnswer(lastSong, answer.getValue())) {
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
                                gameUtil.checkAnswer(name, lastSong)) {
                            answerMatches = true;
                        }
                        if (answer.getValue().contains(name) ||
                                gameUtil.checkAnswer(name, answer.getValue())) {
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
        return winners;
    }

}