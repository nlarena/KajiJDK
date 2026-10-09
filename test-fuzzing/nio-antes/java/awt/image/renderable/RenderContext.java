package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * KajiLibrary's java.awt.image.renderable.RenderContext -- which part, at what scale and by which
 * criterion.
 *
 * <p>The three things needed to turn a {@link RenderableImage} into pixels:
 *
 * <ul>
 *   <li>the <b>transformation</b>, which is what fixes the resolution. There is no "scale" field:
 *       the size comes from how much the matrix enlarges, and that is why the same object serves to
 *       scale, rotate and shear without having a method for each;
 *   <li>the <b>area of interest</b>, so as not to compute what will not be seen. It is a
 *       {@link Shape} and not a rectangle because a rotated region is not a rectangle;
 *   <li>the <b>hints</b>, which say whether quality or speed is preferred.
 * </ul>
 *
 * <h2>The transformation methods, and two that are a typo</h2>
 *
 * <p>{@link #concatenateTransform} applies {@code modTransform} <b>first</b> and then the transform
 * that was already there ({@code [this] = [this] x [modTransform]}); {@link
 * #preConcatenateTransform} applies the existing one first and {@code modTransform} after it
 * ({@code [this] = [modTransform] x [this]}). This note had it the other way round. The difference
 * matters because composing matrices does not commute: rotating and then moving is not the same as
 * moving and then rotating.
 *
 * <p>{@link #concetenateTransform} and {@link #preConcetenateTransform} are the same methods with
 * the name misspelled. They shipped that way in 1.2 (this note said 1999), and have been deprecated
 * since the correct ones were added in 1.3. They remain because there is compiled code that calls
 * them: removing them would fix nothing and would break that.
 */
public class RenderContext implements Cloneable {

    private AffineTransform usr2dev;

    private Shape aoi;

    private RenderingHints hints;

    /**
     * Everything explicit.
     *
     * <p>The transformation is copied, so moving it outside does not move the context's.
     *
     * @throws NullPointerException if the transformation is null. There is no defence against that,
     *     nor was there in the JDK, and it is right that there is none: a context without a
     *     transformation has no resolution, and silently substituting the identity would produce a
     *     rendering at scale 1 that nobody asked for
     */
    public RenderContext(AffineTransform usr2dev, Shape aoi, RenderingHints hints) {
        this.hints = hints;
        this.aoi = aoi;
        this.usr2dev = (AffineTransform) usr2dev.clone();
    }

    /** Only the transformation: the whole image and no hints. */
    public RenderContext(AffineTransform usr2dev) {
        this(usr2dev, null, null);
    }

    /** No area of interest. */
    public RenderContext(AffineTransform usr2dev, RenderingHints hints) {
        this(usr2dev, null, hints);
    }

    /** No hints. */
    public RenderContext(AffineTransform usr2dev, Shape aoi) {
        this(usr2dev, aoi, null);
    }

    /** The quality-versus-speed hints, or null. */
    public RenderingHints getRenderingHints() {
        return this.hints;
    }

    /** Ver {@link #getRenderingHints}. */
    public void setRenderingHints(RenderingHints hints) {
        this.hints = hints;
    }

    /** The user-to-device transformation. A copy is stored. */
    public void setTransform(AffineTransform newTransform) {
        this.usr2dev = (AffineTransform) newTransform.clone();
    }

    /**
     * Applies {@code modTransform} <b>after</b> the existing transformation:
     * {@code [this] = [modTransform] x [this]}. (This javadoc said before.)
     */
    public void preConcatenateTransform(AffineTransform modTransform) {
        this.usr2dev.preConcatenate(modTransform);
    }

    /**
     * The same as {@link #preConcatenateTransform}.
     *
     * @deprecated the name is misspelled; see the class note
     */
    @Deprecated
    public void preConcetenateTransform(AffineTransform modTransform) {
        preConcatenateTransform(modTransform);
    }

    /**
     * Applies {@code modTransform} <b>before</b> the existing transformation:
     * {@code [this] = [this] x [modTransform]}. (This javadoc said after.)
     */
    public void concatenateTransform(AffineTransform modTransform) {
        this.usr2dev.concatenate(modTransform);
    }

    /**
     * The same as {@link #concatenateTransform}.
     *
     * @deprecated the name is misspelled; see the class note
     */
    @Deprecated
    public void concetenateTransform(AffineTransform modTransform) {
        concatenateTransform(modTransform);
    }

    /** The transformation. A copy is returned, so nobody moves it behind its back. */
    public AffineTransform getTransform() {
        return (AffineTransform) this.usr2dev.clone();
    }

    /** The region of interest, or null for the whole image. */
    public void setAreaOfInterest(Shape newAoi) {
        this.aoi = newAoi;
    }

    /** Ver {@link #setAreaOfInterest}. */
    public Shape getAreaOfInterest() {
        return this.aoi;
    }

    /**
     * A copy.
     *
     * <p>The transformation is really copied --it is mutable, and sharing it would ruin both
     * copies--; the area of interest and the hints are shared, which is what the JDK does.
     */
    public Object clone() {
        RenderContext copy = new RenderContext(this.usr2dev, this.aoi, this.hints);
        return copy;
    }
}
