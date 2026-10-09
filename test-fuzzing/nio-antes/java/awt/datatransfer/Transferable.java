package java.awt.datatransfer;

import java.io.IOException;

/**
 * Something that can be transferred: to the clipboard, or by dragging.
 *
 * <p>The central idea is that one and the same datum is offered in **several formats** at once, and
 * whoever receives it picks the one it understands. Copying a selection from a spreadsheet offers at
 * the same time the plain text, the formatted HTML and the native object: pasting it into a text
 * editor brings the first and pasting it into the same spreadsheet brings the last, without anyone
 * having to convert anything unnecessarily.
 *
 * <p>That is why the data is asked for **by format** and not all at once: converting costs, and only
 * the conversion of the format that was actually asked for is paid for.
 */
public interface Transferable {

    /** Which formats it can be handed over in, from best to worst. */
    DataFlavor[] getTransferDataFlavors();

    /** Whether it can be handed over in that format. */
    boolean isDataFlavorSupported(DataFlavor flavor);

    /**
     * The data in that format.
     *
     * @throws UnsupportedFlavorException if the format is not admitted
     * @throws IOException if the data is no longer available
     */
    Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException;
}
