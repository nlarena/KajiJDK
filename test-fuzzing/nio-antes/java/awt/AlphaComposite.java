package java.awt;

/**
 * The twelve Porter-Duff rules plus a global alpha factor.
 *
 * <p>Each rule says what the new drawing does with what was already there: cover it, be covered,
 * clip itself against it, erase it. The extra factor multiplies the alpha of everything drawn
 * before the rule is applied, and it is what allows a fade without touching the colours.
 *
 * <p>The numbering has a jump worth not "tidying": {@code DST} is 9, between {@code DST_OUT} and
 * {@code SRC_ATOP}. The first eight are from 1.2; {@code DST}, {@code SRC_ATOP}, {@code DST_ATOP}
 * and {@code XOR} were added in 1.4 and numbered after them. Renumbering them would break any code
 * that stored the integers. (This note said any serialized value; this class is not serializable.)
 *
 * <p>{@code createContext} returns what really blends. It works in premultiplied ARGB, which is
 * where the twelve rules are sums and products instead of twelve special cases.
 */
public final class AlphaComposite implements Composite {

    /** Clears: neither the source nor the destination remains. */
    public static final int CLEAR = 1;

    /** Only the source; the destination is discarded even if the source is transparent. */
    public static final int SRC = 2;

    public static final int SRC_OVER = 3;

    public static final int DST_OVER = 4;

    public static final int SRC_IN = 5;

    public static final int DST_IN = 6;

    public static final int SRC_OUT = 7;

    public static final int DST_OUT = 8;

    /** Added in 1.4, which is why it is 9 and does not follow SRC. */
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
        // Written in positive on purpose: that way NaN falls into the else and throws, which is
        // right. With `alpha < 0 || alpha > 1` a NaN would slip through and later give undefined
        // pixels.
        if (alpha >= 0.0f && alpha <= 1.0f) {
            this.rule = rule;
            this.extraAlpha = alpha;
        } else {
            throw new IllegalArgumentException("alpha value out of range");
        }
    }

    /** With alpha 1 it returns the shared constant: there is no point making two equal objects. */
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

    /** If nothing changes it returns {@code this}: deriving the same should not cost an object. */
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

    /**
     * Builds the machine that blends.
     *
     * <p>The formats passed in are a hint: the context always works in premultiplied ARGB.
     */
    public CompositeContext createContext(java.awt.image.ColorModel srcColorModel,
            java.awt.image.ColorModel dstColorModel, RenderingHints hints) {
        return new AlphaCompositeContext(this.getRule(), this.getAlpha());
    }
}
