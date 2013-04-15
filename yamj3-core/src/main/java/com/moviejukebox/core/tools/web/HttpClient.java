package com.moviejukebox.core.tools.web;

import com.moviejukebox.core.tools.PropertyTools;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.*;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

@Service("httpClient")
public class HttpClient extends AbstractHttpClient implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);
    
    private final Map<String, Integer> groupLimits = new HashMap<String, Integer>();
    private final List<String> routedHosts = new ArrayList<String>();
    
    public HttpClient() {
        this(null, null);
    }
    
    protected HttpClient(ClientConnectionManager connectionManager, HttpParams httpParams) {
        super(connectionManager, httpParams);
        
        // First we have to read/create the rules
        // Default, can be overridden
        groupLimits.put(".*", 1);
        String limitsProperty = PropertyTools.getProperty("mjb.maxDownloadSlots", ".*=1");
        LOGGER.debug("Using download limits: " + limitsProperty);

        Pattern pattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
        Matcher matcher = pattern.matcher(limitsProperty);
        while (matcher.find()) {
            String group = matcher.group(1);
            try {
                Pattern.compile(group);
                groupLimits.put(group, Integer.parseInt(matcher.group(2)));
            } catch (Exception error) {
                LOGGER.debug("Rule \"" + group + "\" is not valid regexp, ignored");
            }
        }

    }

    @Override
    protected HttpParams createHttpParams() {
        HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, Consts.UTF_8.name());
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpConnectionParams.setSocketBufferSize(params, 8192);

        HttpProtocolParams.setUserAgent(params, "Mozilla/6.0 (Windows NT 6.2; WOW64; rv:16.0.1) Gecko/20121011 Firefox/16.0.1");
        String acceptLanguage = PropertyTools.getProperty("mjb.acceptLanguage");
        if (StringUtils.isNotBlank(acceptLanguage)) {
            params.setParameter("AcceptLanguage", acceptLanguage);
        }
        
        String proxyHost = PropertyTools.getProperty("mjb.proxyHost");
        int proxyPort = PropertyTools.getIntProperty("mjb.proxyPort", 0);
        if (StringUtils.isNotBlank(proxyHost) && proxyPort > 0) {
            // set default proxy
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            ConnRouteParams.setDefaultProxy(params, proxy);
        }
        
        return params;
    }

    @Override
    protected BasicHttpProcessor createHttpProcessor() {
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestDefaultHeaders());
        // Required protocol interceptors
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        // Recommended protocol interceptors
        httpproc.addInterceptor(new RequestClientConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        // HTTP state management interceptors
        httpproc.addInterceptor(new RequestAddCookies());
        httpproc.addInterceptor(new ResponseProcessCookies());
        // HTTP authentication interceptors
        httpproc.addInterceptor(new RequestAuthCache());
        httpproc.addInterceptor(new RequestTargetAuthentication());
        httpproc.addInterceptor(new RequestProxyAuthentication());
        return httpproc;
    }

    protected ClientConnectionManager createClientConnectionManager() {
        PoolingClientConnectionManager clientManager = new PoolingClientConnectionManager();
        clientManager.setDefaultMaxPerRoute(PropertyTools.getIntProperty("mjb.connections.maxPerRoute", 1));
        clientManager.setMaxTotal(PropertyTools.getIntProperty("mjb.connections.maxTotal", 20));
        return clientManager;
    }

    @Override
    public void destroy() throws Exception {
        ClientConnectionManager clientManager = super.getConnectionManager();
        if (clientManager != null) {
            clientManager.shutdown();
        }
    }

    public String requestContent(String uri) throws IOException {
        return this.requestContent(uri, null);
    }
    
    public String requestContent(String uri, Charset charset) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        
        // set route (if not set before)
        setRoute(httpGet);
        
        HttpResponse response = execute(httpGet);
        
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new RuntimeException("Unexpected status " + statusCode + " for uri " + uri);
        }
        
        return readContent(response, charset);
    }
    
    private String readContent(HttpResponse response, Charset charset) throws IOException {

        StringWriter content = new StringWriter(10 * 1024);
        InputStream is = response.getEntity().getContent();
        InputStreamReader isr = null;
        BufferedReader br = null;
        
        try {
            if (charset == null) {
                isr = new InputStreamReader(is, Charset.defaultCharset());
            } else {
                isr = new InputStreamReader(is, charset);
            }
            br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                content.write(line);
            }

            content.flush();
            return content.toString();
        } finally {
            if (br != null) {
                try  {
                    br.close();
                } catch (Exception ignore) {}
            }
            if (isr != null) {
                try  {
                    isr.close();
                } catch (Exception ignore) {}
            }
            try  {
                content.close();
            } catch (Exception ignore) {}
            try  {
                is.close();
            } catch (Exception ignore) {}
        }
    }
    
    private void setRoute(HttpUriRequest request) throws ClientProtocolException {
        HttpHost httpHost = determineTarget(request);
        String key = httpHost.toString();

        synchronized(routedHosts) {
            if (!routedHosts.contains(key)) {
                String group = ".*";
                for (String searchGroup : groupLimits.keySet()) {
                    if (key.matches(searchGroup)) {
                        if (searchGroup.length() > group.length()) {
                            group = searchGroup;
                        }
                    }
                }
                int maxRequests = groupLimits.get(group);
    
                LOGGER.debug("IO download host: {}; rule: {}, maxRequests: {}", key, group, maxRequests);
                routedHosts.add(key);
                
                HttpRoute httpRoute = new HttpRoute(httpHost);
                
                ClientConnectionManager conMan = this.getConnectionManager();
                if (conMan instanceof PoolingClientConnectionManager) {
                    PoolingClientConnectionManager poolMan = (PoolingClientConnectionManager)conMan;
                    poolMan.setMaxPerRoute(httpRoute, maxRequests);
                }
           }
        }
    }
    
    private  static HttpHost determineTarget(HttpUriRequest request) throws ClientProtocolException {
        HttpHost target = null;
        URI requestURI = request.getURI();
        if (requestURI.isAbsolute()) {
            target = URIUtils.extractHost(requestURI);
            if (target == null) {
                throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
            }
        }
        return target;
    }
}