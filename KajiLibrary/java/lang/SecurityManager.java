package java.lang;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

// KajiLibrary's java.lang.SecurityManager. The security manager is degraded away in modern Java
// (JEP 411 deprecated it for removal; JEP 486 disabled it permanently): it can no longer be
// installed, and its constructor throws. KajiJDK follows suit -- no instance can be created, so the
// runtime never consults one -- while keeping the class and its full set of check methods present
// for surface parity. Every check is a permissive no-op (they are unreachable anyway: no
// SecurityManager can exist to run them).
@Deprecated
public class SecurityManager {

    /**
     * @throws UnsupportedOperationException always -- a security manager cannot be created.
     */
    public SecurityManager() {
        throw new UnsupportedOperationException("SecurityManager is not supported");
    }

    public Object getSecurityContext() {
        return null;
    }

    public void checkPermission(Permission perm) {
    }

    public void checkPermission(Permission perm, Object context) {
    }

    public void checkCreateClassLoader() {
    }

    public void checkAccess(Thread t) {
    }

    public void checkAccess(ThreadGroup g) {
    }

    public void checkExit(int status) {
    }

    public void checkExec(String cmd) {
    }

    public void checkLink(String lib) {
    }

    public void checkRead(FileDescriptor fd) {
    }

    public void checkRead(String file) {
    }

    public void checkRead(String file, Object context) {
    }

    public void checkWrite(FileDescriptor fd) {
    }

    public void checkWrite(String file) {
    }

    public void checkDelete(String file) {
    }

    public void checkConnect(String host, int port) {
    }

    public void checkConnect(String host, int port, Object context) {
    }

    public void checkListen(int port) {
    }

    public void checkAccept(String host, int port) {
    }

    public void checkMulticast(InetAddress maddr) {
    }

    public void checkMulticast(InetAddress maddr, byte ttl) {
    }

    public void checkPropertiesAccess() {
    }

    public void checkPropertyAccess(String key) {
    }

    public void checkPrintJobAccess() {
    }

    public void checkPackageAccess(String pkg) {
    }

    public void checkPackageDefinition(String pkg) {
    }

    public void checkSetFactory() {
    }

    public void checkSecurityAccess(String target) {
    }

    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

    /**
     * The call stack's classes, from the most recent to the oldest.
     *
     * <p>It was left out while the VM did not expose the stack to Java, and not through oversight:
     * any value that might have been returned --`null`, an empty array, an invented one-- would have
     * been false, and this method has to return **a value**, unlike the `check*` ones, where an empty
     * body **is** the complete permissive answer. Now the VM exposes the stack
     * ({@link jdk.internal.vm.Stack}) and the method comes out of there.
     *
     * <p>This very call's frames --`Stack.frames()`'s and this method's-- are skipped, because the
     * contract is **the asker's** stack, and the frames of the mechanism that answers are no part of
     * it.
     *
     * <p>A class whose name cannot be resolved is **left out** rather than putting a `null` into the
     * array. The contract says "the stack's classes", and a hole would force everyone walking it to
     * check against null for a case that should not be able to happen; if it does happen, the class
     * was unloaded, and then it is no longer on the stack in any useful sense.
     *
     * <p><strong>Today it is unreachable</strong>, and that is a separate divergence: this class's
     * constructor throws, so no instance can exist from which to call it. It is declared all the same
     * because a subclass compiling against this library has to compile as it would against the JDK,
     * and because the body **is correct** -- it can be verified by the same path that feeds it
     * ({@link jdk.internal.vm.Stack#frames()}), which is what `java/StackCtxTest.java` does.
     */
    protected Class[] getClassContext() {
        String[] frames = jdk.internal.vm.Stack.frames();
        if (frames == null) {
            return new Class[0];
        }
        // It counts first and copies after: the result is an array of exact length, and not one with
        // holes at the end the caller would have to interpret.
        Class[] tmp = new Class[frames.length];
        int n = 0;
        for (int i = 0; i < frames.length; i++) {
            // The first two are `Stack.frames` and this very method.
            if (i < 2) {
                continue;
            }
            int bar = frames[i].indexOf('|');
            String binary = bar < 0 ? frames[i] : frames[i].substring(0, bar);
            try {
                tmp[n] = Class.forName(binary.replace('/', '.'));
                n = n + 1;
            } catch (ClassNotFoundException e) {
                // Left out, see the javadoc.
            }
        }
        Class[] out = new Class[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }
}