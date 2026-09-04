package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Mixer -- un dispositivo de audio con sus lineas.
 *
 * <p>Es a la vez una {@link Line} y el contenedor de otras. La tarjeta de sonido, un dispositivo USB,
 * un mezclador puramente por software.
 *
 * <h2>Fuente y destino, otra vez desde el mezclador</h2>
 *
 * <p>{@link #getSourceLineInfo} son las lineas que <b>entran</b> al mezclador --a las que el programa
 * escribe-- y {@link #getTargetLineInfo} las que <b>salen</b> --de las que el programa lee--. Es la
 * misma inversion de {@link SourceDataLine}, y por la misma razon.
 *
 * <h2>{@link #synchronize}</h2>
 *
 * <p>Ata varias lineas para que arranquen y paren <b>en el mismo instante</b>. Es la unica forma de
 * reproducir varias pistas en sincronia: llamarles {@code start()} de a una deja milisegundos de
 * diferencia, y eso se oye.
 *
 * <p>El argumento {@code maintainSync} pide ademas que se mantengan alineadas durante la
 * reproduccion, y es mas caro. Hay que preguntar con {@link #isSynchronizationSupported} antes.
 *
 * <p>{@link #getMaxLines} devuelve cuantas lineas de ese tipo se pueden abrir a la vez, o
 * {@link AudioSystem#NOT_SPECIFIED} si no hay limite conocido.
 */
public interface Mixer extends Line {

    /** Como se llama este mezclador. */
    Mixer.Info getMixerInfo();

    /** Las lineas que entran al mezclador. Ver la nota de la clase. */
    Line.Info[] getSourceLineInfo();

    /** Las que salen. */
    Line.Info[] getTargetLineInfo();

    /** Las que entran y coinciden con ese descriptor. */
    Line.Info[] getSourceLineInfo(Line.Info info);

    /** Las que salen y coinciden. */
    Line.Info[] getTargetLineInfo(Line.Info info);

    /** Si soporta una linea asi. */
    boolean isLineSupported(Line.Info info);

    /**
     * Una linea de ese tipo, sin abrir.
     *
     * @throws LineUnavailableException si no hay disponible
     * @throws IllegalArgumentException si no soporta ese tipo
     */
    Line getLine(Line.Info info) throws LineUnavailableException;

    /** Cuantas de ese tipo se pueden abrir a la vez. Ver la nota de la clase. */
    int getMaxLines(Line.Info info);

    /** Las lineas de entrada que estan abiertas. */
    Line[] getSourceLines();

    /** Las de salida que estan abiertas. */
    Line[] getTargetLines();

    /**
     * Ata esas lineas para que arranquen y paren juntas. Ver la nota de la clase.
     *
     * @param maintainSync si ademas hay que mantenerlas alineadas mientras suenan
     * @throws IllegalArgumentException si no se pueden sincronizar asi
     */
    void synchronize(Line[] lines, boolean maintainSync);

    /**
     * Las desata.
     *
     * @throws IllegalArgumentException si no estaban atadas
     */
    void unsynchronize(Line[] lines);

    /** Si se pueden atar asi. */
    boolean isSynchronizationSupported(Line[] lines, boolean maintainSync);

    /**
     * Como se llama un mezclador.
     *
     * <p>Cuatro cadenas para mostrar. El constructor es protegido porque estos datos los define quien
     * implementa el mezclador.
     *
     * <p>La igualdad es por <b>identidad</b>: dos mezcladores con el mismo nombre y version siguen
     * siendo dos dispositivos distintos.
     */
    class Info {

        /** El nombre. */
        private final String name;

        /** Quien lo hizo. */
        private final String vendor;

        /** Que es. */
        private final String description;

        /** Que version. */
        private final String version;

        /** Protegido: estos datos los define quien implementa el mezclador. */
        protected Info(String name, String vendor, String description, String version) {
            this.name = name;
            this.vendor = vendor;
            this.description = description;
            this.version = version;
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
        public final String getName() {
            return this.name;
        }

        /** Quien lo hizo. */
        public final String getVendor() {
            return this.vendor;
        }

        /** Que es. */
        public final String getDescription() {
            return this.description;
        }

        /** Que version. */
        public final String getVersion() {
            return this.version;
        }

        /** El nombre y la version. */
        @Override
        public final String toString() {
            return this.name + ", version " + this.version;
        }
    }
}
