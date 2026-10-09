package javax.print;

/**
 * KajiLibrary's javax.print.ServiceUIFactory -- a printer's own graphical interface.
 *
 * <p>A manufacturer may have options the standard attribute model does not cover, and this factory
 * is how it exposes them: it returns components already put together by the driver.
 *
 * <p>It is asked for by <b>role</b> --what the screen is for-- and by <b>type</b> --which class the
 * component is wanted as--, and that crossing is the point: the same administration panel can be
 * asked for as {@link #JCOMPONENT_UI} to put it in a window of one's own, or as {@link #DIALOG_UI}
 * to show it loose.
 *
 * <p>{@link #getUI} returns null if that combination does not exist, which is normal.
 * {@link #getUIClassNamesForRole} serves to ask beforehand.
 *
 * <p>The types are strings and not classes so that asking for a Swing one does not force loading
 * Swing.
 */
public abstract class ServiceUIFactory {

    /** A {@code javax.swing.JComponent}. */
    public static final String JCOMPONENT_UI = "javax.swing.JComponent";

    /** A {@code java.awt.Panel}. */
    public static final String PANEL_UI = "java.awt.Panel";

    /** A {@code java.awt.Dialog}. */
    public static final String DIALOG_UI = "java.awt.Dialog";

    /** A {@code javax.swing.JDialog}. */
    public static final String JDIALOG_UI = "javax.swing.JDialog";

    /** "About" screen. */
    public static final int ABOUT_UIROLE = 1;

    /** Administration screen. */
    public static final int ADMIN_UIROLE = 2;

    /** The main screen. */
    public static final int MAIN_UIROLE = 3;

    /** The first free role for roles of one's own; the lower ones are reserved. */
    public static final int RESERVED_UIROLE = 99;

    /** For subclasses. */
    protected ServiceUIFactory() {
    }

    /**
     * The component of that role and that type, or null if there is none.
     *
     * @param role one of the roles, or one of one's own greater than {@link #RESERVED_UIROLE}
     * @param ui one of the four types
     */
    public abstract Object getUI(int role, String ui);

    /** Which types there are for that role, or null if there are none. */
    public abstract String[] getUIClassNamesForRole(int role);
}
