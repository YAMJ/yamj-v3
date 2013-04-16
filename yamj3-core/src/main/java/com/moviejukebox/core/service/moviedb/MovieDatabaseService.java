package com.moviejukebox.core.service.moviedb;

import com.moviejukebox.core.database.dao.CommonDao;
import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.dao.PersonDao;
import com.moviejukebox.core.database.model.CastCrew;
import com.moviejukebox.core.database.model.Genre;
import com.moviejukebox.core.database.model.Person;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.database.model.dto.CreditDTO;
import com.moviejukebox.core.database.model.type.JobType;
import com.moviejukebox.core.database.model.type.StatusType;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("movieDatabaseService")
public class MovieDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieDatabaseService.class);

    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private CommonDao commonDao;
    
    private HashMap<String,IMovieScanner> registeredMovieScanner = new HashMap<String,IMovieScanner>();
    private HashMap<String,ISeasonScanner> registeredTvShowScanner = new HashMap<String,ISeasonScanner>();
    
    public void registerMovieScanner(IMovieScanner movieScanner) {
        registeredMovieScanner.put(movieScanner.getScannerName().toLowerCase(), movieScanner);
    }

    public void registerTvShowScanner(ISeasonScanner tvShowScanner) {
        registeredTvShowScanner.put(tvShowScanner.getScannerName().toLowerCase(), tvShowScanner);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void scanMetadata(Long id) {
        if (id == null) {
            // nothing to 
            return;
        }
        
        VideoData videoData = mediaDao.getVideoData(id);
        LOGGER.debug("Scanning video data for: " + videoData.getTitle());

        // SCAN
        
        // TODO use configured scanner only
        ScanResult scanResult = ScanResult.OK;
        for (IMovieScanner scanner : registeredMovieScanner.values()) {
            scanResult = scanner.scan(videoData);
        }
        
        // STORAGE
        
        // update genres
        HashSet<Genre> genres = new HashSet<Genre>(0);
        for (Genre genre : videoData.getGenres()) {
            Genre stored = commonDao.getGenre(genre.getName());
            if (stored != null) {
                genres.add(stored);
            } else {
                commonDao.saveEntity(genre);
                genres.add(genre);
            }
        }
        videoData.setGenres(genres);

        // update cast and crew
        updateCastCrew(videoData);
        
        // update video data and reset status
        if (ScanResult.OK.equals(scanResult)) {
            videoData.setStatus(StatusType.DONE);
        } else {
            videoData.setStatus(StatusType.ERROR);
        }
        mediaDao.updateEntity(videoData);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processingError(Long id) {
        VideoData videoData = mediaDao.getVideoData(id);
        if (videoData != null) {
            videoData.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(videoData);
        }
    }
    
    private void updateCastCrew(VideoData videoData) {
        for (CreditDTO dto : videoData.getCreditDTOS()) {
            Person person = null;
            CastCrew castCrew = null;
            
            for (CastCrew credit : videoData.getCredits()) {
                if ((credit.getJobType() == dto.getJobType()) && StringUtils.equalsIgnoreCase(dto.getName(), credit.getPerson().getName())) {
                    castCrew = credit;
                    person = credit.getPerson();
                    break;
                }
            }

            // find person if not found
            if (person == null) {
                person = personDao.getPerson(dto.getName());
            }
            
            if (person != null) {
                // update person id
                if (StringUtils.isNotBlank(dto.getMoviedb()) && StringUtils.isNotBlank(dto.getMoviedbId())) {
                    person.setPersonId(dto.getMoviedb(), dto.getMoviedbId());
                    personDao.updateEntity(person);
                }
            } else {
                // create new person
                person = new Person();
                person.setName(dto.getName());
                if (StringUtils.isNotBlank(dto.getMoviedb()) && StringUtils.isNotBlank(dto.getMoviedbId())) {
                    person.setPersonId(dto.getMoviedb(), dto.getMoviedbId());
                }
                personDao.saveEntity(person);
            }
            
            if (castCrew != null) {
                // update role
                if (StringUtils.isBlank(castCrew.getRole()) 
                    && JobType.ACTOR.equals(castCrew.getJobType())
                    && StringUtils.isNotBlank(dto.getRole())) 
                {
                    castCrew.setRole(dto.getRole());
                    personDao.updateEntity(castCrew);
                }
            } else {
                castCrew = new CastCrew();
                castCrew.setPerson(person);
                castCrew.setVideoData(videoData);
                castCrew.setJobType(dto.getJobType());
                if (StringUtils.isNotBlank(dto.getRole())) {
                    castCrew.setRole(dto.getRole());
                }
                videoData.addCredit(castCrew);
                personDao.saveEntity(castCrew);
            }
        }
    }
}
