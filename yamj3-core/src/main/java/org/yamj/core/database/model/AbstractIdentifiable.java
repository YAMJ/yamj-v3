package org.yamj.core.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import org.yamj.core.hibernate.Identifiable;

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
}