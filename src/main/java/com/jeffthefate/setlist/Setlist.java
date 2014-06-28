package com.jeffthefate.setlist;

import com.jeffthefate.Screenshot;
import com.jeffthefate.SetlistScreenshot;
import com.jeffthefate.TriviaScreenshot;
import com.jeffthefate.utils.*;
import com.jeffthefate.utils.json.JsonUtil;
import com.jeffthefate.utils.json.SetlistResults;
import com.jeffthefate.utils.json.Venue;
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

    private final String PARSE_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private final String SETLIST_DATE_FORMAT = "MMM dd yyyy";
    
    private final String FINAL_SCORES_TEXT = "[Final Scores]";
    private final String CURRENT_SCORES_TEXT = "[Current Scores]";
    
    private String lastSong = "";
    
    private String setlistText = "";
    
    private String currDateString = null;
    
    private Screenshot screenshot;

    private String url;
    private int durationHours = 5;
    private long duration = durationHours * 60 * 60 * 1000;
    
    private boolean isDev;
    
    private String setlistJpgFilename;
    private String fontFilename;
    private int fontSize;
    private int topOffset;
    private int bottomOffset;
    private String setlistFilename;
    private String lastSongFilename;
    private String setlistDir;
    private String banFile;
    
    private Configuration setlistConfig;
    private Configuration gameConfig;
    
    private String currAccount;
    
    public LinkedHashMap<String, String> answers =
            new LinkedHashMap<String, String>();
    
    private ArrayList<ArrayList<String>> nameList =
    		new ArrayList<ArrayList<String>>(0);

    private boolean kill = false;
    
    private ArrayList<String> locList = new ArrayList<String>();
    private ArrayList<String> setList = new ArrayList<String>();
    private ArrayList<String> noteList = new ArrayList<String>();
    private TreeMap<Integer, String> noteMap = new TreeMap<Integer, String>();
    
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
    
    private Logger logger = Logger.getLogger(Setlist.class);

    private String setlistImageName;
    private String scoresImageName;

    private Parse parse;

    public Setlist(String url, boolean isDev,
    		Configuration setlistConfig, Configuration gameConfig,
    		String setlistJpgFilename, String fontFilename, int fontSize,
    		int topOffset, int bottomOffset, String setlistFilename,
            String lastSongFilename, String setlistDir, String banFile,
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
    	this.topOffset = topOffset;
        this.bottomOffset = bottomOffset;
    	this.setlistFilename = setlistFilename;
    	this.lastSongFilename = lastSongFilename;
    	this.setlistDir = setlistDir;
    	this.banFile = banFile;
    	this.nameList = nameList;
    	this.symbolList = symbolList;
    	this.currAccount = currAccount;
        this.setlistImageName = setlistImageName;
        this.scoresImageName = scoresImageName;
        parse = new Parse(appId, restKey);
    }

    public void setUrl(String url) {
        this.url = url;
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
    				setlistText, fontSize, topOffset, bottomOffset,
                    setlistImageName);
            twitterUtil.updateStatus(setlistConfig, finalTweetText,
                    new File(screenshot.getOutputFilename()), -1);
    		postSetlistScoresImage(true);
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
    public String stripSpecialCharacters(String song) {
        song = StringUtils.remove(song, "(");
        song = StringUtils.remove(song, ")");
        song = StringUtils.remove(song, "->");
        song = StringUtils.remove(song, "*");
        song = StringUtils.remove(song, "+");
        song = StringUtils.remove(song, "~");
        song = StringUtils.remove(song, "�");
        song = StringUtils.remove(song, "Ä");
        song = StringUtils.remove(song, "5||");
        song = StringUtils.trim(song);
        return song;
    }

    public String liveSetlist(String url) {
        final String SETLIST_STYLE = "font-family:sans-serif;font-size:14;"
                + "font-weight:normal;margin-top:15px;margin-left:15px;";
        final String LOC_STYLE = "padding-bottom:12px;padding-left:3px;" +
                "color:#3995aa;";
        final String SONG_STYLE = "Color:#000000;Position:Absolute;Top:";
        Document doc = getPageDocument(url);
        char badChar = 65533;
        char apos = 39;
        char endChar = 160;
        boolean hasEncore = false;
        boolean hasSegue = false;
        boolean firstBreak = false;
        boolean secondBreak = false;
        boolean hasGuest = false;
        boolean firstPartial = false;
        boolean lastPartial = false;
        String divStyle;
        String divTemp;
        int divStyleLocation = -1;
        String oldNote;
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
                    SETLIST_STYLE).first().childNodes()) {
                // Location and parent setlist node
                if (node.nodeName().equals("div")) {
                    divStyle = node.attr("style");
                    // Location node
                    if (divStyle.equals(LOC_STYLE)) {
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
                                if (divStyle.startsWith(SONG_STYLE)) {
                                    String[] locations = divStyle.split(
                                            SONG_STYLE);
                                    currentLoc = Integer.parseInt(
                                            locations[1].split(";")[0]);
                                    divText = div.ownText();
                                    divText = StringUtils.remove(divText,
                                            endChar);
                                    String[] songs = divText.split(
                                            "\\d+[\\.]");
                                    if (songs.length > 1) {
                                        currSong = StringUtils.replaceChars(
                                                songs[1], badChar, apos);
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
                    String[] songs = divText.split("\\d+[\\.]");
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
        final String TWEET_DATE_FORMAT = "MM/dd/yy";
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
        currDateString = convertDateFormat(SETLIST_DATE_FORMAT,
                PARSE_DATE_FORMAT, locList.get(0));
        StringBuilder sb = new StringBuilder();
        if (locList.size() < 4) {
        	locList.add(1, "Dave Matthews Band");
        }
		sb.append("[Final] Dave Matthews Band Setlist for ");
		sb.append(convertDateFormat(SETLIST_DATE_FORMAT, TWEET_DATE_FORMAT,
                locList.get(0)));
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
		String noteChar;
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
            Venue venue = jsonUtil.getVenue(parse.getObject("Venue",
                    getVenueId()));
            setVenueCity(venue.getCity());
            setVenueName(venue.getName());
            String lastSongFromFile = fileUtil.readStringFromFile(lastSongFile);
            if (newDate == 0 || (newDate == -1 &&
            		!lastSongFromFile.equals(lastSong))) {
            	if (!stripSpecialCharacters(lastSongFromFile).equals(
            			stripSpecialCharacters(lastSong))) {
            		logger.info("POST NOTIFICATION AND TWEET: " +
            				lastSong);
            		String gameMessage = "";
            		if (!isDev) {
            			postNotification(createPushJsonString(lastSong,
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
                                && !lastSong.toLowerCase().contains("improv")
                                && !lastSong.toLowerCase().contains(
                                    "show begins")) {
	                    	gameMessage = createWinnersMessage(lastSong,
                                    sb.toString());
                			noteSong = lastSong;
	                    }
	                }
	                screenshot = new SetlistScreenshot(
		    				setlistJpgFilename, fontFilename, setlistText,
		    				fontSize, topOffset, bottomOffset,
                            setlistImageName);
	                tweetSong(sb.toString(), gameMessage,
                            new File(screenshot.getOutputFilename()), -1, true);
            	}
            	else {
            		logger.info("POST NOTIFICATION: BLANK");
            		if (!isDev) {
            			postNotification(createPushJsonString("", setlistText,
                                getExpireDateString()));
            		}
            	}
                fileUtil.writeStringToFile(lastSong, lastSongFile);
            }
            else if (fileUtil.readStringFromFile(lastSongFile)
                    .equals(lastSong)) {
            	logger.info("POST NOTIFICATION: BLANK");
            	if (!isDev) {
            		postNotification(createPushJsonString("", setlistText,
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
    /**************************************************************************/
    /*                              Date Helpers                              */
    /**************************************************************************/
    public Date convertStringToDate(String format, String dateString) {
        if (format == null || dateString == null) {
            return null;
        }
    	SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    	Date date = null;
    	try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e2) {
            logger.info("Failed to parse date from " + dateString);
            e2.printStackTrace();
        }
    	return date;
    }

    private String convertDateFormat(String fromFormat, String toFormat,
            String dateString) {
        Date date = convertStringToDate(fromFormat, dateString);
        SimpleDateFormat dateFormat = new SimpleDateFormat(toFormat);
        return dateFormat.format(date.getTime());
    }

    private String getExpireDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(PARSE_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        date.setTime(System.currentTimeMillis() + 300000); // 5 minutes
        return dateFormat.format(date.getTime());
    }
    /**************************************************************************/
    /*                              Parse Helpers                             */
    /**************************************************************************/
    private String getSetlist(String dateString) {
        logger.info("getSetlist dateString: " + dateString);
        if (dateString == null) {
            return null;
        }
        String response;
        try {
            response = parse.get("Setlist", URLEncoder.encode
                    ("where={\"setDate\":{\"__type\":\"Date\",\"iso\":\"" +
                                    dateString + "\"}}", "US-ASCII"));
            return response;
        } catch (UnsupportedEncodingException e) {
            logger.error("Bad data!");
            return null;
        }
    }
    
    private String postSetlist(String json) {
        String response = parse.post("Setlist", json);
        return jsonUtil.getCreated(response).getObjectId();
    }
    
    private boolean putSetlist(String objectId, String json) {
    	logger.info("putSetlist: " + objectId + " : " + json);
        String response = parse.put("Setlist", objectId, json);
        return response != null;
    }
    
    private boolean postNotification(String json) {
        String response = parse.postPush(json);
        return response != null;
    }
    
    private String createSetlistJsonString(String latestSetlist,
            String venueId) {
        currDateString = convertDateFormat(SETLIST_DATE_FORMAT,
                PARSE_DATE_FORMAT, locList.get(0));
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
    
    private String createPushJsonString(String latestSong, String setlist,
            String expireDateString) {
        final String SHORT_DATE_FORMAT = "yyyy-MM-dd";
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dataNode = factory.objectNode();
        ObjectNode whereNode = factory.objectNode();
        whereNode.put("deviceType", "android");
        //whereNode.put("appVersion", "2.0.2");
        dataNode.put("action", "com.jeffthefate.dmb.ACTION_NEW_SONG");
        dataNode.put("song", latestSong);
        dataNode.put("setlist", setlist);
        dataNode.put("shortDate", convertDateFormat(SETLIST_DATE_FORMAT,
                SHORT_DATE_FORMAT, locList.get(0)));
        dataNode.put("venueName", getVenueName());
        dataNode.put("venueCity", getVenueCity());
        dataNode.put("timestamp", Long.toString(System.currentTimeMillis()));
        rootNode.put("where", whereNode);
        rootNode.put("expiration_time", expireDateString);
        rootNode.put("data", dataNode);
        return rootNode.toString();
    }
    
    private String createVenueJson() {
    	String venueName = locList.get(2);
		String venueCity = locList.get(3);
		JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
		ObjectNode rootNode = jsonNodeFactory.objectNode();
		rootNode.put("name", venueName);
		rootNode.put("city", venueCity);
		return rootNode.toString();
    }
    
    private String getResponse(String className, int limit,
    		String where) {
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
                logger.error("Unable to encode where clause!");
                e.printStackTrace();
                return null;
            }
        }
        return parse.get(className, sb.toString());
    }
    
    private int uploadLatest(String latestSetlist) {
        String dateString = convertDateFormat(SETLIST_DATE_FORMAT,
                PARSE_DATE_FORMAT, locList.get(0));
        String getSetlistResponse = getSetlist(latestSetlist);
        if (getSetlistResponse == null) {
            logger.info("Fetch setlist from Parse failed!");
            logger.info(latestSetlist);
            return -1;
        }
        String venueJson = createVenueJson();
        SetlistResults setlistResults = jsonUtil.getSetlistResults(
                getSetlistResponse);
        if (setlistResults.getResults().isEmpty()) {
            Venue venue = jsonUtil.getVenue(getResponse("Venue", 1,
                    venueJson));
            String venueId = venue.getObjectId();
        	if (venueId == null) {
                venue = jsonUtil.getVenue(parse.post("Venue", venueJson));
                venueId = venue.getObjectId();
        	}
        	if (!isDev) {
        		postSetlist(createSetlistJsonString(latestSetlist, venueId));
        	}
            File dir = new File(setlistDir);
            String[] files = dir.list(new FilenameFilter() {
            	public boolean accept(File dir, String filename) {
            		return filename.endsWith(".txt");
        		}
        	});
            Date newDate = convertStringToDate(PARSE_DATE_FORMAT, dateString);
            for (String file : files) {
            	if (file.startsWith("setlist")) {
            		if (convertStringToDate(PARSE_DATE_FORMAT,
            				file.substring(7)).after(newDate)) {
            			logger.info("newer setlist file found!");
            			return 1;
            		}
            	}
            }
            setVenueId(venueId);
            return 0;
        }
        else if (setlistResults.getResults().size() > 1) {
            logger.error("More than one row returned for setlist " +
                    dateString);
            return -1;
        }
        else {
            com.jeffthefate.utils.json.Setlist setlist = setlistResults
                    .getResults().get(0);
        	String venueId = setlist.getVenue().getObjectId();
        	logger.info("VenueId: " + venueId);
        	if (venueId == null) {
                Venue venue = jsonUtil.getVenue(getResponse("Venue", 1,
                        venueJson));
        		venueId = venue.getObjectId();
        		logger.info("VenueId: " + venueId);
        		if (venueId == null) {
                    venue = jsonUtil.getVenue(parse.post("Venue", venueJson));
    				venueId = venue.getObjectId();
    				logger.info("VenueId: " + venueId);
            	}
        	}
            putSetlist(setlist.getObjectId(), createSetlistJsonString(
                    latestSetlist, venueId));
            setVenueId(venueId);
            return -1;
        }
    }
    /**************************************************************************/
    /*                             Twitter Helpers                            */
    /**************************************************************************/
    public Status tweetSong(String setlistMessage, String gameMessage,
                            File file, long replyTo, boolean postGame) {
    	logger.info("Tweet text: " + setlistMessage);
    	logger.info("Tweet length: " + setlistMessage.length());
    	Status status = twitterUtil.updateStatus(setlistConfig, setlistMessage, file,
                replyTo);
    	if (status == null) {
    		return null;
    	}
    	if (postGame && !setlistMessage.toLowerCase(
				Locale.getDefault()).contains("[Encore:]".toLowerCase(
						Locale.getDefault())) &&
                !setlistMessage.toLowerCase(Locale.getDefault()).contains(
                        "[Set Break]".toLowerCase(Locale.getDefault())) &&
                !setlistMessage.toLowerCase(Locale.getDefault()).contains(
                        "Improv".toLowerCase(Locale.getDefault())) &&
                !setlistMessage.toLowerCase(Locale.getDefault()).contains(
                        "Show begins".toLowerCase(Locale.getDefault()))) {
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
    /**************************************************************************/
    /*                              Game Helpers                              */
    /**************************************************************************/
    public String createWinnersMessage(String lastSong, String songMessage) {
        final String CORRECT_ANSWERS_TEXT = "\nCorrect guesses:";
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

    public void banUser(String user) {
    	List<String> banList = fileUtil.readFromFile(banFile);
    	if (!banList.contains(user)) {
    		banList.add(user);
    	}
    	if (!fileUtil.saveToFile(banFile, banList)) {
			twitterUtil.sendDirectMessage(gameConfig, "Copperpot5",
                    "Failed banning user: " + user);
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
                    "Failed unbanning user: " + user);
		}
    }

	public void postSetlistScoresImage(boolean isFinal) {
		if (!usersMap.isEmpty()) {
            TreeMap<String, Integer> sortedUsersMap = sortUsersMap();
			TriviaScreenshot gameScreenshot = new TriviaScreenshot(
                    setlistJpgFilename, fontFilename, "Top Scores",
                    sortedUsersMap, 60, 30, 10, topOffset, bottomOffset,
                    scoresImageName);
            twitterUtil.updateStatus(gameConfig, isFinal ? FINAL_SCORES_TEXT :
                            CURRENT_SCORES_TEXT,
                    new File(gameScreenshot.getOutputFilename()), -1);
		}
	}
	
	public void postSetlistScoresText(boolean isFinal) {
		if (!usersMap.isEmpty()) {
            List<String> winners = new ArrayList<String>(usersMap.keySet());
            String message = createPlayersMessage(winners, usersMap,
                    isFinal ? FINAL_SCORES_TEXT : CURRENT_SCORES_TEXT, "");
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