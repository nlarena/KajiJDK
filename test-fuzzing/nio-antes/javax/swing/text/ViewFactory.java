package javax.swing.text;

/**
 * Who decides which view corresponds to each element.
 *
 * <p>A single method, and in it lies the system's freedom: the same document is seen as plain
 * text, as HTML or as a list, according to who builds the views. An editor changes its look by
 * changing this factory, not the document.
 */
public interface ViewFactory {

    /** The view that corresponds to that element. */
    View create(Element elem);
}
