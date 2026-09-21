package java.security;

// Running a block of code with the privileges of whoever wrote it, not with those of whoever called
// it.
//
// ===============================================================================================
// WHAT `doPrivileged` USED TO MEAN, AND WHAT IT DOES TODAY
// ===============================================================================================
//
// The normal access check demands that **every** domain of the stack have the permission. That
// leaves a trusted library unable to open its own configuration file if an applet called it.
// `doPrivileged` cut the stack there: from that frame upwards nothing more was looked at, so the
// library could do its own thing even though its caller could not.
//
// It was the most delicate construction of the model, because a `doPrivileged` that also uses a
// datum that came from the caller —a file name, for example— lends its privileges to whoever sent
// the datum. That is the classic "confused deputy".
//
// Today it cuts nothing: since JDK 24 there is no `SecurityManager` and there was never access
// control in KajiJDK. `doPrivileged` **runs the action and returns whatever it returns**, which is
// exactly what JDK 25 does —checked against it— and the only honest thing: there are no privileges
// to elevate because there are none to restrict.
//
// `checkPermission`, on the other hand, **throws**. It is the important asymmetry: running code
// without control is the same as running it, but asking "do I have this permission?" and answering
// yes without having looked would be manufacturing an authorisation. JDK 25 throws
// `AccessControlException` and here so does this.
@Deprecated
public final class AccessController {

    // Purely static: it is not instantiated.
    private AccessController() {
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context,
                                     Permission... perms) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action,
                                                 AccessControlContext context,
                                                 Permission... perms) {
        return action.run();
    }

    // The variant for actions that throw checked ones: whatever comes out is wrapped.
    //
    // Only the **checked** ones are wrapped. A `RuntimeException` comes out as it is, and that
    // distinction is of the contract: wrapping everything would force even programming errors to be
    // unwrapped.
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException {
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action,
                                     AccessControlContext context)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action,
                                     AccessControlContext context, Permission... perms)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action,
                                                 AccessControlContext context,
                                                 Permission... perms)
            throws PrivilegedActionException {
        return doPrivileged(action);
    }

    // The context of the current execution.
    //
    // It returns an **empty** one and not null: the VM does not expose the domains of the stack, so
    // there are no domains to enumerate. An empty context is the truth —"there is nothing noted
    // here"— and it is also what keeps whoever keeps it to use it later from meeting a null.
    public static AccessControlContext getContext() {
        return new AccessControlContext(new ProtectionDomain[0]);
    }

    // It always throws. See the header: answering yes without having looked at anything would be
    // manufacturing an authorisation.
    public static void checkPermission(Permission perm) throws AccessControlException {
        if (perm == null) {
            throw new NullPointerException("permission can't be null");
        }
        throw new AccessControlException("checking permissions is not supported", perm);
    }
}
