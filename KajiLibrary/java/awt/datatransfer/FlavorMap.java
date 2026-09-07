package java.awt.datatransfer;

import java.util.Map;

/**
 * The translation between Java's formats and the names the operating system uses.
 *
 * <p>Each platform names the clipboard's formats its own way: Windows says `CF_TEXT`, X11 says
 * `STRING`, macOS says something else. A {@link DataFlavor} is Java's name. This interface is the
 * dictionary between the two.
 *
 * <p>Without it, every Java program wanting to exchange data with a native program would have to
 * know each system's names.
 */
public interface FlavorMap {

    /** Which native name corresponds to each of those formats. */
    Map<DataFlavor, String> getNativesForFlavors(DataFlavor[] flavors);

    /** Which format corresponds to each of those native names. */
    Map<String, DataFlavor> getFlavorsForNatives(String[] natives);
}
