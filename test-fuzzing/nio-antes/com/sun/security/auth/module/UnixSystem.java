package com.sun.security.auth.module;

/**
 * Who the process's user is, according to Unix: name, uid, gid and groups.
 *
 * <h2>Why it cannot be written in Java</h2>
 *
 * <p>Because the numeric identifiers come from {@code getuid}, {@code getgid} and
 * {@code getgroups}, which are system calls. There is neither a property nor an environment
 * variable that has them: {@code user.name} gives the name, and the name does not determine the
 * uid -- one same name may have a different uid on two machines, and a uid may have no name.
 *
 * <h2>Why the constructor fails instead of answering something</h2>
 *
 * <p>Because what this class returns is used in order to decide permissions, and there an
 * invented value is worse than none. {@code getUid()} returning {@code 0} does not mean "I do
 * not know": it means <strong>root</strong>. A program that consults this class in order to
 * know whether it is running as an administrator would receive a yes.
 *
 * <p>That is exactly the case the house avoids: a member that is missing is a legal subset and
 * does not compile on the other side; one that lies compiles and blows up afterwards. Here it
 * blows up with permissions to spare, so failing from the start is the only defensible
 * option.
 *
 * <p>The day the VM has a way of making the three calls, the only thing that changes is the
 * constructor's body: the four fields are already declared as in the JDK, {@code protected}, so
 * that a subclass may fill them on its own if it gets the data from somewhere else.
 *
 * @since 1.4
 */
public class UnixSystem {

    /** The user's name. */
    protected String username;

    /** The user's numeric identifier. */
    protected long uid;

    /** The primary group's numeric identifier. */
    protected long gid;

    /** The identifiers of all the groups it belongs to. */
    protected long[] groups;

    /**
     * It asks the operating system who the process's user is.
     *
     * @throws UnsupportedOperationException always, in this library: the data come from
     *     {@code getuid}, {@code getgid} and {@code getgroups}, and this VM has no way of calling
     *     them
     */
    public UnixSystem() {
        throw new UnsupportedOperationException(
                "UnixSystem's data come from getuid/getgid/getgroups, which this VM cannot call; "
                + "returning an invented uid in a permissions decision would be worse than "
                + "failing");
    }

    /**
     * The user's name.
     *
     * @return the name
     */
    public String getUsername() {
        return username;
    }

    /**
     * The user's numeric identifier.
     *
     * @return the uid
     */
    public long getUid() {
        return uid;
    }

    /**
     * The primary group's numeric identifier.
     *
     * @return the gid
     */
    public long getGid() {
        return gid;
    }

    /**
     * The groups it belongs to.
     *
     * @return the identifiers; it is the internal array, as in the JDK
     */
    public long[] getGroups() {
        return groups;
    }
}
