/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package org.yamj.core.api.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.SystemTools;
import org.yamj.core.tools.StringTools;

public class Skin {

    private static final Logger LOG = LoggerFactory.getLogger(Skin.class);
    // Skin version file
    private static final String SKIN_VERSION_FILENAME = "version.xml";
    // Properties
    private String sourceUrl = "";
    private String name = "";
    private String path = "";
    private List<String> description = new ArrayList<String>();
    private String image = "";
    private String version = "";
    private String skinDate = "";
    private Long fileDate = 0L;
    private String skinDir = "";

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getName() {
        if (StringUtils.isBlank(name)) {
            return path;
        } else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = FilenameUtils.normalize(path, Boolean.TRUE);
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSkinDate() {
        return skinDate;
    }

    public void setSkinDate(String skinDate) {
        this.skinDate = skinDate;
    }

    public Long getFileDate() {
        return fileDate;
    }

    public void setFileDate(Long fileDate) {
        this.fileDate = fileDate;
    }

    public String getSkinDir() {
        return skinDir;
    }

    public void setSkinDir(String skinDir) {
        this.skinDir = skinDir;
    }

    /**
     * Read the skin information from skinVersionFilename in the skin directory
     */
    public void readSkinInformation() {
        if (StringUtils.isBlank(path)) {
            LOG.warn("Skin path is empty, can't read skin information");
            return;
        }
        String skinVersionPath = FilenameUtils.concat(FilenameUtils.concat(skinDir, path), SKIN_VERSION_FILENAME);
        File xmlFile = new File(skinVersionPath);

        if (xmlFile.exists()) {
            LOG.debug("Scanning file '{}'", xmlFile.getAbsolutePath());
        } else {
            LOG.debug("'{}' does not exist, skipping", xmlFile.getAbsolutePath());
            return;
        }

        try {
            XMLConfiguration xmlConfig = new XMLConfiguration(xmlFile);
            setName(xmlConfig.getString("name"));
            setVersion(xmlConfig.getString("version"));
            setSkinDate(xmlConfig.getString("date"));
            setDescription(StringTools.castList(String.class, xmlConfig.getList("description")));
            setSourceUrl(xmlConfig.getString("url"));
            setImage(xmlConfig.getString("image"));
            setFileDate(xmlFile.lastModified());
        } catch (ConfigurationException error) {
            LOG.error("Failed reading version information file '{}'", SKIN_VERSION_FILENAME);
            LOG.warn(SystemTools.getStackTrace(error));
        } catch (Exception error) {
            LOG.error("Failed processing version information file '{}'", SKIN_VERSION_FILENAME);
            LOG.warn(SystemTools.getStackTrace(error));
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
