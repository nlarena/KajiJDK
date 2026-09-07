package java.awt.datatransfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A place to leave something for someone else to take.
 *
 * <p>It has **one** content and **one** owner at a time. Putting something new evicts what was there
 * and tells its owner, which is the only signal it has for releasing whatever it was holding.
 *
 * <p>This class is a **private** clipboard: it lives inside the program and serves to move data
 * between parts of the same application. The system's, the one shared with other programs, is handed
 * over by the {@code Toolkit}, and that is where {@link FlavorMap} comes in to translate formats.
 *
 * <p>{@link #getContents} takes a parameter it **does not use**. It has been in the API since 1.1,
 * where it was going to serve to identify who was asking; it was never used for anything and it
 * stayed. Passing `null` is the normal thing.
 */
public class Clipboard {

    /** The current owner. */
    protected ClipboardOwner owner;

    /** What is inside. */
    protected Transferable contents;

    private final String name;
    private final List<FlavorListener> flavorListeners = new ArrayList<FlavorListener>();

    /** With the given name, so that it can be told apart while debugging. */
    public Clipboard(String name) {
        this.name = name;
    }

    /** What it is called. */
    public String getName() {
        return this.name;
    }

    /**
     * Puts new contents in and evicts the previous owner.
     *
     * <p>The notice to the previous owner goes out **before** the contents change, so that it can
     * still look at what it was holding.
     */
    public synchronized void setContents(Transferable contents, ClipboardOwner owner) {
        ClipboardOwner previous = this.owner;
        Transferable previousContents = this.contents;
        this.owner = owner;
        this.contents = contents;
        if (previous != null && previous != owner) {
            previous.lostOwnership(this, previousContents);
        }
        this.fireFlavorsChanged();
    }

    /**
     * What is inside.
     *
     * @param requestor unused; it has been in the API since 1.1 and never had any effect
     * @return the contents, or `null` if there are none
     */
    public synchronized Transferable getContents(Object requestor) {
        return this.contents;
    }

    /**
     * Which formats what is there can be asked for in.
     *
     * @throws IllegalStateException if the clipboard is not available
     */
    public DataFlavor[] getAvailableDataFlavors() {
        Transferable c = this.getContents(null);
        if (c == null) {
            return new DataFlavor[0];
        }
        return c.getTransferDataFlavors();
    }

    /**
     * Whether what is there can be asked for in that format.
     *
     * @throws NullPointerException if the format is `null`
     * @throws IllegalStateException if the clipboard is not available
     */
    public boolean isDataFlavorAvailable(DataFlavor flavor) {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }
        Transferable c = this.getContents(null);
        if (c == null) {
            return false;
        }
        return c.isDataFlavorSupported(flavor);
    }

    /**
     * What is there, in that format.
     *
     * @throws NullPointerException if the format is `null`
     * @throws IllegalStateException if the clipboard is not available
     * @throws UnsupportedFlavorException if what is there cannot be given in that format
     * @throws IOException if the data is gone
     */
    public Object getData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }
        Transferable c = this.getContents(null);
        if (c == null) {
            throw new UnsupportedFlavorException(flavor);
        }
        return c.getTransferData(flavor);
    }

    /**
     * Adds someone to be told about the changes.
     *
     * <p>A `null` is ignored silently, which is what the JDK does.
     */
    public synchronized void addFlavorListener(FlavorListener listener) {
        if (listener == null) {
            return;
        }
        this.flavorListeners.add(listener);
    }

    /** Removes that listener; a `null` is ignored. */
    public synchronized void removeFlavorListener(FlavorListener listener) {
        if (listener == null) {
            return;
        }
        this.flavorListeners.remove(listener);
    }

    /** The registered listeners. */
    public synchronized FlavorListener[] getFlavorListeners() {
        return this.flavorListeners.toArray(new FlavorListener[this.flavorListeners.size()]);
    }

    /** Tells every listener that the contents changed. */
    private void fireFlavorsChanged() {
        if (this.flavorListeners.isEmpty()) {
            return;
        }
        FlavorEvent e = new FlavorEvent(this);
        FlavorListener[] copy = this.getFlavorListeners();
        for (int i = 0; i < copy.length; i++) {
            copy[i].flavorsChanged(e);
        }
    }
}
