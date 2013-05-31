package com.yamj.core.remote.service;

import com.yamj.common.remote.service.GitHubService;
import com.yamj.core.tools.web.PoolingHttpClient;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("githubService")
public class GitHubServiceImpl implements GitHubService {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubServiceImpl.class);
    private static final String GH_API = "https://api.github.com/repos/";
    private static final String GH_OWNER = "YAMJ";
    private static final String GH_REPO = "yamj-v3";
    @Autowired
    private PoolingHttpClient httpClient;

    @Override
    public String pushDate(String owner, String repository) {
        if (StringUtils.isBlank(owner) || StringUtils.isBlank(repository)) {
            LOG.error("Owner '{}' or repository '{}' cannot be blank", owner, repository);
            throw new IllegalArgumentException("Owner or repository cannot be blank");
        }

        String returnDate = "";

        StringBuilder url = new StringBuilder(GH_API);
        url.append(owner).append("/").append(repository);

        try {
            String jsonData = httpClient.requestContent(url.toString());
            // This is ugly and a bit of a hack, but I don't need to unmarshal the whole object just for a date.
            int posStart = jsonData.indexOf("pushed_at");
            posStart = jsonData.indexOf("20", posStart) + 1;
            int posEnd = jsonData.indexOf('\"', posStart);
            returnDate = jsonData.substring(posStart, posEnd);
            LOG.info("Date: '{}'", returnDate);
        } catch (IOException ex) {
            LOG.warn("Unable to get GitHub information, error: {}", ex.getMessage());
            return returnDate;
        }

        return returnDate;
    }

    @Override
    public String pushDate() {
        return pushDate(GH_OWNER, GH_REPO);
    }
}
