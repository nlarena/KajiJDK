package java.awt;

import java.awt.event.KeyEvent;

/**
 * Something that looks at key events **after** nobody consumed them.
 *
 * <p>It is the other end of {@link KeyEventDispatcher}: the dispatcher goes before everyone and the
 * post-processor goes after. It is for the shortcut that only has to act if the component with the
 * focus did nothing with the key —a menu key, for example—.
 */
public interface KeyEventPostProcessor {

    /**
     * Looks at that already dispatched event.
     *
     * @return `true` if it consumed it and no other post-processor has to see it
     */
    boolean postProcessKeyEvent(KeyEvent e);
}
