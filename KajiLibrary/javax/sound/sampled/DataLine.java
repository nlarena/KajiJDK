package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.DataLine -- una linea por la que pasan datos de audio.
 *
 * <p>Agrega sobre {@link Line} lo que hace falta para mover audio: arrancar, parar, saber donde va, y
 * un bufer.
 *
 * <h2>{@link #isRunning} y {@link #isActive} no son lo mismo</h2>
 *
 * <p>Es la confusion clasica de esta interfaz:
 *
 * <ul>
 *   <li>{@code isRunning} dice si <b>hay audio moviendose ahora mismo</b>;
 *   <li>{@code isActive} dice si la linea <b>esta arrancada</b>, aunque en este instante este esperando
 *       datos.
 * </ul>
 *
 * <p>Una linea de salida a la que no se le escribe esta activa y no corriendo. Para saber si un clip
 * termino hay que mirar {@code isActive}, o mejor escuchar el {@link LineEvent}.
 *
 * <h2>{@link #drain} y {@link #flush} son opuestos</h2>
 *
 * <p>{@code drain} espera a que suene todo lo que hay en el bufer; {@code flush} lo tira. Confundirlos
 * corta el final del audio o cuelga el programa esperando.
 *
 * <h2>Las dos posiciones</h2>
 *
 * <p>{@link #getFramePosition} devuelve un {@code int} y se desborda: a 44100 Hz, a las trece horas y
 * media de audio. {@link #getLongFramePosition} es la version que no tiene ese problema, y es la que
 * hay que usar.
 */
public interface DataLine extends Line {

    /** Espera a que suene todo lo que hay en el bufer. Ver la nota de la clase. */
    void drain();

    /** Tira lo que hay en el bufer. Ver la nota de la clase. */
    void flush();

    /** Empieza a mover audio. */
    void start();

    /** Deja de moverlo, sin tirar el bufer. */
    void stop();

    /** Si hay audio moviendose ahora. Ver la nota de la clase. */
    boolean isRunning();

    /** Si la linea esta arrancada. Ver la nota de la clase. */
    boolean isActive();

    /** Con que formato. */
    AudioFormat getFormat();

    /** El tamano del bufer, en bytes. */
    int getBufferSize();

    /** Cuantos bytes se pueden leer o escribir sin bloquear. */
    int available();

    /**
     * En que cuadro va.
     *
     * <p>No esta marcado como obsoleto, y deberia: se desborda. Ver la nota de la clase y usar
     * {@link #getLongFramePosition}.
     */
    int getFramePosition();

    /** En que cuadro va, sin desbordarse. */
    long getLongFramePosition();

    /** Cuantos microsegundos de audio pasaron. */
    long getMicrosecondPosition();

    /**
     * El nivel de la senal, de 0 a 1, o {@link AudioSystem#NOT_SPECIFIED}.
     *
     * <p>Casi ninguna implementacion lo calcula: pedirlo cuesta recorrer las muestras. Lo normal es que
     * devuelva -1.
     */
    float getLevel();

    /**
     * Que clase de linea de datos es, con que formatos y que tamanos de bufer.
     *
     * <p>Es el {@link Line.Info} que se usa para pedir una linea concreta: se arma con la interfaz que
     * hace falta y el formato que se quiere reproducir o capturar.
     *
     * <p>{@link #matches} agrega a la de la clase base dos condiciones: que <b>todos</b> los formatos
     * del argumento esten soportados por este, y que los rangos de bufer se superpongan.
     */
    class Info extends Line.Info {

        /** Los formatos que acepta. */
        private final AudioFormat[] formats;

        /** El bufer mas chico, en bytes. */
        private final int minBufferSize;

        /** El mas grande. */
        private final int maxBufferSize;

        /**
         * El completo.
         *
         * @param formats los formatos aceptados; null se toma como ninguno
         */
        public Info(Class<?> lineClass, AudioFormat[] formats, int minBufferSize,
                    int maxBufferSize) {
            super(lineClass);
            if (formats == null) {
                this.formats = new AudioFormat[0];
            } else {
                this.formats = formats;
            }
            this.minBufferSize = minBufferSize;
            this.maxBufferSize = maxBufferSize;
        }

        /** Un solo formato y un tamano de bufer exacto. */
        public Info(Class<?> lineClass, AudioFormat format, int bufferSize) {
            this(lineClass, format == null ? null : new AudioFormat[] { format },
                 bufferSize, bufferSize);
        }

        /** Un solo formato, cualquier bufer. */
        public Info(Class<?> lineClass, AudioFormat format) {
            this(lineClass, format == null ? null : new AudioFormat[] { format },
                 AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
        }

        /** Los formatos aceptados; una copia del arreglo. */
        public AudioFormat[] getFormats() {
            AudioFormat[] copy = new AudioFormat[this.formats.length];
            System.arraycopy(this.formats, 0, copy, 0, this.formats.length);
            return copy;
        }

        /** Si acepta ese formato. Usa {@link AudioFormat#matches}, con sus comodines. */
        public boolean isFormatSupported(AudioFormat format) {
            int i = 0;
            while (i < this.formats.length) {
                if (format.matches(this.formats[i])) {
                    return true;
                }
                i = i + 1;
            }
            return false;
        }

        /** El bufer mas chico. */
        public int getMinBufferSize() {
            return this.minBufferSize;
        }

        /** El mas grande. */
        public int getMaxBufferSize() {
            return this.maxBufferSize;
        }

        /** La de la clase base, mas los formatos y los buferes. Ver la nota de la clase. */
        @Override
        public boolean matches(Line.Info info) {
            if (!super.matches(info)) {
                return false;
            }
            if (!(info instanceof Info)) {
                return false;
            }
            Info other = (Info) info;
            if (this.minBufferSize != AudioSystem.NOT_SPECIFIED
                && other.getMaxBufferSize() != AudioSystem.NOT_SPECIFIED
                && other.getMaxBufferSize() < this.minBufferSize) {
                return false;
            }
            if (this.maxBufferSize != AudioSystem.NOT_SPECIFIED
                && other.getMinBufferSize() != AudioSystem.NOT_SPECIFIED
                && other.getMinBufferSize() > this.maxBufferSize) {
                return false;
            }
            AudioFormat[] theirs = other.getFormats();
            int i = 0;
            while (i < theirs.length) {
                if (theirs[i] != null && !isFormatSupported(theirs[i])) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }

        /** La clase, los formatos, y el rango de bufer si se conoce. */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            if (this.formats.length == 1 && this.formats[0] != null) {
                sb.append(" supporting format ").append(this.formats[0]);
            } else if (this.formats.length > 1) {
                sb.append(" supporting ").append(this.formats.length).append(" audio formats");
            }
            if (this.minBufferSize != AudioSystem.NOT_SPECIFIED
                && this.maxBufferSize != AudioSystem.NOT_SPECIFIED) {
                sb.append(", and buffers of ").append(this.minBufferSize).append(" to ")
                    .append(this.maxBufferSize).append(" bytes");
            }
            return sb.toString();
        }
    }
}
