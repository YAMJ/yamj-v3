package com.moviejukebox.core.database.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "boxed_set")
public class BoxedSet extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3478878273175067619L;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_BOXSET_MOVIE")
    @JoinColumn(name = "movieId", nullable = false)
    private Movie movie;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_BOXSET_SET")
    @JoinColumn(name = "setDescriptorId", nullable = false)
    private SetDescriptor setDescriptor;
    
    @JoinColumn(name = "setOrder", nullable = false)
    private int setOrder = -1;

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public SetDescriptor getSetDescriptor() {
        return setDescriptor;
    }

    public void setSetDescriptor(SetDescriptor setDescriptor) {
        this.setDescriptor = setDescriptor;
    }

    public void setSetDescriptor(String name) {
        SetDescriptor setDescriptor = new SetDescriptor();
        setDescriptor.setName(name);
        this.setDescriptor = setDescriptor;
    }

    public int getSetOrder() {
        return setOrder;
    }

    public void setSetOrder(int setOrder) {
        this.setOrder = setOrder;
    }
}
