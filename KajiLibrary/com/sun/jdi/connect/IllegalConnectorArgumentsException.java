package com.sun.jdi.connect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One or several arguments of a {@link Connector} were wrong.
 *
 * <p>It carries **the list of names** of the guilty arguments, and not only a message. It is
 * the difference between a debugger that can mark in red the two fields that are wrong and one
 * that can only show a notice: the connection form is built from
 * {@link Connector#defaultArguments()}, so whoever drew it has the controls indexed by that
 * same name.
 */
public class IllegalConnectorArgumentsException extends Exception {

    private static final long serialVersionUID = -3042212603611350941L;

    /** The names of the arguments that were wrong. Package-private, as in the JDK. */
    List<String> names;

    /**
     * A failure about a single argument.
     *
     * @param s the detail
     * @param name the argument's name
     */
    public IllegalConnectorArgumentsException(String s, String name) {
        super(s);
        this.names = new ArrayList<String>();
        this.names.add(name);
    }

    /**
     * A failure about several arguments.
     *
     * @param s the detail
     * @param names the names; they are copied
     */
    public IllegalConnectorArgumentsException(String s, List<String> names) {
        super(s);
        this.names = new ArrayList<String>(names);
    }

    /** The names of the arguments that were wrong, in a list that cannot be modified. */
    public List<String> argumentNames() {
        return Collections.unmodifiableList(this.names);
    }
}
