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
package org.yamj.common.tools;

import java.net.*;
import java.util.Enumeration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System level helper methods
 *
 * @author Stuart
 */
public final class SystemTools {

    private static final Logger LOG = LoggerFactory.getLogger(SystemTools.class);
    private static String ipv4 = null;
    private static String ipv6 = null;

    private SystemTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Check to see if a system property has been set, and set it if not.
     *
     * @param property
     * @param defaultValue
     * @return
     */
    public static String checkSystemProperty(String property, String defaultValue) {
        String systemProperty = System.getProperty(property, "");
        if (StringUtils.isBlank(systemProperty)) {
            LOG.debug("System property '{}' not found. Setting to '{}'", property, defaultValue);
            System.setProperty(property, defaultValue);
            return defaultValue;
        }
        return systemProperty;
    }
    
    /**
     * Search through the the network adapters and get the IPv4 address of the
     * server.
     *
     * @return
     */
    public static String getIpAddress() {
        return getIpAddress(true);
    }

    /**
     * Search through the network adapters and get the IP address of the server.
     *
     * @param getIpv4 return the IPv4 address, otherwise IPv6
     * @return
     */
    public static String getIpAddress(final boolean getIpv4) { //NOSONAR
        if (getIpv4 && ipv4 != null) {
            return ipv4;
        }

        if (!getIpv4 && ipv6 != null) {
            return ipv6;
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            outerLoop: while (interfaces.hasMoreElements()) {
                NetworkInterface currentInterface = interfaces.nextElement();
                if (!currentInterface.isUp() || currentInterface.isLoopback() || currentInterface.isVirtual()) {
                    continue;
                }

                LOG.trace("Current Interface: {}", currentInterface.toString());
                Enumeration<InetAddress> addresses = currentInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress currentAddress = addresses.nextElement();
                    if (currentAddress.isLoopbackAddress()) {
                        continue;
                    }

                    if (currentAddress instanceof Inet4Address) {
                        ipv4 = currentAddress.getHostAddress();
                        LOG.debug("IPv4 Address: {}", currentAddress.getHostAddress());
                    } else if (currentAddress instanceof Inet6Address) {
                        ipv6 = currentAddress.getHostAddress();
                        LOG.debug("IPv6 Address: {}", currentAddress.getHostAddress());
                    }

                    if (ipv4 != null && ipv6 != null) {
                        break outerLoop;
                    }
                }
            }
        } catch (SocketException ex) {
            LOG.warn("Failed to get IP Address: {}", ex.getMessage());
            LOG.trace("Socket error", ex);
        }

        return getIpv4 ? ipv4 : ipv6;
    }
}
