package javax.imageio.plugins.tiff;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFTagSet -- a group of related TIFF tags.
 *
 * <p>TIFF tag numbers are not global: tag 1 is {@code GPSLatitudeRef} in a GPS directory and
 * {@code InteroperabilityIndex} in an Exif interoperability one. A set is what gives the context.
 * (An earlier note used 0x8769 as the example; that is the Exif directory pointer, and it has no
 * other meaning inside an Exif directory.)
 *
 * <p>That is why sets come in families --the TIFF baseline, Exif, GPS, fax, GeoTIFF-- and why a
 * reader has to know which one it is standing in.
 *
 * <p>Lookup is by number, which is what the file carries, or by name, which is what a person
 * writes. Both return null if it is not there.
 *
 * <p>It is immutable: the list is copied on construction and the two sets it returns are
 * read-only. A concrete subclass builds its list in a {@code static} and it is not touched again.
 *
 * <p>The seven concrete sets --{@code BaselineTIFFTagSet} and friends-- and the classes that handle
 * directories and fields are all in this package. (An earlier note said they were missing, waiting
 * for {@code javax.imageio} and {@code javax.imageio.metadata}; both packages are here now.)
 */
public class TIFFTagSet {

    /** By number. */
    private final Map<Integer, TIFFTag> byNumber = new HashMap<Integer, TIFFTag>();

    /** By name. */
    private final Map<String, TIFFTag> byName = new HashMap<String, TIFFTag>();

    /** The numbers, sorted and read-only. */
    private final SortedSet<Integer> numbers;

    /** The names, sorted and read-only. */
    private final SortedSet<String> names;

    /**
     * @param tags the tags of the group; they are copied
     * @throws IllegalArgumentException if the list is null or has something that is not a
     *     {@link TIFFTag}
     */
    public TIFFTagSet(List<TIFFTag> tags) {
        if (tags == null) {
            throw new IllegalArgumentException("tags == null!");
        }
        TreeSet<Integer> allNumbers = new TreeSet<Integer>();
        TreeSet<String> allNames = new TreeSet<String>();
        Iterator<TIFFTag> it = tags.iterator();
        while (it.hasNext()) {
            TIFFTag tag = it.next();
            if (tag == null) {
                throw new IllegalArgumentException("tags contains a null!");
            }
            Integer number = Integer.valueOf(tag.getNumber());
            String name = tag.getName();
            this.byNumber.put(number, tag);
            this.byName.put(name, tag);
            allNumbers.add(number);
            allNames.add(name);
        }
        this.numbers = Collections.unmodifiableSortedSet(allNumbers);
        this.names = Collections.unmodifiableSortedSet(allNames);
    }

    /** The tag with that number, or null. */
    public TIFFTag getTag(int tagNumber) {
        return this.byNumber.get(Integer.valueOf(tagNumber));
    }

    /** The tag with that name, or null. */
    public TIFFTag getTag(String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException("tagName == null!");
        }
        return this.byName.get(tagName);
    }

    /** The numbers, sorted; read-only. */
    public SortedSet<Integer> getTagNumbers() {
        return this.numbers;
    }

    /** The names, sorted; read-only. */
    public SortedSet<String> getTagNames() {
        return this.names;
    }
}
