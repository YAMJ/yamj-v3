package org.yamj.common.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    @SuppressWarnings("rawtypes")
    public static void printHeader(Class myClass, Logger myLog) {
        String projectName = getProjectName(myClass);

        if (projectName != null) {
            // just print out if project name has been set

            myLog.info("{} {}", projectName, getProjectVersion(myClass));
            myLog.info("{} {}", StringUtils.repeat("~", projectName.length()), StringUtils.repeat("~", getProjectVersion(myClass).length()));
            myLog.info("{}", getModuleName(myClass));
            myLog.info("");
            myLog.info("  Revision: {}", getBuildNumber(myClass));
            myLog.info("Build Time: {}", getBuildTimestamp(myClass));
            myLog.info("");
        }
    }

    /**
     * Get the Project Name This is set to "Yet Another Movie Jukebox"
     *
     * @param myClass
     * @return
     */
    public static String getProjectName(Class myClass) {
        return myClass.getPackage().getImplementationVendor();
    }

    /**
     * Get the version of the project
     *
     * @param myClass
     * @return
     */
    public static String getProjectVersion(Class myClass) {
        return myClass.getPackage().getImplementationVersion();
    }

    /**
     * Get the name of the module
     *
     * @param myClass
     * @return
     */
    public static String getModuleName(Class myClass) {
        return myClass.getPackage().getImplementationTitle();
    }

    /**
     * Get the description of the module
     *
     * @param myClass
     * @return
     */
    public static String getModuleDescription(Class myClass) {
        return myClass.getPackage().getSpecificationTitle();
    }

    /**
     * Get the build date/time
     *
     * @param myClass
     * @return
     */
    public static String getBuildTimestamp(Class myClass) {
        return myClass.getPackage().getSpecificationVendor();
    }

    /**
     * Get the build revision/sha
     *
     * @param myClass
     * @return
     */
    public static String getBuildNumber(Class myClass) {
        return myClass.getPackage().getSpecificationVersion();
    }

    /**
     * Helper method to print the stack trace to the log file
     *
     * @param tw
     * @return
     */
    public static String getStackTrace(Throwable tw) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        tw.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
}
