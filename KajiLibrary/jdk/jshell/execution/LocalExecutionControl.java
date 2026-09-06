package jdk.jshell.execution;

import java.lang.reflect.Method;

import jdk.jshell.spi.ExecutionControl;

/**
 * Como {@link DirectExecutionControl}, pero el codigo del usuario corre en otro hilo.
 *
 * <h2>Para que el hilo</h2>
 *
 * <p>Para poder cortarlo. Un fragmento con un bucle infinito ejecutado en el hilo de JShell cuelga
 * JShell; ejecutado aparte, {@link #stop} lo interrumpe y la sesion sigue viva. Esa es toda la
 * diferencia con la clase de la que hereda, y es la razon por la que este es el motor por omision.
 *
 * <h2>Como corta</h2>
 *
 * <p>Con {@link Thread#interrupt}, que es lo unico que se puede hacer sin arriesgar la consistencia
 * del proceso. Un fragmento que ignora la interrupcion no se detiene, y esta bien que sea asi:
 * matarlo dejaria a medio terminar cualquier cosa que estuviera haciendo, en un proceso donde
 * ademas vive JShell.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Funciona, con el mismo limite que en el JDK: corta lo que se deja cortar.
 *
 * @since 9
 */
public class LocalExecutionControl extends DirectExecutionControl {

    /** El hilo que esta corriendo codigo del usuario ahora, o {@code null}. */
    private Thread corriendo;

    /** Si se pidio cortar y todavia no se atendio. */
    private boolean cortando;

    /**
     * Un motor con ese cargador.
     *
     * @param loaderDelegate quien instala las clases
     */
    public LocalExecutionControl(LoaderDelegate loaderDelegate) {
        super(loaderDelegate);
    }

    /** Un motor con el cargador por omision. */
    public LocalExecutionControl() {
        super();
    }

    /**
     * Un motor que instala las clases en ese cargador.
     *
     * @param loader el cargador donde se definen las clases del usuario
     */
    public LocalExecutionControl(ClassLoader loader) {
        super(new LoaderPropio(loader));
    }

    /**
     * Instala esas clases.
     *
     * @param cbcs los nombres y el bytecode de cada una
     * @throws ClassInstallException si alguna no se pudo instalar
     * @throws NotImplementedException si el cargador no sabe instalar
     * @throws EngineTerminationException si el motor ya no esta
     */
    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        super.load(cbcs);
    }

    /**
     * Llama al metodo en un hilo aparte y espera el resultado.
     *
     * @param doitMethod el metodo
     * @return la representacion del resultado
     * @throws Exception lo que sea que haya fallado, o {@link ExecutionControl.StoppedException}
     */
    @Override
    protected String invoke(Method doitMethod) throws Exception {
        final Object[] resultado = new Object[1];
        final Throwable[] falla = new Throwable[1];
        final Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    resultado[0] = doitMethod.invoke(null, new Object[0]);
                } catch (Throwable e) {
                    falla[0] = e;
                }
            }
        }, "JShell user code");
        synchronized (this) {
            if (cortando) {
                cortando = false;
                throw new StoppedException();
            }
            corriendo = t;
        }
        t.start();
        try {
            t.join();
        } finally {
            synchronized (this) {
                corriendo = null;
            }
        }
        synchronized (this) {
            if (cortando) {
                cortando = false;
                throw new StoppedException();
            }
        }
        if (falla[0] instanceof Exception) {
            throw (Exception) falla[0];
        }
        if (falla[0] != null) {
            throw new RuntimeException(falla[0]);
        }
        return valueString(resultado[0]);
    }

    /**
     * Interrumpe el hilo que este corriendo codigo del usuario.
     *
     * <p>Si no hay ninguno, deja la marca puesta: la peticion puede llegar justo entre que JShell
     * decide ejecutar y el hilo arranca, y perderla ahi seria peor que atenderla tarde.
     *
     * @throws EngineTerminationException si el motor ya no esta
     * @throws InternalException si no se pudo cortar
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        final Thread t;
        synchronized (this) {
            cortando = true;
            t = corriendo;
        }
        if (t != null) {
            t.interrupt();
        }
    }

    /** Aviso de que se va a entrar al codigo del usuario. */
    @Override
    protected void clientCodeEnter() {
    }

    /** Aviso de que se salio del codigo del usuario. */
    @Override
    protected void clientCodeLeave() {
    }

    /**
     * El cargador que se usa cuando a este motor le pasan uno ya hecho.
     *
     * <p>No define nada: las clases del usuario se suponen ya instaladas del otro lado. Es el caso
     * de quien embebe JShell dentro de su propia aplicacion y quiere que los fragmentos vean sus
     * clases.
     */
    private static final class LoaderPropio implements LoaderDelegate {

        private final ClassLoader loader;

        LoaderPropio(ClassLoader loader) {
            this.loader = loader;
        }

        @Override
        public void load(ClassBytecodes[] cbcs)
                throws ClassInstallException, NotImplementedException, EngineTerminationException {
            throw new NotImplementedException("load: el cargador provisto no define clases");
        }

        @Override
        public void classesRedefined(ClassBytecodes[] cbcs) {
        }

        @Override
        public void addToClasspath(String path)
                throws EngineTerminationException, InternalException {
            throw new InternalException("addToClasspath: el cargador provisto no se puede ampliar");
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return loader.loadClass(name);
        }
    }
}
