package javax.swing.colorchooser;

import javax.swing.JComponent;

/**
 * Builds the components a {@link javax.swing.JColorChooser} comes assembled with.
 *
 * <p>The installed look and feel uses it, not the program: it is the point from which a
 * `LookAndFeel` assembles the default chooser -- the four tabs and the preview box.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Both methods throw {@link UnsupportedOperationException}. What they build are **interface**
 * components: sliders, formatted fields, a colour diagram painted pixel by pixel. None of that
 * exists in this library, and it is not a matter of writing more code but of there being no
 * painting or events to rest it on.
 *
 * <p>The bad alternative would be returning an empty array of
 * {@link AbstractColorChooserPanel}: the caller would read it as "this look and feel brings no
 * tabs", which is a valid and false answer, and would assemble an empty chooser without noticing
 * anything.
 */
public class ColorChooserComponentFactory {

    /** It is not instantiated: it is a factory class. */
    private ColorChooserComponentFactory() {
    }

    /**
     * The usual tabs: RGB, HSV, HSL, CMYK and the swatches.
     *
     * <p><b>Not implemented in this library.</b> See the class note.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public static AbstractColorChooserPanel[] getDefaultChooserPanels() {
        throw new UnsupportedOperationException(
                "cannot build the default color chooser panels: they are interactive Swing "
                + "components (sliders, formatted fields, a painted color diagram) and this "
                + "library has no painting or event dispatch");
    }

    /**
     * The box that shows the chosen colour next to the previous one.
     *
     * <p><b>Not implemented in this library.</b> See the class note.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public static JComponent getPreviewPanel() {
        throw new UnsupportedOperationException(
                "cannot build the color preview panel: it is a painted Swing component and this "
                + "library has no painting");
    }
}
