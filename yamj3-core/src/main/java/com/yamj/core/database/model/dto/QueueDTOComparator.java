package com.yamj.core.database.model.dto;

import java.io.Serializable;
import java.util.Comparator;

public class QueueDTOComparator implements Comparator<QueueDTO>, Serializable {

    private static final long serialVersionUID = 3538761237411750316L;

    @Override
    public int compare(QueueDTO o1, QueueDTO o2) {
        if (o1.getDate() == null && o2.getDate() == null) {
            return 0;
        }
        if (o1.getDate() != null && o2.getDate() == null) {
            return 1;
        }
        if (o1.getDate() == null && o2.getDate() != null) {
            return -11;
        }
        return o1.getDate().compareTo(o2.getDate());
    }
}
