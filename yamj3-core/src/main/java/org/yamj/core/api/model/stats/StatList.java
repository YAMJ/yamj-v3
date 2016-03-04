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
package org.yamj.core.api.model.stats;

import java.util.Collections;
import java.util.List;

/**
 * Abstract class for statistics.
 *
 * Should be instantiated with the specific methods for calculation of the first and last items as well as other statistics relevant
 * to the items stored.
 *
 * @author stuart.boston
 * @param <T>
 */
public abstract class StatList<T> {

    private List<T> items = Collections.emptyList();
    private T first = null;
    private T last = null;


    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public void addItem(T item) {
        this.items.add(item);
    }

    public boolean removeItem(T item) {
        return this.items.remove(item);
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public T getLast() {
        return last;
    }

    public void setLast(T last) {
        this.last = last;
    }
    //</editor-fold>

    /**
     * Get the total number of items in the list
     *
     * @return
     */
    public int getCount() {
        return items.size();
    }
}
