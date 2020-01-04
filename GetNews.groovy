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
import groovy.sql.*;
import groovy.sql.Sql;
import groovy.transform.Field
@Field Logger logger = Logger.getLogger("LINE TODAY");
@Field String site = "LINE-TODAY-ENTERTAIN";

DOMConfigurator.configure("C:/log4j.xml");
try{
    String srcUrl = "https://today.line.me/TW/pc/main/100260";	// board-url of LINE TODAY
    getPost(srcUrl);
}catch(Exception e){
    logger.error(getStackTrace(e));
}

def getPost(String srcUrl){
    Document doc = getDoc(srcUrl);
    if(doc == null)
        return;
    Elements docLeft = doc.select("#left_area");
    if(docLeft == null)
        return;
    leftBoard(docLeft);
    popularBoard(srcUrl);

}

def popularBoard(String srcUrl){
    srcUrl = srcUrl.replace("main","popular");
    Document doc = getDoc(srcUrl);
    if(doc == null)
        return;
    Elements popular = doc.select(".list-type-rk > li")
    String id;
    String url;
    String title;
    Timestamp pbDate;
    for(Element post: popular.select("a")){
        id = post.attr("data-articleid");
        url = post.attr("href");
        title = post.attr("title");
        pbDate = unixFm(post.attr("data-pbt"));
        HashMap output = save(id,site,url,title,pbDate)
        // insert news-url into db
		//outputList.add(output);
    }

}

def leftBoard(Elements docLeft){
    String id;
    String url;
    String title;
    Timestamp pbDate;
    for(Element leftPost: docLeft.select("div")){
        // crawl digest-module
        if(leftPost.attr("class").equals("digest-module")){
            Elements postInfo = leftPost.select(".section-hl > a");
            id =postInfo.attr("data-articleid");
            url = postInfo.attr("href");
            title =postInfo.attr("title");
            pbDate  = unixFm(postInfo.attr("data-pbt"));
            HashMap output = save(id,site,url,title,pbDate)
            outputList.add(output);

            postInfo = leftPost.select(".list-type-1 > li > a");
            for(Element listPost: postInfo){
                id = listPost.attr("data-articleid");
                url = listPost.attr("href");
                title = listPost.attr("title");
                pbDate  = unixFm(listPost.attr("data-pbt"));
                output = save(id,site,url,title,pbDate);
                // insert news-url into db
				//outputList.add(output);
            }
        }

        // crawl operation-module
        Elements underPost = leftPost.select(".list-type-1");
        for(Element omPost: underPost.select("li > a")){
            id = omPost.attr("data-articleid");
            url = omPost.attr("href");
            title = omPost.attr("title");
            pbDate  = unixFm(omPost.attr("data-pbt"));
            output = save(id,site,url,title,pbDate);
            // insert news-url into db
			//outputList.add(output);
        }


    }
}


def urlDec(String input){
    String result;
    try{
        result = URLDecoder.decode(input, "UTF-8");
    }catch(Exception e){
        logger.info("failed to decode url.");
        logger.error(getStackTrace(e));
    }
    return result;
}

def Timestamp unixFm(String input){
    Timestamp result = null;
    try{
        result = new Timestamp(Long.parseLong(input))
    }catch(Exception e){
        logger.info("failed to covert unix to Timestamp.")
        logger.error(getStackTrace(e));
    }
    return result;
}

def HashMap save(String postid, String site, String url, String title,Timestamp pbDate) {
    HashMap result = new HashMap();
    result.put("artId", postid);
    result.put("site", site);
    result.put("url", url);
    result.put("title", title);
    result.put("publishDate", pbDate);

    return result;
}

def Document getDoc(String url){
    Document doc = null;
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
                Thread.sleep(1);
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
            return doc;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(streamSource, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        reader.close();
        doc = Jsoup.parse(toUtf8(sb.toString()))
        return doc;

    }catch(Exception e){
        logger.info("failed to get Document.");
        logger.error(getStackTrace(e));
    }

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