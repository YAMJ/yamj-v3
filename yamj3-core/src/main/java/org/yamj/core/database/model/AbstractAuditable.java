package org.yamj.core.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import javax.persistence.*;
import org.yamj.core.hibernate.Auditable;
import org.yamj.core.hibernate.Identifiable;

/**
 * Abstract implementation of an identifiable and auditable object.
 */
@MappedSuperclass
public abstract class AbstractAuditable implements Auditable, Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "create_timestamp", nullable = false, updatable = false)
    private Date createTimestamp;
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "update_timestamp")
    private Date updateTimestamp;
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

    public boolean isNewlyCreated() {
        return (this.id <= 0);
    }

    // GETTER and SETTER
    public Date getCreateTimestamp() {
        return this.createTimestamp;
    }

    public void setCreateTimestamp(final Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public Date getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public void setUpdateTimestamp(final Date updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }
}