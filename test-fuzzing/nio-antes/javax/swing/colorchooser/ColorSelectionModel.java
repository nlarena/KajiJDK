package javax.swing.colorchooser;

import java.awt.Color;
import javax.swing.event.ChangeListener;

/**
 * The chosen colour, and whoever wants to hear about it when it changes.
 *
 * <p>It is what makes a {@link javax.swing.JColorChooser} with four tabs behave as a single
 * thing: every tab reads and writes this model, and none knows about the others. Moving the red
 * slider in RGB updates HSV's numbers because HSV listens to the model, not to the slider.
 *
 * <p>It is also the point where a program hooks in: listening to the model is the way to hear
 * what the user picks without depending on the interface.
 */
public interface ColorSelectionModel {

    /** The chosen colour. */
    Color getSelectedColor();

    /**
     * Picks that colour.
     *
     * <p>If it differs from the one there was, the listeners are told.
     */
    void setSelectedColor(Color color);

    /** Adds a listener of the colour change. */
    void addChangeListener(ChangeListener listener);

    /** Removes a listener of the colour change. */
    void removeChangeListener(ChangeListener listener);
}
