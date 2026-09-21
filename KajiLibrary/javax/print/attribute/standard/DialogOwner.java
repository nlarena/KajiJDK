package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Which window owns the print dialog: the dialog is centred on it and blocks it while open.
 *
 * <p>It is the package's only attribute that is not pure data. The others describe paper, ink or
 * state; this one points at a live object of the graphical interface, and that is why it is the
 * only one that could not be implemented whole here.
 *
 * <p>This class used to say it lacked {@code DialogOwner(java.awt.Window)} and {@code getOwner()}
 * because {@code java.awt.Window} did not exist in this tree --a method whose return type does not
 * exist cannot be declared-- and that the day it appeared they were three lines. It appeared, and
 * these are they.
 *
 * <h2>The window is {@code transient}, and that is not an oversight</h2>
 *
 * <p>An {@link javax.print.attribute.Attribute} is serializable, and a live window cannot be
 * serialized usefully: whatever is recovered on another machine, or in another run, would not be
 * the same window nor could it be. On deserializing the owner is left null, which is exactly what
 * the no-argument constructor means --"the owner is the dialog's own window"-- and therefore a
 * legal state and not a hole.
 *
 * <p>{@code getOwner()} returns null also when it was built with {@code null}, which the JDK
 * accepts without complaint; it was checked against JDK 25.
 */
public final class DialogOwner implements PrintRequestAttribute {

    private static final long serialVersionUID = -1901909867156076547L;

    /** The owner window, or null. `transient` for what the header says. */
    private final transient java.awt.Window owner;

    /** Without an explicit window: the owner is the dialog's own. */
    public DialogOwner() {
        this.owner = null;
    }

    /**
     * With that window as owner.
     *
     * @param window the window; {@code null} is valid and equivalent to the no-argument
     *     constructor, which is what the JDK does
     */
    public DialogOwner(java.awt.Window window) {
        this.owner = window;
    }

    /** The owner window, or null if none was given. */
    public java.awt.Window getOwner() {
        return this.owner;
    }

    public final Class<? extends Attribute> getCategory() {
        return DialogOwner.class;
    }

    public final String getName() {
        return "dialog-owner";
    }
}
