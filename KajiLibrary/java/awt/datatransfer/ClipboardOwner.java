package java.awt.datatransfer;

/**
 * Who to tell when the clipboard is taken away from them.
 *
 * <p>The clipboard has **one** owner at a time: the last one who copied. When someone else copies,
 * the previous owner gets this notice, and that is when it can release whatever it was holding in
 * order to be able to hand it over.
 *
 * <p>There is no guarantee of when it arrives or that it arrives at all: if the program closes
 * first, it never does. That is why it is no use for releasing anything critical.
 */
public interface ClipboardOwner {

    /** Reports that someone else has taken the clipboard. */
    void lostOwnership(Clipboard clipboard, Transferable contents);
}
