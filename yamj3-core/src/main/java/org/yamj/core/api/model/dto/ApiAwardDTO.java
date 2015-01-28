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
package org.yamj.core.api.model.dto;

public class ApiAwardDTO {

    private String event;
    private int year;
    private String award;
    private String source;
    
    public String getEvent() {
        return event;
    }
  
    public void setEvent(String event) {
        this.event = event;
    }
  
    public int getYear() {
        return year;
    }
  
    public void setYear(int year) {
        this.year = year;
    }
  
    public String getAward() {
        return award;
    }
  
    public void setAward(String award) {
        this.award = award;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }  
}
