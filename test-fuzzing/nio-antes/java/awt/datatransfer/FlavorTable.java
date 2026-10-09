package java.awt.datatransfer;

import java.util.List;

/**
 * A {@link FlavorMap} that admits **one-to-many** translations.
 *
 * <p>The correspondence between formats and native names is not a bijection: a Java text may be
 * handed over as several different native formats, and a native format may correspond to several
 * Java classes. The base map returns a single one, the best; this interface returns the whole list,
 * ordered from best to worst.
 */
public interface FlavorTable extends FlavorMap {

    /** Every native name that serves that format, from best to worst. */
    List<String> getNativesForFlavor(DataFlavor flav);

    /** Every format that serves that native name, from best to worst. */
    List<DataFlavor> getFlavorsForNative(String nat);
}
