package javax.imageio.metadata;

import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadata -- the metadata of an image or of a stream.
 *
 * <p>Everything an image file stores besides the pixels: resolution, date, author, colour profile,
 * comments, camera data.
 *
 * <h2>The two formats, and why both are needed</h2>
 *
 * <p>The same metadata can be seen in two ways:
 *
 * <ul>
 *   <li>the <b>native</b> format, which reflects exactly how the file stores it. It loses nothing
 *       and looks like no other format's;
 *   <li>the <b>standard</b> format {@code javax_imageio_1.0}, common to all. It loses whatever is
 *       specific, and in exchange allows writing code that works with any format.
 * </ul>
 *
 * <p>{@link #isStandardMetadataFormatSupported} says whether the standard one is available.
 * {@link #getMetadataFormatNames} lists all the ones this object understands.
 *
 * <p>Conversion between formats --which is how metadata is copied from PNG to JPEG-- goes through
 * the standard one; see {@code javax.imageio.ImageTranscoder}.
 *
 * <h2>{@link #mergeTree} versus {@link #setFromTree}</h2>
 *
 * <p>It is the distinction to be clear on before touching metadata:
 *
 * <ul>
 *   <li>{@code mergeTree} <b>combines</b>: what the new tree says replaces what was there, and what
 *       it does not mention stays as it was;
 *   <li>{@code setFromTree} <b>replaces</b>: it first clears everything and then applies the tree.
 * </ul>
 *
 * <p>Using the first believing it does the second leaves old metadata stuck, and is the most common
 * reason a rewritten image keeps data that was meant to be deleted.
 *
 * <h2>Metadata may be read-only</h2>
 *
 * <p>{@link #isReadOnly} says so, and you have to ask: a plug-in may hand out metadata that cannot
 * be changed. Trying to modify it throws {@link IllegalStateException}.
 *
 * <h2>The eight standard nodes</h2>
 *
 * <p>The eight {@code getStandardXxxNode} are protected and each returns one branch of the standard
 * format. {@link #getStandardTree} puts them together into the full tree, and it is final: a
 * subclass redefines the branches it can fill in and inherits the assembly.
 *
 * <p>By default all eight return null, which means "I know nothing about this". It is right: a
 * format that stores no date should not make one up.
 */
public abstract class IIOMetadata {

    /** Whether the standard format is available. */
    protected boolean standardFormatSupported;

    /** What the native format is called, or null if there is none. */
    protected String nativeMetadataFormatName = null;

    /** The class that describes that format. */
    protected String nativeMetadataFormatClassName = null;

    /** Other formats this object understands. */
    protected String[] extraMetadataFormatNames = null;

    /** The classes that describe them. */
    protected String[] extraMetadataFormatClassNames = null;

    /** The default controller, or null. */
    protected IIOMetadataController defaultController = null;

    /** The one that is set. */
    protected IIOMetadataController controller = null;

    /** Without a standard format and without a native one. */
    protected IIOMetadata() {
    }

    /**
     * Declaring which formats are understood.
     *
     * @param standardMetadataFormatSupported whether the standard one is available
     * @param nativeMetadataFormatName the native one, or null
     * @param extraMetadataFormatNames other ones, or null
     * @throws IllegalArgumentException if the extra names array is empty, or if the extra class
     *     names are missing or do not match it in length (an earlier note also said "if a name is
     *     empty"; nothing checks that)
     */
    protected IIOMetadata(boolean standardMetadataFormatSupported,
                          String nativeMetadataFormatName,
                          String nativeMetadataFormatClassName,
                          String[] extraMetadataFormatNames,
                          String[] extraMetadataFormatClassNames) {
        this.standardFormatSupported = standardMetadataFormatSupported;
        this.nativeMetadataFormatName = nativeMetadataFormatName;
        this.nativeMetadataFormatClassName = nativeMetadataFormatClassName;
        if (extraMetadataFormatNames != null) {
            if (extraMetadataFormatNames.length == 0) {
                throw new IllegalArgumentException("extraMetadataFormatNames.length == 0!");
            }
            if (extraMetadataFormatClassNames == null) {
                throw new IllegalArgumentException("extraMetadataFormatClassNames == null!");
            }
            if (extraMetadataFormatClassNames.length != extraMetadataFormatNames.length) {
                throw new IllegalArgumentException(
                    "extraMetadataFormatClassNames.length != extraMetadataFormatNames.length!");
            }
            this.extraMetadataFormatNames = copy(extraMetadataFormatNames);
            this.extraMetadataFormatClassNames = copy(extraMetadataFormatClassNames);
        } else {
            if (extraMetadataFormatClassNames != null) {
                throw new IllegalArgumentException(
                    "extraMetadataFormatNames == null && extraMetadataFormatClassNames != null!");
            }
        }
    }

    /** Whether the standard format is available. */
    public boolean isStandardMetadataFormatSupported() {
        return this.standardFormatSupported;
    }

    /** Whether it cannot be modified. See the class note. */
    public abstract boolean isReadOnly();

    /** What the native format is called, or null. */
    public String getNativeMetadataFormatName() {
        return this.nativeMetadataFormatName;
    }

    /** The other formats, or null. It is a copy. */
    public String[] getExtraMetadataFormatNames() {
        return copy(this.extraMetadataFormatNames);
    }

    /**
     * All the formats this object understands.
     *
     * <p>The native one first, then the standard one if available, and then the extra ones. The
     * order matters: a program that wants maximum fidelity takes the first.
     *
     * @return null if it understands none
     */
    public String[] getMetadataFormatNames() {
        String nativeName = getNativeMetadataFormatName();
        String standardName = null;
        if (isStandardMetadataFormatSupported()) {
            standardName = IIOMetadataFormatImpl.standardMetadataFormatName;
        }
        String[] extraNames = getExtraMetadataFormatNames();
        int count = 0;
        if (nativeName != null) {
            count = count + 1;
        }
        if (standardName != null) {
            count = count + 1;
        }
        if (extraNames != null) {
            count = count + extraNames.length;
        }
        if (count == 0) {
            return null;
        }
        String[] result = new String[count];
        int at = 0;
        if (nativeName != null) {
            result[at] = nativeName;
            at = at + 1;
        }
        if (standardName != null) {
            result[at] = standardName;
            at = at + 1;
        }
        if (extraNames != null) {
            int i = 0;
            while (i < extraNames.length) {
                result[at] = extraNames[i];
                at = at + 1;
                i = i + 1;
            }
        }
        return result;
    }

    /**
     * The schema of that format.
     *
     * <p>It is loaded by reflection from the declared class name, looking for its static
     * {@code getInstance} method. It is the convention the documentation asks for, and it is how a
     * format defined by a plug-in becomes visible without this class knowing it.
     *
     * @throws IllegalArgumentException if the name is null or not one of the declared ones
     * @throws IllegalStateException if the class that describes it could not be loaded
     */
    public IIOMetadataFormat getMetadataFormat(String formatName) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (this.standardFormatSupported
            && formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return IIOMetadataFormatImpl.getStandardFormatInstance();
        }
        String className = null;
        if (formatName.equals(this.nativeMetadataFormatName)) {
            className = this.nativeMetadataFormatClassName;
        } else if (this.extraMetadataFormatNames != null) {
            int i = 0;
            while (i < this.extraMetadataFormatNames.length) {
                if (formatName.equals(this.extraMetadataFormatNames[i])) {
                    className = this.extraMetadataFormatClassNames[i];
                }
                i = i + 1;
            }
        }
        if (className == null) {
            throw new IllegalArgumentException("Unsupported format name");
        }
        try {
            Class<?> cls = Class.forName(className, true, getClass().getClassLoader());
            java.lang.reflect.Method meth = cls.getMethod("getInstance");
            return (IIOMetadataFormat) meth.invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException("Can't obtain format");
        }
    }

    /**
     * The metadata tree in that format.
     *
     * @return null if this object has nothing to say in that format
     * @throws IllegalArgumentException if the format is not one of the declared ones
     */
    public abstract Node getAsTree(String formatName);

    /**
     * Combines that tree with what is already there. See the class note.
     *
     * @throws IllegalStateException if it is read-only
     * @throws IllegalArgumentException if the format is not one of the declared ones
     * @throws IIOInvalidTreeException if the tree does not follow the format
     */
    public abstract void mergeTree(String formatName, Node root) throws IIOInvalidTreeException;

    /** The colour branch of the standard format, or null if unknown. */
    protected IIOMetadataNode getStandardChromaNode() {
        return null;
    }

    /** The compression one, or null. */
    protected IIOMetadataNode getStandardCompressionNode() {
        return null;
    }

    /** The data layout one, or null. */
    protected IIOMetadataNode getStandardDataNode() {
        return null;
    }

    /** The size and resolution one, or null. */
    protected IIOMetadataNode getStandardDimensionNode() {
        return null;
    }

    /** The date and version one, or null. */
    protected IIOMetadataNode getStandardDocumentNode() {
        return null;
    }

    /** The embedded texts one, or null. */
    protected IIOMetadataNode getStandardTextNode() {
        return null;
    }

    /** The tiling one, or null. */
    protected IIOMetadataNode getStandardTileNode() {
        return null;
    }

    /** The transparency one, or null. */
    protected IIOMetadataNode getStandardTransparencyNode() {
        return null;
    }

    /**
     * The full standard tree, built from the eight branches.
     *
     * <p>It is final: a subclass redefines the branches it can fill in and inherits the assembly.
     * Those that return null do not appear.
     */
    protected final IIOMetadataNode getStandardTree() {
        IIOMetadataNode root =
            new IIOMetadataNode(IIOMetadataFormatImpl.standardMetadataFormatName);
        appendIfPresent(root, getStandardChromaNode());
        appendIfPresent(root, getStandardCompressionNode());
        appendIfPresent(root, getStandardDataNode());
        appendIfPresent(root, getStandardDimensionNode());
        appendIfPresent(root, getStandardDocumentNode());
        appendIfPresent(root, getStandardTextNode());
        appendIfPresent(root, getStandardTileNode());
        appendIfPresent(root, getStandardTransparencyNode());
        return root;
    }

    /**
     * Replaces everything with that tree. See the class note.
     *
     * <p>This implementation is {@link #reset} followed by {@link #mergeTree}, which is exactly
     * what it means.
     *
     * @throws IllegalStateException if it is read-only
     * @throws IllegalArgumentException if the format is not one of the declared ones
     * @throws IIOInvalidTreeException if the tree does not follow the format
     */
    public void setFromTree(String formatName, Node root) throws IIOInvalidTreeException {
        reset();
        mergeTree(formatName, root);
    }

    /**
     * Back to the initial state.
     *
     * @throws IllegalStateException if it is read-only
     */
    public abstract void reset();

    /** Who fills in this metadata; null uses the default one. */
    public void setController(IIOMetadataController controller) {
        this.controller = controller;
    }

    /** The one that is set. */
    public IIOMetadataController getController() {
        return this.controller;
    }

    /** The default one, or null. */
    public IIOMetadataController getDefaultController() {
        return this.defaultController;
    }

    /** Whether there is one. */
    public boolean hasController() {
        return getController() != null;
    }

    /**
     * Asks the controller to fill it in.
     *
     * @return whether the user accepted
     * @throws IllegalStateException if there is no controller
     */
    public boolean activateController() {
        if (!hasController()) {
            throw new IllegalStateException("hasController() == false!");
        }
        return getController().activate(this);
    }

    /** Appends the branch if it is not null. */
    private static void appendIfPresent(IIOMetadataNode root, IIOMetadataNode node) {
        if (node != null) {
            root.appendChild(node);
        }
    }

    /** A copy of the array, or null. */
    private static String[] copy(String[] source) {
        if (source == null) {
            return null;
        }
        String[] result = new String[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
