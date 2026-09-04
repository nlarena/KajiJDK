package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Line -- un camino por donde pasa audio.
 *
 * <p>La abstraccion central del paquete: un mezclador, una entrada de microfono, una salida de
 * parlantes, un clip cargado en memoria. Todos son lineas.
 *
 * <h2>Abrir no es lo mismo que arrancar</h2>
 *
 * <p>{@link #open} reserva el recurso del sistema; recien ahi la linea tiene sus controles y se le
 * puede preguntar por ellos. Arrancar el flujo de audio es otra cosa y esta en {@link DataLine}.
 *
 * <p>Es {@link AutoCloseable}, asi que sirve en un {@code try} con recursos -- y conviene, porque una
 * linea abierta que no se cierra deja el dispositivo tomado para todo el sistema, no solo para este
 * programa.
 *
 * <p>{@link #close} de una linea ya cerrada no hace nada.
 *
 * <h2>Los controles llegan despues de abrir</h2>
 *
 * <p>{@link #getControls} sobre una linea cerrada devuelve un arreglo vacio, no null. No es un error:
 * los controles dependen del recurso concreto que se reservo, y antes de abrir no hay ninguno.
 */
public interface Line extends AutoCloseable {

    /** Que clase de linea es y que formatos acepta. */
    Line.Info getLineInfo();

    /**
     * Reserva el recurso del sistema.
     *
     * @throws LineUnavailableException si esta ocupado
     * @throws IllegalStateException si ya estaba abierta con otros parametros
     */
    void open() throws LineUnavailableException;

    /** Lo libera. Sobre una linea cerrada no hace nada. */
    void close();

    /** Si esta abierta. */
    boolean isOpen();

    /** Las perillas que tiene; vacio si esta cerrada. Ver la nota de la clase. */
    Control[] getControls();

    /** Si tiene esa perilla. */
    boolean isControlSupported(Control.Type control);

    /**
     * Esa perilla.
     *
     * @throws IllegalArgumentException si no la tiene
     */
    Control getControl(Control.Type control);

    /** Registra un escucha de apertura, cierre, arranque y parada. */
    void addLineListener(LineListener listener);

    /** Lo da de baja. */
    void removeLineListener(LineListener listener);

    /**
     * Que clase de linea es.
     *
     * <p>Se usa para <b>pedir</b> una linea sin tener una: se arma un {@code Info} que describa lo que
     * hace falta y se le pasa a {@code AudioSystem.getLine}. Es el patron de todo el paquete.
     *
     * <p>{@link #matches} no es simetrico, igual que en {@link AudioFormat}: pregunta si el argumento
     * <b>satisface</b> a este. Un {@code Info} de {@code Line} coincide con uno de
     * {@code SourceDataLine}, y no al reves, porque toda linea de salida es una linea.
     */
    class Info {

        /** Que interfaz de linea describe. */
        private final Class<?> lineClass;

        /**
         * @param lineClass la interfaz de linea; null se toma como {@code Line}
         */
        public Info(Class<?> lineClass) {
            if (lineClass == null) {
                this.lineClass = Line.class;
            } else {
                this.lineClass = lineClass;
            }
        }

        /** Que interfaz de linea describe. */
        public Class<?> getLineClass() {
            return this.lineClass;
        }

        /** Si el argumento satisface a este. Ver la nota de la clase: no es simetrico. */
        public boolean matches(Info info) {
            return getLineClass().isAssignableFrom(info.getLineClass());
        }

        /**
         * El nombre de la clase, sin el paquete {@code javax.sound.sampled}.
         *
         * <p>Se lo saca porque en la practica todas las clases de linea estan ahi, y repetirlo hace
         * ilegible el texto de un {@code DataLine.Info} con su formato.
         */
        @Override
        public String toString() {
            final String prefix = "javax.sound.sampled.";
            String full = getLineClass().toString();
            int index = full.indexOf(prefix);
            if (index < 0) {
                return full;
            }
            return full.substring(0, index) + full.substring(index + prefix.length());
        }
    }
}
