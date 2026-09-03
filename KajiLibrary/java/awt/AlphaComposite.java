package java.awt;

/**
 * Las doce reglas de Porter-Duff mas un factor de alfa global.
 *
 * <p>Cada regla dice que hace el dibujo nuevo con lo que ya estaba: taparlo, ser tapado, recortarse
 * contra el, borrarlo. El factor extra multiplica el alfa de todo lo que se dibuja antes de aplicar
 * la regla, y es lo que permite un fundido sin tocar los colores.
 *
 * <p>La numeracion tiene un salto que conviene no "ordenar": {@code DST} vale 9 y no 3, entre
 * {@code DST_OUT} y {@code SRC_ATOP}. Las ocho primeras son las de 1.2; {@code DST},
 * {@code SRC_ATOP}, {@code DST_ATOP} y {@code XOR} se agregaron en 1.4 y se numeraron a
 * continuacion. Renumerarlas romperia cualquier valor serializado.
 *
 * <h2>Lo que falta y por que</h2>
 *
 * <p>{@code createContext(ColorModel, ColorModel, RenderingHints)} --el metodo de
 * {@code Composite}-- no esta: sus dos primeros parametros son {@code java.awt.image.ColorModel},
 * que no existe en KajiLibrary. Es el unico miembro ausente y es el que mezcla pixeles de verdad;
 * todo lo que elige y describe la regla esta completo.
 */
public final class AlphaComposite implements Composite {

    /** Borra: ni el origen ni el destino quedan. */
    public static final int CLEAR = 1;

    /** Solo el origen; el destino se descarta aunque el origen sea transparente. */
    public static final int SRC = 2;

    public static final int SRC_OVER = 3;

    public static final int DST_OVER = 4;

    public static final int SRC_IN = 5;

    public static final int DST_IN = 6;

    public static final int SRC_OUT = 7;

    public static final int DST_OUT = 8;

    /** Agregada en 1.4, por eso vale 9 y no sigue a SRC. */
    public static final int DST = 9;

    public static final int SRC_ATOP = 10;

    public static final int DST_ATOP = 11;

    public static final int XOR = 12;

    private static final int MIN_RULE = CLEAR;

    private static final int MAX_RULE = XOR;

    public static final AlphaComposite Clear = new AlphaComposite(CLEAR);

    public static final AlphaComposite Src = new AlphaComposite(SRC);

    public static final AlphaComposite Dst = new AlphaComposite(DST);

    public static final AlphaComposite SrcOver = new AlphaComposite(SRC_OVER);

    public static final AlphaComposite DstOver = new AlphaComposite(DST_OVER);

    public static final AlphaComposite SrcIn = new AlphaComposite(SRC_IN);

    public static final AlphaComposite DstIn = new AlphaComposite(DST_IN);

    public static final AlphaComposite SrcOut = new AlphaComposite(SRC_OUT);

    public static final AlphaComposite DstOut = new AlphaComposite(DST_OUT);

    public static final AlphaComposite SrcAtop = new AlphaComposite(SRC_ATOP);

    public static final AlphaComposite DstAtop = new AlphaComposite(DST_ATOP);

    public static final AlphaComposite Xor = new AlphaComposite(XOR);

    float extraAlpha;

    int rule;

    private AlphaComposite(int rule) {
        this(rule, 1.0f);
    }

    private AlphaComposite(int rule, float alpha) {
        if (rule < MIN_RULE || rule > MAX_RULE) {
            throw new IllegalArgumentException("unknown composite rule");
        }
        // Escrito en positivo a proposito: asi NaN cae en el else y tira, que es lo correcto. Con
        // `alpha < 0 || alpha > 1` un NaN pasaria de largo y despues daria pixeles indefinidos.
        if (alpha >= 0.0f && alpha <= 1.0f) {
            this.rule = rule;
            this.extraAlpha = alpha;
        } else {
            throw new IllegalArgumentException("alpha value out of range");
        }
    }

    /** Con alfa 1 devuelve la constante compartida: no tiene sentido fabricar dos objetos iguales. */
    public static AlphaComposite getInstance(int rule) {
        switch (rule) {
            case CLEAR:
                return Clear;
            case SRC:
                return Src;
            case DST:
                return Dst;
            case SRC_OVER:
                return SrcOver;
            case DST_OVER:
                return DstOver;
            case SRC_IN:
                return SrcIn;
            case DST_IN:
                return DstIn;
            case SRC_OUT:
                return SrcOut;
            case DST_OUT:
                return DstOut;
            case SRC_ATOP:
                return SrcAtop;
            case DST_ATOP:
                return DstAtop;
            case XOR:
                return Xor;
            default:
                throw new IllegalArgumentException("unknown composite rule");
        }
    }

    public static AlphaComposite getInstance(int rule, float alpha) {
        if (alpha == 1.0f) {
            return getInstance(rule);
        }
        return new AlphaComposite(rule, alpha);
    }

    public float getAlpha() {
        return extraAlpha;
    }

    public int getRule() {
        return rule;
    }

    /** Si no cambia nada devuelve {@code this}: derivar lo mismo no deberia costar un objeto. */
    public AlphaComposite derive(int rule) {
        return (this.rule == rule) ? this : getInstance(rule, this.extraAlpha);
    }

    public AlphaComposite derive(float alpha) {
        return (this.extraAlpha == alpha) ? this : getInstance(this.rule, alpha);
    }

    public int hashCode() {
        return (Float.floatToIntBits(extraAlpha) * 31 + rule);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AlphaComposite)) {
            return false;
        }
        AlphaComposite ac = (AlphaComposite) obj;
        if (rule != ac.rule) {
            return false;
        }
        if (extraAlpha != ac.extraAlpha) {
            return false;
        }
        return true;
    }
}
