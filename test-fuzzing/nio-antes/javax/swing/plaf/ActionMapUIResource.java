package javax.swing.plaf;

import javax.swing.ActionMap;

/**
 * An {@link ActionMap} marked as set by the look and feel.
 *
 * <p>The mark is the whole class. It serves so that on changing look and feel this table is
 * replaced and whatever the program set is respected; see {@link UIResource}.
 */
public class ActionMapUIResource extends ActionMap implements UIResource {

    public ActionMapUIResource() {
    }
}
