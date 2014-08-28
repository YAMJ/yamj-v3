/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.tools.xml;

import java.io.*;
import java.net.URL;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.yamj.core.service.file.tools.FileTools;

/**
 * Generic set of routines to process the DOM model data Used for read XML files.
 *
 * @author Stuart.Boston
 *
 */
public final class DOMHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DOMHelper.class);
    private static final String DEFAULT_RETURN = "";
    private static final String YES = "yes";
    private static final String TYPE_ROOT = "xml";
    public static final String TYPE_MOVIE = "movie";
    public static final String TYPE_TVSHOW = "tvshow";
    public static final String TYPE_EPISODE = "episodedetails";
    
    private DOMHelper() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Add a child element to a parent element
     *
     * @param doc
     * @param parentElement
     * @param elementName
     * @param elementValue
     */
    public static void appendChild(Document doc, Element parentElement, String elementName, String elementValue) {
        appendChild(doc, parentElement, elementName, elementValue, null);
    }

    /**
     * Add a child element to a parent element with a set of attributes
     *
     * @param doc
     * @param parentElement
     * @param elementName
     * @param elementValue
     * @param childAttributes
     */
    public static void appendChild(Document doc, Element parentElement, String elementName, String elementValue, Map<String, String> childAttributes) {
        Element child = doc.createElement(elementName);
        Text text = doc.createTextNode(elementValue);
        child.appendChild(text);

        if (childAttributes != null && !childAttributes.isEmpty()) {
            for (Map.Entry<String, String> attrib : childAttributes.entrySet()) {
                child.setAttribute(attrib.getKey(), attrib.getValue());
            }
        }

        parentElement.appendChild(child);
    }

    /**
     * Append a child element to a parent element with a single attribute/value pair
     *
     * @param doc
     * @param parentElement
     * @param elementName
     * @param elementValue
     * @param attribName
     * @param attribValue
     */
    public static void appendChild(Document doc, Element parentElement, String elementName, String elementValue, String attribName, String attribValue) {
        Element child = doc.createElement(elementName);
        Text text = doc.createTextNode(elementValue);
        child.appendChild(text);
        child.setAttribute(attribName, attribValue);
        parentElement.appendChild(child);
    }

    /**
     * Convert a DOM document to a string
     *
     * @param doc
     * @return
     * @throws TransformerException
     */
    public static String convertDocToString(Document doc) throws TransformerException {
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, YES);
        trans.setOutputProperty(OutputKeys.INDENT, YES);

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        return sw.toString();
    }

    /**
     * Create a blank Document
     *
     * @return a Document
     * @throws ParserConfigurationException
     */
    public static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        return docBuilder.newDocument();
    }

    /**
     * Get a DOM document from the supplied string
     *
     * @param docString
     * @return
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public static Document getDocFromString(String docString) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        InputSource is = new InputSource(new StringReader(docString));
        DocumentBuilder db;

        db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        return doc;
    }

    /**
     * Get a DOM document from the supplied file
     *
     * @param xmlFile
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document getDocFromFile(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        URL url = xmlFile.toURI().toURL();
        InputStream in = url.openStream();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;

        // Custom error handler
        db.setErrorHandler(new SaxErrorHandler());

        try {
            doc = db.parse(in);
        } catch (SAXParseException ex) {
            if (FilenameUtils.isExtension(xmlFile.getName().toLowerCase(), "xml")) {
                throw new SAXParseException("Failed to process file as XML", null, ex);
            } else {
                // Try processing the file a different way
                doc = null;
            }
        } finally {
            // close the stream
            in.close();
        }

        if (doc == null) {
            // try wrapping the file in a root
            StringReader sr = new StringReader(wrapInXml(FileTools.readFileToString(xmlFile)));

            try {
                doc = db.parse(new InputSource(sr));
            } finally {
                sr.close();
            }
        }

        if (doc != null) {
            doc.getDocumentElement().normalize();
        }
        return doc;
    }

    /**
     * Gets the string value from a list of tag element names passed
     *
     * @param element
     * @param tagNames
     * @return
     */
    public static String getValueFromElement(Element element, String... tagNames) {
        String returnValue = DEFAULT_RETURN;
        NodeList nlElement;
        Element tagElement;
        NodeList tagNodeList;

        for (String tagName : tagNames) {
            try {
                nlElement = element.getElementsByTagName(tagName);
                if (nlElement != null) {
                    tagElement = (Element) nlElement.item(0);
                    if (tagElement != null) {
                        tagNodeList = tagElement.getChildNodes();
                        if (tagNodeList != null && tagNodeList.getLength() > 0) {
                            returnValue = ((Node) tagNodeList.item(0)).getNodeValue();
                        }
                    }
                }
            } catch (DOMException ex) {
                LOG.trace("DOM processing exception", ex);
            } catch (NullPointerException ex) {
                // Shouldn't really catch null pointer exceptions, but there you go.
                LOG.trace("Null pointer exception", ex);
            }
        }

        return returnValue;
    }

    /**
     * Get an element from a parent element node
     *
     * @param eParent
     * @param elementName
     * @return
     */
    public static Element getElementByName(Element eParent, String elementName) {
        NodeList nlParent = eParent.getElementsByTagName(elementName);
        for (int looper = 0; looper < nlParent.getLength(); looper++) {
            if (nlParent.item(looper).getNodeType() == Node.ELEMENT_NODE) {
                return (Element) nlParent.item(looper);
            }
        }
        return null;
    }

    /**
     * Write the Document out to a file using nice formatting
     *
     * @param doc The document to save
     * @param localFilename The file to write to
     * @return
     */
    public static boolean writeDocumentToFile(Document doc, String localFilename) {
        return writeDocumentToFile(doc, new File(localFilename));
    }

    /**
     * Write the Document out to a file using nice formatting
     *
     * @param doc The document to save
     * @param localFile The file to write to
     * @return
     */
    public static boolean writeDocumentToFile(Document doc, File localFile) {
        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();

            // Define the output properties
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            trans.setOutputProperty(OutputKeys.INDENT, YES);
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            doc.setXmlStandalone(true);

            trans.transform(new DOMSource(doc), new StreamResult(localFile));
            return true;
        } catch (Exception ex) {
            LOG.error("Error writing the document to {}", localFile);
            LOG.error("Error", ex);
            return false;
        }
    }

    /**
     * Override the standard Sax ErrorHandler with this one, to minimise noise about failed parsing errors
     */
    public static class SaxErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            throw new SAXParseException(null, null, exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw new SAXParseException(null, null, exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw new SAXParseException(null, null, exception);
        }
    }

    /**
     * Take a file and wrap it in a new root element
     *
     * @param fileString
     * @return
     */
    public static String wrapInXml(String fileString) {
        StringBuilder newOutput = new StringBuilder(fileString);

        int posMovie = fileString.indexOf("<" + TYPE_MOVIE);
        int posTvShow = fileString.indexOf("<" + TYPE_TVSHOW);
        int posEpisode = fileString.indexOf("<" + TYPE_EPISODE);

        boolean posValid = Boolean.FALSE;

        if (posMovie == -1) {
            posMovie = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posTvShow == -1) {
            posTvShow = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posEpisode == -1) {
            posEpisode = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posValid) {
            int pos = Math.min(posMovie, Math.min(posTvShow, posEpisode));
            newOutput.insert(pos, "<" + TYPE_ROOT + ">");
            newOutput.append("</").append(TYPE_ROOT).append(">");
        }

        return newOutput.toString();
    }
}
