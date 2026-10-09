package java.awt.font;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * The conditions a text is going to be measured and drawn under.
 *
 * <p>A text has no absolute size: how much it measures depends on the scale it is drawn at and on
 * whether the outline is smoothed or not. This object gathers those conditions so that measuring and
 * drawing agree.
 *
 * <p>Fractional metrics are the least obvious distinction. Without them, each character's advance is
 * rounded to a whole pixel, and a word's width is the sum of those roundings; with them the advance
 * is carried with decimals and only rounded at the end. The difference accumulates: the same
 * sentence may measure several pixels differently depending on which is used.
 */
public class FontRenderContext {

    private final AffineTransform tx;
    private final Object aaHintValue;
    private final Object fmHintValue;
    private final boolean defaulting;

    /**
     * One with everything defaulted.
     *
     * <p>It is for the subclasses that work out their conditions on demand.
     */
    protected FontRenderContext() {
        this.tx = null;
        this.aaHintValue = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        this.fmHintValue = RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
        this.defaulting = true;
    }

    /** With the smoothing and the metrics given as booleans. */
    public FontRenderContext(AffineTransform tx, boolean isAntiAliased,
            boolean usesFractionalMetrics) {
        if (tx != null && !tx.isIdentity()) {
            this.tx = new AffineTransform(tx);
        } else {
            this.tx = null;
        }
        if (isAntiAliased) {
            this.aaHintValue = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
        } else {
            this.aaHintValue = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        }
        if (usesFractionalMetrics) {
            this.fmHintValue = RenderingHints.VALUE_FRACTIONALMETRICS_ON;
        } else {
            this.fmHintValue = RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
        }
        this.defaulting = false;
    }

    /**
     * With the smoothing and the metrics given as {@link RenderingHints} values.
     *
     * <p>It accepts more shades than the boolean version: smoothing has, besides yes and no, the
     * modes for subpixel screens.
     *
     * @throws IllegalArgumentException if either of the two values does not match its key
     */
    public FontRenderContext(AffineTransform tx, Object aaHint, Object fmHint) {
        if (tx != null && !tx.isIdentity()) {
            this.tx = new AffineTransform(tx);
        } else {
            this.tx = null;
        }
        if (aaHint == null) {
            this.aaHintValue = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        } else if (RenderingHints.KEY_TEXT_ANTIALIASING.isCompatibleValue(aaHint)) {
            this.aaHintValue = aaHint;
        } else {
            throw new IllegalArgumentException("AA hint:" + aaHint);
        }
        if (fmHint == null) {
            this.fmHintValue = RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
        } else if (RenderingHints.KEY_FRACTIONALMETRICS.isCompatibleValue(fmHint)) {
            this.fmHintValue = fmHint;
        } else {
            throw new IllegalArgumentException("FM hint:" + fmHint);
        }
        this.defaulting = false;
    }

    /** Whether there is a transform other than the identity. */
    public boolean isTransformed() {
        if (this.defaulting) {
            return false;
        }
        return this.tx != null;
    }

    /** The transform's type, as {@link AffineTransform#getType} classifies it. */
    public int getTransformType() {
        if (this.defaulting || this.tx == null) {
            return AffineTransform.TYPE_IDENTITY;
        }
        return this.tx.getType();
    }

    /** The transform; the identity if there is none. */
    public AffineTransform getTransform() {
        if (this.tx == null) {
            return new AffineTransform();
        }
        return new AffineTransform(this.tx);
    }

    /**
     * Whether the text is going to be smoothed.
     *
     * <p>It returns `true` for any smoothing mode, the subpixel ones included; to know which one,
     * {@link #getAntiAliasingHint}.
     */
    public boolean isAntiAliased() {
        return this.aaHintValue != RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
                && this.aaHintValue != RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
    }

    /** Whether the advances are carried with decimals. */
    public boolean usesFractionalMetrics() {
        return this.fmHintValue == RenderingHints.VALUE_FRACTIONALMETRICS_ON;
    }

    /** The smoothing mode. */
    public Object getAntiAliasingHint() {
        if (this.defaulting) {
            return RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        }
        return this.aaHintValue;
    }

    /** The metrics mode. */
    public Object getFractionalMetricsHint() {
        if (this.defaulting) {
            return RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
        }
        return this.fmHintValue;
    }

    /** Equality by transform and by the two modes. */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        return this.equals((FontRenderContext) obj);
    }

    /**
     * The same, with the type already known.
     *
     * <p>It exists on top of {@link #equals(Object)} because it is called per text and per glyph,
     * and saving the type check on that path shows.
     */
    public boolean equals(FontRenderContext rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs == null) {
            return false;
        }
        if (!rhs.getTransform().equals(this.getTransform())) {
            return false;
        }
        if (!rhs.getAntiAliasingHint().equals(this.getAntiAliasingHint())) {
            return false;
        }
        return rhs.getFractionalMetricsHint().equals(this.getFractionalMetricsHint());
    }

    public int hashCode() {
        int hash = this.tx == null ? 0 : this.tx.hashCode();
        if (this.defaulting) {
            hash = hash + RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT.hashCode();
            hash = hash + RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT.hashCode();
        } else {
            hash = hash + this.aaHintValue.hashCode();
            hash = hash + this.fmHintValue.hashCode();
        }
        return hash;
    }
}
