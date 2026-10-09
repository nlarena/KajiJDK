package com.sun.jdi;

/**
 * ClassNotLoadedException of the debugged machine.
 *
 * @since 1.3
 */
public class ClassNotLoadedException extends Exception {

    private final String className;

    /**
     * With the name of the class that is missing.
     *
     * <p>The name does NOT go into the message: the JDK keeps it separately and leaves the message
     * empty. It is reproduced because a program that formats the message would see something
     * else.
     *
     */
    public ClassNotLoadedException(String className) {
        super();
        this.className = className;
    }

    /**
     * With the class's name and a message.
     *
     *
     */
    public ClassNotLoadedException(String className, String message) {
        super(message);
        this.className = className;
    }

    /**
     * The name of the class that is missing.
     *
     * @return the name
     */
    public String className() {
        return className;
    }
}
