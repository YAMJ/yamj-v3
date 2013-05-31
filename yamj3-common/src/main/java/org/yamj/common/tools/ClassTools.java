package org.yamj.common.tools;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Tools that are useful to the classes themselves, not necessary for functionality
 *
 * @author stuart.boston
 */
public class ClassTools {

    /**
     * Print the title and build information
     *
     * @param myClass Class to get the information for
     * @param myLog Logger to write the information to
     */
    public static void printHeader(Class myClass, Logger myLog) {
        // Contains "Yet Another Movie Jukebox"
        String projectName = myClass.getPackage().getImplementationVendor();
        // Project version
        String projectVersion = myClass.getPackage().getImplementationVersion();
        // Module Name
        String moduleName = myClass.getPackage().getImplementationTitle();
        // Module description
//        String moduleDesc = myClass.getPackage().getSpecificationTitle();
        // Build timestamp
        String buildTimestamp = myClass.getPackage().getSpecificationVendor();
        // Build number
        String buildNumber = myClass.getPackage().getSpecificationVersion();

        if (projectName != null) {
            // just print out if project name has been set
            
            myLog.info("{} {}", projectName, projectVersion);
            myLog.info("{} {}", StringUtils.repeat("~", projectName.length()), StringUtils.repeat("~", projectVersion.length()));
            myLog.info("{}", moduleName);
            myLog.info("");
            myLog.info("  Revision: {}", buildNumber);
            myLog.info("Build Time: {}", buildTimestamp);
            myLog.info("");
        }
    }
}
