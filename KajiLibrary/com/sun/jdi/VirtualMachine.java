package com.sun.jdi;

/**
 * La maquina virtual que se esta depurando, vista desde el depurador.
 *
 * <p>Es el objeto raiz de JDI: lo devuelve un conector de {@link com.sun.jdi.connect}, y de el
 * cuelga todo lo demas --las clases cargadas, los hilos, la cola de eventos, el gestor de
 * peticiones.
 *
 * <h2>Las consultas de capacidad</h2>
 *
 * <p>La mitad de esta interfaz son metodos {@code canXxx()}. No es redundancia: JDWP es un
 * protocolo negociado, y una VM del otro lado puede no soportar --o tener apagado-- casi cualquier
 * servicio. Un depurador serio pregunta antes de ofrecer la funcion en su interfaz, porque la
 * alternativa es enterarse con una excepcion en el medio de una sesion.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Estan declarados los miembros que **no necesitan el resto de JDI**: las constantes de traza,
 * el control de ejecucion, las capacidades, el estrato por omision y los datos de identificacion.
 *
 * <p>Faltan los que devuelven otros reflejos --`allClasses`, `allThreads`, `eventQueue`,
 * `eventRequestManager`, la familia `mirrorOf`-- porque cada uno arrastra su parte de
 * `com.sun.jdi`, `com.sun.jdi.event` y `com.sun.jdi.request`, que todavia no estan. Se agregan
 * cuando esos paquetes existan; declararlos ahora con tipos inventados seria peor que no tenerlos.
 */
public interface VirtualMachine extends Mirror {

    /** No trazar nada del trafico JDWP. */
    int TRACE_NONE = 0;

    /** Trazar los paquetes que salen hacia la VM depurada. */
    int TRACE_SENDS = 0x01;

    /** Trazar los paquetes que llegan de la VM depurada. */
    int TRACE_RECEIVES = 0x02;

    /** Trazar los eventos que llegan. */
    int TRACE_EVENTS = 0x04;

    /** Trazar la creacion de reflejos de tipo. */
    int TRACE_REFTYPES = 0x08;

    /** Trazar la creacion de reflejos de objeto. */
    int TRACE_OBJREFS = 0x10;

    /** Trazar todo. */
    int TRACE_ALL = 0x00ffffff;

    /**
     * Suspende todos los hilos de la VM depurada.
     *
     * <p>Las suspensiones se **cuentan**: dos `suspend()` piden dos `resume()`. Es lo que permite
     * que dos partes del depurador suspendan sin pisarse.
     */
    void suspend();

    /** Levanta una suspension. Ver el conteo en {@link #suspend}. */
    void resume();

    /**
     * El proceso de la VM depurada, o `null` si el depurador no la lanzo.
     *
     * <p>Solo hay proceso cuando se llego por un {@link com.sun.jdi.connect.LaunchingConnector}: si
     * el depurador se **adjunto** a una VM que ya corria, no tiene su `Process`.
     */
    Process process();

    /**
     * Corta la sesion de depuracion y libera todo.
     *
     * <p>La VM depurada **sigue corriendo**: se le levantan las suspensiones y se le sacan las
     * peticiones de evento. Es lo contrario de {@link #exit}.
     */
    void dispose();

    /**
     * Termina la VM depurada con ese codigo de salida.
     *
     * <p>Despues de esto no se le puede preguntar nada mas.
     */
    void exit(int exitCode);

    /** Si se pueden pedir eventos por modificacion de un campo. */
    boolean canWatchFieldModification();

    /** Si se pueden pedir eventos por lectura de un campo. */
    boolean canWatchFieldAccess();

    /** Si se puede leer el bytecode de un metodo. */
    boolean canGetBytecodes();

    /** Si se puede saber si un miembro es sintetico. */
    boolean canGetSyntheticAttribute();

    /** Si se pueden listar los monitores que un hilo tiene tomados. */
    boolean canGetOwnedMonitorInfo();

    /** Si se puede saber por que monitor esta esperando un hilo. */
    boolean canGetCurrentContendedMonitor();

    /** Si se puede saber que hilos esperan por un monitor. */
    boolean canGetMonitorInfo();

    /** Si una peticion de evento se puede filtrar por instancia. */
    boolean canUseInstanceFilters();

    /** Si se pueden redefinir clases ya cargadas. */
    boolean canRedefineClasses();

    /**
     * Si una redefinicion puede agregar metodos.
     *
     * @deprecated Ninguna VM lo soporta desde hace mucho, y la especificacion de JDWP lo dejo de
     *     lado. Da `false` siempre.
     */
    @Deprecated
    boolean canAddMethod();

    /**
     * Si una redefinicion puede cambiar la forma de la clase sin restricciones.
     *
     * @deprecated Igual que {@link #canAddMethod}: quedo sin soporte.
     */
    @Deprecated
    boolean canUnrestrictedlyRedefineClasses();

    /** Si se pueden descartar marcos de la pila de un hilo. */
    boolean canPopFrames();

    /** Si se puede leer el atributo `SourceDebugExtension` de una clase. */
    boolean canGetSourceDebugExtension();

    /** Si se puede pedir el evento de muerte de la VM. */
    boolean canRequestVMDeathEvent();

    /** Si un evento de salida de metodo puede traer el valor devuelto. */
    boolean canGetMethodReturnValues();

    /** Si se pueden contar y listar las instancias de un tipo. */
    boolean canGetInstanceInfo();

    /** Si una peticion de evento se puede filtrar por nombre de archivo fuente. */
    boolean canUseSourceNameFilters();

    /** Si se puede forzar el retorno anticipado de un metodo. */
    boolean canForceEarlyReturn();

    /**
     * Si la VM depurada se puede modificar.
     *
     * <p>Con `false` la sesion es de solo lectura: se puede mirar, no tocar. Es el caso de un
     * volcado de memoria abierto como si fuera una VM.
     */
    boolean canBeModified();

    /** Si se pueden pedir eventos de monitor. */
    boolean canRequestMonitorEvents();

    /** Si un evento de monitor puede decir en que marco ocurrio. */
    boolean canGetMonitorFrameInfo();

    /** Si se puede leer la version del formato de archivo de clase. */
    boolean canGetClassFileVersion();

    /** Si se puede leer el pool de constantes de una clase. */
    boolean canGetConstantPool();

    /**
     * Si se puede consultar informacion de modulos.
     *
     * <p>Es `default` --y no abstracto-- porque llego con Java 9, y una implementacion de JDI
     * escrita antes tiene que seguir compilando. Por omision dice que no, que es la respuesta
     * correcta para cualquiera de esas.
     */
    default boolean canGetModuleInfo() {
        return false;
    }

    /**
     * Fija el estrato por omision para el codigo con varios lenguajes fuente.
     *
     * <p>Un `.class` generado desde JSP lleva mapas de linea para dos "estratos" --el bytecode y el
     * JSP-- y esto elige cual usar cuando nadie pide uno.
     *
     * @param stratum el nombre del estrato, o `null` para el que la clase declare como suyo
     */
    void setDefaultStratum(String stratum);

    /** El estrato por omision, o `null` si es el que cada clase declare. */
    String getDefaultStratum();

    /** Una descripcion legible de la VM depurada. */
    String description();

    /** La version de la VM depurada, como la reporta ella. */
    String version();

    /** El nombre de la VM depurada, como lo reporta ella. */
    String name();

    /**
     * Fija que trafico JDWP se traza.
     *
     * @param traceFlags una combinacion `or` de las constantes `TRACE_*`
     */
    void setDebugTraceMode(int traceFlags);
}
