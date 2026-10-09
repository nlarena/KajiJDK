package javax.swing.plaf.synth;

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.io.InputStream;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLookAndFeel;

/**
 * The look and feel that draws nothing on its own: somebody else describes it.
 *
 * <h2>What problem it solves</h2>
 *
 * <p>Writing a look and feel for Swing meant writing a hundred classes. Synth inverts that: the
 * hundred classes are already there --they are the {@code SynthXxxUI}-- and what changes is a
 * file that says what colour each part is and which image it is drawn with. A designer can
 * change the look without writing Java.
 *
 * <p>{@link #load} is where that description comes in.
 * {@link javax.swing.plaf.nimbus.NimbusLookAndFeel} is the other path: instead of reading a
 * file, it brings the description written in code.
 *
 * <h2>The style factory</h2>
 *
 * <p>{@link #setStyleFactory} is the joining point. Each {@code SynthXxxUI} asks the factory for
 * its region's style, and the factory is the only thing that knows where the description came
 * from. It is static --and not an instance field-- because the looks and feels consult it
 * without having the look and feel at hand.
 *
 * <h2>State in this library</h2>
 *
 * <p>The factory's registry, the regions and the life cycle work. What there is not are the
 * {@code SynthXxxUI} classes, which are forty drawing classes and are almost the whole package;
 * that is why {@link #createUI} has nothing to return and {@link #load} has nothing to read.
 * What is complete is what {@code javax.swing.plaf.nimbus} needs.
 *
 * @since 1.5
 */
public class SynthLookAndFeel extends BasicLookAndFeel {

    private static SynthStyleFactory factory;

    /** One. */
    public SynthLookAndFeel() {
    }

    /**
     * It sets where the styles come from.
     *
     * <p>It is static because the looks and feels consult it without having the look and feel at
     * hand. The consequence is that there is a single one per process, which is consistent with
     * there being a single look and feel per process.
     *
     * @param cache the factory, or {@code null} for having none
     */
    public static void setStyleFactory(SynthStyleFactory cache) {
        synchronized (SynthLookAndFeel.class) {
            factory = cache;
        }
    }

    /**
     * Where the styles come from.
     *
     * @return the factory, or {@code null}
     */
    public static SynthStyleFactory getStyleFactory() {
        synchronized (SynthLookAndFeel.class) {
            return factory;
        }
    }

    /**
     * That component's style for that region.
     *
     * @param c the component
     * @param region the region
     * @return the style, or {@code null} if there is no factory
     */
    public static SynthStyle getStyle(JComponent c, Region region) {
        final SynthStyleFactory f = getStyleFactory();
        return f == null ? null : f.getStyle(c, region);
    }

    /**
     * It asks again for the style of that component and of every one it contains.
     *
     * <p>It is needed when something the factory looks at in order to decide changes: a component's
     * name, a property, the whole look and feel. Without this, the components already on the screen
     * would be left with the old style.
     *
     * @param c the component to go down from
     */
    public static void updateStyles(Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).updateUI();
        }
        if (c instanceof Container) {
            final Container cont = (Container) c;
            for (int i = 0; i < cont.getComponentCount(); i++) {
                updateStyles(cont.getComponent(i));
            }
        }
    }

    /**
     * The region that corresponds to that component.
     *
     * @param c the component
     * @return the region, or {@code null} if its identifier corresponds to none
     */
    public static Region getRegion(JComponent c) {
        return Region.byUI(c.getUIClassID());
    }

    /**
     * That component's look and feel.
     *
     * @param c the component
     * @return the look and feel, or {@code null}
     * @throws UnsupportedOperationException in this library, which does not have the
     *     {@code SynthXxxUI} classes
     */
    public static ComponentUI createUI(JComponent c) {
        final String name = "javax.swing.plaf.synth.Synth" + c.getUIClassID();
        try {
            final Class<?> k = Class.forName(name);
            final java.lang.reflect.Method m = k.getMethod("createUI", JComponent.class);
            return (ComponentUI) m.invoke(null, c);
        } catch (Exception e) {
            throw new Error("no synth class for " + c.getUIClassID(), e);
        }
    }

    /**
     * That component's state, as Synth understands it.
     *
     * <p>Three cases and nothing else: disabled, enabled, and enabled with the focus. The other
     * {@link SynthConstants} states -- pressed, with the cursor over it, selected -- are added by
     * each look and feel looking at its own model, because a component with no buttons has no way
     * of being pressed.
     *
     * @param c the component
     * @return the combination of flags
     */
    static int stateOf(java.awt.Component c) {
        if (c == null || !c.isEnabled()) {
            return SynthConstants.DISABLED;
        }
        if (c.isFocusOwner()) {
            return SynthConstants.ENABLED | SynthConstants.FOCUSED;
        }
        return SynthConstants.ENABLED;
    }

    /**
     * It asks the factory for that context's style and installs it on the component.
     *
     * <p>It is what each {@code SynthXxxUI} does on installing itself and every time something
     * changes. If there is no factory it blows up with {@code NullPointerException}, and
     * <strong>that is measured</strong>: in the JDK, installing any Synth look and feel without
     * having loaded a style file first throws exactly that exception. It is not an oversight of
     * this library; it is that Synth has no default look, and that is precisely the point of the
     * package.
     *
     * @param context the context
     * @return the new style
     */
    static SynthStyle update(SynthContext context) {
        final SynthStyleFactory f = getStyleFactory();
        final SynthStyle style = f.getStyle(context.getComponent(), context.getRegion());
        if (style != null) {
            style.installDefaults(new SynthContext(context.getComponent(),
                    context.getRegion(), style, context.getComponentState()));
        }
        return style;
    }

    /**
     * It reads the look's description from a file.
     *
     * @param input where to read from
     * @param resourceBase the class the resources the file names are resolved against
     * @throws ParseException if the file is not understood
     * @throws UnsupportedOperationException in this library, which does not have the reader
     */
    public void load(InputStream input, Class<?> resourceBase) throws ParseException {
        throw new UnsupportedOperationException("this library has no reader of "
                + "synth descriptions");
    }

    /** It installs itself. */
    @Override
    public void initialize() {
        super.initialize();
    }

    /** It uninstalls itself; it releases the style factory. */
    @Override
    public void uninitialize() {
        setStyleFactory(null);
        super.uninitialize();
    }

    /**
     * The table of values.
     *
     * @return the table
     */
    @Override
    public UIDefaults getDefaults() {
        return super.getDefaults();
    }

    /**
     * Whether it serves on this platform.
     *
     * @return true: it does not depend on the platform
     */
    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    /**
     * Whether it is the platform's own look and feel.
     *
     * @return false
     */
    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    /**
     * What it is.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Synth Look and Feel";
    }

    /**
     * The name to show.
     *
     * @return {@code "Synth Look and Feel"}
     */
    @Override
    public String getName() {
        return "Synth Look and Feel";
    }

    /**
     * The short identifier.
     *
     * @return {@code "Synth"}
     */
    @Override
    public String getID() {
        return "Synth";
    }

    /**
     * Whether the style has to be checked when the component changes container.
     *
     * <p>With {@code false} work is saved; with {@code true} a look and feel may decide the style
     * according to where the component is --a button inside a tool bar looks different--. By
     * default no, because most looks and feels do not need it and checking it costs on every
     * addition.
     *
     * @return false
     */
    public boolean shouldUpdateStyleOnAncestorChanged() {
        return false;
    }

    /**
     * Whether that property change forces checking the style.
     *
     * @param ev what changed
     * @return true if the style may have changed
     */
    protected boolean shouldUpdateStyleOnEvent(PropertyChangeEvent ev) {
        final String n = ev.getPropertyName();
        return "name".equals(n) || "componentOrientation".equals(n)
                || "ancestor".equals(n) && shouldUpdateStyleOnAncestorChanged();
    }
}
