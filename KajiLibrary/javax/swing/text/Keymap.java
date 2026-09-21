package javax.swing.text;

import javax.swing.Action;
import javax.swing.KeyStroke;

/**
 * What each key does: a map from combinations to actions.
 *
 * <p>The maps are chained through their resolving parent, just like the styles: one of one's own
 * with two keys changed hangs from the default map and inherits the rest. Changing an
 * application's shortcut is adding a link, not copying the table.
 *
 * <p>{@link #getDefaultAction} is the one that attends to what matched nothing: in an editor,
 * the one that inserts the character that was typed.
 *
 * <p>Swing replaced it with {@code InputMap} and {@code ActionMap}, which hold for any component;
 * this one stayed because the text components still expose it.
 */
public interface Keymap {

    String getName();

    /** The action for what is not bound; see the interface note. */
    Action getDefaultAction();

    void setDefaultAction(Action a);

    /** That combination's action, looking at the parent maps too. */
    Action getAction(KeyStroke key);

    KeyStroke[] getBoundKeyStrokes();

    Action[] getBoundActions();

    KeyStroke[] getKeyStrokesForAction(Action a);

    /** Whether that combination is bound in <em>this</em> map, without looking at the parents. */
    boolean isLocallyDefined(KeyStroke key);

    void addActionForKeyStroke(KeyStroke key, Action a);

    void removeKeyStrokeBinding(KeyStroke keys);

    void removeBindings();

    Keymap getResolveParent();

    void setResolveParent(Keymap parent);
}
