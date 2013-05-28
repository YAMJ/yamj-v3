package com.yamj.core.tools.web;

import com.moviejukebox.api.common.http.AbstractPoolingHttpClient;
import com.yamj.common.tools.PropertyTools;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

public class PoolingHttpClient extends AbstractPoolingHttpClient implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(PoolingHttpClient.class);
    private final Map<String, Integer> groupLimits = new HashMap<String, Integer>();
    private final List<String> routedHosts = new ArrayList<String>();
    private int connectionsMaxPerRoute = 1;
    private int connectionsMaxTotal = 20;

    public PoolingHttpClient() {
        this(null, null);
    }

    public PoolingHttpClient(ClientConnectionManager conman) {
        this(conman, null);
    }

    public PoolingHttpClient(HttpParams params) {
        this(null, params);
    }

    public PoolingHttpClient(ClientConnectionManager connectionManager, HttpParams httpParams) {
        super(connectionManager, httpParams);

        // First we have to read/create the rules
        // Default, can be overridden
        groupLimits.put(".*", 1);
        String limitsProperty = PropertyTools.getProperty("yamj3.http.maxDownloadSlots", ".*=1");
        LOG.debug("Using download limits: {}" , limitsProperty);

        Pattern pattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
        Matcher matcher = pattern.matcher(limitsProperty);
        while (matcher.find()) {
            String group = matcher.group(1);
            try {
                Pattern.compile(group);
                groupLimits.put(group, Integer.parseInt(matcher.group(2)));
            } catch (Exception error) {
                LOG.debug("Rule '{}' is not valid regexp, ignored",group);
            }
        }
    }

    public int getConnectionsMaxPerRoute() {
        return connectionsMaxPerRoute;
    }

    public void setConnectionsMaxPerRoute(int connectionsMaxPerRoute) {
        this.connectionsMaxPerRoute = connectionsMaxPerRoute;
    }

    public int getConnectionsMaxTotal() {
        return connectionsMaxTotal;
    }

    public void setConnectionsMaxTotal(int connectionsMaxTotal) {
        this.connectionsMaxTotal = connectionsMaxTotal;
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        PoolingClientConnectionManager clientManager = new PoolingClientConnectionManager();
        clientManager.setDefaultMaxPerRoute(connectionsMaxPerRoute);
        clientManager.setMaxTotal(connectionsMaxTotal);
        return clientManager;
    }

    @Override
    public void destroy() throws Exception {
        ClientConnectionManager clientManager = super.getConnectionManager();
        if (clientManager != null) {
            clientManager.shutdown();
        }
    }

    @Override
    public String requestContent(URL url) throws IOException {
        return this.requestContent(url, null);
    }

    @Override
    public String requestContent(URL url, Charset charset) throws IOException {
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid url " + url, ex);
        }
        
        return this.requestContent(uri, charset);
    }

    @Override
    public String requestContent(String uri) throws IOException {
        return this.requestContent(uri, null);
    }

    @Override
    public String requestContent(String uri, Charset charset) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        return this.requestContent(httpGet, charset);
    }

    @Override
    public String requestContent(URI uri) throws IOException {
        return this.requestContent(uri, null);
    }

    @Override
    public String requestContent(URI uri, Charset charset) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        return this.requestContent(httpGet, charset);
    }

    @Override
    public String requestContent(HttpGet httpGet) throws IOException {
        return this.requestContent(httpGet, null);
    }

    @Override
    public String requestContent(HttpGet httpGet, Charset charset) throws IOException {
        // set route (if not set before)
        setRoute(httpGet);

        HttpResponse response = execute(httpGet);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new RuntimeException("Unexpected status " + statusCode + " for uri " + httpGet.getURI());
        }

        return readContent(response, charset);
    }

    private void setRoute(HttpUriRequest request) throws ClientProtocolException {
        HttpHost httpHost = determineTarget(request);
        String key = httpHost.toString();

        synchronized (routedHosts) {
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

                LOG.debug("IO download host: {}; rule: {}, maxRequests: {}", key, group, maxRequests);
                routedHosts.add(key);

                HttpRoute httpRoute = new HttpRoute(httpHost);

                ClientConnectionManager conMan = this.getConnectionManager();
                if (conMan instanceof PoolingClientConnectionManager) {
                    PoolingClientConnectionManager poolMan = (PoolingClientConnectionManager) conMan;
                    poolMan.setMaxPerRoute(httpRoute, maxRequests);
                }
            }
        }
    }

    private static HttpHost determineTarget(HttpUriRequest request) throws ClientProtocolException {
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