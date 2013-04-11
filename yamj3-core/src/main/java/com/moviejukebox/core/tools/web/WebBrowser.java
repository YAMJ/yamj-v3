package com.moviejukebox.core.tools.web;

import com.moviejukebox.core.tools.PropertyTools;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web browser with simple cookies support
 */
public class WebBrowser {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebBrowser.class);
    
    private static String PROXY_HOST = PropertyTools.getProperty("mjb.proxyHost");
    private static String PROXY_PORT = PropertyTools.getProperty("mjb.proxyPort");
    private static String PROXY_USERNAME = PropertyTools.getProperty("mjb.proxyUsername");
    private static String PROXY_PASSWORD = PropertyTools.getProperty("mjb.proxyPassword");
    private static String ENCODED_PASSWORD = encodePassword();
    private static int CONNECT_TIMEOUT = PropertyTools.getIntProperty("mjb.timeout.connect", 25000);
    private static int READ_TIMEOUT = PropertyTools.getIntProperty("mjb.timeout.read", 90000);
    private static String ACCEPT_LANGUAGE = PropertyTools.getProperty("mjb.acceptLanguage");

    private Map<String, String> browserProperties;
    private Map<String, Map<String, String>> cookies;
    private int imageRetryCount;


    public WebBrowser() {
        browserProperties = new HashMap<String, String>();
        browserProperties.put("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
        if (StringUtils.isNotBlank(ACCEPT_LANGUAGE)) {
            browserProperties.put("Accept-Language", ACCEPT_LANGUAGE.trim());
        }

        cookies = new HashMap<String, Map<String, String>>();

        imageRetryCount = PropertyTools.getIntProperty("mjb.imageRetryCount", 3);
        if (imageRetryCount < 1) {
            imageRetryCount = 1;
        }
    }

    public void addBrowserProperty(String key, String value) {
        this.browserProperties.put(key, value);
    }
    
    private static String encodePassword() {
        if (PROXY_USERNAME != null) {
            return ("Basic " + new String(Base64.encodeBase64((PROXY_USERNAME + ":" + PROXY_PASSWORD).getBytes())));
        } else {
            return "";
        }
    }

    public String request(String url) throws IOException {
        return request(new URL(url));
    }

    public String request(String url, Charset charset) throws IOException {
        return request(new URL(url), charset);
    }

    public URLConnection openProxiedConnection(URL url) throws IOException {
        if (PROXY_HOST != null) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", PROXY_HOST);
            System.getProperties().put("proxyPort", PROXY_PORT);
        }

        URLConnection cnx = url.openConnection();

        if (PROXY_USERNAME != null) {
            cnx.setRequestProperty("Proxy-Authorization", ENCODED_PASSWORD);
        }

        cnx.setConnectTimeout(CONNECT_TIMEOUT);
        cnx.setReadTimeout(READ_TIMEOUT);

        return cnx;
    }

    public String request(URL url) throws IOException {
        return request(url, null);
    }

    public String request(URL url, Charset charset) throws IOException {
        LOGGER.debug("Requesting " + url.toString());

        // get the download limit for the host
        //ThreadExecutor.enterIO(url);
        StringWriter content = new StringWriter(10 * 1024);
        try {

            URLConnection cnx = null;

            try {
                cnx = openProxiedConnection(url);

                sendHeader(cnx);
                readHeader(cnx);

                BufferedReader in = null;
                try {

                    // If we fail to get the URL information we need to exit gracefully
                    if (charset == null) {
                        in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), getCharset(cnx)));
                    } else {
                        in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), charset));
                    }

                    String line;
                    while ((line = in.readLine()) != null) {
                        content.write(line);
                    }
                    // Attempt to force close connection
                    // We have HTTP connections, so these are always valid
                    content.flush();
                } catch (FileNotFoundException ex) {
                    LOGGER.error("URL not found: " + url.toString());
                } catch (IOException ex) {
                    LOGGER.error("Error getting URL " + url.toString() + ", " + ex.getMessage());
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (SocketTimeoutException ex) {
                LOGGER.error("Timeout Error with " + url.toString());
            } finally {
                if (cnx != null) {
                    if (cnx instanceof HttpURLConnection) {
                        ((HttpURLConnection) cnx).disconnect();
                    }
                }
            }
            return content.toString();
        } finally {
            content.close();
            //ThreadExecutor.leaveIO();
        }
    }

    /**
     * Download the image for the specified URL into the specified file.
     *
     * @throws IOException
     */
    public boolean downloadImage(File imageFile, String imageURL) throws IOException {

        String fixedImageURL;
        if (imageURL.contains(" ")) {
            fixedImageURL = imageURL.replaceAll(" ", "%20");
        } else {
            fixedImageURL = imageURL;
        }

        URL url = new URL(fixedImageURL);

        LOGGER.debug("Attempting to download '" + fixedImageURL + "'");

        //ThreadExecutor.enterIO(url);
        boolean success = Boolean.FALSE;
        int retryCount = imageRetryCount;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        int reportedLength = 0;
        try {
            while (!success && retryCount > 0) {
                URLConnection cnx = openProxiedConnection(url);

                sendHeader(cnx);
                readHeader(cnx);

                reportedLength = cnx.getContentLength();
                inputStream = cnx.getInputStream();
                outputStream = new FileOutputStream(imageFile);
                // TODO
                //int inputStreamLength = FileTools.copy(inputStream, outputStream);
                int inputStreamLength = 0;
                
                if (reportedLength < 0 || reportedLength == inputStreamLength) {
                    success = Boolean.TRUE;
                } else {
                    retryCount--;
                    LOGGER.debug("Image download attempt failed, bytes expected: " + reportedLength + ", bytes received: " + inputStreamLength);
                }
            }
        } finally {
            //ThreadExecutor.leaveIO();

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                    // ignore this error
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException ignore) {
                    // ignore this error
                }
            }
        }

        if (success) {
            LOGGER.debug("Sucessfully downloaded '" + imageURL + "' to '" + imageFile.getAbsolutePath() + "', Size: " + reportedLength);
        } else {
            LOGGER.debug("Failed " + imageRetryCount + " times to download image, aborting. URL: " + imageURL);
        }
        return success;
    }

    /**
     * Check the URL to see if it's one of the special cases that needs to be worked around
     *
     * @param URL The URL to check
     * @param cnx The connection that has been opened
     */
    private void checkRequest(URLConnection checkCnx) {
        String checkUrl = checkCnx.getURL().getHost().toLowerCase();

        // A workaround for the need to use a referrer for thetvdb.com
        if (checkUrl.contains("thetvdb")) {
            checkCnx.setRequestProperty("Referer", "http://forums.thetvdb.com/");
        }

        // A workaround for the kinopoisk.ru site
        if (checkUrl.contains("kinopoisk")) {
            checkCnx.setRequestProperty("Accept", "text/html, text/plain");
            checkCnx.setRequestProperty("Accept-Language", "ru");
            checkCnx.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        }
    }

    private void sendHeader(URLConnection cnx) {
        // send browser properties
        for (Map.Entry<String, String> browserProperty : browserProperties.entrySet()) {
            cnx.setRequestProperty(browserProperty.getKey(), browserProperty.getValue());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("SetRequestProperty:" + browserProperty.getKey() + "='" + browserProperty.getValue() + "'");
            }
        }

        // send cookies
        String cookieHeader = createCookieHeader(cnx);
        if (!cookieHeader.isEmpty()) {
            cnx.setRequestProperty("Cookie", cookieHeader);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Cookie:" + cookieHeader);
            }
        }

        checkRequest(cnx);
    }

    private String createCookieHeader(URLConnection cnx) {
        String host = cnx.getURL().getHost();
        StringBuilder cookiesHeader = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> domainCookies : cookies.entrySet()) {
            if (host.endsWith(domainCookies.getKey())) {
                for (Map.Entry<String, String> cookie : domainCookies.getValue().entrySet()) {
                    cookiesHeader.append(cookie.getKey());
                    cookiesHeader.append("=");
                    cookiesHeader.append(cookie.getValue());
                    cookiesHeader.append(";");
                }
            }
        }
        if (cookiesHeader.length() > 0) {
            // remove last ; char
            cookiesHeader.deleteCharAt(cookiesHeader.length() - 1);
        }
        return cookiesHeader.toString();
    }

    private void readHeader(URLConnection cnx) {
        // read new cookies and update our cookies
        for (Map.Entry<String, List<String>> header : cnx.getHeaderFields().entrySet()) {
            if ("Set-Cookie".equals(header.getKey())) {
                for (String cookieHeader : header.getValue()) {
                    String[] cookieElements = cookieHeader.split(" *; *");
                    if (cookieElements.length >= 1) {
                        String[] firstElem = cookieElements[0].split(" *= *");
                        String cookieName = firstElem[0];
                        String cookieValue = firstElem.length > 1 ? firstElem[1] : null;
                        String cookieDomain = null;
                        // find cookie domain
                        for (int i = 1; i < cookieElements.length; i++) {
                            String[] cookieElement = cookieElements[i].split(" *= *");
                            if ("domain".equals(cookieElement[0])) {
                                cookieDomain = cookieElement.length > 1 ? cookieElement[1] : null;
                                break;
                            }
                        }
                        if (cookieDomain == null) {
                            // if domain isn't set take current host
                            cookieDomain = cnx.getURL().getHost();
                        }
                        Map<String, String> domainCookies = cookies.get(cookieDomain);
                        if (domainCookies == null) {
                            domainCookies = new HashMap<String, String>();
                            cookies.put(cookieDomain, domainCookies);
                        }
                        // add or replace cookie
                        domainCookies.put(cookieName, cookieValue);
                    }
                }
            }
        }
    }

    private Charset getCharset(URLConnection cnx) {
        Charset charset = null;
        // content type will be string like "text/html; charset=UTF-8" or "text/html"
        String contentType = cnx.getContentType();
        if (contentType != null) {
            // changed 'charset' to 'harset' in regexp because some sites send 'Charset'
            Matcher m = Pattern.compile("harset *=[ '\"]*([^ ;'\"]+)[ ;'\"]*").matcher(contentType);
            if (m.find()) {
                String encoding = m.group(1);
                try {
                    charset = Charset.forName(encoding);
                } catch (UnsupportedCharsetException error) {
                    // there will be used default charset
                }
            }
        }
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        // LOGGER.debug("Detected charset " + charset);
        return charset;
    }

    /**
     * Get URL - allow to know if there is some redirect
     *
     * @param urlString
     * @return
     */
    public String getUrl(final String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            LOGGER.warn("Unable to convert URL: " + urlString + " - Error: " + ex.getMessage());
            return null;
        }

        ///ThreadExecutor.enterIO(url);

        try {
            URLConnection cnx = openProxiedConnection(url);
            sendHeader(cnx);
            readHeader(cnx);
            return cnx.getURL().toString();
        } catch (IOException ex) {
            LOGGER.warn("Unable to retrieve URL: " + urlString + " - Error: " + ex.getMessage());
            return null;
        } finally {
            //ThreadExecutor.leaveIO();
        }
    }
}
