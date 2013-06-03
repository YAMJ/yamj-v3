package org.yamj.core.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.yamj.core.hibernate.Identifiable;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Abstract implementation of an identifiable object.
 *
 * @author <a href="mailto:markus@bader-it.de">Markus Bader</a>
 */
@MappedSuperclass
public abstract class AbstractIdentifiable implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @JsonIgnore
    @Transient
    private String jsonCallback;

    @Override
    public long getId() {
        return this.id;
    }

    @SuppressWarnings("unused")
    private void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}