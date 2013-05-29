package com.yamj.core.service.artwork.poster;

import javax.annotation.Resource;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class YahooPosterScannerTest extends AbstractJUnit4SpringContextTests {

    @Resource(name = "yahooPosterScanner")
    private YahooPosterScanner yahooPosterScanner;

    @Test
    public void testPosterUrl() {
        String url = yahooPosterScanner.getPosterUrl("Avatar", 2009);
        System.err.println(url);
    }
}