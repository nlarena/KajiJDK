package java.awt.datatransfer;

import java.io.IOException;
import java.io.StringReader;

/**
 * A string ready to be copied to the clipboard.
 *
 * <p>It is the {@link Transferable} implementation that settles ninety per cent of the cases, and it
 * is written so that copying text is one line.
 *
 * <p>It also implements {@link ClipboardOwner} doing nothing on losing the clipboard: a string is
 * already in memory and there is no resource to release. It is the notice that can be ignored with a
 * clear conscience.
 */
public class StringSelection implements Transferable, ClipboardOwner {

    private static final int STRING = 0;
    private static final int PLAIN_TEXT = 1;

    private static final DataFlavor[] flavors = {
        DataFlavor.stringFlavor,
        DataFlavor.plainTextFlavor
    };

    private final String data;

    /** With the string to transfer. */
    public StringSelection(String data) {
        this.data = data;
    }

    /** Java text first, plain text after. */
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    /** Whether it is one of the two text formats. */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (int i = 0; i < flavors.length; i++) {
            if (flavor.equals(flavors[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * The string, or a reader over it.
     *
     * <p>The plain-text format hands over a {@code Reader} and not the string: it is what its
     * representation class declares, and returning the string would break whoever relies on it.
     *
     * @throws UnsupportedFlavorException if the format is not a text one
     * @throws IOException never in practice: the string is already in memory
     */
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (flavor.equals(flavors[STRING])) {
            return this.data;
        }
        if (flavor.equals(flavors[PLAIN_TEXT])) {
            return new StringReader(this.data == null ? "" : this.data);
        }
        throw new UnsupportedFlavorException(flavor);
    }

    /** It does nothing: a string in memory has no resources to release. */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
