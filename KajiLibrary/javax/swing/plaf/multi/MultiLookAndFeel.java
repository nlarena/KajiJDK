package javax.swing.plaf.multi;

import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

/**
 * The look and feel that does not draw: it shares each component out between the main one and the
 * auxiliaries.
 *
 * <h2>What for</h2>
 *
 * <p>For hanging on Swing observers that need the same calls as the look and feel that does the
 * real work: a screen reader, a logger of what the user does, a help system that follows the
 * focus. They are added with {@link UIManager#addAuxiliaryLookAndFeel} and from then on each
 * component receives a look and feel from {@code javax.swing.plaf.multi} instead of the plain one.
 *
 * <h2>Where it is decided</h2>
 *
 * <p>In {@link #createUIs}, which the package's thirty {@code createUI} methods call. There the
 * main look and feel is asked for its own, then each auxiliary, and the multiplexer is returned.
 *
 * <p>Except when there is only one: in that case that one is returned, unwrapped. It is not a
 * cosmetic optimization --it is the normal case, that of an application with no auxiliaries-- and
 * wrapping it would cost one extra call in every operation of every component on the screen.
 *
 * <h2>This look and feel has no values of its own</h2>
 *
 * <p>{@link #getDefaults} returns the table of the one that does draw. It is consistent with what
 * this class is: it contributes neither colours nor typefaces, it only shares out.
 *
 * <h2>State in this library</h2>
 *
 * <p>It works: the registration of auxiliaries is real and so is the sharing out. What there is not
 * is any implemented look and feel to share out from, so in practice there is nothing to multiplex
 * yet. The mechanism is there and is tested.
 *
 * @since 1.2
 */
public class MultiLookAndFeel extends LookAndFeel {

    /** One. */
    public MultiLookAndFeel() {
    }

    /**
     * The name to show.
     *
     * @return {@code "Multiplexing Look and Feel"}
     */
    @Override
    public String getName() {
        return "Multiplexing Look and Feel";
    }

    /**
     * The short identifier.
     *
     * @return {@code "Multiplex"}
     */
    @Override
    public String getID() {
        return "Multiplex";
    }

    /**
     * What it does.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Allows multiple UI instances per component instance";
    }

    /**
     * Whether it is the platform's own look and feel.
     *
     * @return false: this one draws nothing
     */
    @Override
    public boolean isNativeLookAndFeel() {
        return false;
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
     * The table of values.
     *
     * @return the one of the look and feel that does draw
     */
    @Override
    public UIDefaults getDefaults() {
        return UIManager.getDefaults();
    }

    /**
     * It builds a component's list of looks and feels.
     *
     * <p>First the main look and feel's and then each auxiliary's, in the order in which they were
     * added. That order is what decides who answers when a method returns a value: the
     * first one, that is the main one.
     *
     * <p>If the main one gave none, there is nothing to add auxiliaries to and it returns {@code
     * null}: a component with no look and feel is the main look and feel's problem, and covering it
     * up with an empty multiplexer would turn it into a failure later and somewhere else.
     *
     * @param mui the multiplexer that would be left in charge
     * @param uis the list to note them in; it is modified
     * @param target the component
     * @return the multiplexer, or the only look and feel if there are no auxiliaries, or {@code
     *     null}
     */
    public static ComponentUI createUIs(ComponentUI mui, Vector<ComponentUI> uis,
            JComponent target) {
        ComponentUI ui = UIManager.getDefaults().getUI(target);
        if (ui == null) {
            return null;
        }
        uis.addElement(ui);
        final LookAndFeel[] auxiliaries = UIManager.getAuxiliaryLookAndFeels();
        if (auxiliaries != null) {
            for (int i = 0; i < auxiliaries.length; i++) {
                final UIDefaults table = auxiliaries[i].getDefaults();
                if (table == null) {
                    continue;
                }
                ui = table.getUI(target);
                if (ui != null) {
                    uis.addElement(ui);
                }
            }
        }
        return uis.size() == 1 ? uis.elementAt(0) : mui;
    }

    /**
     * The list of looks and feels as an array.
     *
     * @param uis the list, or {@code null}
     * @return a new array; empty if the list was {@code null}
     */
    protected static ComponentUI[] uisToArray(Vector<? extends ComponentUI> uis) {
        if (uis == null) {
            return new ComponentUI[0];
        }
        final ComponentUI[] out = new ComponentUI[uis.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = uis.elementAt(i);
        }
        return out;
    }
}
