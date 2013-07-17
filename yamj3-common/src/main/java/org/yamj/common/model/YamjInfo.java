/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.lang.management.ManagementFactory;
import java.util.EnumMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.DateTimeTools;
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
    private String projectName;
    private String projectVersion;
    private String moduleName;
    private String moduleDescription;
    private DateTime buildDateTime;
    private String buildRevision;
    private int processorCores;
    private String javaVersion;
    private String osArch;
    private String osName;
    private String osVersion;
    private DateTime startUpDateTime;
    private Map<MetaDataType, Long> counts;
    private String databaseIp;
    private String databaseName;
    private String ipAddress;

    @SuppressWarnings("unused")
    private YamjInfo() {
    }

    @SuppressWarnings("rawtypes")
    public YamjInfo(Class myClass) {
        // YAMJ Stuff
        this.projectName = myClass.getPackage().getImplementationVendor();
        this.projectVersion = myClass.getPackage().getImplementationVersion();
        this.moduleName = myClass.getPackage().getImplementationTitle();
        this.moduleDescription = myClass.getPackage().getSpecificationTitle();
        if (myClass.getPackage().getSpecificationVendor() != null) {
            this.buildDateTime = DateTimeTools.parseDate(myClass.getPackage().getSpecificationVendor(), DateTimeTools.BUILD_FORMAT);
        } else {
            this.buildDateTime = new DateTime();
        }
        this.buildRevision = myClass.getPackage().getSpecificationVersion();

        // System Stuff
        this.processorCores = Runtime.getRuntime().availableProcessors();
        this.javaVersion = SystemUtils.JAVA_VERSION;
        this.osArch = SystemUtils.OS_ARCH;
        this.osName = SystemUtils.OS_NAME;
        this.osVersion = SystemUtils.OS_VERSION;

        // Times
        this.startUpDateTime = new DateTime(ManagementFactory.getRuntimeMXBean().getStartTime());

        // Counts
        this.counts = new EnumMap<MetaDataType, Long>(MetaDataType.class);

        // IP Address
        this.ipAddress = SystemTools.getIpAddress(Boolean.TRUE);

        // Database IP & Name
        findDatabaseInfo();
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    @JsonIgnore //Ignore this for JSON output (use the String version instead
    public DateTime getBuildDateTime() {
        return buildDateTime;
    }

    @JsonIgnore //Ignore this for JSON output (use the String version instead
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
        return DateTimeTools.convertDateToString(buildDateTime, DateTimeTools.BUILD_FORMAT);
    }

    public String getStartUpTime() {
        return DateTimeTools.convertDateToString(startUpDateTime, DateTimeTools.BUILD_FORMAT);
    }

    public String getUptime() {
        return DateTimeTools.formatDurationText(ManagementFactory.getRuntimeMXBean().getUptime());
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseIp() {
        return databaseIp;
    }

    public String getIpAddress() {
        return ipAddress;
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
    public void printHeader(Logger log) {
        if (StringUtils.isNotBlank(projectName)) {
            // just print out if project name has been set
            log.info("{} {}", projectName, projectVersion);
            log.info("{} {}", StringUtils.repeat("~", projectName.length()), StringUtils.repeat("~", projectVersion.length()));
            log.info("{}", moduleName);
            log.info("");
            log.info("  Revision: {}", buildRevision);
            log.info("Build Time: {}", getBuildDate());
            log.info("      Java: {}", javaVersion);
            log.info("");
        }
    }

    /**
     * Output the header information to the log file
     *
     */
    public void printHeader() {
        printHeader(LOG);
    }

    /**
     * Display some information on the system
     */
    public void printSystemInfo() {
        printSystemInfo(LOG);
    }

    /**
     * Display some information on the system
     *
     * @param log
     */
    public void printSystemInfo(Logger log) {
        log.info("Operating System: {}", osName);
        log.info("         Version: {}", osVersion);
        log.info("    Architecture: {}", osArch);
        log.info(" Processor Cores: {}", processorCores);
        log.info("");
        log.info("Core Start Time : {}", getStartUpTime());
        log.info("Core Uptime     : {}", getUptime());
    }

    private void findDatabaseInfo() {
        String dbUrl = PropertyTools.getProperty("yamj3.database.url", "");
        if (StringUtils.containsIgnoreCase(dbUrl, "derby")) {
            this.databaseName = "Derby Embedded";
            this.databaseIp = "localhost";
        } else if (dbUrl.contains("/") && StringUtils.containsIgnoreCase(dbUrl, "mysql")) {
            this.databaseName = dbUrl.substring(dbUrl.lastIndexOf('/') + 1);
            this.databaseIp = dbUrl.substring(dbUrl.indexOf("//") + 2, dbUrl.lastIndexOf('/'));
        } else {
            this.databaseName = "UNKNOWN";
            this.databaseIp = "UNKNOWN";
        }
    }
}
