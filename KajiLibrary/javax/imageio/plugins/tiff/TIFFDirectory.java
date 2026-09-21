package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFDirectory -- a TIFF directory, with its fields.
 *
 * <p>A TIFF is an eight-byte header and a chain of directories; each one lists its fields sorted
 * by tag number. This is one of those directories, already read.
 *
 * <h2>Directories nest</h2>
 *
 * <p>A field whose tag is a pointer --see {@link TIFFTag#isIFDPointer}-- carries not a value but
 * <b>another directory</b>. That is how a JPEG with Exif has, inside, a TIFF with the Exif
 * directory hanging from the main one and the position one hanging from that. {@link #getParentTag}
 * says which tag this one hangs from; null if it is the top one.
 *
 * <h2>One field per tag number</h2>
 *
 * <p>{@link #addTIFFField} <b>replaces</b> whatever field there was with that number; the format
 * does not allow two fields with the same tag in one directory. {@link #getTIFFFields} returns them
 * sorted by number, which is how they are written to the file.
 *
 * <h2>The tag sets decide which names there are</h2>
 *
 * <p>{@link #getTag} looks in the declared sets, in the order they were declared. A number no set
 * knows has no name, and the field can be kept anyway --as anonymous-- but nobody knows what it
 * means. Adding the missing set with {@link #addTagSet} is what makes those same tags start to
 * make sense.
 */
public class TIFFDirectory implements Cloneable {

    /** What TIFF's native metadata format is called. */
    static final String NATIVE_FORMAT = "javax_imageio_tiff_image_1.0";

    /** The declared sets, in lookup order. */
    private List<TIFFTagSet> tagSets;

    /** Which tag this directory hangs from, or null. */
    private TIFFTag parentTag;

    /** The fields, sorted by tag number. */
    private TreeMap<Integer, TIFFField> fields = new TreeMap<Integer, TIFFField>();

    /**
     * An empty directory.
     *
     * @param tagSets which sets to resolve tag numbers against
     * @param parentTag which tag it hangs from, or null if it is the main one
     * @throws NullPointerException if the array of sets is null
     */
    public TIFFDirectory(TIFFTagSet[] tagSets, TIFFTag parentTag) {
        if (tagSets == null) {
            throw new NullPointerException("tagSets == null!");
        }
        this.tagSets = new ArrayList<TIFFTagSet>(tagSets.length);
        int i = 0;
        while (i < tagSets.length) {
            this.tagSets.add(tagSets[i]);
            i = i + 1;
        }
        this.parentTag = parentTag;
    }

    /**
     * The main directory of that metadata.
     *
     * <p>It reads the TIFF native format tree, so it works for any {@link IIOMetadata} that
     * declares it, not only for the one {@link #getAsMetadata} returns.
     *
     * @throws NullPointerException if the metadata is null
     * @throws IllegalArgumentException if it does not understand the TIFF native format
     * @throws IIOInvalidTreeException if the tree does not have the shape the format asks for
     */
    public static TIFFDirectory createFromMetadata(IIOMetadata tiffImageMetadata)
            throws IIOInvalidTreeException {
        if (tiffImageMetadata == null) {
            throw new NullPointerException("tiffImageMetadata == null");
        }
        if (!supportsNativeFormat(tiffImageMetadata)) {
            throw new IllegalArgumentException("Parameter does not support required metadata format!");
        }
        Node root = tiffImageMetadata.getAsTree(NATIVE_FORMAT);
        Node ifd = firstElement(root);
        if (ifd == null || !"TIFFIFD".equals(ifd.getNodeName())) {
            throw new IIOInvalidTreeException("Root must have a TIFFIFD child", root);
        }
        return fromIFDNode(ifd, null);
    }

    /** The declared sets. A copy of the array. */
    public TIFFTagSet[] getTagSets() {
        return this.tagSets.toArray(new TIFFTagSet[this.tagSets.size()]);
    }

    /**
     * Adds a set at the end of the lookup list.
     *
     * <p>Adding one that is already there does nothing; in particular it does not move it, so it
     * does not change who wins when two sets declare the same number.
     *
     * @throws NullPointerException if it is null
     */
    public void addTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new NullPointerException("tagSet == null");
        }
        if (!this.tagSets.contains(tagSet)) {
            this.tagSets.add(tagSet);
        }
    }

    /**
     * Removes it. The fields already there are not touched: they just stop getting a new name.
     *
     * @throws NullPointerException if it is null
     */
    public void removeTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new NullPointerException("tagSet == null");
        }
        this.tagSets.remove(tagSet);
    }

    /** Which tag this directory hangs from, or null if it is the main one. */
    public TIFFTag getParentTag() {
        return this.parentTag;
    }

    /**
     * The tag with that number, looking in the declared sets in order.
     *
     * @return null if no set knows it
     */
    public TIFFTag getTag(int tagNumber) {
        int i = 0;
        while (i < this.tagSets.size()) {
            TIFFTagSet set = this.tagSets.get(i);
            if (set != null) {
                TIFFTag tag = set.getTag(tagNumber);
                if (tag != null) {
                    return tag;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /** How many fields there are. */
    public int getNumTIFFFields() {
        return this.fields.size();
    }

    /** Whether there is a field with that tag number. */
    public boolean containsTIFFField(int tagNumber) {
        return this.fields.containsKey(Integer.valueOf(tagNumber));
    }

    /**
     * Adds the field, replacing whatever had the same number. See the class note.
     *
     * @throws NullPointerException if it is null
     */
    public void addTIFFField(TIFFField f) {
        if (f == null) {
            throw new NullPointerException("f == null");
        }
        this.fields.put(Integer.valueOf(f.getTagNumber()), f);
    }

    /**
     * The field with that tag number.
     *
     * @return null if it is not there
     */
    public TIFFField getTIFFField(int tagNumber) {
        return this.fields.get(Integer.valueOf(tagNumber));
    }

    /** Removes the field with that number. If it is not there, does nothing. */
    public void removeTIFFField(int tagNumber) {
        this.fields.remove(Integer.valueOf(tagNumber));
    }

    /** All the fields, sorted by tag number. A copy of the array. */
    public TIFFField[] getTIFFFields() {
        return this.fields.values().toArray(new TIFFField[this.fields.size()]);
    }

    /** Removes all the fields. The sets and the parent tag stay. */
    public void removeTIFFFields() {
        this.fields.clear();
    }

    /**
     * This directory as image metadata.
     *
     * <p>It is a <b>snapshot</b>: later changes to this directory are not seen in the metadata, nor
     * the other way round. {@link #createFromMetadata} undoes the trip.
     */
    public IIOMetadata getAsMetadata() {
        TIFFDirectory snapshot;
        try {
            snapshot = clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
        return new TIFFMetadata(snapshot);
    }

    /**
     * A copy with its own fields.
     *
     * <p>The field list and the set list are copied, and so is each field --with
     * {@link TIFFField#clone}--, so removing or changing fields in the copy does not touch the
     * original.
     *
     * <p>The JDK shares part of the state here: its copy keeps its own low-numbered tags but shares
     * the map of the high ones, and a {@code removeTIFFFields()} on the original empties half of
     * the copy's fields. This implementation copies both, which is what a copy means.
     */
    @Override
    public TIFFDirectory clone() throws CloneNotSupportedException {
        TIFFDirectory copy = (TIFFDirectory) super.clone();
        copy.tagSets = new ArrayList<TIFFTagSet>(this.tagSets);
        copy.fields = new TreeMap<Integer, TIFFField>();
        for (java.util.Map.Entry<Integer, TIFFField> entry : this.fields.entrySet()) {
            copy.fields.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    /** This directory's tree in the native format, as a {@code TIFFIFD} node. */
    IIOMetadataNode getAsIFDNode() {
        IIOMetadataNode ifd = new IIOMetadataNode("TIFFIFD");
        if (this.parentTag != null) {
            ifd.setAttribute("parentTagNumber", Integer.toString(this.parentTag.getNumber()));
            String parentName = this.parentTag.getName();
            if (parentName != null) {
                ifd.setAttribute("parentTagName", parentName);
            }
        }
        StringBuilder names = new StringBuilder();
        int i = 0;
        while (i < this.tagSets.size()) {
            TIFFTagSet set = this.tagSets.get(i);
            if (set != null) {
                if (names.length() > 0) {
                    names.append(',');
                }
                names.append(set.getClass().getName());
            }
            i = i + 1;
        }
        ifd.setAttribute("tagSets", names.toString());
        for (TIFFField field : this.fields.values()) {
            if (field.hasDirectory()) {
                // A pointer is not written as a field: it is written as the directory it points to,
                // nested, with the number of the tag that brought it. That way the tree has the
                // same shape as the file.
                ifd.appendChild(field.getDirectory().getAsIFDNode());
            } else {
                ifd.appendChild(field.getAsNativeNode());
            }
        }
        return ifd;
    }

    /** Rebuilds a directory from a {@code TIFFIFD} node. */
    static TIFFDirectory fromIFDNode(Node ifd, TIFFTag parentTag) throws IIOInvalidTreeException {
        List<TIFFTagSet> sets = new ArrayList<TIFFTagSet>();
        String names = attribute(ifd, "tagSets");
        if (names != null && names.length() > 0) {
            String[] pieces = names.split(",");
            int i = 0;
            while (i < pieces.length) {
                TIFFTagSet set = tagSetForClassName(pieces[i].trim());
                if (set != null) {
                    sets.add(set);
                }
                i = i + 1;
            }
        }
        TIFFDirectory dir =
            new TIFFDirectory(sets.toArray(new TIFFTagSet[sets.size()]), parentTag);
        NodeList children = ifd.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String name = child.getNodeName();
                if ("TIFFField".equals(name)) {
                    try {
                        dir.addTIFFField(TIFFField.createFromMetadataNode(null, child));
                    } catch (RuntimeException e) {
                        throw new IIOInvalidTreeException(e.getMessage(), e, child);
                    }
                } else if ("TIFFIFD".equals(name)) {
                    dir.addTIFFField(subdirectoryField(dir, child));
                } else {
                    throw new IIOInvalidTreeException("Unexpected node " + name, child);
                }
            }
            i = i + 1;
        }
        // The fields were read without a set so as not to resolve against the wrong one; now that
        // the directory has its own, they are resolved again and take their name.
        dir.resolveNames();
        return dir;
    }

    /** Resolves the tag numbers again against this directory's sets. */
    private void resolveNames() {
        TreeMap<Integer, TIFFField> resolved = new TreeMap<Integer, TIFFField>();
        for (java.util.Map.Entry<Integer, TIFFField> entry : this.fields.entrySet()) {
            TIFFField field = entry.getValue();
            TIFFTag tag = getTag(field.getTagNumber());
            if (tag != null && tag != field.getTag() && !field.hasDirectory()
                && tag.isDataTypeOK(field.getType())) {
                field = new TIFFField(tag, field.getType(), field.getCount(), field.getData());
            }
            resolved.put(entry.getKey(), field);
        }
        this.fields = resolved;
    }

    /** The pointer field that stands for a nested directory of the tree. */
    private static TIFFField subdirectoryField(TIFFDirectory parent, Node ifd)
            throws IIOInvalidTreeException {
        String numberText = attribute(ifd, "parentTagNumber");
        if (numberText == null) {
            throw new IIOInvalidTreeException("Nested TIFFIFD without parentTagNumber", ifd);
        }
        int number;
        try {
            number = Integer.parseInt(numberText);
        } catch (NumberFormatException e) {
            throw new IIOInvalidTreeException("Bad parentTagNumber " + numberText, ifd);
        }
        TIFFTag tag = parent.getTag(number);
        if (tag == null) {
            String name = attribute(ifd, "parentTagName");
            if (name == null) {
                name = TIFFTag.UNKNOWN_TAG_NAME;
            }
            tag = new TIFFTag(name, number, 1 << TIFFTag.TIFF_LONG);
        }
        TIFFDirectory sub = fromIFDNode(ifd, tag);
        // The tree does not say at which position of the file the directory was --it is not a piece
        // of data of the metadata format-- so 1 is used, which is what the JDK does.
        return new TIFFField(tag, TIFFTag.TIFF_LONG, 1L, sub);
    }

    /** The built-in set with that name, or null if it is none of the seven. */
    private static TIFFTagSet tagSetForClassName(String className) {
        if ("javax.imageio.plugins.tiff.BaselineTIFFTagSet".equals(className)) {
            return BaselineTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.FaxTIFFTagSet".equals(className)) {
            return FaxTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifParentTIFFTagSet".equals(className)) {
            return ExifParentTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifTIFFTagSet".equals(className)) {
            return ExifTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifGPSTagSet".equals(className)) {
            return ExifGPSTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifInteroperabilityTagSet".equals(className)) {
            return ExifInteroperabilityTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.GeoTIFFTagSet".equals(className)) {
            return GeoTIFFTagSet.getInstance();
        }
        return byReflection(className);
    }

    /**
     * A plug-in's set, through its static {@code getInstance}.
     *
     * <p>It is the convention the tree takes for granted when it stores only the class name. If
     * anything fails --the class is not there, it lacks the method, it does not return a set-- the
     * set is skipped: a TIFF missing a profile is still read, with that profile's tags anonymous.
     */
    private static TIFFTagSet byReflection(String className) {
        try {
            Class<?> cls = Class.forName(className);
            Object instance = cls.getMethod("getInstance").invoke(null);
            if (instance instanceof TIFFTagSet) {
                return (TIFFTagSet) instance;
            }
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

    /** Whether that metadata declares the TIFF native format. */
    private static boolean supportsNativeFormat(IIOMetadata metadata) {
        String[] names = metadata.getMetadataFormatNames();
        if (names == null) {
            return false;
        }
        int i = 0;
        while (i < names.length) {
            if (NATIVE_FORMAT.equals(names[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** The value of that attribute, or null if it is not there. */
    private static String attribute(Node node, String name) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) {
            return null;
        }
        Node attr = attrs.getNamedItem(name);
        if (attr == null) {
            return null;
        }
        return attr.getNodeValue();
    }

    /** The first child that is an element, or null. */
    private static Node firstElement(Node node) {
        NodeList children = node.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return children.item(i);
            }
            i = i + 1;
        }
        return null;
    }
}
