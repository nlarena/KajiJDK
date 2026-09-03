package java.awt;

/**
 * Que puede hacer una cadena de buffers de dibujo: si se puede intercambiar paginas y que queda en
 * el buffer de atras despues de intercambiar.
 *
 * <p>{@code isPageFlipping()} no guarda un booleano propio: es {@code getFlipContents() != null}.
 * La razon es que las dos cosas no pueden contradecirse. Si hubiera un campo aparte, alguien podria
 * construir una capacidad que dice que intercambia paginas pero no dice que queda despues, y eso no
 * significa nada.
 */
public class BufferCapabilities implements Cloneable {

    private ImageCapabilities frontCaps;

    private ImageCapabilities backCaps;

    private FlipContents flipContents;

    /**
     * Los dos {@code ImageCapabilities} son obligatorios; el {@code FlipContents} no, y su
     * ausencia es la forma de decir "esta cadena no intercambia paginas, copia".
     */
    public BufferCapabilities(ImageCapabilities frontCaps, ImageCapabilities backCaps,
            FlipContents flipContents) {
        if (frontCaps == null || backCaps == null) {
            throw new IllegalArgumentException("Image capabilities specified cannot be null");
        }
        this.frontCaps = frontCaps;
        this.backCaps = backCaps;
        this.flipContents = flipContents;
    }

    public ImageCapabilities getFrontBufferCapabilities() {
        return frontCaps;
    }

    public ImageCapabilities getBackBufferCapabilities() {
        return backCaps;
    }

    public boolean isPageFlipping() {
        return getFlipContents() != null;
    }

    public FlipContents getFlipContents() {
        return flipContents;
    }

    /**
     * Falso en la clase base. Quien de verdad sabe si hace falta pantalla completa es la
     * implementacion concreta del dispositivo, y esta clase solo describe.
     */
    public boolean isFullScreenRequired() {
        return false;
    }

    public boolean isMultiBufferAvailable() {
        return false;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Que queda en el buffer de atras despues de intercambiar.
     *
     * <p>Es una enumeracion anterior a {@code enum} --por eso hereda de {@code AttributeValue}-- y
     * la diferencia importa: {@code PRIOR} y {@code COPIED} son las dos utiles y son opuestas. Con
     * PRIOR el buffer de atras queda con lo que se estaba mostrando (sirve para animar sobre lo
     * anterior); con COPIED queda con lo que se acaba de mostrar.
     */
    public static final class FlipContents extends AttributeValue {

        private static int I_UNDEFINED = 0;

        private static int I_BACKGROUND = 1;

        private static int I_PRIOR = 2;

        private static int I_COPIED = 3;

        private static final String[] NAMES = {"undefined", "background", "prior", "copied"};

        /** No se sabe que queda: hay que redibujar todo. */
        public static final FlipContents UNDEFINED = new FlipContents(I_UNDEFINED);

        /** Queda pintado con el color de fondo. */
        public static final FlipContents BACKGROUND = new FlipContents(I_BACKGROUND);

        public static final FlipContents PRIOR = new FlipContents(I_PRIOR);

        public static final FlipContents COPIED = new FlipContents(I_COPIED);

        private FlipContents(int type) {
            super(type, NAMES);
        }

        public String toString() {
            return super.toString();
        }

        public int hashCode() {
            return super.hashCode();
        }
    }
}
