package javax.swing.text;

import javax.swing.event.ChangeListener;

/**
 * A named set of attributes, which reports when it changes.
 *
 * <p>It is what makes changing a style change at once everything that uses it: the paragraphs do
 * not copy their attributes, they hang from the style as their resolving parent, and when the
 * style changes it reports and they all repaint.
 *
 * <p>The name may be {@code null}: an anonymous style serves just as well to hang from, only it
 * cannot be asked for by name.
 */
public interface Style extends MutableAttributeSet {

    String getName();

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);
}
