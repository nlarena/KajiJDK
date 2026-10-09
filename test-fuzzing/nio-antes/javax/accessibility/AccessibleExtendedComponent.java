package javax.accessibility;

/**
 * What an accessible component can tell beyond its geometry.
 *
 * <p>The three things it adds have something in common: they are text the user **sees but that is
 * not the content** of the control. The tip that appears when the pointer passes, the title of the
 * box around it, the key that activates it. For whoever does not see the screen, that is
 * information that would be lost.
 */
public interface AccessibleExtendedComponent extends AccessibleComponent {

    /** The pop-up tip, or `null` if it has none. */
    String getToolTipText();

    /** The title of the box around it, or `null` if it has none. */
    String getTitledBorderText();

    /** The keyboard shortcuts that activate it, or `null` if it has none. */
    AccessibleKeyBinding getAccessibleKeyBinding();
}
