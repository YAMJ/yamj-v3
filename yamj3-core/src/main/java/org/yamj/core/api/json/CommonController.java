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
package org.yamj.core.api.json;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.dto.ApiAwardDTO;
import org.yamj.core.api.model.dto.ApiBoxedSetDTO;
import org.yamj.core.api.model.dto.ApiNameDTO;
import org.yamj.core.api.model.dto.ApiRatingDTO;
import org.yamj.core.api.model.dto.ApiTargetDTO;
import org.yamj.core.api.options.OptionsBoxedSet;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.api.options.OptionsMultiType;
import org.yamj.core.api.options.OptionsRating;
import org.yamj.core.api.options.OptionsSingleType;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Country;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@ResponseBody
@RequestMapping(value = "/api/**", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
public class CommonController {

    private static final Logger LOG = LoggerFactory.getLogger(CommonController.class);
    @Autowired
    private JsonApiStorageService jsonApi;

    //<editor-fold defaultstate="collapsed" desc="Watched Methods">
    @RequestMapping("/watched")
    public ApiStatus markWatched(
            @RequestParam(required = true, defaultValue = "") String filename,
            @RequestParam(required = false, defaultValue = "-1") Integer amount) {

        int percentage;
        if (amount < 0) {
            percentage = 0;
        } else if (amount > 100) {
            percentage = 100;
        } else {
            percentage = amount;
        }

        LOG.info("Received watched command for filename '{}' to amount '{}'", filename, percentage);
        // TODO: Add write to database command
        if (StringUtils.isBlank(filename)) {
            return new ApiStatus(400, "No filename for watched command");
        } else {
            return new ApiStatus(200, "Watch command successful");
        }
    }

    @RequestMapping("/watched/movie/{id}")
    public ApiStatus markWatchedMovie(@ModelAttribute("options") OptionsId options) {
        return updateWatched(options.getId(), true);
    }

    @RequestMapping("/unwatched/movie/{id}")
    public ApiStatus markUnwatchedMovie(@ModelAttribute("options") OptionsId options) {
        return updateWatched(options.getId(), false);
    }

    private ApiStatus updateWatched(Long id, boolean watched) {
        if (id != null && id > 0L) {
            VideoData video = jsonApi.getVideoData(id);

            // Check to see if the status is the same
            if (video.isWatchedApi() == watched) {
                LOG.info("Watched status for {}-{} is already {}, not changing", video.getId(), video.getTitle(), watched(watched));
                return new ApiStatus(200, "Watched status of '" + watched(watched) + "' unchanged");
            }

            LOG.info("Setting watched status for {}-{} to {} from {}",
                    video.getId(),
                    video.getTitle(),
                    watched(watched),
                    watched(video.isWatchedApi()));

            video.setWatchedApi(watched);
            jsonApi.updateVideoData(video);

            return new ApiStatus(200, "Sucessfully update watch status for " + video.getId() + "-" + video.getTitle() + " to " + watched(video.isWatchedApi()));
        } else {
            return new ApiStatus(400, "No ID provided");
        }
    }

    private String watched(boolean watched) {
        return watched ? "watched" : "unwatched";
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre Methods">
    @RequestMapping("/genre")
    public ApiWrapperList<ApiTargetDTO> getGenreFilename(@RequestParam(required = true, defaultValue = "") String filename) {
        LOG.info("Getting genres for filename '{}'", filename);
        ApiWrapperList<ApiTargetDTO> wrapper = new ApiWrapperList<>();
        List<ApiTargetDTO> results = jsonApi.getGenreFilename(wrapper, filename);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping("/genre/{name}")
    public ApiWrapperSingle<ApiTargetDTO> getGenre(@PathVariable String name) {
        Genre genre;
        ApiWrapperSingle<ApiTargetDTO> wrapper = new ApiWrapperSingle<>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting genre with ID '{}'", name);
            genre = jsonApi.getGenre(Long.parseLong(name));
        } else {
            LOG.info("Getting genre with name '{}'", name);
            genre = jsonApi.getGenre(name);
        }
        if (genre != null) {
            wrapper.setResult(new ApiTargetDTO(genre));
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping("/genres/list")
    public ApiWrapperList<ApiTargetDTO> getGenres(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting genre list: used={}, full={}", options.getUsed(), options.getFull());

        ApiWrapperList<ApiTargetDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        List<ApiTargetDTO> results = jsonApi.getGenres(wrapper);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping("/genres/add")
    public ApiStatus genreAdd(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = true, defaultValue = "") String target) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(target)) {
            LOG.info("Adding genre '{}' with target '{}'", name, target);
            boolean result = this.jsonApi.addGenre(name, target);
            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully added genre '" + name + "' with target '" + target + "'");
            } else {
                status.setStatus(400);
                status.setMessage("Genre '" + name + "' already exists");
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name/target specified, genre not added");
        }
        return status;
    }

    @RequestMapping("/genres/update")
    public ApiStatus genreUpdate(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "") String target) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name)) {
            LOG.info("Updating genre '{}' with target '{}'", name, target);

            boolean result;
            if (StringUtils.isNumeric(name)) {
                result = this.jsonApi.updateGenre(Long.valueOf(name), target);
            } else {
                result = this.jsonApi.updateGenre(name, target);
            }

            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully updated genre '" + name + "' with target '" + target + "'");
            } else {
                status.setStatus(400);
                status.setMessage("Genre '" + name + "' does not exist");
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name specified, genre not updated");
        }
        return status;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Studio Methods">
    @RequestMapping("/studio/{name}")
    public ApiWrapperSingle<Studio> getStudio(@PathVariable String name) {
        Studio studio;
        ApiWrapperSingle<Studio> wrapper = new ApiWrapperSingle<>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting studio with ID '{}'", name);
            studio = jsonApi.getStudio(Long.parseLong(name));
        } else {
            LOG.info("Getting studio '{}'", name);
            studio = jsonApi.getStudio(name);
        }
        wrapper.setResult(studio);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping("/studios/list")
    public ApiWrapperList<Studio> getStudios(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting studio list with {}", options.toString());

        ApiWrapperList<Studio> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        List<Studio> results = jsonApi.getStudios(wrapper);
        wrapper.setResults(results);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Country Methods">
    @RequestMapping("/country")
    public ApiWrapperList<ApiTargetDTO> getCountryFilename(@RequestParam(required = true, defaultValue = "") String filename) {
        LOG.info("Getting countries for filename '{}'", filename);
        ApiWrapperList<ApiTargetDTO> wrapper = new ApiWrapperList<>();
        List<ApiTargetDTO> results = jsonApi.getCountryFilename(wrapper, filename);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping("/country/{name}")
    public ApiWrapperSingle<ApiTargetDTO> getCountry(@PathVariable String name) {
        Country country;
        ApiWrapperSingle<ApiTargetDTO> wrapper = new ApiWrapperSingle<>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting country with ID '{}'", name);
            country = jsonApi.getCountry(Long.parseLong(name));
        } else {
            LOG.info("Getting country with name '{}'", name);
            country = jsonApi.getCountry(name);
        }
        if (country != null) {
            wrapper.setResult(new ApiTargetDTO(country));
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping("/countries/list")
    public ApiWrapperList<ApiTargetDTO> getCountries(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting contries list: used={}, full={}", options.getUsed(), options.getFull());

        ApiWrapperList<ApiTargetDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        List<ApiTargetDTO> results = jsonApi.getCountries(wrapper);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping("/countries/add")
    public ApiStatus countryAdd(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = true, defaultValue = "") String target) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(target)) {
            LOG.info("Adding country '{}' with target '{}'", name, target);
            boolean result = this.jsonApi.addCountry(name, target);
            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully added country '" + name + "' with target '" + target + "'");
            } else {
                status.setStatus(400);
                status.setMessage("Country '" + name + "' already exists");
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name/target specified, country not added");
        }
        return status;
    }

    @RequestMapping("/countries/update")
    public ApiStatus countryUpdate(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "") String target) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name)) {
            LOG.info("Updating country '{}' with target '{}'", name, target);

            boolean result;
            if (StringUtils.isNumeric(name)) {
                result = this.jsonApi.updateCountry(Long.valueOf(name), target);
            } else {
                result = this.jsonApi.updateCountry(name, target);
            }

            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully updated country '" + name + "' with target '" + target + "'");
            } else {
                status.setStatus(400);
                status.setMessage("Country '" + name + "' does not exist");
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name specified, country not updated");
        }
        return status;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Award Methods">
    @RequestMapping("/awards/list")
    public ApiWrapperList<ApiAwardDTO> getAwards(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting award list with {}", options.toString());

        ApiWrapperList<ApiAwardDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getAwards(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Certification Methods">
    @RequestMapping("/certifications/list")
    public ApiWrapperList<Certification> getCertifications(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting certifications list with {}", options.toString());

        ApiWrapperList<Certification> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getCertifications(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="VideoSource Methods">
    @RequestMapping("/videosources/list")
    public ApiWrapperList<ApiNameDTO> getVideoSources(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting video sources list with {}", options.toString());

        ApiWrapperList<ApiNameDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getVideoSources(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Rating Methods">
    @RequestMapping("/ratings/list")
    public ApiWrapperList<ApiRatingDTO> getRatings(@ModelAttribute("options") OptionsRating options) {
        LOG.info("Getting ratings list with {}", options.toString());

        ApiWrapperList<ApiRatingDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getRatings(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Alphabetical Methods">
    @RequestMapping("/alphabetical/list")
    public ApiWrapperList<ApiNameDTO> getAlphabeticals(@ModelAttribute("options") OptionsMultiType options) {
        LOG.info("Getting alphabetical list with {}", options.toString());

        ApiWrapperList<ApiNameDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getAlphabeticals(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Boxed-Set Methods">
    @RequestMapping("/boxset/list")
    public ApiWrapperList<ApiBoxedSetDTO> getBoxSets(@ModelAttribute("options") OptionsBoxedSet options) {
        LOG.info("Getting boxset list with {}", options.toString());

        ApiWrapperList<ApiBoxedSetDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getBoxedSets(wrapper));
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }

    @RequestMapping("/boxset/{id}")
    public ApiWrapperSingle<ApiBoxedSetDTO> getBoxSet(@ModelAttribute("options") OptionsBoxedSet options) {
        LOG.info("Getting boxset with {}", options.toString());

        ApiWrapperSingle<ApiBoxedSetDTO> wrapper = new ApiWrapperSingle<>();
        wrapper.setOptions(options);
        wrapper.setResult(jsonApi.getBoxedSet(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>
}
