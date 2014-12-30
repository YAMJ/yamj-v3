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
package org.yamj.filescanner.tools;

import com.thoughtworks.xstream.io.StreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.stereotype.Service;

@Service("xmlTools")
public class XmlTools {

    private static final Logger LOG = LoggerFactory.getLogger(XmlTools.class);
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    /**
     * Read a file from disk
     *
     * @param <T>
     * @param filename
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T read(String filename, Class<T> clazz) {
        LOG.debug("Reading filename '{}' of type {}", filename, clazz.getSimpleName());

        // http://static.springsource.org/spring/docs/3.0.x/spring-framework-reference/html/oxm.html
        FileInputStream is = null;
        try {
            is = new FileInputStream(filename);
            return (T) unmarshaller.unmarshal(new StreamSource(is));
        } catch (FileNotFoundException ex) {
            LOG.warn("File not found '{}'", filename);
            LOG.trace("Exception:", ex);
        } catch (IOException ex) {
            LOG.warn("IO exception for '{}', Error: {}", filename, ex.getMessage());
            LOG.trace("Exception:", ex);
        } catch (XmlMappingException ex) {
            LOG.warn("XML Mapping error for '{}', Error: {}", filename, ex.getMessage());
            LOG.trace("Exception:", ex);
        } catch (StreamException ex) {
            LOG.warn("Stream exception for '{}', Error {}", filename, ex.getMessage());
            LOG.trace("Exception:", ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    LOG.warn("Failed to close library file '{}', Error: {}", filename, ex.getMessage());
                    LOG.trace("Exception:", ex);
                }
            }
        }
        return null;
    }

    /**
     * Save a file to disk
     *
     * @param <T>
     * @param filename
     * @param objectToSave
     */
    public <T> void save(String filename, T objectToSave) {
        LOG.info("Attempting to save {} to {}", objectToSave.getClass().getSimpleName(), filename);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filename);
            marshaller.marshal(objectToSave, new StreamResult(os));
            LOG.info("Saving completed");
        } catch (FileNotFoundException ex) {
            LOG.warn("File not found: {}", filename);
            LOG.trace("Exception:", ex);
        } catch (IOException ex) {
            LOG.warn("IO exception for: {}, Error: {}", filename, ex.getMessage());
            LOG.trace("Exception:", ex);
        } catch (XmlMappingException ex) {
            LOG.warn("XML Mapping error for: {}, Error: {}", filename, ex.getMessage());
            LOG.trace("Exception:", ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    LOG.warn("Failed to close library file: {}, Error: {}", filename, ex.getMessage());
                    LOG.trace("Exception:", ex);
                }
            }
        }
        LOG.info("Failed to save '{}'. Please check it for errors", filename);
    }
}
