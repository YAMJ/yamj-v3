package com.moviejukebox.common.cmdline;

/**
 * This class defines a command line exception.
 */
public class CmdLineException extends Exception {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 5975436536951550183L;

    /**
     * Constructor of the exception.
     *
     * @param message the detail message
     */
    public CmdLineException(final String message) {
        super(message);
    }

    /**
     * Constructor of the exception.
     *
     * @param cause the cause
     */
    public CmdLineException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructor of the exception.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public CmdLineException(final String message, final Throwable cause) {
        super(message, cause);
    }
}