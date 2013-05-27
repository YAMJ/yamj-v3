package com.yamj.core.tools.web;

import com.yamj.common.tools.PropertyTools;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

public class PoolingHttpClient extends DefaultHttpClient implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(PoolingHttpClient.class);
    private final Map<String, Integer> groupLimits = new HashMap<String, Integer>();
    private final List<String> routedHosts = new ArrayList<String>();
    private String proxyHost = null;
    private int proxyPort = 0;
    private String proxyUsername = null;
    private String proxyPassword = null;
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
        LOG.debug("Using download limits: {}", limitsProperty);

        Pattern pattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
        Matcher matcher = pattern.matcher(limitsProperty);
        while (matcher.find()) {
            String group = matcher.group(1);
            try {
                Pattern.compile(group);
                groupLimits.put(group, Integer.parseInt(matcher.group(2)));
            } catch (Exception error) {
                LOG.debug("Rule '{}' is not valid regexp, ignored", group);
            }
        }
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
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
    protected HttpParams createHttpParams() {
        HttpParams params = super.createHttpParams();
        HttpProtocolParams.setContentCharset(params, Consts.UTF_8.name());
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpConnectionParams.setSocketBufferSize(params, 8192);

        // set user agent
        HttpProtocolParams.setUserAgent(params, "Mozilla/6.0 (Windows NT 6.2; WOW64; rv:16.0.1) Gecko/20121011 Firefox/16.0.1");

        // set default proxy
        if (StringUtils.isNotBlank(proxyHost) && proxyPort > 0) {
            if (StringUtils.isNotBlank(proxyUsername) && StringUtils.isNotBlank(proxyPassword)) {
                getCredentialsProvider().setCredentials(
                        new AuthScope(proxyHost, proxyPort),
                        new UsernamePasswordCredentials(proxyUsername, proxyPassword));
            }

            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            ConnRouteParams.setDefaultProxy(params, proxy);
        }

        return params;
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
            throw new RemoteException("Unexpected status " + statusCode + " for uri " + uri);
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
                try {
                    br.close();
                } catch (Exception ignore) {
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ignore) {
                }
            }
            try {
                content.close();
            } catch (Exception ignore) {
            }
            try {
                is.close();
            } catch (Exception ignore) {
            }
        }
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