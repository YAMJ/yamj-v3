package org.yamj.common.type;

/**
 * Enum for the exit codes for the services
 */
public enum ExitType {
    /*
     * Program exited normally
     */

    SUCCESS(0),
    /*
     * Error with a command line property
     */
    CMDLINE_ERROR(1),
    /*
     * Error with a configuration
     */
    CONFIG_ERROR(2),
    /*
     * No directory found
     */
    NO_DIRECTORY(3),
    /*
     * Failed to connect to the server
     */
    CONNECT_FAILURE(4),
    /*
     * Failed to start
     */
    STARTUP_FAILURE(4),
    /*
     * Unable to watch a directory
     */
    WATCH_FAILURE(5);
    private int returnValue;

    private ExitType(int returnValue) {
        this.returnValue = returnValue;
    }

    public int getReturn() {
        return this.returnValue;
    }
}
