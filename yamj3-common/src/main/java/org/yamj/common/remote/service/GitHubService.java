package org.yamj.common.remote.service;

public interface GitHubService {

    /**
     * Get the push date for the default repository
     *
     * @return
     */
    String pushDate();

    /**
     * Get the push date for the given owner/repository combination
     *
     * @param owner
     * @param repository
     * @return
     */
    String pushDate(String owner, String repository);
}
