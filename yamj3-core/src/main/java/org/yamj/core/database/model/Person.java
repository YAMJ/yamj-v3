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

import static org.yamj.plugin.api.common.Constants.ALL;

import org.yamj.plugin.api.metadata.tools.MetadataTools;
import org.yamj.plugin.api.metadata.tools.PersonName;

import java.util.*;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;

@NamedQueries({    
    @NamedQuery(name = Person.QUERY_REQUIRED,
        query = "FROM Person p WHERE p.id = :id"
    ),
    @NamedQuery(name = Person.QUERY_ORPHANS,
        query = "SELECT p.id FROM Person p WHERE not exists (select 1 from CastCrew c where c.castCrewPK.person=p)"
    ),
    @NamedQuery(name = Person.UPDATE_RESCAN_ALL,
        query = "UPDATE Person SET status='UPDATED' WHERE status not in ('NEW','UPDATED')"
    ),
    @NamedQuery(name = Person.UPDATE_STATUS,
        query = "UPDATE Person SET status=:status WHERE id=:id"
    ),
    @NamedQuery(name = Person.UPDATE_STATUS_RECHECK,
        query = "UPDATE Person p set p.status='UPDATED',p.filmographyStatus='NEW' "+
                "WHERE p.status not in ('NEW','UPDATED') AND (p.lastScanned is null or p.lastScanned<=:compareDate)"
    ),
    @NamedQuery(name = Person.UPDATE_FILMOGRAPHY_STATUS,
        query = "UPDATE Person SET filmographyStatus=:status WHERE id=:id"
    )
})

@NamedNativeQueries({    
    @NamedNativeQuery(name = Person.QUERY_SCANNING_QUEUE,
        query = "SELECT p.id,'PERSON' as metatype,(case when p.update_timestamp is null then p.create_timestamp else p.update_timestamp end) as maxdate "+
                "FROM person p WHERE p.status in ('NEW','UPDATED') ORDER BY maxdate ASC"
    ),
    @NamedNativeQuery(name = Person.QUERY_FILMOGRAPHY_QUEUE,
        query = "SELECT p.id,'FILMOGRAPHY' as metatype,(case when p.update_timestamp is null then p.create_timestamp else p.update_timestamp end) as maxdate "+
                "FROM person p WHERE p.status='DONE' and (p.filmography_status is null or p.filmography_status in ('NEW','UPDATED'))  ORDER BY maxdate ASC"
    )
})

@Entity
@Table(name = "person",
        uniqueConstraints = @UniqueConstraint(name = "UIX_PERSON_NATURALID", columnNames = {"identifier"}),
        indexes = {
            @Index(name = "IX_PERSON_STATUS", columnList = "status"),
            @Index(name = "IX_PERSON_FILMOGRAPHY_STATUS", columnList = "filmography_status"),
            @Index(name = "IX_PERSON_NAME", columnList = "name")}
)
@SuppressWarnings("unused")
public class Person extends AbstractScannable {

    private static final long serialVersionUID = 660066902996412843L;
    public static final String QUERY_REQUIRED = "person.required";
    public static final String QUERY_ORPHANS = "person.orphans";
    public static final String UPDATE_RESCAN_ALL = "person.rescanAll";
    public static final String UPDATE_STATUS = "person.updateStatus";
    public static final String UPDATE_STATUS_RECHECK = "person.updateStatus.forRecheck";
    public static final String UPDATE_FILMOGRAPHY_STATUS = "person.updateFilmographyStatus";
    public static final String QUERY_SCANNING_QUEUE = "person.scanning.queue";
    public static final String QUERY_FILMOGRAPHY_QUEUE = "person.filmography.queue";
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Temporal(value = TemporalType.DATE)
    @Column(name = "birth_day")
    private Date birthDay;

    @Column(name = "birth_place", length = 255)
    private String birthPlace;

    @Column(name = "birth_name", length = 255)
    private String birthName;

    @Temporal(value = TemporalType.DATE)
    @Column(name = "death_day")
    private Date deathDay;

    @Column(name = "death_place", length = 255)
    private String deathPlace;

    @Lob
    @Column(name = "biography", length = 50000)
    private String biography;

    @Column(name = "skip_scan_api", length = 255)
    private String skipScanApi;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "person_ids", 
            joinColumns = @JoinColumn(name = "person_id"), foreignKey = @ForeignKey(name = "FK_PERSON_SOURCEIDS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 40)
    private Map<String, String> sourceDbIdMap = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "person_override",
            joinColumns = @JoinColumn(name = "person_id"), foreignKey = @ForeignKey(name = "FK_PERSON_OVERRIDE"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<>(OverrideFlag.class);

    @Type(type = "statusType")
    @Column(name = "filmography_status", nullable = false, length = 30)
    private StatusType filmographyStatus;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
    private Set<FilmParticipation> filmography = new HashSet<>(0);

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
    private Artwork photo;

    @Transient
    private Set<FilmParticipation> newFilmography = new HashSet<>(0);

    // CONSTRUCTORS
    
    public Person() {
        super();
    }

    public Person(String identifier) {
        super(identifier);
    }

    // GETTER and SETTER

    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    public void setName(String name, String source) {
        if (StringUtils.isNotBlank(name)) {
            setName(MetadataTools.fixScannedValue(name.trim()));
            setOverrideFlag(OverrideFlag.NAME, source);
        }
    }

    public void removeName(String source) {
        if (hasOverrideSource(OverrideFlag.NAME, source)) {
            setName(getIdentifier());
            removeOverrideFlag(OverrideFlag.NAME);
        }
    }

    public String getFirstName() {
        return firstName;
    }

    private void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setFirstName(String firstName, String source) {
        if (StringUtils.isNotBlank(firstName)) {
            setFirstName(MetadataTools.fixScannedValue(firstName.trim()));
            setOverrideFlag(OverrideFlag.FIRSTNAME, source);
        }
    }

    public void removeFirstName(String source) {
        if (hasOverrideSource(OverrideFlag.FIRSTNAME, source)) {
            PersonName dto = MetadataTools.splitFullName(getIdentifier());
            setFirstName(dto.getFirstName());
            removeOverrideFlag(OverrideFlag.FIRSTNAME);
        }
    }

    public String getLastName() {
        return lastName;
    }

    private void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setLastName(String lastName, String source) {
        if (StringUtils.isNotBlank(lastName)) {
            setLastName(MetadataTools.fixScannedValue(lastName.trim()));
            setOverrideFlag(OverrideFlag.LASTNAME, source);
        }
    }

    public void removeLastName(String source) {
        if (hasOverrideSource(OverrideFlag.LASTNAME, source)) {
            PersonName dto = MetadataTools.splitFullName(getIdentifier());
            setLastName(dto.getLastName());
            removeOverrideFlag(OverrideFlag.LASTNAME);
        }
    }
    
    public Date getBirthDay() {
        return birthDay;
    }

    private void setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
    }

    public void setBirthDay(Date birthDay, String source) {
        if (birthDay != null) {
            setBirthDay(birthDay);
            setOverrideFlag(OverrideFlag.BIRTHDAY, source);
        }
    }

    public void removeBirthDay(String source) {
        if (hasOverrideSource(OverrideFlag.BIRTHDAY, source)) {
            setBirthDay(null);
            removeOverrideFlag(OverrideFlag.BIRTHDAY);
        }
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    private void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public void setBirthPlace(String birthPlace, String source) {
        if (StringUtils.isNotBlank(birthPlace)) {
            setBirthPlace(MetadataTools.fixScannedValue(birthPlace.trim()));
            setOverrideFlag(OverrideFlag.BIRTHPLACE, source);
        }
    }

    public void removeBirthPlace(String source) {
        if (hasOverrideSource(OverrideFlag.BIRTHPLACE, source)) {
            setBirthPlace(null);
            removeOverrideFlag(OverrideFlag.BIRTHPLACE);
        }
    }

    public String getBirthName() {
        return birthName;
    }

    private void setBirthName(String birthName) {
        this.birthName = birthName;
    }

    public void setBirthName(String birthName, String source) {
        if (StringUtils.isNotBlank(birthName)) {
            setBirthName(MetadataTools.fixScannedValue(birthName.trim()));
            setOverrideFlag(OverrideFlag.BIRTHNAME, source);
        }
    }

    public  void removeBirthName(String source) {
        if (hasOverrideSource(OverrideFlag.BIRTHNAME, source)) {
            setBirthName(null);
            removeOverrideFlag(OverrideFlag.BIRTHNAME);
        }
    }

    public Date getDeathDay() {
        return deathDay;
    }

    private void setDeathDay(Date deathDay) {
        this.deathDay = deathDay;
    }

    public void setDeathDay(Date deathDay, String source) {
        if (deathDay != null) {
            setDeathDay(deathDay);
            setOverrideFlag(OverrideFlag.DEATHDAY, source);
        }
    }

    public void removeDeathDay(String source) {
        if (hasOverrideSource(OverrideFlag.DEATHDAY, source)) {
            setDeathDay(null);
            removeOverrideFlag(OverrideFlag.DEATHDAY);
        }
    }

    public String getDeathPlace() {
        return deathPlace;
    }

    private void setDeathPlace(String deathPlace) {
        this.deathPlace = deathPlace;
    }

    public void setDeathPlace(String deathPlace, String source) {
        if (StringUtils.isNotBlank(deathPlace)) {
            setDeathPlace(MetadataTools.fixScannedValue(deathPlace.trim()));
            setOverrideFlag(OverrideFlag.DEATHPLACE, source);
        }
    }

    public void removeDeathPlace(String source) {
        if (hasOverrideSource(OverrideFlag.DEATHPLACE, source)) {
            setDeathPlace(null);
            removeOverrideFlag(OverrideFlag.DEATHPLACE);
        }
    }

    public String getBiography() {
        return biography;
    }

    private void setBiography(String biography) {
        this.biography = biography;
    }

    public void setBiography(String biography, String source) {
        if (StringUtils.isNotBlank(biography)) {
            setBiography(MetadataTools.fixScannedValue(biography.trim()));
            setOverrideFlag(OverrideFlag.BIOGRAPHY, source);
        }
    }

    public void removeBiography(String source) {
        if (hasOverrideSource(OverrideFlag.BIOGRAPHY, source)) {
            setBiography(null);
            removeOverrideFlag(OverrideFlag.BIOGRAPHY);
        }
    }

    @Override
    protected Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    private void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
        this.sourceDbIdMap = sourceDbIdMap;
    }

    @Override
    protected Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    private void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    @Override
    protected String getSkipScanApi() {
        return skipScanApi;
    }

    @Override
    protected void setSkipScanApi(String skipScanApi) {
        this.skipScanApi = skipScanApi;
    }

    @Override
    public boolean isAllScansSkipped() {
        return ALL.equalsIgnoreCase(getSkipScanApi());
    }

    @Override
    public boolean isSkippedScan(String sourceDb) {
        return StringUtils.containsIgnoreCase(getSkipScanApi(), sourceDb);
    }
    
    public StatusType getFilmographyStatus() {
        return filmographyStatus;
    }

    public void setFilmographyStatus(StatusType filmographyStatus) {
        this.filmographyStatus = filmographyStatus;
    }

    public Set<FilmParticipation> getFilmography() {
        return filmography;
    }

    public void setFilmography(Set<FilmParticipation> filmography) {
        this.filmography = filmography;
    }

    public Artwork getPhoto() {
        return photo;
    }

    public void setPhoto(Artwork photo) {
        this.photo = photo;
    }

    // TRANSIENT METHODS
    
    public Set<FilmParticipation> getNewFilmography() {
        return newFilmography;
    }

    public void setNewFilmography(Set<FilmParticipation> newFilmography) {
        this.newFilmography = newFilmography;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getIdentifier())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Person)) {
            return false;
        }
        Person other = (Person) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getIdentifier(), other.getIdentifier())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Person [ID=");
        sb.append(getId());
        sb.append(", identifier=");
        sb.append(getIdentifier());
        sb.append(", name=");
        sb.append(getName());
        sb.append("]");
        return sb.toString();
    }
}
