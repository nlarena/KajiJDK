package jdk.jshell.execution;

/**
 * El vocabulario del protocolo entre JShell y el motor que ejecuta.
 *
 * <h2>Por que hay un protocolo y no llamadas</h2>
 *
 * <p>El motor puede estar en otra maquina virtual --de eso se trata {@link JdiExecutionControl}--,
 * asi que las operaciones de {@link jdk.jshell.spi.ExecutionControl} viajan como mensajes por un par
 * de flujos. Cada mensaje es un nombre de comando y sus argumentos; cada respuesta es un codigo y lo
 * que corresponda.
 *
 * <p>Los nombres de comando son el texto de su propia constante --{@code CMD_LOAD} vale
 * {@code "CMD_LOAD"}-- porque asi el volcado del flujo se lee sin tabla de traduccion. En un
 * protocolo que sirve para depurar un depurador, eso vale mas que los tres bytes que se ahorrarian
 * numerandolos.
 *
 * <h2>{@link #COMMAND_PREFIX}</h2>
 *
 * <p>Va delante de cada respuesta. El flujo lleva mezcladas dos cosas --lo que el programa del
 * usuario imprime y las respuestas del motor-- y esta marca es lo que las separa: sin ella, un
 * {@code System.out.println} del usuario que dijera {@code "CMD_LOAD"} seria indistinguible de una
 * respuesta.
 *
 * <p>Los valores son los del JDK 25 y no se pueden elegir: un motor de esta biblioteca tiene que
 * poder hablar con el agente del JDK y al reves.
 */
final class RemoteCodes {

    /**
     * La marca que precede a cada respuesta del motor.
     *
     * <p>En hexadecimal es {@code 0xC03DC03D}, o sea "code" repetido dos veces. Que sea un patron
     * reconocible a ojo no es coqueteria: cuando el flujo se desincroniza, es lo unico que permite
     * ver donde volvio a engancharse.
     */
    static final int COMMAND_PREFIX = 0xC03DC03D;

    /**
     * El texto con que viaja un {@code null}.
     *
     * <p>{@code writeUTF} no sabe escribir {@code null} y los valores del usuario pueden serlo. Este
     * centinela lleva caracteres de control justamente para que ningun {@code toString} razonable lo
     * produzca por accidente. Es un compromiso conocido; esta escrito asi en el JDK y se lo copia
     * porque el otro lado del flujo puede ser el agente del JDK.
     */
    static final String NULO = "\u0002*?*NULL*?*\u0003";

    /** Cerrar el motor. */
    static final String CMD_CLOSE = "CMD_CLOSE";

    /** Instalar clases nuevas. */
    static final String CMD_LOAD = "CMD_LOAD";

    /** Reemplazar el codigo de clases que ya estaban. */
    static final String CMD_REDEFINE = "CMD_REDEFINE";

    /** Llamar a un metodo. */
    static final String CMD_INVOKE = "CMD_INVOKE";

    /** Leer el valor de una variable. */
    static final String CMD_VAR_VALUE = "CMD_VAR_VALUE";

    /** Agregar una entrada al classpath. */
    static final String CMD_ADD_CLASSPATH = "CMD_ADD_CLASSPATH";

    /** Cortar lo que se este ejecutando. */
    static final String CMD_STOP = "CMD_STOP";

    /** Salio bien. */
    static final int RESULT_SUCCESS = 100;

    /** El motor se termino y no va a atender mas. */
    static final int RESULT_TERMINATED = 101;

    /** Ese motor no implementa esa operacion. */
    static final int RESULT_NOT_IMPLEMENTED = 102;

    /** Fallo el motor, no el codigo del usuario. */
    static final int RESULT_INTERNAL_PROBLEM = 103;

    /** El codigo del usuario lanzo una excepcion. */
    static final int RESULT_USER_EXCEPTION = 104;

    /**
     * El codigo del usuario llamo a algo que todavia no esta definido.
     *
     * <p>"Corralled" es como JShell llama al metodo de relleno que pone en lugar de uno que el
     * usuario menciono pero aun no escribio. Ejecutarlo no es un error del programa: es la forma en
     * que JShell avisa que falta esa definicion.
     */
    static final int RESULT_CORRALLED = 105;

    /** No se pudieron instalar las clases. */
    static final int RESULT_CLASS_INSTALL_EXCEPTION = 106;

    /** Se corto por un {@link #CMD_STOP}. */
    static final int RESULT_STOPPED = 107;

    /** Como {@link #RESULT_USER_EXCEPTION}, y ademas viaja la cadena de causas. */
    static final int RESULT_USER_EXCEPTION_CHAINED = 108;

    private RemoteCodes() {
    }
}
