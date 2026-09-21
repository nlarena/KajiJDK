package javax.swing.text;

import javax.swing.plaf.basic.BasicTextUI;

/**
 * The old name of {@link BasicTextUI}.
 *
 * <p>It adds nothing: it exists only so that the code written before the class moved to
 * <code>javax.swing.plaf.basic</code> goes on compiling. It is an empty class on purpose, and
 * deleting it would break that code without gaining anything.
 *
 * @deprecated Use {@link BasicTextUI}.
 */
@Deprecated
public abstract class DefaultTextUI extends BasicTextUI {

    /** Nothing to build; see the class note. */
    protected DefaultTextUI() {
    }
}
