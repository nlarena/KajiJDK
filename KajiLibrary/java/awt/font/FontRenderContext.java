package java.awt.font;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * Las condiciones en las que se va a medir y dibujar un texto.
 *
 * <p>Un texto no tiene un tamaño absoluto: cuánto mide depende de la escala a la que se dibuje y de
 * si se suaviza el contorno o no. Este objeto junta esas condiciones para que medir y dibujar den lo
 * mismo.
 *
 * <p>Las métricas fraccionarias son la distinción menos obvia. Sin ellas, el avance de cada carácter
 * se redondea a un píxel entero, y el ancho de una palabra es la suma de esos redondeos; con ellas
 * el avance se lleva con decimales y sólo se redondea al final. La diferencia se acumula: la misma
 * frase puede medir varios píxeles distinto según cuál se use.
 */
public class FontRenderContext {

    private final AffineTransform tx;
    private final Object aaHintValue;
    private final Object fmHintValue;
    private final boolean defaulting;

    /**
     * Uno con todo por omisión.
     *
     * <p>Es para las subclases que calculan sus condiciones a demanda.
     */
    protected FontRenderContext() {
        this.tx = null;
        this.aaHintValue = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        this.fmHintValue = RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
        this.defaulting = true;
    }

    /** Con el suavizado y las métricas dados como booleanos. */
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
     * Con el suavizado y las métricas dados como valores de {@link RenderingHints}.
     *
     * <p>Acepta más matices que la versión de booleanos: el suavizado tiene, además de sí y no, los
     * modos para pantallas de subpíxeles.
     *
     * @throws IllegalArgumentException si alguno de los dos valores no corresponde a su clave
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

    /** Si hay una transformación que no sea la identidad. */
    public boolean isTransformed() {
        if (this.defaulting) {
            return false;
        }
        return this.tx != null;
    }

    /** El tipo de la transformación, como lo clasifica {@link AffineTransform#getType}. */
    public int getTransformType() {
        if (this.defaulting || this.tx == null) {
            return AffineTransform.TYPE_IDENTITY;
        }
        return this.tx.getType();
    }

    /** La transformación; la identidad si no hay. */
    public AffineTransform getTransform() {
        if (this.tx == null) {
            return new AffineTransform();
        }
        return new AffineTransform(this.tx);
    }

    /**
     * Si el texto se va a suavizar.
     *
     * <p>Devuelve `true` para cualquier modo de suavizado, incluidos los de subpíxeles; para saber
     * cuál, {@link #getAntiAliasingHint}.
     */
    public boolean isAntiAliased() {
        return this.aaHintValue != RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
                && this.aaHintValue != RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
    }

    /** Si los avances se llevan con decimales. */
    public boolean usesFractionalMetrics() {
        return this.fmHintValue == RenderingHints.VALUE_FRACTIONALMETRICS_ON;
    }

    /** El modo de suavizado. */
    public Object getAntiAliasingHint() {
        if (this.defaulting) {
            return RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        }
        return this.aaHintValue;
    }

    /** El modo de métricas. */
    public Object getFractionalMetricsHint() {
        if (this.defaulting) {
            return RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
        }
        return this.fmHintValue;
    }

    /** Igualdad por transformación y por los dos modos. */
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
     * Lo mismo, con el tipo ya conocido.
     *
     * <p>Existe además de {@link #equals(Object)} porque se llama por texto y por glifo, y ahorrar
     * la comprobación de tipo en ese camino se nota.
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
