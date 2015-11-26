package org.yamj.core.api.json;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Test;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.DigestedResponseReader;
import org.yamj.api.common.http.SimpleHttpClientBuilder;

public class ArtworkControllerTest {

    @Test
    public void uploadImage() throws Exception {
        final long id=1;
        
        HttpClient httpClient = new SimpleHttpClientBuilder().build();
        HttpPost httpPost = new HttpPost("http://localhost:8888/yamj3/api/artwork/add/fanart/movie/" + id);

        File file = new File("c:/test.png");
        HttpEntity reqEntity = MultipartEntityBuilder.create().addBinaryBody("image", file, ContentType.MULTIPART_FORM_DATA, file.getName()).build();
        httpPost.setEntity(reqEntity);

        System.out.println("Executing request " + httpPost.getRequestLine());
        DigestedResponse response = DigestedResponseReader.postContent(httpClient, httpPost, Charset.forName("UTF-8"));
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusCode());
        System.out.println(response.getContent());
    }
}
