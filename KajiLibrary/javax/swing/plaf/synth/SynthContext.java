package javax.swing.plaf.synth;

import javax.swing.JComponent;

/**
 * Everything that has to be known in order to draw a part of a component.
 *
 * <h2>The four data</h2>
 *
 * <p>The component, which part of it is being drawn, with which style, and in what state. They go
 * together because they are always needed together: a button's colour depends on which button it
 * is, on whether its background or its border is being drawn, on the style, and on whether it is
 * pressed.
 *
 * <p>That it is an object and not four parameters is what allows a painter to receive a single
 * thing and adding one more datum to the context not to change a hundred and thirty signatures.
 *
 * <h2>It is immutable</h2>
 *
 * <p>One is built for each drawing operation. In the JDK this came to be reused for performance
 * reasons and was reverted: a shared context that changes under the feet of whoever is looking at
 * it is a source of errors that appear only when repainting fast.
 *
 * @since 1.5
 */
public class SynthContext {

    private final JComponent component;
    private final Region region;
    private final SynthStyle style;
    private final int state;

    /**
     * A context with those data.
     *
     * @param component the component
     * @param region the part that is being drawn
     * @param style the style
     * @param state the state, a combination of {@link SynthConstants}' flags
     * @throws NullPointerException if any of the first three is {@code null}
     */
    public SynthContext(JComponent component, Region region, SynthStyle style, int state) {
        if (component == null || region == null || style == null) {
            throw new NullPointerException("You must supply a non-null component, region and style");
        }
        this.component = component;
        this.region = region;
        this.style = style;
        this.state = state;
    }

    /**
     * One that accepts a null style.
     *
     * <p>The public constructor demands all three, and rightly: a program that builds a context by
     * hand and passes it a null style made a mistake. But the package's {@code SynthXxxUI} classes
     * need to build the context <em>before</em> having the style -- it is with that context that
     * they ask the factory for it -- and that is why this version exists. The JDK does the same,
     * and that is why an uninstalled look and feel's {@code getContext} answers with a null style
     * instead of blowing up. Measured.
     *
     * @param component the component
     * @param region the part
     * @param style the style, which may be null
     * @param state the state
     * @param internal to tell this version from the public one
     */
    SynthContext(JComponent component, Region region, SynthStyle style, int state,
            boolean internal) {
        this.component = component;
        this.region = region;
        this.style = style;
        this.state = state;
    }

    /**
     * The component.
     *
     * @return the component
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * The part that is being drawn.
     *
     * @return the region
     */
    public Region getRegion() {
        return region;
    }

    /**
     * The style.
     *
     * @return the style
     */
    public SynthStyle getStyle() {
        return style;
    }

    /**
     * The component's state.
     *
     * @return the combination of {@link SynthConstants}' flags
     */
    public int getComponentState() {
        return state;
    }
}
