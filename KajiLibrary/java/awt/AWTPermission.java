package java.awt;

import java.security.BasicPermission;

/**
 * An AWT permission: "show a window without the warning banner", "read the clipboard", "move the
 * mouse from code".
 *
 * <p>It adds not a single method to {@code BasicPermission}: all the logic --the {@code "*"}
 * wildcard, comparison by name, the permission collection-- is already there. It exists only so
 * that the class name tells the family apart, which is how security policies are written.
 *
 * <p>The constructor with actions ignores the second parameter. It is in the API because a policy
 * instantiates permissions through that signature; an AWTPermission has no actions and
 * {@code getActions()} returns the empty string.
 */
public final class AWTPermission extends BasicPermission {

    private static final long serialVersionUID = 8890392402588814465L;

    public AWTPermission(String name) {
        super(name);
    }

    public AWTPermission(String name, String actions) {
        super(name, actions);
    }
}
