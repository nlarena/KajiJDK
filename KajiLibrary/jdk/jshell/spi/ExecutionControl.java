package jdk.jshell.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * El motor que ejecuta lo que se escribe en `jshell`.
 *
 * <p>Es la frontera entre la mitad que **compila** y la mitad que **corre**, y esa frontera existe
 * porque las dos suelen estar en procesos distintos: `jshell` compila cada fragmento a un `.class`
 * en su propia VM y se lo manda a otra para ejecutarlo. La razon es que el codigo que uno teclea en
 * una consola se cuelga, se rompe y llama a `System.exit`, y ninguna de las tres cosas puede
 * llevarse puesta la consola.
 *
 * <p>De ahi la forma de la interfaz, que si no se lee rara:
 *
 * <ul>
 *   <li>{@link #load} y {@link #redefine} mandan **bytes**, no objetos: del otro lado no hay las
 *       mismas clases.
 *   <li>{@link #invoke} y {@link #varValue} devuelven **cadenas**: el valor real vive en la otra
 *       VM, y lo unico que puede cruzar es su representacion.
 *   <li>{@link #stop} existe aparte de `close`: cortar un fragmento colgado no es cerrar el motor.
 * </ul>
 *
 * <p>Las excepciones anidadas dividen el mismo eje: {@link RunException} es "el codigo del usuario
 * fallo" --se muestra y la sesion sigue-- y {@link EngineTerminationException} es "el motor se
 * murio" --hay que rearmarlo.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>La interfaz y sus diez tipos anidados estan enteros; los dos {@code generate} estaticos
 * tambien, y de verdad: buscan el proveedor con {@link ServiceLoader} y le pasan los parametros.
 * Lo que no hay es ningun proveedor instalado, asi que hoy lanzan
 * {@link IllegalArgumentException} por no encontrarlo --que es lo mismo que hace el JDK cuando se
 * nombra un motor que no esta.
 *
 * @since 9
 */
public interface ExecutionControl extends AutoCloseable {

    /**
     * Carga esas clases en el motor.
     *
     * @param cbcs las clases, cada una con su nombre y sus bytes
     * @throws ClassInstallException si alguna no se pudo cargar; la excepcion dice cuales si
     * @throws NotImplementedException si el motor no sabe cargar clases
     * @throws EngineTerminationException si el motor se murio
     */
    void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException;

    /**
     * Reemplaza el cuerpo de clases ya cargadas.
     *
     * <p>Es lo que permite redefinir un metodo sin reiniciar la sesion. No todos los motores
     * pueden: el que no puede lanza {@link NotImplementedException} y `jshell` recompila.
     *
     * @throws ClassInstallException si alguna no se pudo redefinir
     * @throws NotImplementedException si el motor no sabe redefinir
     * @throws EngineTerminationException si el motor se murio
     */
    void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException;

    /**
     * Llama a un metodo estatico sin argumentos y devuelve su resultado como texto.
     *
     * @param className la clase
     * @param methodName el metodo, estatico y sin argumentos
     * @return el resultado ya convertido a texto en la VM que ejecuta
     * @throws RunException si el codigo del usuario lanzo, se corto o no resolvio
     * @throws EngineTerminationException si el motor se murio
     * @throws InternalException si fallo la maquinaria, no el codigo del usuario
     */
    String invoke(String className, String methodName)
            throws RunException, EngineTerminationException, InternalException;

    /**
     * El valor de una variable, como texto.
     *
     * @param className la clase que la declara
     * @param varName el nombre de la variable
     * @throws RunException si leerla lanzo
     * @throws EngineTerminationException si el motor se murio
     * @throws InternalException si fallo la maquinaria
     */
    String varValue(String className, String varName)
            throws RunException, EngineTerminationException, InternalException;

    /**
     * Agrega una ruta al classpath del motor.
     *
     * @throws EngineTerminationException si el motor se murio
     * @throws InternalException si la ruta no se pudo agregar
     */
    void addToClasspath(String path) throws EngineTerminationException, InternalException;

    /**
     * Corta lo que se este ejecutando.
     *
     * <p>Se llama **desde otro hilo**: el que pidio la ejecucion esta bloqueado esperandola. El
     * motor sigue vivo despues.
     *
     * @throws EngineTerminationException si el motor se murio
     * @throws InternalException si no se pudo cortar
     */
    void stop() throws EngineTerminationException, InternalException;

    /**
     * Una operacion propia de este motor, que la interfaz no cubre.
     *
     * <p>Es la valvula de escape del SPI: un motor con capacidades extra las expone por aca, y el
     * cliente que las conozca las nombra por su cadena.
     *
     * @throws RunException si el codigo del usuario lanzo
     * @throws EngineTerminationException si el motor se murio
     * @throws InternalException si el motor no conoce ese comando
     */
    Object extensionCommand(String command, Object arg)
            throws RunException, EngineTerminationException, InternalException;

    /**
     * Cierra el motor.
     *
     * <p>Redeclarado sin {@code throws} --{@link AutoCloseable#close()} lo declara-- para que un
     * `try`-con-recursos sobre un `ExecutionControl` no obligue a atrapar nada.
     */
    @Override
    void close();

    /**
     * El motor que nombre esa especificacion.
     *
     * <p>La especificacion es {@code nombre} o {@code nombre:clave(valor),clave(valor)}. El nombre
     * se busca entre los {@link ExecutionControlProvider} instalados; los parametros se le pasan
     * encima de sus {@link ExecutionControlProvider#defaultParameters()}.
     *
     * @param env el entorno que el motor usa para hablar con el usuario
     * @param spec la especificacion
     * @return el motor
     * @throws IllegalArgumentException si la especificacion esta mal formada o nombra un motor que
     *     no esta instalado
     * @throws Throwable lo que sea que el proveedor lance al construirlo
     */
    static ExecutionControl generate(ExecutionEnv env, String spec) throws Throwable {
        if (env == null) {
            throw new NullPointerException("env");
        }
        if (spec == null) {
            throw new NullPointerException("spec");
        }
        int corte = spec.indexOf(':');
        String nombre = corte < 0 ? spec : spec.substring(0, corte);
        Map<String, String> parametros = corte < 0
                ? new HashMap<String, String>()
                : parsearParametros(spec.substring(corte + 1));
        return generate(env, nombre.trim(), parametros);
    }

    /**
     * El motor de ese nombre, con esos parametros.
     *
     * @param env el entorno que el motor usa para hablar con el usuario
     * @param name el nombre del proveedor
     * @param parameters los parametros, o `null` para los del proveedor
     * @return el motor
     * @throws IllegalArgumentException si no hay ningun proveedor con ese nombre
     * @throws Throwable lo que sea que el proveedor lance al construirlo
     */
    static ExecutionControl generate(ExecutionEnv env, String name, Map<String, String> parameters)
            throws Throwable {
        if (env == null) {
            throw new NullPointerException("env");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        Iterator<ExecutionControlProvider> it =
                ServiceLoader.load(ExecutionControlProvider.class).iterator();
        while (it.hasNext()) {
            ExecutionControlProvider p = it.next();
            if (name.equals(p.name())) {
                return p.generate(env, parameters == null ? p.defaultParameters() : parameters);
            }
        }
        throw new IllegalArgumentException("no ExecutionControlProvider named: " + name);
    }

    /** Parte {@code clave(valor),clave(valor)}. De uso interno de los dos `generate`. */
    private static Map<String, String> parsearParametros(String texto) {
        Map<String, String> out = new HashMap<String, String>();
        int i = 0;
        int n = texto.length();
        while (i < n) {
            int abre = texto.indexOf('(', i);
            if (abre < 0) {
                throw new IllegalArgumentException("expected '(' in: " + texto);
            }
            int cierra = texto.indexOf(')', abre);
            if (cierra < 0) {
                throw new IllegalArgumentException("expected ')' in: " + texto);
            }
            String clave = texto.substring(i, abre).trim();
            if (clave.isEmpty()) {
                throw new IllegalArgumentException("empty parameter name in: " + texto);
            }
            out.put(clave, texto.substring(abre + 1, cierra));
            i = cierra + 1;
            if (i < n) {
                if (texto.charAt(i) != ',') {
                    throw new IllegalArgumentException("expected ',' in: " + texto);
                }
                i++;
            }
        }
        return out;
    }

    /**
     * Una clase compilada, lista para mandar al motor.
     *
     * <p>{@link Serializable} porque tiene que cruzar a la otra VM; y por eso mismo copia los bytes
     * al entrar y al salir, para que nadie modifique lo que ya se mando.
     */
    final class ClassBytecodes implements Serializable {

        private static final long serialVersionUID = 54506481972415973L;

        private final String name;
        private final byte[] bytecodes;

        /**
         * Una clase con ese nombre y esos bytes.
         *
         * @param name el nombre binario
         * @param bytecodes el contenido del `.class`; se copia
         */
        public ClassBytecodes(String name, byte[] bytecodes) {
            this.name = name;
            this.bytecodes = bytecodes.clone();
        }

        /** Una copia de los bytes del `.class`. */
        public byte[] bytecodes() {
            return this.bytecodes.clone();
        }

        /** El nombre binario de la clase. */
        public String name() {
            return this.name;
        }
    }

    /** La raiz de todo lo que puede salir mal en un motor. */
    abstract class ExecutionControlException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Con ese detalle.
         *
         * @param message el detalle
         */
        public ExecutionControlException(String message) {
            super(message);
        }
    }

    /**
     * No se pudieron instalar todas las clases.
     *
     * <p>Trae **cuales si** entraron: la carga no es atomica, y `jshell` necesita saber que quedo
     * a medio camino para no volver a mandarlo.
     */
    class ClassInstallException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        private final boolean[] installed;

        /**
         * Con ese detalle y ese mapa de exito.
         *
         * @param message el detalle
         * @param installed una posicion por clase, en el orden en que se mandaron
         */
        public ClassInstallException(String message, boolean[] installed) {
            super(message);
            this.installed = installed == null ? null : installed.clone();
        }

        /** Que clases entraron, en el orden en que se mandaron. */
        public boolean[] installed() {
            return this.installed == null ? null : this.installed.clone();
        }
    }

    /**
     * El motor se murio: no se le puede pedir nada mas.
     *
     * <p>Es la unica de la familia que **no** deja seguir la sesion. Las demas hablan del codigo
     * del usuario o de un servicio que falta; esta habla del motor.
     */
    class EngineTerminationException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        /**
         * Con ese detalle.
         *
         * @param message el detalle
         */
        public EngineTerminationException(String message) {
            super(message);
        }
    }

    /** Fallo la maquinaria del motor, no el codigo del usuario. */
    class InternalException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        /**
         * Con ese detalle.
         *
         * @param message el detalle
         */
        public InternalException(String message) {
            super(message);
        }
    }

    /**
     * Este motor no implementa esa operacion.
     *
     * <p>Hereda de {@link InternalException} y no de la raiz: para el que llama es un fallo de la
     * maquinaria, y quien no distingue los casos la maneja igual.
     */
    class NotImplementedException extends InternalException {

        private static final long serialVersionUID = 1L;

        /**
         * Con ese detalle.
         *
         * @param message el detalle
         */
        public NotImplementedException(String message) {
            super(message);
        }
    }

    /**
     * La ejecucion del codigo del usuario no llego a terminar bien.
     *
     * <p>Abstracta y con constructor privado: la lista de subclases es cerrada --lanzo, no resolvio
     * o se corto-- y un motor no puede inventar una cuarta. Es un tipo sellado escrito antes de que
     * el lenguaje tuviera `sealed`.
     */
    abstract class RunException extends ExecutionControlException {

        private static final long serialVersionUID = 1L;

        private RunException(String message) {
            super(message);
        }
    }

    /**
     * El codigo del usuario lanzo una excepcion.
     *
     * <p>Trae el **nombre** de la clase de la excepcion y no la excepcion: la clase vive en la otra
     * VM y puede no existir en la de `jshell`.
     */
    class UserException extends RunException {

        private static final long serialVersionUID = 1L;

        private final String causeExceptionClass;

        /**
         * Con ese detalle, esa clase y esa pila.
         *
         * @param message el mensaje de la excepcion original
         * @param causeExceptionClass el nombre de su clase
         * @param stackElements su pila, tal como se vio en la otra VM
         */
        public UserException(String message, String causeExceptionClass,
                StackTraceElement[] stackElements) {
            super(message);
            this.causeExceptionClass = causeExceptionClass;
            setStackTrace(stackElements);
        }

        /** El nombre de la clase de la excepcion que lanzo el codigo del usuario. */
        public String causeExceptionClass() {
            return this.causeExceptionClass;
        }
    }

    /**
     * El fragmento uso algo que todavia no esta definido.
     *
     * <p>Es el caso que hace usable una consola: en `jshell` se puede escribir un metodo que llame
     * a otro que aun no existe. El fragmento compila, y recien al ejecutarlo sale esto, con el
     * identificador de lo que falta.
     */
    class ResolutionException extends RunException {

        private static final long serialVersionUID = 1L;

        private final int id;

        /**
         * Con ese identificador y esa pila.
         *
         * @param id el identificador de lo que falta
         * @param stackElements la pila, tal como se vio en la otra VM
         */
        public ResolutionException(int id, StackTraceElement[] stackElements) {
            super("resolution exception: " + id);
            this.id = id;
            setStackTrace(stackElements);
        }

        /** El identificador de lo que falta. */
        public int id() {
            return this.id;
        }
    }

    /** Se corto la ejecucion con {@link ExecutionControl#stop()}. */
    class StoppedException extends RunException {

        private static final long serialVersionUID = 1L;

        /** Sin detalle: no hay nada mas que decir que "lo cortaron". */
        public StoppedException() {
            super("stopped by user");
        }
    }
}
