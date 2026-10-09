package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageReadParam;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFImageReadParam -- which tags to recognize when
 * reading a TIFF.
 *
 * <p>A TIFF may carry tags from any profile, and the reader only knows how to interpret the ones of
 * the sets declared to it. This class is that declaration.
 *
 * <p>It comes with four built-in ones --the baseline one, the fax one, the one that leads to the
 * Exif directories and the GeoTIFF one--, which covers what comes out of a camera and of almost any
 * program. Adding one of your own with {@link #addAllowedTagSet} is what makes a reader understand
 * a specialized TIFF.
 *
 * <h2>{@link #setReadUnknownTags}</h2>
 *
 * <p>Off by default: the tags no set declares are <b>discarded</b> when reading.
 *
 * <p>Turning it on keeps them as anonymous fields --named {@link TIFFTag#UNKNOWN_TAG_NAME}-- and is
 * what is needed to rewrite a TIFF without losing what is not understood. A read-and-write-back
 * flow with this off silently drops everything the reader does not recognize.
 */
public final class TIFFImageReadParam extends ImageReadParam {

    /** The sets the reader will recognize, in lookup order. */
    private final List<TIFFTagSet> allowedTagSets = new ArrayList<TIFFTagSet>();

    /** Whether to keep unknown tags. See the class note. */
    private boolean readUnknownTags = false;

    /** With the four built-in sets. */
    public TIFFImageReadParam() {
        this.allowedTagSets.add(BaselineTIFFTagSet.getInstance());
        this.allowedTagSets.add(FaxTIFFTagSet.getInstance());
        this.allowedTagSets.add(ExifParentTIFFTagSet.getInstance());
        this.allowedTagSets.add(GeoTIFFTagSet.getInstance());
    }

    /**
     * Adds a set at the end of the list.
     *
     * <p>Adding one that is already there does nothing.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void addAllowedTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new IllegalArgumentException("tagSet == null!");
        }
        if (!this.allowedTagSets.contains(tagSet)) {
            this.allowedTagSets.add(tagSet);
        }
    }

    /**
     * Removes it.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void removeAllowedTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new IllegalArgumentException("tagSet == null!");
        }
        this.allowedTagSets.remove(tagSet);
    }

    /**
     * The declared sets.
     *
     * <p>It is a copy: adding to the returned list declares nothing. The JDK returns the live list,
     * and whoever modifies it changes the parameter behind its back; going through
     * {@link #addAllowedTagSet} makes no difference for code that uses it as a read-only list.
     */
    public List<TIFFTagSet> getAllowedTagSets() {
        return new ArrayList<TIFFTagSet>(this.allowedTagSets);
    }

    /** Whether to keep the unknown ones. See the class note. */
    public void setReadUnknownTags(boolean readUnknownTags) {
        this.readUnknownTags = readUnknownTags;
    }

    /** Whether they are kept. */
    public boolean getReadUnknownTags() {
        return this.readUnknownTags;
    }
}
