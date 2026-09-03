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
     * Las clases de la pila de llamadas, de la mas reciente a la mas vieja.
     *
     * <p>Estuvo afuera mientras la VM no expuso la pila a Java, y no por olvido: cualquier valor que
     * se hubiera devuelto --`null`, un arreglo vacio, uno inventado-- habria sido falso, y este
     * metodo tiene que devolver **un valor**, a diferencia de los `check*`, donde un cuerpo vacio
     * **es** la respuesta permisiva completa. Ahora la VM expone la pila
     * ({@link jdk.internal.vm.Stack}) y el metodo sale de ahi.
     *
     * <p>Se saltean los cuadros de esta misma llamada --el de `Stack.frames()` y el de este metodo--
     * porque el contrato es la pila **del que pregunta**, y los cuadros del mecanismo que responde no
     * son parte de ella.
     *
     * <p>Una clase cuyo nombre no se puede resolver se **omite** en vez de meter un `null` en el
     * arreglo. El contrato dice "las clases de la pila", y un hueco obligaria a todo el que lo
     * recorra a chequear contra nulo por un caso que no deberia poder pasar; si pasa, es que la clase
     * se descargo, y entonces ya no esta en la pila en ningun sentido util.
     *
     * <p><strong>Hoy es inalcanzable</strong>, y eso es una divergencia aparte: el constructor de
     * esta clase tira, asi que no puede existir una instancia desde la cual llamarlo. Se declara
     * igual porque una subclase que compile contra esta biblioteca tiene que compilar como contra el
     * JDK, y porque el cuerpo **es correcto** -- se puede verificar por el mismo camino que lo
     * alimenta ({@link jdk.internal.vm.Stack#frames()}), que es lo que hace `java/StackCtxTest.java`.
     */
    protected Class[] getClassContext() {
        String[] cuadros = jdk.internal.vm.Stack.frames();
        if (cuadros == null) {
            return new Class[0];
        }
        // Se cuenta primero y se copia despues: el resultado es un arreglo de largo exacto, y no uno
        // con huecos al final que el llamador tendria que interpretar.
        Class[] tmp = new Class[cuadros.length];
        int n = 0;
        for (int i = 0; i < cuadros.length; i++) {
            // Los dos primeros son `Stack.frames` y este mismo metodo.
            if (i < 2) {
                continue;
            }
            int barra = cuadros[i].indexOf('|');
            String binario = barra < 0 ? cuadros[i] : cuadros[i].substring(0, barra);
            try {
                tmp[n] = Class.forName(binario.replace('/', '.'));
                n = n + 1;
            } catch (ClassNotFoundException e) {
                // Se omite, ver el javadoc.
            }
        }
        Class[] out = new Class[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }
}