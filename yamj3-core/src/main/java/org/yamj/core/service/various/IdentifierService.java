/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
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
package org.yamj.core.service.various;

import static org.apache.commons.lang3.text.WordUtils.capitalize;
import static org.yamj.core.tools.YamjTools.splitFullName;
import static org.yamj.plugin.api.metadata.MetadataTools.isVoiceRole;

import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.tools.PersonName;
import org.yamj.plugin.api.model.type.JobType;
import org.yamj.plugin.api.transliteration.Transliterator;
import ro.fortsoft.pf4j.PluginManager;

@Service("identifierService")
@DependsOn("pluginManager")
public class IdentifierService {

    private static final Logger LOG = LoggerFactory.getLogger(IdentifierService.class);
    private static final Pattern CLEAN_STRING_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-\\(\\)]");
    private static final char[] CLEAN_DELIMITERS = new char[]{'.', ' ', '_', '-'};

    @Autowired
    private PluginManager pluginManager;
    
    @Value("${yamj3.transliterate:false}")
    private boolean transliterationEnabled;

    private Transliterator transliterator;
    
    @PostConstruct
    public void init() {
        LOG.trace("Initialize identifier service");

        if (transliterationEnabled) {
            for (Transliterator transliter : pluginManager.getExtensions(Transliterator.class)) {
                if (this.transliterator == null) {
                    this.transliterator = transliter;
                    LOG.info("Use transliterator: {}", transliter.getClass().getName());
                } else {
                    LOG.warn("Another transliterator present, but ignored: {}", transliter.getClass().getName()); 
                }
            }
        
            if (this.transliterator == null) {
                LOG.info("No transliteration service present: no transliteration could be done");
                // transliteration not possible; regardless what is configured
                this.transliterationEnabled = false;
            }
        } else {
            LOG.info("Transliteration is not enabled");
        }
    }
    
    public String cleanIdentifier(final String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        
        String result = input;
        if (this.transliterationEnabled) {
            result = transliterator.transliterate(result);
        }
        
        // format ß to ss
        result = result.replaceAll("ß", "ss");
        // remove all accents from letters
        result = StringUtils.stripAccents(result);
        // capitalize first letter
        result = capitalize(result, CLEAN_DELIMITERS);
        // remove punctuation and symbols
        result = result.replaceAll("[\\p{Po}|\\p{S}]", "");
        // just leave characters and digits
        result = CLEAN_STRING_PATTERN.matcher(result).replaceAll(" ").trim();
        // remove double whitespaces
        result = result.replaceAll("( )+", " ").trim();
        
        return result;
    }
    
    public CreditDTO createCredit(final String source, final JobType jobType, final String name) {
        return createCredit(source, null, jobType, name);
    }

    public CreditDTO createCredit(final String source, final String sourceId, final JobType jobType, final String name) {
        final String trimmedName = StringUtils.trimToNull(name);
        if (trimmedName == null) {
            return null;
        }
        
        final String identifier = cleanIdentifier(trimmedName);
        if (StringUtils.isBlank(identifier)) {
            LOG.warn("Empty identifier for {} {} '{}'", source, jobType.name().toLowerCase(), trimmedName);
            return null;
        }

        CreditDTO credit = new CreditDTO(source, sourceId, jobType, identifier, trimmedName);
        final PersonName personName = splitFullName(trimmedName);
        credit.setFirstName(personName.getFirstName());
        credit.setLastName(personName.getLastName());
        return credit;
    }

    public CreditDTO createCredit(final String source, final JobType jobType, final String name, final String role) {
        return createCredit(source, null, jobType, name, role);
    }

    public CreditDTO createCredit(final String source, final String sourceId, final JobType jobType, final String name, final String role) {
        CreditDTO credit = this.createCredit(source, sourceId, jobType, name);
        if (credit != null && role != null) {
            credit.setRole(role);
            credit.setVoice(isVoiceRole(role));
        }
        return credit;
    }
}
