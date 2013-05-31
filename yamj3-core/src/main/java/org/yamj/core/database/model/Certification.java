package org.yamj.core.database.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "certification")
public class Certification extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 5949467240717893584L;
    
    @NaturalId(mutable = true)
    @Column(name = "certification_text", nullable = false, length = 50)
    private String certificationText;
    
    @Column(name = "country", length = 100, nullable = false)
    private String country;

    // GETTER and SETTER
    
    public String getCertificationText() {
        return certificationText;
    }

    public void setCertificationText(String certificationText) {
        this.certificationText = certificationText;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        final int prime = 17;
        int result = 1;
        result = prime * result + (this.certificationText == null ? 0 : this.certificationText.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Certification)) {
            return false;
        }
        Certification castOther = (Certification) other;
        return StringUtils.equals(this.certificationText, castOther.certificationText);
    }
}
