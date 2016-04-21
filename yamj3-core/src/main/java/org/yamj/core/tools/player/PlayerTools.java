/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.tools.player;

import static org.yamj.plugin.api.common.Constants.UTF8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.DigestedResponseReader;
import org.yamj.api.common.http.SimpleHttpClientBuilder;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.tools.player.davidbox.DavidBoxPlayerPath;
import org.yamj.core.tools.player.davidbox.DavidBoxWrapper;

/**
 * Functions for finding information on players
 *
 * @author Stuart
 */
public final class PlayerTools {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerTools.class);
    private static final ObjectMapper MAPPER = new XmlMapper();
    private static final HttpClient HTTP = new SimpleHttpClientBuilder().build();
    private static final String XML_PLAYER_IDENT = "<syabasCommandServerXml>";
    // List of the players found
    private final List<PlayerInfo> players = new ArrayList<>();
    // Timeout for scanning
    private int scanTimeout;
    // Base scan range
    private String baseIpAddress;
    // Port
    private int port;

    private static final String FILE_OP = "/file_operation?arg0=list_user_storage_file&arg1=&arg2=0&arg3=100&arg4=true&arg5=true&arg6=true&arg7=";
    private static final String GET_DEV_NAME = "/system?arg0=get_nmt_device_name";

    /**
     * Create the player tools with a base IP address range, port 8008 & timeout 75
     *
     * @param baseIpAddress the first 3 octlets of the IP address, e.g. "192.168.0"
     */
    public PlayerTools(String baseIpAddress) {
        this(baseIpAddress, 8008, 75);
    }

    /**
     * Create the player tools with a base IP address range, port and timeout
     *
     * @param baseIpAddress
     * @param port
     * @param scanTimeout
     */
    public PlayerTools(String baseIpAddress, int port, int scanTimeout) {
        setBaseIpAddress(baseIpAddress);
        setPort(port);
        setScanTimeout(scanTimeout);
    }

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public int getScanTimeout() {
        return scanTimeout;
    }

    public void setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
    }

    public String getBaseIpAddress() {
        return baseIpAddress;
    }

    public void setBaseIpAddress(String baseIpAddress) {
        if (baseIpAddress.endsWith(".")) {
            this.baseIpAddress = baseIpAddress;
        } else {
            this.baseIpAddress = baseIpAddress + ".";
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    //</editor-fold>

    public void scan() {
        LOG.info("Scanning for players in '{}*' address range");
        List<String> ipList = scanForPlayers(baseIpAddress, port, 1, 255, scanTimeout);

        for (String ipAddr : ipList) {
            PlayerInfo player = new PlayerInfo();
            player.setIpAddress(ipAddr);
            player.setName(getPlayerName(ipAddr, port));
            getPathInfo(player, ipAddr, port);
            players.add(player);
        }

        LOG.info("Found {} players.", players.size());
        for (PlayerInfo player : players) {
            LOG.info(player.toString());
        }

    }

    /**
     * Scan a list of IP addresses for players
     *
     * @param baseIpAddress
     * @param port
     * @param scanStart
     * @param scanEnd
     * @param timeout
     * @return
     */
    private static List<String> scanForPlayers(String baseIpAddress, int port, int scanStart, int scanEnd, int timeout) {
        List<String> playerList = new ArrayList<>();

        for (int i = (scanStart < 1 ? 1 : scanStart); i <= (scanEnd > 255 ? 255 : scanEnd); i++) {
            try (Socket mySocket = new Socket()) {
                String ipToScan = baseIpAddress + i;
                LOG.debug("Scanning {}", ipToScan);

                SocketAddress address = new InetSocketAddress(ipToScan, port);
                mySocket.connect(address, timeout);

                try (PrintWriter out = new PrintWriter(mySocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()))) {
                    out.println("setting");
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                        if (fromServer.equals(XML_PLAYER_IDENT)) {
                            playerList.add(ipToScan);
                            break;
                        }
                    }
                }
            } catch (IOException ex) {
                LOG.trace("IO error during scan", ex);
            }
        }

        LOG.info("Found {} players: {}", playerList.size(), playerList.toString());
        return playerList;
    }

    /**
     * Build the URL for getting the player information
     *
     * @param ipAddress
     * @param ipPort
     * @param operation
     * @return
     */
    private static String buildUrl(String ipAddress, int ipPort, String operation) {
        StringBuilder url = new StringBuilder("http://");
        url.append(ipAddress);
        url.append(":").append(ipPort);
        url.append(operation);

        LOG.debug("URL: {}", url.toString());
        return url.toString();
    }

    /**
     * Get information on the paths held in the player
     *
     * @param addr
     * @param port
     * @return
     */
    private static PlayerInfo getPathInfo(PlayerInfo player, final String addr, final int port) {
        String url = buildUrl(addr, port, FILE_OP);

        try {
            DigestedResponse response = getRequest(url);
            if (ResponseTools.isOK(response)) {
                DavidBoxWrapper wrapper = MAPPER.readValue(response.getContent(), DavidBoxWrapper.class);

                if (wrapper.getResponse().getFileList() != null) {
                    for (DavidBoxPlayerPath db : wrapper.getResponse().getFileList()) {
                        LOG.debug("DavidBoxPlayer: {}", db.toString());
                        if (StringUtils.isBlank(player.getName())) {
                            player.setName(db.getName());
                        }

                        if (StringUtils.isBlank(player.getDeviceType())) {
                            player.setDeviceType(db.getDeviceType());
                        }

                        PlayerPath path = new PlayerPath();
                        path.setSourcePath(db.getPath());
                        path.setTargetPath(db.getPath());
                        path.setPlayerInfo(player);
                        player.getPaths().add(path);
                        LOG.debug("Found path for '{}': {}", addr, path.toString());
                    }
                }
            } else {
                LOG.warn("Request for path infos failed with status {}", response.getStatusCode());
            }
        } catch (IOException ex) {
            LOG.trace("Error getting path info", ex);
        }
        return player;
    }

    /**
     * Get the player name information
     *
     * @param addr
     * @param port
     * @return
     */
    private static String getPlayerName(String addr, int port) {
        String url = buildUrl(addr, port, GET_DEV_NAME);
        String playerName = "UNKNOWN";
        try {
            DigestedResponse response = getRequest(url);
            if (ResponseTools.isOK(response)) {
                DavidBoxWrapper wrapper = MAPPER.readValue(response.getContent(), DavidBoxWrapper.class);

                if (wrapper.getResponse() != null) {
                    playerName = wrapper.getResponse().getName();
                    LOG.info("Found player name '{}' for '{}'", playerName, addr);
                }
            } else {
                LOG.warn("Request for player name failed with status {}", response.getStatusCode());
            }
        } catch (IOException ex) {
            LOG.trace("Error getting player name", ex);
        }
        return playerName;
    }

    private static DigestedResponse getRequest(final String url) {
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(HttpHeaders.ACCEPT, "application/xml");
            final DigestedResponse response = DigestedResponseReader.requestContent(HTTP, httpGet, UTF8);

            return response;
        } catch (IOException ex) {
            LOG.warn("Error retrieving URL: {}", url, ex);
            return null;
        }
    }
}
