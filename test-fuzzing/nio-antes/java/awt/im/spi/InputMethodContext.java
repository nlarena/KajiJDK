package java.awt.im.spi;

import java.awt.Window;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import javax.swing.JFrame;

/**
 * Through which an {@link InputMethod} talks to the client and to the framework.
 *
 * <p>It inherits {@link InputMethodRequests}, which is the asking half --where the cursor is, what
 * text is around-- and adds the telling half: the event with the composed text, and the auxiliary
 * windows.
 *
 * <p>The framework provides it, not the input method. The latter receives it only once, in
 * {@link InputMethod#setInputMethodContext}.
 *
 * <h2>Why there are two ways to create a window</h2>
 *
 * <p>{@link #createInputMethodWindow} gives an AWT window and {@link #createInputMethodJFrame} a
 * Swing one. It is not duplication for the sake of it: an input method window has to appear
 * **without stealing focus** --if it stole it, the client would stop receiving keystrokes, which is
 * the only thing the input method is trying to get-- and that is solved differently in each
 * component set. The Swing one exists because an AWT window inside a Swing application mixes badly.
 */
public interface InputMethodContext extends InputMethodRequests {

    /**
     * Sends an input method event to the client.
     *
     * <p>It is how the text travels: the first `committedCharacterCount` characters of the iterator
     * are final and the rest is what is still being composed.
     *
     * @param id the event type, one of `InputMethodEvent`'s
     * @param text the committed text followed by the composed text, or `null` if there is none
     * @param committedCharacterCount how many characters at the start are final
     * @param caret where the cursor goes within the composed text, or `null`
     * @param visiblePosition which part of the composed text is worth keeping in view, or `null`
     */
    void dispatchInputMethodEvent(int id, AttributedCharacterIterator text,
            int committedCharacterCount, TextHitInfo caret, TextHitInfo visiblePosition);

    /**
     * An AWT window for the input method.
     *
     * <p>It does not take focus when shown, and it does not take it away from the client. See the
     * class note.
     *
     * @param title the title; it may not be visible, depending on the decoration
     * @param attachToInputContext whether the framework has to route input events to the same
     *     context as the client
     */
    Window createInputMethodWindow(String title, boolean attachToInputContext);

    /**
     * A Swing window for the input method.
     *
     * <p>The same as {@link #createInputMethodWindow}, with a {@link JFrame}.
     *
     * @param title the title; it may not be visible, depending on the decoration
     * @param attachToInputContext whether the framework has to route input events to the same
     *     context as the client
     */
    JFrame createInputMethodJFrame(String title, boolean attachToInputContext);

    /**
     * Asks --or stops asking-- for the input method to be told when the client's window moves or
     * changes size.
     *
     * <p>It is off by default, and rightly so: following the window costs, and only a method that
     * opens windows of its own that have to stay attached to the text needs it.
     *
     * @param inputMethod the input method that receives the notices
     * @param enable whether it wants them
     */
    void enableClientWindowNotification(InputMethod inputMethod, boolean enable);
}
