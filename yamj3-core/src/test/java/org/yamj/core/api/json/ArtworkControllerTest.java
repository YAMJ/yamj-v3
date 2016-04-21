package org.yamj.core.api.json;

import static org.yamj.plugin.api.common.Constants.UTF8;

import java.io.File;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.DigestedResponseReader;
import org.yamj.api.common.http.SimpleHttpClientBuilder;
import org.yamj.core.AbstractTest;

public class ArtworkControllerTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkControllerTest.class);

    @Test
    public void emptyTest() {
        // no test 
    }

    @Ignore
    public void uploadImage() throws Exception {
        final long id=1;
        
        HttpPost httpPost = new HttpPost("http://localhost:8888/yamj3/api/artwork/add/fanart/movie/" + id);

        File file = new File("c:/test.png");
        HttpEntity reqEntity = MultipartEntityBuilder.create().addBinaryBody("image", file, ContentType.MULTIPART_FORM_DATA, file.getName()).build();
        httpPost.setEntity(reqEntity);

        LOG.info("Executing request {}", httpPost.getRequestLine());
        DigestedResponse response = DigestedResponseReader.postContent(new SimpleHttpClientBuilder().build(), httpPost, UTF8);
        LOG.info("Response code:    {}", response.getStatusCode());
        LOG.info("Response content: {}", response.getContent());
    }
}
