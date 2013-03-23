package com.moviejukebox.core.database;

import java.util.Date;

import javax.annotation.Resource;

import com.moviejukebox.core.database.model.Artwork;
import com.moviejukebox.core.database.model.Movie;
import com.moviejukebox.core.database.model.VideoFile;
import com.moviejukebox.core.database.model.VideoFilePart;
import com.moviejukebox.core.database.model.type.ArtworkType;
import com.moviejukebox.core.database.model.type.FormatType;
import com.moviejukebox.core.database.model.type.MovieType;
import com.moviejukebox.core.database.model.type.OverrideFlag;
import com.moviejukebox.core.database.service.MovieService;
import com.moviejukebox.core.database.spring.ApplicationContextLoader;

public class Main {

    @Resource(name="movieService")
    private MovieService movieService;

    public static void main(String[] args) {
        Main main = new Main();
        new ApplicationContextLoader().load(main, "META-INF/spring/applicationContext.xml");
    
        Movie movie = new Movie();
        movie.setBaseFilename("Avatar 2009");
        movie.setBaseName("Avatar_2009");
        movie.setMovieType(MovieType.MOVIE);
        movie.setFormatType(FormatType.FILE);
        movie.setTitle("Avatar");
        movie.setYear("2009");
        movie.setBaseFilename("Avatar 2000");
        movie.setMjbRevision("r3567");
        movie.addGenre("Action");
        movie.addGenre("Adventure");
        movie.setCertification("12");
        movie.getOverrideFlags().put(OverrideFlag.TITLE, "imdb");

        VideoFile videoFile = new VideoFile();
        videoFile.setNumberParts(1);
        videoFile.setFirstPart(1);
        videoFile.setLastPart(1);
        videoFile.setFileSize(123456L);
        videoFile.setFileDate(new Date());
        movie.addVideoFile(videoFile);
        
        VideoFilePart videoPart = new VideoFilePart();
        videoPart.setTitle("Part 1");
        videoPart.setPart(1);
        videoFile.addVideoPart(videoPart);
        
        Artwork videoimage = new Artwork();
        videoimage.setType(ArtworkType.VIDEOIMAGE);
        videoimage.setFilename("my_artwork");
        videoPart.setVideoimage(videoimage);
        
        main.movieService.storeMovie(movie);

        // DELETE
        //main.movieService.deleteMovie(movie);
    }
}
