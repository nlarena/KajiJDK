package java.awt.font;

/**
 * Cuánto y con qué prioridad puede estirarse o encogerse un glifo al justificar un renglón.
 *
 * <p>Justificar no es repartir el sobrante en partes iguales. Hay lugares donde el texto se estira
 * bien —los espacios entre palabras— y lugares donde estirar se nota feo —entre dos letras de la
 * misma palabra—. Cada glifo declara acá en qué grupo está y cuánto tolera.
 *
 * <p>El algoritmo trabaja por **prioridades**: primero reparte todo lo que puede en el grupo de
 * prioridad más alta, y sólo si no alcanza pasa al siguiente. Un glifo `absorb` se come todo el
 * sobrante que quede de su prioridad en vez de repartirlo, que es como se estira una raya de kashida
 * en la escritura árabe.
 */
public final class GlyphJustificationInfo {

    /** La prioridad más alta: el alargamiento de trazo de la escritura árabe. */
    public static final int PRIORITY_KASHIDA = 0;

    /** Los espacios entre palabras. */
    public static final int PRIORITY_WHITESPACE = 1;

    /** El espacio entre letras de una misma palabra. */
    public static final int PRIORITY_INTERCHAR = 2;

    /** No se justifica. */
    public static final int PRIORITY_NONE = 3;

    /** Cuánto pesa este glifo al repartir. */
    public final float weight;

    /** En qué grupo entra al estirar. */
    public final int growPriority;

    /** Si al estirar se come todo el sobrante de su prioridad. */
    public final boolean growAbsorb;

    /** Cuánto puede crecer del lado izquierdo. */
    public final float growLeftLimit;

    /** Cuánto puede crecer del lado derecho. */
    public final float growRightLimit;

    /** En qué grupo entra al encoger. */
    public final int shrinkPriority;

    /** Si al encoger absorbe todo el faltante de su prioridad. */
    public final boolean shrinkAbsorb;

    /** Cuánto puede achicarse del lado izquierdo. */
    public final float shrinkLeftLimit;

    /** Cuánto puede achicarse del lado derecho. */
    public final float shrinkRightLimit;

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si el peso o algún límite es negativo, o si alguna prioridad
     *     no es una de las cuatro
     */
    public GlyphJustificationInfo(float weight, boolean growAbsorb, int growPriority,
            float growLeftLimit, float growRightLimit, boolean shrinkAbsorb, int shrinkPriority,
            float shrinkLeftLimit, float shrinkRightLimit) {
        if (weight < 0) {
            throw new IllegalArgumentException("weight is negative");
        }
        if (!priorityIsValid(growPriority)) {
            throw new IllegalArgumentException("Invalid grow priority");
        }
        if (growLeftLimit < 0) {
            throw new IllegalArgumentException("growLeftLimit is negative");
        }
        if (growRightLimit < 0) {
            throw new IllegalArgumentException("growRightLimit is negative");
        }
        if (!priorityIsValid(shrinkPriority)) {
            throw new IllegalArgumentException("Invalid shrink priority");
        }
        if (shrinkLeftLimit < 0) {
            throw new IllegalArgumentException("shrinkLeftLimit is negative");
        }
        if (shrinkRightLimit < 0) {
            throw new IllegalArgumentException("shrinkRightLimit is negative");
        }
        this.weight = weight;
        this.growAbsorb = growAbsorb;
        this.growPriority = growPriority;
        this.growLeftLimit = growLeftLimit;
        this.growRightLimit = growRightLimit;
        this.shrinkAbsorb = shrinkAbsorb;
        this.shrinkPriority = shrinkPriority;
        this.shrinkLeftLimit = shrinkLeftLimit;
        this.shrinkRightLimit = shrinkRightLimit;
    }

    /** Si el número es una de las cuatro prioridades. */
    private static boolean priorityIsValid(int priority) {
        return priority >= PRIORITY_KASHIDA && priority <= PRIORITY_NONE;
    }

    public String toString() {
        return "GlyphJustificationInfo: weight=" + this.weight + ", growAbsorb="
                + this.growAbsorb + ", growPriority=" + this.growPriority + ", shrinkAbsorb="
                + this.shrinkAbsorb + ", shrinkPriority=" + this.shrinkPriority;
    }
}
