package java.awt;

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * The context that really blends: the twelve Porter-Duff rules, pixel by pixel.
 *
 * <p>All blending comes down to two numbers per operand. Each rule says what fraction of the source
 * and what fraction of the destination survive —{@code Fa} and {@code Fb}— and the result is always
 *
 * <pre>colour = Fa * sourceColour + Fb * destinationColour
 * alpha  = Fa * sourceAlpha  + Fb * destinationAlpha</pre>
 *
 * <p>That holds **only** with the colour premultiplied by its alpha, and it is why this context
 * premultiplies on the way in and undoes it on the way out. Without premultiplying, every rule
 * would need its own special case so as not to tint the result with the colour of an invisible
 * pixel.
 *
 * <p>The two factors depend only on the alphas, and from that comes the twelve-row table that is
 * the heart of the class. `SRC_OVER`, the usual one, is `Fa = 1` and `Fb = 1 - sourceAlpha`: the
 * whole source, and of the destination whatever the source lets through.
 *
 * <p>It is not public: it is how {@link AlphaComposite#createContext} is written.
 */
class AlphaCompositeContext implements CompositeContext {

    private final int rule;
    private final float extraAlpha;

    /** With the rule and the extra alpha applied to the source. */
    AlphaCompositeContext(int rule, float extraAlpha) {
        this.rule = rule;
        this.extraAlpha = extraAlpha;
    }

    /** There are no resources to release. */
    public void dispose() {
    }

    /**
     * Blends the source with the destination and writes the result.
     *
     * <p>The three rasters are walked from their **own** origin and over the smallest rectangle of
     * the three: they need not be at the same place on the plane nor be the same size.
     */
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
        int w = Math.min(Math.min(src.getWidth(), dstIn.getWidth()), dstOut.getWidth());
        int h = Math.min(Math.min(src.getHeight(), dstIn.getHeight()), dstOut.getHeight());
        int sx = src.getMinX();
        int sy = src.getMinY();
        int ix = dstIn.getMinX();
        int iy = dstIn.getMinY();
        int ox = dstOut.getMinX();
        int oy = dstOut.getMinY();
        boolean srcHasAlpha = src.getNumBands() > 3;
        boolean inHasAlpha = dstIn.getNumBands() > 3;
        boolean outHasAlpha = dstOut.getNumBands() > 3;
        int[] s = new int[src.getNumBands()];
        int[] d = new int[dstIn.getNumBands()];
        int[] o = new int[dstOut.getNumBands()];
        float[] f = new float[2];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                s = src.getPixel(sx + i, sy + j, s);
                d = dstIn.getPixel(ix + i, iy + j, d);
                float as = (srcHasAlpha ? s[3] / 255.0f : 1.0f) * this.extraAlpha;
                float ad = inHasAlpha ? d[3] / 255.0f : 1.0f;
                this.factors(as, ad, f);
                float alpha = f[0] * as + f[1] * ad;
                for (int c = 0; c < 3; c++) {
                    // Premultiply, blend and undo: an invisible pixel's colour contributes nothing,
                    // which is exactly what premultiplication guarantees.
                    float cs = s[c] * as;
                    float cd = d[c] * ad;
                    float v = f[0] * cs + f[1] * cd;
                    if (alpha > 0.0f) {
                        v = v / alpha;
                    } else {
                        v = 0.0f;
                    }
                    o[c] = clampByte(v);
                }
                if (outHasAlpha) {
                    o[3] = clampByte(alpha * 255.0f);
                }
                dstOut.setPixel(ox + i, oy + j, o);
            }
        }
    }

    /**
     * The rule's two factors, for those alphas.
     *
     * <p>It is the Porter-Duff table. `f[0]` is how much of the source survives and `f[1]` how much
     * of the destination.
     */
    private void factors(float as, float ad, float[] f) {
        if (this.rule == AlphaComposite.CLEAR) {
            f[0] = 0.0f;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.SRC) {
            f[0] = 1.0f;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.DST) {
            f[0] = 0.0f;
            f[1] = 1.0f;
        } else if (this.rule == AlphaComposite.SRC_OVER) {
            f[0] = 1.0f;
            f[1] = 1.0f - as;
        } else if (this.rule == AlphaComposite.DST_OVER) {
            f[0] = 1.0f - ad;
            f[1] = 1.0f;
        } else if (this.rule == AlphaComposite.SRC_IN) {
            f[0] = ad;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.DST_IN) {
            f[0] = 0.0f;
            f[1] = as;
        } else if (this.rule == AlphaComposite.SRC_OUT) {
            f[0] = 1.0f - ad;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.DST_OUT) {
            f[0] = 0.0f;
            f[1] = 1.0f - as;
        } else if (this.rule == AlphaComposite.SRC_ATOP) {
            f[0] = ad;
            f[1] = 1.0f - as;
        } else if (this.rule == AlphaComposite.DST_ATOP) {
            f[0] = 1.0f - ad;
            f[1] = as;
        } else {
            // XOR: each survives where the other is not.
            f[0] = 1.0f - ad;
            f[1] = 1.0f - as;
        }
    }

    /** A value brought into a byte. */
    private static int clampByte(float v) {
        int i = (int) (v + 0.5f);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }
}
