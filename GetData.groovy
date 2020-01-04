import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.net.URLEncoder;
import java.net.URLDecoder;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.ArrayList;
import org.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.sql.Timestamp;
import java.net.URL;
import org.apache.log4j.* ;
import org.apache.log4j.xml.DOMConfigurator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.*;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.json.JSONArray;
import org.json.JSONObject;
import groovy.sql.*;
import groovy.sql.Sql;
import groovy.transform.Field
@Field Logger logger = Logger.getLogger("${LOG}");
@Field String jobtxdate = "${LASTTXDATE}";
@Field String sitename = "${SITENAME}";
@Field DateFormat jobtxDf = new SimpleDateFormat("yyyy-MM-dd");
@Field DateFormat pbtimeDf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
@Field firstRuleString = ",";
// data replace regex tools
@Field endRuleString = "【延伸閱讀】,本新聞文字、照片、影片專供蘋果《好蘋友》壹會員閱覽，版權所有，禁止任何媒體、社群網站、論壇，在紙本或網路部分引用、改寫、轉貼分享，違者必究。,【相關話題】, 查看原始文章,立即加入三立新聞網Line官方帳號，給你最新焦點話題,點開加入自由電子報LINE官方帳號，新聞脈動隨時掌握！,點開加入自由電子報LINE官方帳號，新聞脈動隨時掌握！,加入「電影神搜」LINE好友，最新電影情報不漏接！,【更多壹週刊新聞】";
@Field replaceRuleString = "（([\u4e00-\u9fa5_a-zA-Z0-9].)*圖／(([\u4e00-\u9fa5_a-zA-Z0-9].*))）,(【[\u4e00-\u9fa5_a-zA-Z0-9]*.*╱)(([\u4e00-\u9fa5_a-zA-Z0-9]*)*報導】),（編輯：([\u4e00-\u9fa5_a-zA-Z0-9]*)）,（警告：以下有巨雷）,〔(記者[\u4e00-\u9fa5_a-zA-Z0-9].*)／([\u4e00-\u9fa5_a-zA-Z0-9].*)報導〕,記者[\u4e00-\u9fa5_a-zA-Z0-9].*／專訪,<延伸閱讀>,《電影預告》,（延伸閱讀：）,- 廣告 - 內文未完請往下捲動-,（撰文：([\u4e00-\u9fa5_a-zA-Z0-9]*.*)|攝影：([\u4e00-\u9fa5_a-zA-Z0-9]*.*)）,加入「[\u4e00-\u9fa5_a-zA-Z0-9]*.*」LINE好友，隨時掌握熱門電影動態！,立即加入「[\u4e00-\u9fa5_a-zA-Z0-9]*.*」，給你最新最潮的資訊。,加入「[\u4e00-\u9fa5_a-zA-Z0-9]*.*」LINE好友，最新電影情報不漏接！,（([\u4e00-\u9fa5_a-zA-Z0-9]*.*)報導）";

DOMConfigurator.configure("C:/log4j.xml");
try{
 viewData();
}catch(Exception e){
 logger.error(getStackTrace(e));
}

def viewData(){
	String artId = input.id;
	String url = input.url;
	String title = input.title;
	Timestamp publishdate = new Timestamp(pbtimeDf.parse(input.publishdate).getTime());

	try {
		getArticle(artId,url,title,publishdate)
		getComment(artId,url,title);
	}catch(Exception e) {
		logger.info("might get null JSON. url: "+url+" artId: "+artId+" title: "+title);
		logger.error(getStackTrace(e));
	}
}

def getComment(String artId, String url, String title){
	boolean flag = false;
	String comUrl = "https://api.today.line.me/webapi/comment/list?articleId="+artId+"&country=TW&direction=ASC&sort=TIME";
	String jss = getJString(comUrl);
	if(jss==null){
		return;
	}
	int base = 100000000;
	int totalComment = 0;
	JSONObject commentJSON;
	try {
		commentJSON = new JSONObject(jss);
		if(commentJSON.opt("code")!=200){
			return;
		}
	} catch (Exception e) {
		logger.info("transform commentJSON to JSON failed. url -> " +comUrl );
		logger.error(getStackTrace(e));
	}
	totalComment = commentJSON.opt("result").opt("comments").opt("count")
	if(!(totalComment < 100)){
		totalComment = (int)(totalComment/100)*100
	}else{
		totalComment = 0
	}

	for(int p = totalComment; p >= 0; p-=100){
		comUrl = "https://api.today.line.me/webapi/comment/list?articleId="+artId+"&country=TW&direction=ASC&sort=TIME"+"&pivot="+(base+p)+"&limit=100"
		jss = getJString(comUrl);
		if(jss==null){
			continue;
		}
		try {
			commentJSON = new JSONObject(jss);
			if(commentJSON.opt("code")!=200){
				return;
			}
		} catch (Exception e) {
			logger.info("transform commentJSON to JSON failed. url -> " +comUrl );
			logger.error(getStackTrace(e));
		}
		JSONArray commentList = commentJSON.opt("result").opt("comments").opt("comments");
		for(int c = commentList.length()-1  ; c >= 0; c--){
			JSONObject comment =  commentList.get(c);
			String comId = comment.opt("commentSn")
			int replyNum = comment.opt("ext").opt("replyCount")

			if(replyNum!=0){
				getReply(artId, comId,url,title,replyNum)
			}

			String comAuthor = comment.opt("displayName")
			String comContent = comment.opt("contents").opt(0).opt("extData").opt("content")
			int comInt1 = 0;
			int comInt2 = 0;
			if(comment.opt("ext").opt("likeCount").has("up")){
				comInt1 =  comment.opt("ext").opt("likeCount").opt("up")
			}
			if(comment.opt("ext").opt("likeCount").has("down")){
				comInt2 =  comment.opt("ext").opt("likeCount").opt("down")
			}
			Timestamp comTime = unixFm(comment.opt('createdDate'));
			if(checkTime(comTime)){
				HashMap output = save(comId+comUrl,sitename,artId,artId,url,title,comAuthor,comContent,comInt1,comInt2,comTime)
				outputList.add(output);
			}else{
				flag = true;
				break;
			}
		}

		if(flag){
			break;
		}
	}
}

def getReply(String artId, String comId, String url, String title, Integer replyNum){
	boolean flag = false;
	int base = 100000000;
	if(!(  replyNum < 100)){
		replyNum = (int)( replyNum/100)*100
	}else{
		replyNum = 0
	}

	for(int p = replyNum; p >= 0; p-=100){
		String replyUrl = "https://api.today.line.me/webapi/comment/list?articleId="+artId+"&country=TW&sort=TIME&parentCommentSn="+comId+"&pivot="+(base+replyNum)+"&direction=ASC&limit=100"
		String jss = getJString(replyUrl)
		if(jss==null)
			return;
		JSONObject replyJSON;
		try{
			replyJSON = new JSONObject(jss);
			if(replyJSON.opt("code")!=200){
				return;
			}
		} catch (Exception e) {
			logger.info("transform replyJSON to JSON failed. url -> " +replyUrl );
			logger.error(getStackTrace(e));
		}
		JSONArray replyList = replyJSON.opt("result").opt("comments").opt("comments");
		for(int r = replyList.length()-1  ; r >= 0; r--){
			JSONObject reply = replyList.get(r);
			String repId = reply.opt('commentSn')
			String repAuthor = reply.opt('displayName')
			String repContent = reply.opt('contents').opt(0).opt('extData').opt('content')
			Timestamp repTime = unixFm(reply.opt('createdDate'));
			if(checkTime(repTime)){
				HashMap output = save(repId+replyUrl,sitename,artId,comId,url,title,repAuthor,repContent,0,0,repTime);
				//outputList.add(output);

			}else{
				flag = true;
				break;
			}
		}
		if(flag){
			break;
		}
	}
}

def getArticle(String artId, String url, String title, Timestamp publishdate){
	if(!checkTime(publishdate)){
		return;
	}
	Document doc = getDoc(url);
	if(doc==null){
		return;
	}

	String author = doc.select(".publisher").text();
	content = getCleanedContent(doc)

	int int1 = 0;
	String jss = getJString("https://api.today.line.me/webapi/article/dinfo_v2?articleIds="+artId+"&country=TW")
	if(jss==null){
		return;
	}

	JSONObject postJSON;
	try {
		postJSON = new JSONObject(jss);
		if(postJSON.opt("code") != 200){
			return;
		}
		int1 = postJSON.opt("result").opt("likeViews").opt("count");
	} catch (Exception e) {
		logger.info("transform postJSON to JSON failed. url -> " +"https://api.today.line.me/webapi/article/dinfo_v2?articleIds="+id+"&country=TW" );
		logger.error(getStackTrace(e));
	}

	HashMap output = save(artId,sitename,artId,null,url,title,author,content,int1,0,publishdate);
	//outputList.add(output);
}

def getCleanedContent(Document input){
	String result;
	StringBuilder sb = new StringBuilder();
	List<String> removeList = new ArrayList<>()

	Elements contentTemp = input.select("article > p");
	for(Element el: contentTemp){
		List<Element> eList = new ArrayList<>()
		eList = el.getAllElements()
		for(i=1;i < eList.size();i++){
			if(eList.get(i).hasAttr("href")){
				removeList.add(eList.get(i).cssSelector())
			}
		}
	}
	for(String query: removeList){
		input.select(query).remove()
	}

	contentTemp = input.select("article > p")
	for(Element el: contentTemp){
		sb.append(el.text())
	}
	result = cleanArticleContent(sb.toString())
	return result;
}

def String cleanArticleContent(String content) {
	List<String> firstRule = new ArrayList<>();
	List<String> endRule = new ArrayList<>();
	List<String> replaceRule = new ArrayList<>();
	if(!firstRuleString.equals(""))
		firstRule = Arrays.asList(firstRuleString.split(","));
	if(!endRuleString.equals(""))
		endRule = Arrays.asList(endRuleString.split(","));
	if(!replaceRuleString.equals(""))
		replaceRule = Arrays.asList(replaceRuleString.split(","));


	for(String rule:firstRule){
		if(content.indexOf(rule)!=-1){
			content = content.substring(content.indexOf(rule)+rule.length(),content.length());
			break;
		}
	}
	for(String rule:endRule){
		if(content.indexOf(rule)!=-1){
			content = content.substring(0,content.indexOf(rule));
			break;
		}
	}

	for(String rule: replaceRule){
		if( Pattern.compile(rule).matcher(content).find()){
			content = content.replaceAll(rule,"")
		}
	}

	return content.trim();
}

def Timestamp unixFm(long input){
	Timestamp result = null;
	try{
		result = new Timestamp(input)
	}catch(Exception e){
		logger.info("failed to covert unix to Timestamp.")
		logger.error(getStackTrace(e));
	}
	return result;
}

def HashMap save(String postid, String site, String rid, String pid, String pageurl, String postTitle,String authorName, String content, Integer int1,Integer int2, Timestamp articleDate) {
	HashMap result = new HashMap();
	result.put("postid", MD5(postid));
	result.put("site", site);
	result.put("rid", MD5(rid));
	result.put("pid", MD5(pid));
	result.put("pageurl", urlDec(pageurl));
	result.put("postTitle", postTitle);
	result.put("authorName", authorName);
	result.put("content", content);
	result.put("int1", int1);
	result.put("int2",int2)
	result.put("articleDate", articleDate);

	return result;
}

def Date dateFormat(String input) {
	Date result = new Date();
	try {
		if (input.length() == 10) {
			result = jobtxDf.parse(input);
		}
	} catch (Exception e) {
		logger.error(getStackTrace(e));
	}
	return result;
}

def boolean checkTime(Timestamp input) {
	boolean result = false;
	if (input.getTime() - dateFormat(jobtxdate).getTime() > 0) {
		result = true;
	}
	return result;
}

def urlDec(String input){
	String result;
	try{
		result = URLDecoder.decode(input, "UTF-8");
	}catch(Exception e){
		logger.info("failed to decode url.")
		logger.error(getStackTrace(e));
	}
	return result;
}

def String MD5(String input) {
	if(input==null)
		return null;
	try {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(input.getBytes());
		String md5 = new BigInteger(1, md.digest()).toString(16);
		return fillMD5(md5);
	} catch (Exception e) {
		throw new RuntimeException("MD5 failed: " + e.getMessage(), e);
	}
}

def String fillMD5(String md5) {
	return md5.length() == 32 ? md5 : fillMD5("0" + md5);
}

def Document getDoc(String url){
	Document result = null;
	try{
		URL u = new URL(url);
		int status;
		if ("https".equalsIgnoreCase(u.getProtocol())) {
			ignoreSsl();
		}
		URLConnection conn = getConnection(u);
		status = ((HttpURLConnection) conn).getResponseCode();
		InputStream streamSource = null;
		if (status == 200) {
			streamSource = conn.getInputStream();
		} else {
			for (int i = 0; i < 3; i++) {
				Thread.sleep(5000);
				conn = getConnection(u);
				status = ((HttpURLConnection) conn).getResponseCode();
				if (status == 200) {
					streamSource = conn.getInputStream();
					break;
				} else {
					streamSource = ((HttpURLConnection) conn).getErrorStream();
				}
			}
		}

		if(status != 200){
			println "[Step 1]";
			println "Failed to get document.";
			println "Status: "+status+" Message: "+parseSrc(streamSource).text();
			return result;
		}

		result = Jsoup.parse(parseSrc(streamSource));
		return result;
	}catch(Exception e){
		println "Failed to get document.";
		e.printStackTrace() ;
	}
}

def String getJString(String url){
	String result = null;
	try{
		URL u = new URL(url);
		int status;
		if ("https".equalsIgnoreCase(u.getProtocol())) {
			ignoreSsl();
		}
		URLConnection conn = getConnection(u);
		status = ((HttpURLConnection) conn).getResponseCode();
		InputStream streamSource = null;
		if (status == 200) {
			streamSource = conn.getInputStream();
		} else {
			for (int i = 0; i < 3; i++) {
				Thread.sleep(5000);
				conn = getConnection(u);
				status = ((HttpURLConnection) conn).getResponseCode();
				if (status == 200) {
					streamSource = conn.getInputStream();
					break;
				} else {
					streamSource = ((HttpURLConnection) conn).getErrorStream();
				}
			}
		}

		if(status != 200){
			println "[Step 1]";
			println "Failed to get document.";
			println "Status: "+status+" Message: "+parseSrc(streamSource).text();
			return result;
		}

		result = parseSrc(streamSource);
		return result;
	}catch(Exception e){
		println "Failed to get document.";
		e.printstacktrace();
	}
}

def String parseSrc(InputStream input){
	String result = null;
	BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
	StringBuilder sb = new StringBuilder();
	String line = null;
	while ((line = reader.readLine()) != null) {
		sb.append(line + "\n");
	}
	reader.close();
	result = toUtf8(sb.toString())
	return result;
}

def URLConnection getConnection(URL u) throws Exception {
	URLConnection conn;
	conn = u.openConnection();
	conn.setRequestProperty("User-Agent", "Mozilla/5.0");
	conn.getHeaderFields();
	conn.setConnectTimeout(10000);
	conn.setReadTimeout(10000);
	return conn;
}

def void ignoreSsl() throws Exception {
	HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String urlHostName, SSLSession session) {
					System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
					return true;
				}
			};
	trustAllHttpsCertificates();
	HttpsURLConnection.setDefaultHostnameVerifier(hv);
}

def void trustAllHttpsCertificates() throws Exception {
	TrustManager[] trustAllCerts = new TrustManager[1];
	TrustManager tm = new miTM();
	trustAllCerts[0] = tm;
	SSLContext sc = SSLContext.getInstance("SSL");
	sc.init(null, trustAllCerts, null);
	HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
}

class miTM implements TrustManager, X509TrustManager {
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}

	public boolean isServerTrusted(X509Certificate[] certs) {
		return true;
	}

	public boolean isClientTrusted(X509Certificate[] certs) {
		return true;
	}

	public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		return;
	}

	public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		return;
	}
}

def String toUtf8(String str) throws Exception {
	str = str.replaceAll("\\u0000", "");
	return new String(str.getBytes("UTF-8"), "UTF-8");
}

def getStackTrace(e){
	StringWriter sw = new StringWriter();
	e.printStackTrace(new PrintWriter(sw));
	return sw.toString();
}