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
package org.yamj.common.model;

import static org.yamj.common.tools.DateTimeTools.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.tools.SystemTools;
import org.yamj.common.type.MetaDataType;

/**
 * Provides information on the build of YAMJ<br>
 * This comes from the manifest file when the classes are built<br>
 * Also there is some system level information collected<br>
 *
 * @author stuart.boston
 */
public class YamjInfo {

    private static final Logger LOG = LoggerFactory.getLogger(YamjInfo.class);
    private static final String UNKNOWN = "UNKNOWN";
    private static YamjInfo CORE_INFO = null;
    
    // Project properties/information
    private String projectName;
    private String projectVersion;
    private String moduleName;
    private String moduleDescription;
    private DateTime buildDateTime;
    private String buildRevision;
    private Boolean buildDirty;
    // Server properties/information
    private int processorCores;
    private String javaVersion;
    private String osArch;
    private String osName;
    private String osVersion;
    // Service properties/information
    private DateTime startUpDateTime;
    private Map<MetaDataType, Long> counts;
    private String databaseIp;
    private String databaseName;
    private String coreIp;
    private int corePort;
    private String baseArtworkUrl;
    private String baseMediainfoUrl;
    private String basePhotoUrl;
    private String baseTrailerUrl;
    private String skinDir;

    @SuppressWarnings("unused")
    private YamjInfo() {
        this(YamjInfoBuild.COMMON);
    }

    public YamjInfo(YamjInfoBuild yamjInfoBuild) {
        // YAMJ Stuff
        processPropertiesFile(yamjInfoBuild.getFilename());

        // System Stuff
        this.processorCores = Runtime.getRuntime().availableProcessors();
        this.javaVersion = SystemUtils.JAVA_VERSION;
        this.osArch = SystemUtils.OS_ARCH;
        this.osName = SystemUtils.OS_NAME;
        this.osVersion = SystemUtils.OS_VERSION;

        // Times
        this.startUpDateTime = new DateTime(ManagementFactory.getRuntimeMXBean().getStartTime());

        // Counts
        this.counts = new EnumMap<>(MetaDataType.class);

        // IP Address
        final String coreUrl = PropertyTools.getProperty("yamj3.core.url", "");
        if (StringUtils.isBlank(coreUrl)) {
            this.coreIp = SystemTools.getIpAddress(true);
        } else {
            this.coreIp = coreUrl;
        }

        // Core Port
        this.corePort = PropertyTools.getIntProperty("yamj3.core.port", 
        		NumberUtils.toInt(System.getProperty("yamj3.core.port"), 8888));
        System.err.println(System.getProperty("yamj3.core.port"));
        
        // Database IP & Name
        findDatabaseInfo();

        this.baseArtworkUrl = buildBaseUrl(PropertyTools.getProperty("yamj3.file.storage.artwork", ""));
        this.baseMediainfoUrl = buildBaseUrl(PropertyTools.getProperty("yamj3.file.storage.mediainfo", ""));
        this.basePhotoUrl = buildBaseUrl(PropertyTools.getProperty("yamj3.file.storage.photo", ""));
        this.baseTrailerUrl = buildBaseUrl(PropertyTools.getProperty("yamj3.file.storage.trailer", ""));
        this.skinDir = buildBaseUrl(PropertyTools.getProperty("yamj3.file.storage.skins", "./skins/"));
    }

    public static YamjInfo getCoreInfo() {
        if (CORE_INFO == null) {
            CORE_INFO = new YamjInfo(YamjInfoBuild.CORE);
        }
        return CORE_INFO;
    }

    private void processPropertiesFile(String filename) {
        Properties properties = new Properties();
        try (InputStream res = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (res == null) {
                LOG.warn("Unable to open '{}' file", filename);
            } else {
                properties.load(res);

                this.projectName = properties.get("yamj.org").toString();
                this.projectVersion = properties.get("yamj.version").toString();
                this.moduleName = properties.get("yamj.name").toString();
                this.moduleDescription = properties.get("yamj.description").toString();

                this.buildDateTime = parseDate(properties.get("git.build.time").toString(), BUILD_FORMAT);
                this.buildRevision = properties.get("git.commit.id.abbrev").toString();
                this.buildDirty = asBoolean(properties.get("git.dirty").toString());
            }
        } catch (IOException ex) {
            LOG.warn("Failed to get build properties from '{}' file", filename); 
            LOG.trace("Load error", ex);
        }
    }

    private static boolean asBoolean(final String valueToConvert) {
        if (StringUtils.isNotBlank(valueToConvert)) {
            return Boolean.parseBoolean(StringUtils.trimToEmpty(valueToConvert));
        }
        // Return "true" if the input is invalid
        return true;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    //Ignore this for JSON output (use the String version instead
    @JsonIgnore
    public DateTime getBuildDateTime() {
        return buildDateTime;
    }

    //Ignore this for JSON output (use the String version instead
    @JsonIgnore
    public DateTime getStartUpDateTime() {
        return startUpDateTime;
    }

    public String getBuildRevision() {
        return buildRevision;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getModuleDescription() {
        return moduleDescription;
    }

    public Boolean isBuildDirty() {
        return buildDirty;
    }

    public int getProcessorCores() {
        return processorCores;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getBuildDate() {
        return convertDateToString(buildDateTime, BUILD_FORMAT);
    }

    public String getStartUpTime() {
        return convertDateToString(startUpDateTime, BUILD_FORMAT);
    }

    public String getUptime() {
        return formatDurationText(ManagementFactory.getRuntimeMXBean().getUptime());
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseIp() {
        return databaseIp;
    }

    public String getCoreIp() {
        return coreIp;
    }

    public int getCorePort() {
        return corePort;
    }

    public String getBaseArtworkUrl() {
        return baseArtworkUrl;
    }

    public String getBaseMediainfoUrl() {
        return baseMediainfoUrl;
    }

    public String getBasePhotoUrl() {
        return basePhotoUrl;
    }
    
    public String getBaseTrailerUrl() {
        return baseTrailerUrl;
    }

    public String getSkinDir() {
        return skinDir;
    }

    public Map<MetaDataType, Long> getCounts() {
        return counts;
    }

    public void setCounts(Map<MetaDataType, Long> counts) {
        this.counts = counts;
    }

    public void addCount(MetaDataType type, long count) {
        this.counts.put(type, count);
    }

    /**
     * Output the header information to the log file
     *
     */
    public void printHeader() {
        if (StringUtils.isNotBlank(projectName)) {
            // just print out if project name has been set
            LOG.info("{} {}", projectName, projectVersion);
            LOG.info("{} {}", StringUtils.repeat("~", projectName.length()), StringUtils.repeat("~", projectVersion.length()));
            LOG.info("{}", moduleName);
            LOG.info("");
            LOG.info("  Revision: {} {}", buildRevision, buildDirty ? "(custom build)" : "");
            LOG.info("Build Time: {}", getBuildDate());
            LOG.info("      Java: {}", javaVersion);
            LOG.info("");
        }
    }

    /**
     * Display some information on the system
     *
     */
    public void printSystemInfo() {
        LOG.info("Operating System: {}", osName);
        LOG.info("         Version: {}", osVersion);
        LOG.info("    Architecture: {}", osArch);
        LOG.info(" Processor Cores: {}", processorCores);
        LOG.info("");
        LOG.info("Core Start Time : {}", getStartUpTime());
        LOG.info("Core Uptime     : {}", getUptime());
    }

    /**
     * Calculate the database name and IP address from the connection URL
     */
    private void findDatabaseInfo() {
        try {
            String dbUrl = PropertyTools.getProperty("yamj3.database.url", "");
            if ("mysql".equals(System.getProperty("spring.profiles.active"))) {
                this.databaseName = "MySQL: " + dbUrl.substring(dbUrl.lastIndexOf('/') + 1);
                this.databaseIp = dbUrl.substring(dbUrl.indexOf("//") + 2, dbUrl.lastIndexOf('/'));
            } else if ("hsql".equals(System.getProperty("spring.profiles.active"))) {
                this.databaseName = "HSQL: yamj3"; 
                this.databaseIp = "localhost:9001";
            } else if (StringUtils.containsIgnoreCase(dbUrl, "derby")) {
                this.databaseName = "Derby: embedded";
                this.databaseIp = "localhost";
            } else if (dbUrl.contains("/") && StringUtils.containsIgnoreCase(dbUrl, "mysql")) {
                this.databaseName = "MySQL: " + dbUrl.substring(dbUrl.lastIndexOf('/') + 1);
                this.databaseIp = dbUrl.substring(dbUrl.indexOf("//") + 2, dbUrl.lastIndexOf('/'));
            } else if (StringUtils.containsIgnoreCase(dbUrl, "hsql")) {
                this.databaseName = "HSQL: InProc Server";
                this.databaseIp = "localhost";
            }   else {
                this.databaseName = UNKNOWN;
                this.databaseIp = UNKNOWN;
            }
        } catch (Exception ex) {
            LOG.warn("Failed to determine database name", ex);
            this.databaseName = UNKNOWN;
            this.databaseIp = UNKNOWN;
        }
    }

    /**
     * Create the URL to the web server based on the core IP address and port
     *
     * @param additionalPath
     * @return The generated URL
     */
    private String buildBaseUrl(String additionalPath) {
        try {
            StringBuilder path = new StringBuilder("/");
            path.append(FilenameUtils.normalize(additionalPath, true));
            if (!path.toString().endsWith("/")) {
                path.append("/");
            }
            URI uri = new URI("http", null, coreIp, corePort, path.toString(), null, null);
            return uri.toString();
        } catch (URISyntaxException ex) {
            LOG.warn("Failed to encode base URL: {}", ex.getMessage());
            LOG.trace("URI syntax error", ex);
            return "";
        }
    }
}
