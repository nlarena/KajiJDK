package com.sun.jdi.connect;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A way of getting a {@link com.sun.jdi.VirtualMachine}.
 *
 * <p>There are three, and they are the three subinterfaces: {@link LaunchingConnector} starts
 * the VM, {@link AttachingConnector} attaches to one that is already running, and
 * {@link ListeningConnector} waits for the VM to connect to the debugger. The third exists
 * because sometimes the one that starts first is the debugged program
 * -- {@code -agentlib:jdwp=server=n} -- and then the debugger is the one that listens.
 *
 * <h2>The arguments, and why they are so odd</h2>
 *
 * <p>Each connector is configured with a map of {@link Argument} that the connector itself
 * hands over filled with default values: the client asks for {@link #defaultArguments()},
 * changes what it likes and gives it back. It is the other way round from the usual -- the
 * caller does not build the map, it receives it -- and it is on purpose: each connector has
 * different arguments, and a generic debugger has to be able to show a form for one it does not
 * know. That is why each `Argument` brings its label, its description, whether it is compulsory
 * and how to validate itself.
 *
 * <p>`Argument`'s four subinterfaces -- string, integer, boolean and choice -- are the four
 * kinds of control that form needs to know how to draw.
 */
public interface Connector {

    /** The connector's short name, for instance {@code "com.sun.jdi.SocketAttach"}. */
    String name();

    /** A readable description, to show the user. */
    String description();

    /** The transport this connector talks through. */
    Transport transport();

    /**
     * A new map of arguments, with the default values set.
     *
     * <p>It is a copy: modifying it does not affect the connector, and it has to be given back to
     * it on connecting.
     */
    Map<String, Argument> defaultArguments();

    /**
     * A configuration argument of a {@link Connector}.
     *
     * <p>It is {@link Serializable} so that a debugger may keep a connection configuration and
     * open it again.
     */
    interface Argument extends Serializable {

        /** The name the argument appears under in the map. */
        String name();

        /** A short label, to put beside the control. */
        String label();

        /** An explanation, for the help. */
        String description();

        /** The current value, as text. */
        String value();

        /**
         * It fixes the value.
         *
         * <p>It does not validate: {@link #isValid} is separate, so that an interface may show a
         * half-typed value without rejecting it character by character.
         */
        void setValue(String value);

        /** Whether that text would be an acceptable value for this argument. */
        boolean isValid(String value);

        /** Whether the argument has to have a value before connecting. */
        boolean mustSpecify();
    }

    /** An {@link Argument} whose value is free text. */
    interface StringArgument extends Argument {

        /**
         * Whether that text serves.
         *
         * <p>For a text argument, any string serves: the usual implementation returns `true`.
         */
        boolean isValid(String value);
    }

    /** An {@link Argument} whose value is a bounded integer. */
    interface IntegerArgument extends Argument {

        /**
         * It fixes the value.
         *
         * <p>A value outside {@link #min()}..{@link #max()} is accepted all the same: validating is
         * separate, for the same reason as in {@link Argument#setValue}.
         */
        void setValue(int value);

        /** Whether that text is an integer within the range. */
        boolean isValid(String value);

        /** Whether that integer is within the range. */
        boolean isValid(int value);

        /** That integer written as this argument would write it. */
        String stringValueOf(int value);

        /** The current value as an integer. */
        int intValue();

        /** The largest acceptable one. */
        int max();

        /** The smallest acceptable one. */
        int min();
    }

    /** An {@link Argument} whose value is `true` or `false`. */
    interface BooleanArgument extends Argument {

        /** It fixes the value. */
        void setValue(boolean value);

        /** Whether that text is one of the two values this argument recognizes. */
        boolean isValid(String value);

        /** That boolean written as this argument would write it. */
        String stringValueOf(boolean value);

        /** The current value as a boolean. */
        boolean booleanValue();
    }

    /** An {@link Argument} whose value comes from a closed list. */
    interface SelectedArgument extends Argument {

        /** The possible values. */
        List<String> choices();

        /** Whether that text is one of {@link #choices()}'. */
        boolean isValid(String value);
    }
}
