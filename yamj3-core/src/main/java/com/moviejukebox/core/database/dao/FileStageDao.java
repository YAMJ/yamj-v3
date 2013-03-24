package com.moviejukebox.core.database.dao;

import com.moviejukebox.core.database.model.FileStage;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;
import org.springframework.stereotype.Service;

@Service("fileStageDao")
public class FileStageDao extends ExtendedHibernateDaoSupport {

    public FileStage getFileStage(final long id) {
        return this.getHibernateTemplate().get(FileStage.class, id);
    }

    public void deleteFileStage(final long id) {
        FileStage fileStage = getFileStage(id);
        if (fileStage != null) {
            this.deleteEntity(fileStage);
        }
    }
}
