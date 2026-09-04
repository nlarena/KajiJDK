package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Control -- una perilla de una linea de audio.
 *
 * <p>Volumen, silencio, balance, reverberacion. Una {@link Line} publica los que tenga y se piden por
 * {@link Control.Type}.
 *
 * <h2>Por que se pide por tipo y no hay metodos</h2>
 *
 * <p>Porque que controles existen depende del dispositivo, y no se sabe hasta abrir la linea. Si
 * {@code Line} tuviera {@code setVolume}, habria que decidir que hace en una linea que no tiene
 * volumen. Con este esquema, un programa pregunta con {@code isControlSupported} y adapta su interfaz
 * a lo que hay.
 *
 * <p>Las cuatro subclases cubren las cuatro formas de una perilla: booleana, continua, de opciones, y
 * compuesta. Un proveedor puede definir tipos nuevos, pero no formas nuevas.
 */
public abstract class Control {

    /** Que perilla es. */
    private final Type type;

    /** Para las subclases. */
    protected Control(Type type) {
        this.type = type;
    }

    /** Que perilla es. */
    public Type getType() {
        return this.type;
    }

    /** El tipo y la palabra {@code control}. */
    @Override
    public String toString() {
        return getType() + " control";
    }

    /**
     * Que perilla es.
     *
     * <p>No es un enum: el constructor es protegido para que un proveedor pueda definir controles que
     * la plataforma no conoce. La igualdad es por <b>identidad</b>, no por nombre -- a diferencia de
     * {@link AudioFormat.Encoding}, donde es por nombre.
     *
     * <p>Esa diferencia es deliberada y conviene notarla: una codificacion con el mismo nombre <b>es</b>
     * la misma codificacion, mientras que dos controles que se llamen igual en mezcladores distintos no
     * son la misma perilla.
     */
    public static class Type {

        /** El nombre, para mostrar. */
        private final String name;

        /** Protegido: los tipos los define quien provee el mezclador. */
        protected Type(String name) {
            this.name = name;
        }

        /** Por identidad. Ver la nota de la clase. */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /** El de identidad. */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /** El nombre. */
        @Override
        public final String toString() {
            return this.name;
        }
    }
}
