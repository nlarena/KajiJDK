package com.sun.management;

import javax.management.openmbean.CompositeData;

/**
 * A VM option: its current value, whether it may be changed, and <strong>where it came
 * from</strong>.
 *
 * <h2>Why the origin is the important datum</h2>
 *
 * <p>Because the value alone is not enough in order to understand anything. An option that is
 * worth the same as the default may have been put there by the user on the command line, or
 * nobody may have touched it; and one that is worth something strange may be an explicit
 * decision or {@link Origin#ERGONOMIC ergonomics} -- the VM adjusting itself to the hardware it
 * found.
 *
 * <p>Telling those cases apart is what separates "it is badly configured" from "the VM
 * decided this and it has to be understood why". It is the reason {@link Origin} has eight
 * values and not two.
 *
 * @since 1.6
 */
public class VMOption {

    private final String name;
    private final String value;
    private final boolean writeable;
    private final Origin origin;

    /**
     * An option.
     *
     * @param name the name
     * @param value the current value, as text
     * @param writeable whether it may be changed with the VM running
     * @param origin where the value came from
     * @throws NullPointerException if the name, the value or the origin is {@code null}
     */
    public VMOption(final String name, final String value, final boolean writeable,
            final Origin origin) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        if (origin == null) {
            throw new NullPointerException("origin");
        }
        this.name = name;
        this.value = value;
        this.writeable = writeable;
        this.origin = origin;
    }

    /**
     * The name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The current value, as text.
     *
     * <p>Always text, even though the option should be numeric or boolean: they are hundreds of
     * options with different types and there is no class that covers them all.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Where the value came from.
     *
     * @return the origin
     */
    public Origin getOrigin() {
        return origin;
    }

    /**
     * Whether it may be changed with the VM already running.
     *
     * <p>Most may not: they size structures that are built on starting. Only those marked
     * {@code manageable} in the VM accept changes while hot.
     *
     * @return whether it is writeable
     */
    public boolean isWriteable() {
        return writeable;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "VM option: " + name + " value: " + value + " " + " origin: " + origin
                + " " + (writeable ? "(read-write)" : "(read-only)");
    }

    /**
     * It rebuilds an option from its open form.
     *
     * <p>It is what is needed on the client's side when the option has travelled over a JMX
     * connection: what arrives is a generic {@link CompositeData} and this turns it back into the
     * object.
     *
     * @param cd the open form, or {@code null}
     * @return the option, or {@code null} if {@code cd} was {@code null}
     * @throws IllegalArgumentException if {@code cd} does not have a {@code VMOption}'s shape
     */
    public static VMOption from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        if (!cd.containsKey("name") || !cd.containsKey("value") || !cd.containsKey("origin")
                || !cd.containsKey("writeable")) {
            throw new IllegalArgumentException(
                    "the CompositeData does not have the shape of a VMOption");
        }
        return new VMOption((String) cd.get("name"), (String) cd.get("value"),
                ((Boolean) cd.get("writeable")).booleanValue(),
                Origin.valueOf((String) cd.get("origin")));
    }

    /** Where an option's value came from. */
    public enum Origin {
        /** Nobody touched it: it is the value the VM comes with. */
        DEFAULT,
        /** From the command line, on starting. */
        VM_CREATION,
        /** From an environment variable. */
        ENVIRON_VAR,
        /** From a configuration file. */
        CONFIG_FILE,
        /** A management interface changed it with the VM running. */
        MANAGEMENT,
        /** The VM chose it by itself, according to the hardware it found. */
        ERGONOMIC,
        /** A tool that connected to the already started process put it there. */
        ATTACH_ON_DEMAND,
        /** From somewhere else. */
        OTHER
    }
}
