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
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import javax.persistence.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;

/**
 * Abstract implementation of a scannable object.
 */
@MappedSuperclass
public abstract class AbstractScannable extends AbstractAuditable
        implements IScannable, Serializable {

    private static final long serialVersionUID = -8036305537317711196L;
    protected static final String SKIP_ALL = "all";
    
    /**
     * This will be generated from a scanned file name.
     */
    @NaturalId
    @Column(name = "identifier", length = 200, nullable = false)
    private String identifier;
    
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_scanned")
    private Date lastScanned;

    @Column(name = "retries", nullable = false)
    private int retries = 0;

    @Transient
    private Set<String> modifiedSources;
    
    // CONSTRUCTORS
    
    public AbstractScannable() {
        super();
    }
    
    public AbstractScannable(String identifier) {
        super();
        this.identifier = identifier;
    }
    
    // GETTER and SETTER
    
    public final String getIdentifier() {
        return identifier;
    }

    @SuppressWarnings("unused")
    private final void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public final StatusType getStatus() {
        return status;
    }
    
    public final void setStatus(StatusType status) {
        this.status = status;
    }

    @Override
    public final Date getLastScanned() {
        return lastScanned;
    }

    public final void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    @Override
    public final int getRetries() {
        return retries;
    }

    public final void setRetries(int retries) {
        this.retries = retries;
    }

    // TRANSIENT METHODS
    
    public final void addModifiedSource(String source) {
        if (!"all".equalsIgnoreCase(source)) {
            if (modifiedSources == null) {
                modifiedSources = new HashSet<>(1);
            }
            modifiedSources.add(source);
        }
    }

    public final void addModifiedSources(Set<String> sources) {
        if (CollectionUtils.isNotEmpty(sources)) {
            if (modifiedSources == null) {
                modifiedSources = new HashSet<>(sources.size());
            }
            modifiedSources.addAll(sources);
        }
    }

    public final boolean hasModifiedSource() {
        return CollectionUtils.isNotEmpty(modifiedSources);
    }
    
    public final Set<String> getModifiedSources() {
        return modifiedSources;
    }
    
    // SOURCE DB METHODS
    
    abstract Map<String, String> getSourceDbIdMap();
        
    @Override
    public final String getSourceDbId(String sourceDb) {
        return getSourceDbIdMap().get(sourceDb);
    }
    
    @Override
    public final boolean setSourceDbId(String sourceDb, String id) {
        if (StringUtils.isBlank(sourceDb) || StringUtils.isBlank(id)) {
            return false;
        }
        
        String newId = id.trim();
        String oldId = getSourceDbIdMap().put(sourceDb, newId);
        final boolean changed = !StringUtils.equals(oldId, newId);
        if (oldId != null && changed) {
            addModifiedSource(sourceDb);
        }
        return changed;
    }

    @Override
    public final boolean removeSourceDbId(String sourceDb) {
        if (getSourceDbIdMap().remove(sourceDb) != null) {
            addModifiedSource(sourceDb);
            return true;
        }
        return false;
    }
    
    abstract String getSkipScanApi();

    abstract void setSkipScanApi(String skipScanApi);
    
    public final boolean enableApiScan(String sourceDb) {
        // store the actual setting
        final String oldSkipScanApi = getSkipScanApi();
        
        // check if something has to be done
        if (sourceDb == null || oldSkipScanApi == null) {
            return false;
        }


        final String newSkipScanApi;
        if (SKIP_ALL.equalsIgnoreCase(sourceDb)) {
            newSkipScanApi = null;
        } else {
            HashSet<String> skipScans = new HashSet<>();
            for (String skipped : oldSkipScanApi.split(";")) {
                if (!skipped.equalsIgnoreCase(sourceDb)) {
                    // add skipped scan if not enabled
                    skipScans.add(skipped);
                }
            }
            
            if (skipScans.isEmpty()) {
                newSkipScanApi = null;
            } else {
                newSkipScanApi = StringUtils.join(skipScans, ';');
            }
        }
        setSkipScanApi(newSkipScanApi);

        // return true if something has changed 
        return !StringUtils.equalsIgnoreCase(oldSkipScanApi, newSkipScanApi);
    }

    public final boolean disableApiScan(String sourceDb) {
        if (sourceDb == null) {
            return false;
        }
        
        // store the actual setting
        final String oldSkipScanApi = getSkipScanApi();
        
        final String newSkipScanApi;
        if (SKIP_ALL.equalsIgnoreCase(sourceDb)) {
            newSkipScanApi = SKIP_ALL;
        } else if (getSkipScanApi() == null) {
            newSkipScanApi = sourceDb;
        } else if (SKIP_ALL.equalsIgnoreCase(oldSkipScanApi)) {
            // nothing to do if already all scans are skipped
            newSkipScanApi = SKIP_ALL;
        } else {
            final HashSet<String> skipScans = new HashSet<>(Arrays.asList(getSkipScanApi().split(";")));
            skipScans.add(sourceDb);
            newSkipScanApi = StringUtils.join(skipScans, ';');
        }
        setSkipScanApi(newSkipScanApi);

        // return true if something has changed 
        return !StringUtils.equalsIgnoreCase(oldSkipScanApi, newSkipScanApi);
    }

    // OVERRIDE METHODS

    abstract Map<OverrideFlag, String> getOverrideFlags();
    
    @Override
    public final void setOverrideFlag(OverrideFlag overrideFlag, String source) {
        getOverrideFlags().put(overrideFlag, source.toLowerCase());
    }

    protected final void removeOverrideFlag(OverrideFlag overrideFlag) {
        getOverrideFlags().remove(overrideFlag);
    }

    @Override
    public final String getOverrideSource(OverrideFlag overrideFlag) {
        return getOverrideFlags().get(overrideFlag);
    }

    protected final boolean hasOverrideSource(OverrideFlag overrideFlag, String sourceDb) {
        return StringUtils.equals(getOverrideSource(overrideFlag), sourceDb);
    }
    
    public final boolean removeOverrideSource(final String sourceDb) {
        boolean removed = false;
        for (Iterator<Entry<OverrideFlag, String>> it = getOverrideFlags().entrySet().iterator(); it.hasNext();) {
            Entry<OverrideFlag, String> e = it.next();
            if (StringUtils.equalsIgnoreCase(e.getValue(), sourceDb)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }
}
