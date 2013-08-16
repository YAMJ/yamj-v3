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
package org.yamj.core.api.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.yamj.core.api.model.DataItem;

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
public class OptionsIndexPerson extends OptionsId {

    private String job = "";
    private String dataitems = "";
    @JsonIgnore
    List<DataItem> dataitemList = new ArrayList<DataItem>();

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    @JsonIgnore
    public List<String> getJobList() {
        return splitList(job);
    }

    public String getDataitems() {
        return dataitems;
    }

    public void setDataitems(String dataitems) {
        this.dataitems = dataitems;
        dataitemList.clear();
    }

    /**
     * Split the additionalDataItems into a list of DataItems
     *
     * @return
     */
    public List<DataItem> splitDataitems() {
        if (CollectionUtils.isEmpty(dataitemList)) {
            for (String item : StringUtils.split(dataitems, ",")) {
                DataItem di = DataItem.fromString(item);
                if (di != DataItem.UNKNOWN) {
                    dataitemList.add(di);
                }
            }
        }
        return dataitemList;
    }
}
