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

    /**
     * Check the installation date of the default owner/repository
     *
     * @param buildDate
     * @param maxAge
     * @return
     */
    boolean checkInstallationDate(String buildDate, int maxAgeDays);

    /**
     * Check the installation date of the owner/repository
     *
     * @param owner
     * @param repository
     * @param buildDate
     * @param maxAge
     * @return
     */
    boolean checkInstallationDate(String owner, String repository, String buildDate, int maxAgeDays);
}
