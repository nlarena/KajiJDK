package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.Port -- un conector fisico del equipo.
 *
 * <p>El microfono, los parlantes, la entrada de linea, los auriculares. No declara ningun metodo
 * propio: un puerto no mueve datos, se abre para poder <b>controlarlo</b> -- subirle el volumen,
 * silenciarlo.
 *
 * <p>Esa es toda la diferencia con {@link DataLine}: por una linea de datos pasa audio del programa,
 * por un puerto pasa audio que el programa no toca.
 */
public interface Port extends Line {

    /**
     * Que conector es.
     *
     * <p>Trae seis constantes con los conectores habituales. {@link #isSource} dice de que lado esta:
     * <b>fuente</b> es lo que entra al mezclador --microfono, entrada de linea-- y <b>destino</b> lo
     * que sale --parlantes, auriculares--.
     *
     * <p>Es el mismo criterio invertido de {@link SourceDataLine}: siempre desde el punto de vista del
     * mezclador.
     */
    class Info extends Line.Info {

        /** El microfono; fuente. */
        public static final Info MICROPHONE = new Info(Port.class, "MICROPHONE", true);

        /** La entrada de linea; fuente. */
        public static final Info LINE_IN = new Info(Port.class, "LINE_IN", true);

        /** El lector de discos compactos; fuente. */
        public static final Info COMPACT_DISC = new Info(Port.class, "COMPACT_DISC", true);

        /** Los parlantes; destino. */
        public static final Info SPEAKER = new Info(Port.class, "SPEAKER", false);

        /** Los auriculares; destino. */
        public static final Info HEADPHONE = new Info(Port.class, "HEADPHONE", false);

        /** La salida de linea; destino. */
        public static final Info LINE_OUT = new Info(Port.class, "LINE_OUT", false);

        /** Como se llama. */
        private final String name;

        /** Si entra al mezclador. */
        private final boolean isSource;

        /**
         * @param name como se llama
         * @param isSource si entra al mezclador; ver la nota de la clase
         */
        public Info(Class<?> lineClass, String name, boolean isSource) {
            super(lineClass);
            this.name = name;
            this.isSource = isSource;
        }

        /** Como se llama. */
        public String getName() {
            return this.name;
        }

        /** Si entra al mezclador. Ver la nota de la clase. */
        public boolean isSource() {
            return this.isSource;
        }

        /** La de la clase base, y ademas el nombre y el lado tienen que coincidir. */
        @Override
        public boolean matches(Line.Info info) {
            if (!super.matches(info)) {
                return false;
            }
            if (!(info instanceof Info)) {
                return false;
            }
            Info other = (Info) info;
            return this.name.equals(other.getName()) && this.isSource == other.isSource();
        }

        /** Por nombre y lado. */
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Info)) {
                return false;
            }
            Info other = (Info) obj;
            return this.name.equals(other.name) && this.isSource == other.isSource;
        }

        /** Coherente con {@link #equals}. */
        @Override
        public final int hashCode() {
            return this.name.hashCode();
        }

        /** El nombre y de que lado esta. */
        @Override
        public final String toString() {
            String dir;
            if (this.isSource) {
                dir = " source port";
            } else {
                dir = " target port";
            }
            return this.name + dir;
        }
    }
}
