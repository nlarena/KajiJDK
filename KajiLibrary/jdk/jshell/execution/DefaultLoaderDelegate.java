package jdk.jshell.execution;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

/**
 * El cargador que usa {@link DirectExecutionControl} cuando no le dan otro.
 *
 * <h2>Como instala</h2>
 *
 * <p>Guarda los bytes en un mapa y define la clase la primera vez que alguien la busca. Definirla en
 * el acto seria mas simple y estaria mal: JShell manda las clases de un fragmento juntas y en
 * cualquier orden, y una clase que hereda de otra del mismo lote fallaria si le toca ir primero.
 * Definiendo al buscar, la resolucion natural del cargador pide las que hagan falta.
 *
 * <h2>Redefinir</h2>
 *
 * <p>Reemplazar el codigo de una clase ya cargada es algo que solo puede hacer la instrumentacion de
 * la VM. Aca la clase vieja se olvida y la nueva se define de cero, que es lo que se puede hacer
 * desde un cargador: el {@code Class} anterior sigue existiendo para quien lo tenga en la mano, pero
 * lo que se busque de ahora en mas es el nuevo.
 *
 * <p>Esa diferencia se nota en un solo caso y hay que decirla: un objeto creado con la version
 * anterior conserva su comportamiento anterior.
 */
final class DefaultLoaderDelegate implements LoaderDelegate {

    private final Cargador cargador;
    private final Map<String, Class<?>> clases = new HashMap<String, Class<?>>();

    DefaultLoaderDelegate() {
        this.cargador = new Cargador();
        Thread.currentThread().setContextClassLoader(cargador);
    }

    /** El cargador de verdad: guarda bytes y define recien cuando se los pide. */
    private static final class Cargador extends URLClassLoader {

        private final Map<String, byte[]> bytes = new HashMap<String, byte[]>();

        Cargador() {
            super(new URL[0]);
        }

        synchronized void anotar(String nombre, byte[] b) {
            bytes.put(nombre, b);
        }

        synchronized boolean tiene(String nombre) {
            return bytes.containsKey(nombre);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            final byte[] b;
            synchronized (this) {
                b = bytes.get(name);
            }
            if (b == null) {
                return super.findClass(name);
            }
            return defineClass(name, b, 0, b.length);
        }

        void agregar(URL u) {
            addURL(u);
        }
    }

    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        final boolean[] puestas = new boolean[cbcs.length];
        try {
            for (int i = 0; i < cbcs.length; i++) {
                cargador.anotar(cbcs[i].name(), cbcs[i].bytecodes());
                puestas[i] = true;
            }
            // Recien despues de anotarlas todas se las resuelve, para que una que hereda de otra del
            // mismo lote encuentre a su padre sin importar en que orden vinieron.
            for (int i = 0; i < cbcs.length; i++) {
                clases.put(cbcs[i].name(), cargador.loadClass(cbcs[i].name()));
            }
        } catch (Throwable e) {
            throw new ClassInstallException("load: " + e.getMessage(), puestas);
        }
    }

    @Override
    public void classesRedefined(ClassBytecodes[] cbcs) {
        for (final ClassBytecodes cbc : cbcs) {
            cargador.anotar(cbc.name(), cbc.bytecodes());
        }
    }

    @Override
    public void addToClasspath(String path) throws EngineTerminationException, InternalException {
        try {
            for (final String p : path.split(File.pathSeparator)) {
                if (!p.isEmpty()) {
                    cargador.agregar(new File(p).toURI().toURL());
                }
            }
        } catch (MalformedURLException e) {
            throw new InternalException("addToClasspath: " + e.getMessage());
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        final Class<?> c = clases.get(name);
        if (c != null) {
            return c;
        }
        if (cargador.tiene(name)) {
            return cargador.loadClass(name);
        }
        throw new ClassNotFoundException(name + " not found");
    }
}
