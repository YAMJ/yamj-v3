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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package org.yamj.core.service.trailer.online;

import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.core.database.model.type.ContainerType;
import org.yamj.core.service.trailer.TrailerDownloadDTO;
import org.yamj.core.web.HTMLTools;

@Service("youTubeDownloadParser")
public class YouTubeDownloadParser  {

    /*
     * For the complete YouTubeParser see https://github.com/axet/vget.
     * YAMJ just needs to parse the download; the download itself is done by http client. 
     */
    
    private static final Logger LOG = LoggerFactory.getLogger(YouTubeDownloadParser.class);
    public static final String TRAILER_BASE_URL = "https://www.youtube.com/watch?v=";
    public static final String TRAILER_INFO_URL = "https://www.youtube.com/get_video_info?authuser=0&el=embedded&video_id=";
    
    private enum Quality {
        P3072, P2304, P2160, P1440, P1080, P720, P520, P480, P360, P270, P240, P224, P144
    }

    static final class StreamInfo {
        public final ContainerType container;
        public final Quality quality;

        public StreamInfo(ContainerType container, Quality quality) {
            this.container = container;
            this.quality = quality;
        }
    }

    static final class VideoDownload implements Comparable<VideoDownload> {
        public final StreamInfo streamInfo;
        public final URL url;

        public VideoDownload(StreamInfo streamInfo, URL url) {
            this.streamInfo = streamInfo;
            this.url = url;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(streamInfo.container)
                    .append(streamInfo.quality)
                    .append(url)
                    .toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof VideoDownload)) {
                return false;
            }
            final VideoDownload other = (VideoDownload) obj;
            return new EqualsBuilder()
                    .append(streamInfo.container, other.streamInfo.container)
                    .append(streamInfo.quality, other.streamInfo.quality)
                    .append(url, other.url)
                    .isEquals();
        }

        @Override
        public int compareTo(VideoDownload other) {
            Integer i1 = streamInfo.quality.ordinal();
            Integer i2 = other.streamInfo.quality.ordinal();
            return i1.compareTo(i2);
        }
    }

    // http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    static final Map<Integer, StreamInfo> ITAG_MAP = new HashMap<Integer, StreamInfo>() {
        private static final long serialVersionUID = 6207047619035836958L;
        {
            put(120, new StreamInfo(ContainerType.FLV, Quality.P720));
            put(102, new StreamInfo(ContainerType.WEBM, Quality.P720));
            put(101, new StreamInfo(ContainerType.WEBM, Quality.P360));
            put(100, new StreamInfo(ContainerType.WEBM, Quality.P360));
            put(85, new StreamInfo(ContainerType.MP4, Quality.P1080));
            put(84, new StreamInfo(ContainerType.MP4,Quality.P720));
            put(83, new StreamInfo(ContainerType.MP4, Quality.P240));
            put(82, new StreamInfo(ContainerType.MP4, Quality.P360));
            put(46, new StreamInfo(ContainerType.WEBM, Quality.P1080));
            put(45, new StreamInfo(ContainerType.WEBM, Quality.P720));
            put(44, new StreamInfo(ContainerType.WEBM, Quality.P480));
            put(43, new StreamInfo(ContainerType.WEBM, Quality.P360));
            put(38, new StreamInfo(ContainerType.MP4, Quality.P3072));
            put(37, new StreamInfo(ContainerType.MP4, Quality.P1080));
            put(36, new StreamInfo(ContainerType.GP3, Quality.P240));
            put(35, new StreamInfo(ContainerType.FLV, Quality.P480));
            put(34, new StreamInfo(ContainerType.FLV, Quality.P360));
            put(22, new StreamInfo(ContainerType.MP4, Quality.P720));
            put(18, new StreamInfo(ContainerType.MP4, Quality.P360));
            put(17, new StreamInfo(ContainerType.GP3, Quality.P144));
            put(13, new StreamInfo(ContainerType.GP3, Quality.P144));
            put(6, new StreamInfo(ContainerType.FLV, Quality.P270));
            put(5, new StreamInfo(ContainerType.FLV, Quality.P240));
        }
    };

    static final class DecryptSignature {
        String sig;

        public DecryptSignature(String signature) {
            this.sig = signature;
        }

        String s(int b, int e) {
            return sig.substring(b, e);
        }

        String s(int b) {
            return sig.substring(b, b + 1);
        }

        String se(int b) {
            return s(b, sig.length());
        }

        String s(final int begin, final int end, final int step) {
            String str = "";
            int b = begin;
            while (b != end) {
                str += sig.charAt(b);
                b += step;
            }
            return str;
        }

        // https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/youtube.py
        String decrypt() {
            switch (sig.length()) {
            case 93:
                return s(86, 29, -1) + s(88) + s(28, 5, -1);
            case 92:
                return s(25) + s(3, 25) + s(0) + s(26, 42) + s(79) + s(43, 79) + s(91) + s(80, 83);
            case 91:
                return s(84, 27, -1) + s(86) + s(26, 5, -1);
            case 90:
                return s(25) + s(3, 25) + s(2) + s(26, 40) + s(77) + s(41, 77) + s(89) + s(78, 81);
            case 89:
                return s(84, 78, -1) + s(87) + s(77, 60, -1) + s(0) + s(59, 3, -1);
            case 88:
                return s(7, 28) + s(87) + s(29, 45) + s(55) + s(46, 55) + s(2) + s(56, 87) + s(28);
            case 87:
                return s(6, 27) + s(4) + s(28, 39) + s(27) + s(40, 59) + s(2) + se(60);
            case 86:
                return s(80, 72, -1) + s(16) + s(71, 39, -1) + s(72) + s(38, 16, -1) + s(82) + s(15, 0, -1);
            case 85:
                return s(3, 11) + s(0) + s(12, 55) + s(84) + s(56, 84);
            case 84:
                return s(78, 70, -1) + s(14) + s(69, 37, -1) + s(70) + s(36, 14, -1) + s(80) + s(0, 14, -1);
            case 83:
                return s(80, 63, -1) + s(0) + s(62, 0, -1) + s(63);
            case 82:
                return s(80, 37, -1) + s(7) + s(36, 7, -1) + s(0) + s(6, 0, -1) + s(37);
            case 81:
                return s(56) + s(79, 56, -1) + s(41) + s(55, 41, -1) + s(80) + s(40, 34, -1) + s(0) + s(33, 29, -1) + s(34)
                                + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
            case 80:
                return s(1, 19) + s(0) + s(20, 68) + s(19) + s(69, 80);
            case 79:
                return s(54) + s(77, 54, -1) + s(39) + s(53, 39, -1) + s(78)
                                + s(38, 34, -1) + s(0) + s(33, 29, -1) + s(34)
                                + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
            default:    
                throw new RuntimeException("Unable to decrypt signature; key length" + sig.length() + " not supported");
            }
        }
    }

    @Autowired
    private PoolingHttpClient httpClient;

    public TrailerDownloadDTO extract(String videoId) {
        LOG.info("Determine download URL for YouTube video '{}'", videoId);

        Set<VideoDownload> videoDownloads = new TreeSet<>();
        try {
            boolean extractEmbedded = false;
            try {
                streamCapture(videoDownloads, videoId);
                extractEmbedded = (videoDownloads.size() == 0);
            } catch (Exception e) {
                extractEmbedded = true;
            }
            
            if (extractEmbedded) {
                extractEmbedded(videoDownloads, videoId);
            }
        } catch (Exception e) {
            LOG.warn("Failed to retrieve YouTube download url: {}", e.getMessage());
            LOG.trace("YouTube parser error", e);
        }

        if (videoDownloads.size() == 0) {
            return null;
        }
        
        // use first from sorted set of video downloads
        VideoDownload videoDownload = videoDownloads.iterator().next();
        return new TrailerDownloadDTO(videoDownload.streamInfo.container, videoDownload.url);
    }

    private void streamCapture(Set<VideoDownload> videoDownloads, String videoId) throws Exception {
        DigestedResponse response = httpClient.requestContent(TRAILER_BASE_URL + videoId);
        if (response.getStatusCode() != 200) {
            throw new HttpResponseException(response.getStatusCode(), "Failed to access trailer base");
        }
        extractHtmlInfo(videoDownloads, response.getContent());
    }

    private static void filter(Set<VideoDownload> videoDownloads, String itag, URL url) {
        Integer i = Integer.decode(itag);
        StreamInfo streamInfo = ITAG_MAP.get(i);
        if (streamInfo != null) {
            videoDownloads.add(new VideoDownload(streamInfo, url));
        }
    }

    private void extractEmbedded(Set<VideoDownload> videoDownloads, String videoId) throws Exception {
        DigestedResponse response = httpClient.requestContent(TRAILER_INFO_URL + videoId);
        if (response.getStatusCode() != 200) {
            throw new HttpResponseException(response.getStatusCode(), "Failed to access trailer info page");
        }

        Map<String, String> map = getQueryMap(response.getContent());
        if ("fail".equals(map.get("status"))) {
            if ("150".equals(map.get("errorcode")))
                throw new RuntimeException("Embedding is disabled"); //NOSONAR
            if ("100".equals(map.get("errorcode")))
                throw new RuntimeException("Video is deleted"); //NOSONAR
            throw new Exception(HTMLTools.decodeUrl(map.get("reason"))); //NOSONAR
        }

        final String urlEncodedFmtStreamMap = HTMLTools.decodeUrl(map.get("url_encoded_fmt_stream_map"));
        extractUrlEncodedVideos(videoDownloads, urlEncodedFmtStreamMap);
    }

    private static Map<String, String> getQueryMap(String qs) throws URISyntaxException {
        List<NameValuePair> list = URLEncodedUtils.parse(new URI(null, null, null, -1, null, qs, null), "UTF-8");
        HashMap<String, String> map = new HashMap<>();
        for (NameValuePair p : list) {
            map.put(p.getName(), p.getValue());
        }
        return map;
    }

    private static void extractHtmlInfo(Set<VideoDownload> videoDownloads, String html) throws Exception { //NOSONAR
        
        Matcher ageMatcher = Pattern.compile("(verify_age)").matcher(html);
        if (ageMatcher.find()) {
            throw new RuntimeException("Age restriction, account required"); //NOSONAR
        }
        
        
        Matcher playerMatcher = Pattern.compile("(unavailable-player)").matcher(html);
        if (playerMatcher.find()) {
            throw new RuntimeException("Video player is unavailable"); //NOSONAR
        }


        Matcher urlEncoded = Pattern.compile("\"url_encoded_fmt_stream_map\":\"([^\"]*)\"").matcher(html);
        if (urlEncoded.find()) {
            final String urlEncodedFmtStreamMap = urlEncoded.group(1);

            // normal embedded video, unable to grab age restricted videos
            Matcher urlMatcher = Pattern.compile("url=(.*)").matcher(urlEncodedFmtStreamMap);
            if (urlMatcher.find()) {
                String sline = urlMatcher.group(1);
                extractUrlEncodedVideos(videoDownloads, sline);
            }
            
            // stream video
            Matcher streamMatcher = Pattern.compile("stream=(.*)").matcher(urlEncodedFmtStreamMap);
            if (streamMatcher.find()) {
                String[] urlStrings = streamMatcher.group(1).split("stream=");
                for (String urlString : urlStrings) {
                    urlString = StringEscapeUtils.unescapeJava(urlString);
                    Matcher paramsMatcher = Pattern.compile("(sparams.*)&itag=(\\d+)&.*&conn=rtmpe(.*),").matcher(urlString);
                    if (paramsMatcher.find()) {
                        String sparams = paramsMatcher.group(1);
                        String itag = paramsMatcher.group(2);
                        String url = paramsMatcher.group(3);
                        url = "http" + url + "?" + sparams;
                        url = HTMLTools.decodeUrl(url);
                        filter(videoDownloads, itag, new URL(url));
                    }
                }
            }
        }

        // separate streams
        Matcher adaptiveMatcher = Pattern.compile("\"adaptive_fmts\": \"([^\"]*)\"").matcher(html);
        if (adaptiveMatcher.find()) {
            final String urlEncodedFmtStreamMap = adaptiveMatcher.group(1);
            
            // normal embedded video, unable to grab age restricted videos
            Matcher urlMatcher = Pattern.compile("url=(.*)").matcher(urlEncodedFmtStreamMap);
            if (urlMatcher.find()) {
                String sline = urlMatcher.group(1);
                extractUrlEncodedVideos(videoDownloads, sline);
            }
            
            // stream video
            Matcher streamMatcher = Pattern.compile("stream=(.*)").matcher(urlEncodedFmtStreamMap);
            if (streamMatcher.find()) {
                String[] urlStrings = streamMatcher.group(1).split("stream=");
                for (String urlString : urlStrings) {
                    urlString = StringEscapeUtils.unescapeJava(urlString);
                    Matcher paramsMatcher = Pattern .compile("(sparams.*)&itag=(\\d+)&.*&conn=rtmpe(.*),").matcher(urlString);
                    if (paramsMatcher.find()) {
                        String sparams = paramsMatcher.group(1);
                        String itag = paramsMatcher.group(2);
                        String url = paramsMatcher.group(3);
                        url = HTMLTools.decodeUrl("http" + url + "?" + sparams);
                        filter(videoDownloads, itag, new URL(url));
                    }
                }
            }
        }
    }

    private static void extractUrlEncodedVideos(Set<VideoDownload> videoDownloads, String sline) {
        String[] urlStrings = sline.split("url=");
        for (String urlString : urlStrings) {
            urlString = StringEscapeUtils.unescapeJava(urlString);
            String urlFull = HTMLTools.decodeUrl(urlString);

            String url = null;
            Matcher urlMatcher = Pattern.compile("([^&,]*)[&,]").matcher(urlString);
            if (urlMatcher.find()) {
                url = HTMLTools.decodeUrl(urlMatcher.group(1));
            }

            String itag = null;
            Matcher itagMatcher = Pattern.compile("itag=(\\d+)").matcher(urlFull);
            if (itagMatcher.find()) {
                itag = itagMatcher.group(1);
            }

            String sig = null;
            Matcher sigMatcher1 = Pattern.compile("&signature=([^&,]*)").matcher(urlFull);
            if (sigMatcher1.find()) {
                sig = sigMatcher1.group(1);
            }
            
            if (sig == null) {
                Matcher sigMatcher2 = Pattern.compile("sig=([^&,]*)").matcher(urlFull);
                if (sigMatcher2.find()) {
                    sig = sigMatcher2.group(1);
                }
            }
            
            if (sig == null) {
                Matcher sigMatcher3 = Pattern.compile("[&,]s=([^&,]*)").matcher(urlFull);
                if (sigMatcher3.find()) {
                    sig = sigMatcher3.group(1);
                    sig = new DecryptSignature(sig).decrypt();
                }
            }
            
            if (url != null && itag != null && sig != null) {
                try {
                    url += "&signature=" + sig;
                    filter(videoDownloads, itag, new URL(url));
                } catch (MalformedURLException e) { //NOSONAR
                    // ignore bad URLs
                }
                continue;
            }
        }
    }
}
