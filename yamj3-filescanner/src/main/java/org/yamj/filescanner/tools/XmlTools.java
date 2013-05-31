package org.yamj.filescanner.tools;

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
    public <T> T read(String filename, Class<T> clazz) {
        LOG.debug("Reading filename '{}' of type {}", filename, clazz.getSimpleName());

        // http://static.springsource.org/spring/docs/3.0.x/spring-framework-reference/html/oxm.html
        FileInputStream is = null;
        try {
            is = new FileInputStream(filename);
            return (T) unmarshaller.unmarshal(new StreamSource(is));
        } catch (FileNotFoundException ex) {
            LOG.warn("File not found: {}", filename);
        } catch (IOException ex) {
            LOG.warn("IO exception for: {}, Error: {}", filename, ex.getMessage());
        } catch (XmlMappingException ex) {
            LOG.warn("XML Mapping error for: {}, Error: {}", filename, ex.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    LOG.warn("Failed to close library file: {}, Error: {}", filename, ex.getMessage());
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
        } catch (FileNotFoundException ex) {
            LOG.warn("File not found: {}", filename);
        } catch (IOException ex) {
            LOG.warn("IO exception for: {}, Error: {}", filename, ex.getMessage());
        } catch (XmlMappingException ex) {
            LOG.warn("XML Mapping error for: {}, Error: {}", filename, ex.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    LOG.warn("Failed to close library file: {}, Error: {}", filename, ex.getMessage());
                }
            }
        }
        LOG.info("Saving completed");
    }
}
