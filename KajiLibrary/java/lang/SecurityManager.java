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
}
