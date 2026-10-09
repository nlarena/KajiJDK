package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that something undoable happened.
 *
 * <p>It is the hook between whoever <em>produces</em> the edits --a document, a model-- and
 * whoever <em>manages</em> them, typically a {@link javax.swing.undo.UndoManager}. That
 * separation is what allows a document to know nothing about undo stacks: it only reports, and
 * whoever keeps track subscribes.
 */
public interface UndoableEditListener extends EventListener {

    /** Notice that something that can be undone was done. */
    void undoableEditHappened(UndoableEditEvent e);
}
