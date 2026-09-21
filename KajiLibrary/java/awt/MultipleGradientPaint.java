package java.awt;

import java.awt.geom.AffineTransform;

/**
 * The base of the gradients with **several stops**.
 *
 * <p>A {@link GradientPaint} goes from one colour to another. These go through a list of colours,
 * each anchored to a fraction of the way, and between two stops the colour is interpolated. It is
 * the difference between a gradient and a rainbow.
 *
 * <p>Three decisions are declared here because they apply to all:
 *
 * <ul>
 *   <li>the <strong>cycle</strong>, which says what happens beyond the way: the end colour is
 *       stretched, the gradient is repeated, or it is repeated as a mirror, which is the only one
 *       of the three that leaves no visible seam;
 *   <li>the <strong>colour space</strong> interpolation happens in. Halfway from black to white in
 *       sRGB is darker than the physically halfway grey, because sRGB is not linear in light;
 *       interpolating in linear RGB gives the physically halfway grey, which looks lighter. (This
 *       note had the sRGB middle lighter.) Neither is always the right one, which is why it is
 *       chosen;
 *   <li>the gradient's own <strong>transformation</strong>, which is composed with the drawing's
 *       and allows rotating or stretching the gradient without touching the shape.
 * </ul>
 */
public abstract class MultipleGradientPaint implements Paint {

    /** What happens outside the gradient's way. */
    public static enum CycleMethod {

        /** The end colour is stretched. */
        NO_CYCLE,

        /**
         * The gradient is repeated as a mirror, without a seam. (This comment and the next were
         * swapped.)
         */
        REFLECT,

        /** The gradient is repeated, with a seam at each turn. */
        REPEAT
    }

    /** Which colour space interpolation happens in. */
    public static enum ColorSpaceType {

        /** In sRGB, as the colours look. */
        SRGB,

        /** In linear RGB, proportional to light. */
        LINEAR_RGB
    }

    final float[] fractions;
    final Color[] colors;
    final AffineTransform gradientTransform;
    final CycleMethod cycleMethod;
    final ColorSpaceType colorSpace;
    final int transparency;

    /**
     * With the stops, the cycle, the space and the transformation.
     *
     * @throws NullPointerException if any of the five is missing
     * @throws IllegalArgumentException if there are fewer than two stops, if the arrays differ in
     *     length, or if the fractions are not in 0..1 and in strictly increasing order
     */
    MultipleGradientPaint(float[] fractions, Color[] colors, CycleMethod cycleMethod,
            ColorSpaceType colorSpace, AffineTransform gradientTransform) {
        if (fractions == null) {
            throw new NullPointerException("Fractions array cannot be null");
        }
        if (colors == null) {
            throw new NullPointerException("Colors array cannot be null");
        }
        if (cycleMethod == null) {
            throw new NullPointerException("Cycle method cannot be null");
        }
        if (colorSpace == null) {
            throw new NullPointerException("Color space cannot be null");
        }
        if (gradientTransform == null) {
            throw new NullPointerException("Gradient transform cannot be null");
        }
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Colors and fractions must have equal size");
        }
        if (colors.length < 2) {
            throw new IllegalArgumentException("User must specify at least 2 colors");
        }
        // The fractions have to grow **strictly**: two stops at the same place would be a
        // zero-width jump and there is no way to interpolate between them.
        float previous = -1.0f;
        for (int i = 0; i < fractions.length; i++) {
            if (fractions[i] < 0.0f || fractions[i] > 1.0f) {
                throw new IllegalArgumentException(
                        "Fraction values must be in the range 0 to 1: " + fractions[i]);
            }
            if (fractions[i] <= previous) {
                throw new IllegalArgumentException("Keyframe fractions must be increasing: "
                        + fractions[i]);
            }
            previous = fractions[i];
            if (colors[i] == null) {
                throw new NullPointerException("Colors array cannot have null entries");
            }
        }
        this.fractions = fractions.clone();
        this.colors = colors.clone();
        this.cycleMethod = cycleMethod;
        this.colorSpace = colorSpace;
        this.gradientTransform = (AffineTransform) gradientTransform.clone();
        boolean opaque = true;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i].getAlpha() != 0xFF) {
                opaque = false;
                break;
            }
        }
        this.transparency = opaque ? Transparency.OPAQUE : Transparency.TRANSLUCENT;
    }

    /** Where each stop is, from 0 to 1. */
    public final float[] getFractions() {
        return this.fractions.clone();
    }

    /** The colour of each stop. */
    public final Color[] getColors() {
        return this.colors.clone();
    }

    /** What happens outside the way. */
    public final CycleMethod getCycleMethod() {
        return this.cycleMethod;
    }

    /** Which space interpolation happens in. */
    public final ColorSpaceType getColorSpace() {
        return this.colorSpace;
    }

    /** The gradient's own transformation. */
    public final AffineTransform getTransform() {
        return (AffineTransform) this.gradientTransform.clone();
    }

    /** `OPAQUE` if all the stops are opaque, `TRANSLUCENT` if any is not. */
    public final int getTransparency() {
        return this.transparency;
    }

    /**
     * The colour at fraction `t` of the way, with the cycle already resolved.
     *
     * <p>Both subclasses use it: the only thing that changes between a linear gradient and a radial
     * one is how `t` is computed.
     */
    final int colorForFraction(float t) {
        float f = t;
        if (this.cycleMethod == CycleMethod.NO_CYCLE) {
            if (f < 0.0f) {
                f = 0.0f;
            } else if (f > 1.0f) {
                f = 1.0f;
            }
        } else {
            // `f - floor(f)` keeps the fractional part, already in [0, 1) for negatives too. (This
            // comment spoke of Java's remainder keeping the dividend's sign and having to add one;
            // the code does not use the remainder.)
            f = f - (float) Math.floor(f);
            if (this.cycleMethod == CycleMethod.REFLECT) {
                float folded = (t - (float) Math.floor(t / 2) * 2);
                if (folded > 1.0f) {
                    f = 2.0f - folded;
                } else {
                    f = folded;
                }
            }
        }
        int i = 0;
        while (i < this.fractions.length - 1 && f > this.fractions[i + 1]) {
            i = i + 1;
        }
        if (f <= this.fractions[0]) {
            return this.colors[0].getRGB();
        }
        if (f >= this.fractions[this.fractions.length - 1]) {
            return this.colors[this.colors.length - 1].getRGB();
        }
        float lo = this.fractions[i];
        float hi = this.fractions[i + 1];
        float u = (f - lo) / (hi - lo);
        return this.blend(this.colors[i], this.colors[i + 1], u);
    }

    /** Interpolates two colours, in the space that was declared. */
    private int blend(Color a, Color b, float u) {
        int aa = a.getAlpha();
        int ab = b.getAlpha();
        int alpha = (int) (aa + (ab - aa) * u + 0.5f);
        if (this.colorSpace == ColorSpaceType.LINEAR_RGB) {
            float r = interpolateLinear(a.getRed(), b.getRed(), u);
            float g = interpolateLinear(a.getGreen(), b.getGreen(), u);
            float bl = interpolateLinear(a.getBlue(), b.getBlue(), u);
            return (alpha << 24) | (toByte(r) << 16) | (toByte(g) << 8) | toByte(bl);
        }
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * u + 0.5f);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * u + 0.5f);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * u + 0.5f);
        return (alpha << 24) | (r << 16) | (g << 8) | bl;
    }

    /** Interpolates two sRGB components by going through linear light. */
    private static float interpolateLinear(int a, int b, float u) {
        double la = toLinear(a / 255.0);
        double lb = toLinear(b / 255.0);
        return (float) (toSrgb(la + (lb - la) * u) * 255.0);
    }

    /** From sRGB to linear light. */
    private static double toLinear(double c) {
        if (c <= 0.04045) {
            return c / 12.92;
        }
        return Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** From linear light to sRGB. */
    private static double toSrgb(double c) {
        if (c <= 0.0031308) {
            return c * 12.92;
        }
        return 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
    }

    /** A `float` from 0 to 255 brought into a byte. */
    private static int toByte(float v) {
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
