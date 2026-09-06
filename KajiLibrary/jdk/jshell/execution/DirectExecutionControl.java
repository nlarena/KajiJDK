package jdk.jshell.execution;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.SPIResolutionException;

/**
 * El motor que ejecuta los fragmentos <strong>en esta misma maquina virtual</strong>, por reflexion.
 *
 * <h2>Que hace</h2>
 *
 * <p>JShell compila cada fragmento a una clase con un metodo, se la manda a {@link #load}, y despues
 * pide {@link #invoke}. Este motor instala las clases con un {@link LoaderDelegate}, busca el metodo
 * y lo llama. No hay proceso aparte ni protocolo: es una llamada.
 *
 * <h2>Por que el resultado es un {@code String}</h2>
 *
 * <p>Porque el motor puede estar del otro lado de un socket, y ahi devolver el objeto obligaria a
 * serializarlo --a el y a todo lo que cuelgue de el--. Se manda su representacion y listo. Que la
 * version local haga lo mismo no es desperdicio: es lo que garantiza que un fragmento se vea igual
 * corriendo local o remoto.
 *
 * <h2>Las excepciones del usuario</h2>
 *
 * <p>Una excepcion del codigo del usuario no es una falla de este motor, asi que no puede subir tal
 * cual: se la convierte en {@link ExecutionControl.UserException}, que lleva el nombre de la clase
 * original y la traza. La conversion pasa por {@link #throwConvertedInvocationException}, que esta
 * separada justamente para que un motor remoto pueda hacerla distinto.
 *
 * <p>El caso raro es {@link SPIResolutionException}: no es un error del programa sino la forma en
 * que JShell avisa que el fragmento uso algo que todavia no esta definido. Por eso se la reconoce y
 * se la convierte en {@link ExecutionControl.ResolutionException}, que JShell entiende.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Funciona. Definir una clase desde su bytecode y llamarla por reflexion es todo lo que necesita,
 * y las dos cosas andan. Lo unico que no puede es {@link #stop}: cortar lo que se esta ejecutando
 * requiere que la ejecucion pase por otro hilo, y eso lo agrega {@link LocalExecutionControl}.
 *
 * @since 9
 */
public class DirectExecutionControl implements ExecutionControl {

    private final LoaderDelegate loaderDelegate;

    /**
     * Un motor con ese cargador.
     *
     * @param loaderDelegate quien instala las clases
     */
    public DirectExecutionControl(LoaderDelegate loaderDelegate) {
        this.loaderDelegate = loaderDelegate;
    }

    /** Un motor con el cargador por omision. */
    public DirectExecutionControl() {
        this(new DefaultLoaderDelegate());
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
        loaderDelegate.load(cbcs);
        classesRedefined(cbcs);
    }

    /**
     * Reemplaza el codigo de clases que ya estaban.
     *
     * @param cbcs las clases y su bytecode nuevo
     * @throws ClassInstallException si no se pudieron reemplazar
     * @throws NotImplementedException si este motor no sabe redefinir
     * @throws EngineTerminationException si el motor ya no esta
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        throw new NotImplementedException("redefine not supported");
    }

    /**
     * Aviso de que esas clases cambiaron de contenido.
     *
     * @param cbcs las clases redefinidas
     * @throws NotImplementedException si este motor no lo soporta
     * @throws EngineTerminationException si el motor ya no esta
     */
    protected void classesRedefined(ClassBytecodes[] cbcs)
            throws NotImplementedException, EngineTerminationException {
        loaderDelegate.classesRedefined(cbcs);
    }

    /**
     * Llama a ese metodo estatico sin argumentos y devuelve su resultado.
     *
     * @param className la clase
     * @param methodName el metodo
     * @return la representacion del resultado
     * @throws RunException si el codigo del usuario fallo
     * @throws InternalException si fallo el motor
     * @throws EngineTerminationException si el motor ya no esta
     */
    @Override
    public String invoke(String className, String methodName)
            throws RunException, InternalException, EngineTerminationException {
        final Method m;
        try {
            m = findClass(className).getDeclaredMethod(methodName, new Class<?>[0]);
            m.setAccessible(true);
        } catch (Throwable e) {
            throw new InternalException("invoke: " + e.getMessage());
        }
        clientCodeEnter();
        try {
            return invoke(m);
        } catch (InvocationTargetException e) {
            return throwConvertedInvocationException(e.getCause());
        } catch (Throwable e) {
            return throwConvertedOtherException(e);
        } finally {
            clientCodeLeave();
        }
    }

    /**
     * El valor de una variable estatica, como texto.
     *
     * @param className la clase
     * @param varName la variable
     * @return la representacion del valor
     * @throws RunException si el codigo del usuario fallo
     * @throws EngineTerminationException si el motor ya no esta
     * @throws InternalException si fallo el motor
     */
    @Override
    public String varValue(String className, String varName)
            throws RunException, EngineTerminationException, InternalException {
        final Object v;
        try {
            final java.lang.reflect.Field f = findClass(className).getDeclaredField(varName);
            f.setAccessible(true);
            v = f.get(null);
        } catch (Throwable e) {
            throw new InternalException("varValue: " + e.getMessage());
        }
        return valueString(v);
    }

    /**
     * Agrega una entrada al camino de busqueda de clases.
     *
     * @param path la entrada
     * @throws EngineTerminationException si el motor ya no esta
     * @throws InternalException si no se pudo agregar
     */
    @Override
    public void addToClasspath(String path) throws EngineTerminationException, InternalException {
        loaderDelegate.addToClasspath(path);
    }

    /**
     * Corta lo que se este ejecutando.
     *
     * @throws EngineTerminationException si el motor ya no esta
     * @throws InternalException si no se pudo cortar
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        throw new NotImplementedException("stop: not supported");
    }

    /**
     * Una operacion que no esta en la interfaz, para motores con capacidades propias.
     *
     * @param command el nombre de la operacion
     * @param arg su argumento
     * @return lo que devuelva
     * @throws RunException si el codigo del usuario fallo
     * @throws EngineTerminationException si el motor ya no esta
     * @throws InternalException si fallo el motor
     */
    @Override
    public Object extensionCommand(String command, Object arg)
            throws RunException, EngineTerminationException, InternalException {
        throw new NotImplementedException("extensionCommand: " + command);
    }

    /** Cierra el motor. */
    @Override
    public void close() {
    }

    /**
     * Busca una clase instalada.
     *
     * @param className el nombre
     * @return la clase
     * @throws ClassNotFoundException si no esta
     */
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        return loaderDelegate.findClass(className);
    }

    /**
     * La llamada propiamente dicha.
     *
     * <p>Esta separada para que las subclases puedan envolverla: {@link LocalExecutionControl} la
     * corre en otro hilo, que es lo que hace posible cortarla.
     *
     * @param doitMethod el metodo
     * @return la representacion del resultado
     * @throws Exception lo que sea que haya fallado
     */
    protected String invoke(Method doitMethod) throws Exception {
        return valueString(doitMethod.invoke(null, new Object[0]));
    }

    /**
     * Como se ve un valor del lado de JShell.
     *
     * <p>Los textos van entre comillas y el resto por su {@code toString}. Sin las comillas, el
     * {@code null} y la cadena {@code "null"} se verian igual, y son cosas distintas.
     *
     * @param value el valor
     * @return su representacion
     */
    protected static String valueString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Character) {
            return "'" + value + "'";
        }
        return value.toString();
    }

    /**
     * Convierte una excepcion que lanzo el codigo del usuario.
     *
     * <p>Nunca devuelve: siempre lanza. Devuelve {@code String} para poder escribirse como
     * {@code return throwConverted...(e)} en el lugar donde hace falta un valor, que es una forma
     * de que el compilador sepa que ese camino no sigue.
     *
     * @param ex lo que lanzo el usuario
     * @return nunca
     * @throws RunException la version convertida
     * @throws InternalException si la conversion misma fallo
     */
    protected String throwConvertedInvocationException(Throwable ex)
            throws RunException, InternalException {
        if (ex instanceof SPIResolutionException) {
            final SPIResolutionException e = (SPIResolutionException) ex;
            throw new ResolutionException(e.id(), ex.getStackTrace());
        }
        throw new UserException(String.valueOf(ex.getMessage()), ex.getClass().getName(),
                ex.getStackTrace());
    }

    /**
     * Convierte una excepcion que no vino del codigo del usuario.
     *
     * @param ex lo que fallo
     * @return nunca
     * @throws RunException si igual corresponde tratarla como del usuario
     * @throws InternalException lo habitual
     */
    protected String throwConvertedOtherException(Throwable ex)
            throws RunException, InternalException {
        throw new InternalException(ex.toString() + Arrays.toString(ex.getStackTrace()));
    }

    /**
     * Aviso de que se va a entrar al codigo del usuario.
     *
     * @throws InternalException si el motor no esta en condiciones
     */
    protected void clientCodeEnter() throws InternalException {
    }

    /**
     * Aviso de que se salio del codigo del usuario.
     *
     * @throws InternalException si el motor no esta en condiciones
     */
    protected void clientCodeLeave() throws InternalException {
    }
}
