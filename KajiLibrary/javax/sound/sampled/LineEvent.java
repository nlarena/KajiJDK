package javax.sound.sampled;

import java.util.EventObject;

/**
 * KajiLibrary's javax.sound.sampled.LineEvent -- una linea se abrio, cerro, arranco o paro.
 *
 * <p>Cuatro tipos, en dos pares que se confunden:
 *
 * <ul>
 *   <li>{@link Type#OPEN} y {@link Type#CLOSE} son sobre el <b>recurso</b>: la linea reservo o libero
 *       el dispositivo;
 *   <li>{@link Type#START} y {@link Type#STOP} son sobre el <b>flujo</b>: hay o no hay audio pasando.
 * </ul>
 *
 * <p>Una linea puede abrirse y cerrarse sin sonar nunca, y puede arrancar y parar muchas veces
 * mientras esta abierta. Un clip que termina emite {@code STOP}, no {@code CLOSE}.
 *
 * <p>{@link #getFramePosition} dice en que cuadro paso; para {@code OPEN} y {@code CLOSE} puede ser
 * {@link AudioSystem#NOT_SPECIFIED}.
 */
public class LineEvent extends EventObject {

    private static final long serialVersionUID = -1274246333383880410L;

    /** Cual de los cuatro. */
    private final Type type;

    /** En que cuadro. */
    private final long position;

    /**
     * @param line de que linea
     * @param type cual de los cuatro
     * @param position en que cuadro, o {@link AudioSystem#NOT_SPECIFIED}
     * @throws IllegalArgumentException si la linea es null
     */
    public LineEvent(Line line, Type type, long position) {
        super(line);
        this.type = type;
        this.position = position;
    }

    /** De que linea. */
    public final Line getLine() {
        return (Line) getSource();
    }

    /** Cual de los cuatro. */
    public final Type getType() {
        return this.type;
    }

    /** En que cuadro. */
    public final long getFramePosition() {
        return this.position;
    }

    /** El tipo, la palabra {@code event}, y la linea. */
    @Override
    public String toString() {
        String s = "";
        if (this.type != null) {
            s = this.type.toString() + " ";
        }
        s = s + "event from line " + getLine();
        return s;
    }

    /**
     * Cual de los cuatro.
     *
     * <p>No es un enum por la misma razon que el resto del paquete: la API es de 1999 y los enums son
     * de 2004. Aca la igualdad es por <b>identidad</b>, como en {@link Control.Type}.
     */
    public static class Type {

        /** La linea reservo el dispositivo. */
        public static final Type OPEN = new Type("Open");

        /** Lo libero. */
        public static final Type CLOSE = new Type("Close");

        /** Empezo a pasar audio. */
        public static final Type START = new Type("Start");

        /** Dejo de pasar. */
        public static final Type STOP = new Type("Stop");

        /** El nombre, para mostrar. */
        private final String name;

        /** Protegido: los tipos los define la plataforma. */
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
        public String toString() {
            return this.name;
        }
    }
}
