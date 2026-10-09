package javax.swing.plaf.synth;

import javax.swing.JComponent;

/**
 * Where the styles come from.
 *
 * <h2>Why a factory and not a table</h2>
 *
 * <p>Because a component's style may depend on more things than its type: on its name, on who
 * its container is, on a property that was set on it. A table could only look at the type; a
 * factory looks at whatever it likes.
 *
 * <p>It is the point where a look and feel decides how everything looks.
 * {@link SynthLookAndFeel} asks it for a style for each component and for each region of each
 * component.
 *
 * @since 1.5
 */
public abstract class SynthStyleFactory {

    /** A factory. */
    public SynthStyleFactory() {
    }

    /**
     * That component's style for that region.
     *
     * @param c the component
     * @param id the region
     * @return the style; it cannot be {@code null}
     */
    public abstract SynthStyle getStyle(JComponent c, Region id);
}
