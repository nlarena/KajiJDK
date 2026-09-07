package javax.swing;

import java.io.Serializable;

/**
 * Cuanto quiere medir algo: minimo, preferido, maximo y donde tiene su linea de alineacion.
 *
 * <h2>Una dimension a la vez</h2>
 *
 * <p>Esta clase no habla de anchos ni de altos: habla de <em>una</em> dimension. Un componente en
 * una caja horizontal aporta dos de estos objetos, uno por eje, y cada eje se resuelve por
 * separado. Es lo que permite que el mismo par de rutinas sirva para acomodar en fila o en
 * columna: cambia cual de los dos ejes se reparte y cual se alinea.
 *
 * <h2>Repartir y alinear</h2>
 *
 * <p>Son las dos operaciones, y son distintas:
 *
 * <ul>
 * <li><strong>Repartir</strong> ({@link #calculateTiledPositions}) pone a los hijos uno detras de
 * otro y les da a cada uno una parte del espacio. Si sobra, cada uno crece en proporcion a lo que
 * puede crecer; si falta, cada uno se achica en proporcion a lo que puede achicarse. Nunca se
 * pisan.
 * <li><strong>Alinear</strong> ({@link #calculateAlignedPositions}) los pone a todos en el mismo
 * lugar, cada uno colgado de su linea de alineacion. Es lo que hace que una fila de botones quede
 * centrada, o que sus bases coincidan.
 * </ul>
 *
 * <p>La alineacion va de 0 a 1: 0 es "mi punto de enganche esta en mi borde de arriba (o
 * izquierdo)", 1 en el de abajo (o derecho), y 0.5 al medio. Un componente que quiere alinear por
 * su linea de base dice que fraccion de su alto queda por encima de ella.
 *
 * <p>Las sumas se hacen en {@code long} y se recortan al maximo entero: sumar tres hijos que dicen
 * "sin tope" no debe dar un numero negativo, que es lo que pasaria con un desborde silencioso.
 */
public class SizeRequirements implements Serializable {

    /** Lo menos que puede medir. */
    public int minimum;

    /** Lo que quiere medir. */
    public int preferred;

    /** Lo mas que puede medir. */
    public int maximum;

    /** Donde esta su linea de enganche, de 0 a 1; ver la nota de la clase. */
    public float alignment;

    /** Un pedido de tamano cero, alineado al centro. */
    public SizeRequirements() {
        minimum = 0;
        preferred = 0;
        maximum = 0;
        alignment = 0.5f;
    }

    public SizeRequirements(int min, int pref, int max, float a) {
        minimum = min;
        preferred = pref;
        maximum = max;
        alignment = a > 1.0f ? 1.0f : a < 0.0f ? 0.0f : a;
    }

    public String toString() {
        return "[" + minimum + "," + preferred + "," + maximum + "]@" + alignment;
    }

    /**
     * Lo que piden entre todos si van uno detras de otro: la suma de cada cosa.
     *
     * <p>La alineacion del total es 0.5 y no la de nadie en particular: una fila no hereda el
     * enganche de sus hijos, lo decide quien la ubique.
     */
    public static SizeRequirements getTiledSizeRequirements(SizeRequirements[] children) {
        SizeRequirements total = new SizeRequirements();
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            total.minimum = (int) Math.min((long) total.minimum + (long) req.minimum,
                    Integer.MAX_VALUE);
            total.preferred = (int) Math.min((long) total.preferred + (long) req.preferred,
                    Integer.MAX_VALUE);
            total.maximum = (int) Math.min((long) total.maximum + (long) req.maximum,
                    Integer.MAX_VALUE);
        }
        return total;
    }

    /**
     * Lo que piden entre todos si van encimados y alineados.
     *
     * <p>Se mide por separado lo que sobresale de cada lado de la linea de enganche y se toma el
     * peor de cada lado: el conjunto necesita lo que necesita el que mas sube mas lo que necesita
     * el que mas baja. La alineacion resultante es la que deja esa linea en su lugar, calculada
     * sobre los tamanos preferidos.
     */
    public static SizeRequirements getAlignedSizeRequirements(SizeRequirements[] children) {
        SizeRequirements totalAscent = new SizeRequirements();
        SizeRequirements totalDescent = new SizeRequirements();
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];

            int ascent = (int) (req.alignment * req.minimum);
            int descent = req.minimum - ascent;
            totalAscent.minimum = Math.max(ascent, totalAscent.minimum);
            totalDescent.minimum = Math.max(descent, totalDescent.minimum);

            ascent = (int) (req.alignment * req.preferred);
            descent = req.preferred - ascent;
            totalAscent.preferred = Math.max(ascent, totalAscent.preferred);
            totalDescent.preferred = Math.max(descent, totalDescent.preferred);

            ascent = (int) (req.alignment * req.maximum);
            descent = req.maximum - ascent;
            totalAscent.maximum = Math.max(ascent, totalAscent.maximum);
            totalDescent.maximum = Math.max(descent, totalDescent.maximum);
        }
        int min = (int) Math.min((long) totalAscent.minimum + (long) totalDescent.minimum,
                Integer.MAX_VALUE);
        int pref = (int) Math.min((long) totalAscent.preferred + (long) totalDescent.preferred,
                Integer.MAX_VALUE);
        int max = (int) Math.min((long) totalAscent.maximum + (long) totalDescent.maximum,
                Integer.MAX_VALUE);
        // La alineacion sale del minimo y no del preferido: es la unica que se cumple siempre,
        // porque el conjunto nunca mide menos que su minimo. Con el preferido, una caja apretada
        // engancharia por una linea que no tiene.
        float alignment = 0.0f;
        if (min > 0) {
            alignment = (float) totalAscent.minimum / min;
            alignment = alignment > 1.0f ? 1.0f : alignment < 0.0f ? 0.0f : alignment;
        }
        return new SizeRequirements(min, pref, max, alignment);
    }

    /** Reparte de adelante hacia atras; ver {@link #calculateTiledPositions(int,
     * SizeRequirements, SizeRequirements[], int[], int[], boolean)}. */
    public static void calculateTiledPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans) {
        calculateTiledPositions(allocated, total, children, offsets, spans, true);
    }

    /**
     * Reparte {@code allocated} entre los hijos, uno detras de otro.
     *
     * <p>Si el espacio alcanza para lo preferido, cada uno crece; si no, cada uno se achica. En
     * los dos casos el reparto es proporcional al margen de maniobra de cada uno, no al tamano:
     * un hijo que no puede achicarse no se achica aunque sea el mas grande.
     *
     * <p>{@code forward} en {@code false} llena desde el final, que es como se acomoda una fila
     * en un idioma que se lee de derecha a izquierda.
     */
    public static void calculateTiledPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans, boolean forward) {
        // Las sumas van en long: los maximos suelen ser enormes y la suma desbordaria.
        long min = 0;
        long pref = 0;
        long max = 0;
        for (int i = 0; i < children.length; i++) {
            min += children[i].minimum;
            pref += children[i].preferred;
            max += children[i].maximum;
        }
        if (allocated >= pref) {
            expandedTile(allocated, min, pref, max, children, offsets, spans, forward);
        } else {
            compressedTile(allocated, min, pref, max, children, offsets, spans, forward);
        }
    }

    /** Falta espacio: cada uno cede una fraccion de lo que puede ceder. */
    private static void compressedTile(int allocated, long min, long pref, long max,
            SizeRequirements[] request, int[] offsets, int[] spans, boolean forward) {
        float totalPlay = Math.min(pref - allocated, pref - min);
        float useableSpace = pref - min;
        float factor = (useableSpace == 0.0f) ? 0.0f : totalPlay / useableSpace;

        int totalOffset;
        if (forward) {
            totalOffset = 0;
            for (int i = 0; i < spans.length; i++) {
                offsets[i] = totalOffset;
                SizeRequirements req = request[i];
                float play = factor * (req.preferred - req.minimum);
                spans[i] = (int) (req.preferred - play);
                totalOffset = (int) Math.min((long) totalOffset + (long) spans[i],
                        Integer.MAX_VALUE);
            }
        } else {
            totalOffset = allocated;
            for (int i = 0; i < spans.length; i++) {
                SizeRequirements req = request[i];
                float play = factor * (req.preferred - req.minimum);
                spans[i] = (int) (req.preferred - play);
                offsets[i] = totalOffset - spans[i];
                totalOffset = (int) Math.max((long) totalOffset - (long) spans[i], 0);
            }
        }
    }

    /** Sobra espacio: cada uno toma una fraccion de lo que puede crecer. */
    private static void expandedTile(int allocated, long min, long pref, long max,
            SizeRequirements[] request, int[] offsets, int[] spans, boolean forward) {
        float totalPlay = Math.min(allocated - pref, max - pref);
        float useableSpace = max - pref;
        float factor = (useableSpace == 0.0f) ? 0.0f : totalPlay / useableSpace;

        int totalOffset;
        if (forward) {
            totalOffset = 0;
            for (int i = 0; i < spans.length; i++) {
                offsets[i] = totalOffset;
                SizeRequirements req = request[i];
                int play = (int) (factor * (req.maximum - req.preferred));
                spans[i] = (int) Math.min((long) req.preferred + (long) play, Integer.MAX_VALUE);
                totalOffset = (int) Math.min((long) totalOffset + (long) spans[i],
                        Integer.MAX_VALUE);
            }
        } else {
            totalOffset = allocated;
            for (int i = 0; i < spans.length; i++) {
                SizeRequirements req = request[i];
                int play = (int) (factor * (req.maximum - req.preferred));
                spans[i] = (int) Math.min((long) req.preferred + (long) play, Integer.MAX_VALUE);
                offsets[i] = totalOffset - spans[i];
                totalOffset = (int) Math.max((long) totalOffset - (long) spans[i], 0);
            }
        }
    }

    /** Alinea a todos en el mismo lugar, cada uno por su linea de enganche. */
    public static void calculateAlignedPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans) {
        calculateAlignedPositions(allocated, total, children, offsets, spans, true);
    }

    /**
     * Alinea a todos en el mismo lugar; {@code normal} en {@code false} da vuelta el eje.
     *
     * <p>El espacio se parte en dos por la linea del total, y cada hijo toma de cada mitad lo que
     * su maximo le permita. Un hijo con maximo enorme llena; uno con maximo chico queda de su
     * tamano, colgado de la linea.
     */
    public static void calculateAlignedPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans, boolean normal) {
        float totalAlignment = normal ? total.alignment : 1.0f - total.alignment;
        int totalAscent = (int) (allocated * totalAlignment);
        int totalDescent = allocated - totalAscent;
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            float alignment = normal ? req.alignment : 1.0f - req.alignment;
            int maxAscent = (int) (req.maximum * alignment);
            int maxDescent = req.maximum - maxAscent;
            int ascent = Math.min(totalAscent, maxAscent);
            int descent = Math.min(totalDescent, maxDescent);

            offsets[i] = totalAscent - ascent;
            spans[i] = (int) Math.min((long) ascent + (long) descent, Integer.MAX_VALUE);
        }
    }

    /**
     * Reparte un cambio de tamano entre varios pedidos.
     *
     * <p>Devuelve un arreglo vacio: en el JDK esta es una rutina auxiliar que quedo sin
     * implementar —siempre devolvio {@code new int[0]}— y copiar su comportamiento es lo unico
     * honesto. Quien reparte de verdad es {@link #calculateTiledPositions}.
     */
    public static int[] adjustSizes(int delta, SizeRequirements[] children) {
        return new int[0];
    }
}
