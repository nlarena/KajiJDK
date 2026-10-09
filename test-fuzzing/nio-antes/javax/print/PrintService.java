package javax.print;

import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;

/**
 * KajiLibrary's javax.print.PrintService -- a printer.
 *
 * <p>It serves two things: creating jobs, and above all <b>asking it what it can do</b> before
 * sending it anything. Half of the interface is that.
 *
 * <h2>The four levels of question</h2>
 *
 * <p>They look alike and are not, and choosing wrongly is the typical error:
 *
 * <ul>
 *   <li>{@link #isAttributeCategorySupported} asks about the <b>category</b>: whether the printer
 *       understands the concept of duplex;
 *   <li>{@link #getSupportedAttributeValues} returns <b>which values</b> that category can take,
 *       for a given format and context;
 *   <li>{@link #isAttributeValueSupported} asks about a concrete value;
 *   <li>{@link #getDefaultAttributeValue} returns the one used if nothing is asked for.
 * </ul>
 *
 * <p>That the last two take the {@link DocFlavor} and an {@link AttributeSet} is what makes them
 * useful: a printer may do duplex in PostScript and not in plain text, or be unable to combine
 * duplex with a certain paper size. The set passed is the rest of what one intends to ask for.
 *
 * <h2>{@code getSupportedAttributeValues} returns {@code Object}</h2>
 *
 * <p>It is awkward and there is no alternative: depending on the category it returns an array of
 * values, a single value representing a range, or null. Each standard attribute's documentation
 * says which.
 *
 * <h2>{@link #equals} and {@link #hashCode} are declared</h2>
 *
 * <p>Redeclaring {@code Object}'s in an interface changes nothing technically. It is there to
 * document the contract: two objects that represent <b>the same printer</b> have to be equal, even
 * if they are different instances obtained in different lookups.
 */
public interface PrintService {

    /** The name, for display. */
    String getName();

    /** A new job. See {@link DocPrintJob}: it serves only once. */
    DocPrintJob createPrintJob();

    /** Registers a listener for the printer's changes. */
    void addPrintServiceAttributeListener(PrintServiceAttributeListener listener);

    /** Unregisters it. */
    void removePrintServiceAttributeListener(PrintServiceAttributeListener listener);

    /** The printer's current attributes. */
    PrintServiceAttributeSet getAttributes();

    /**
     * A single one, by category.
     *
     * @throws NullPointerException if the category is null
     * @throws IllegalArgumentException if it is not a {@link PrintServiceAttribute}
     */
    <T extends PrintServiceAttribute> T getAttribute(Class<T> category);

    /** The formats it accepts. */
    DocFlavor[] getSupportedDocFlavors();

    /** Whether it accepts that format. */
    boolean isDocFlavorSupported(DocFlavor flavor);

    /** The attribute categories it understands. */
    Class<?>[] getSupportedAttributeCategories();

    /** Whether it understands that category. See the class note. */
    boolean isAttributeCategorySupported(Class<? extends Attribute> category);

    /** The value it uses if nothing is asked for, or null. */
    Object getDefaultAttributeValue(Class<? extends Attribute> category);

    /**
     * Which values that category can take in that context.
     *
     * <p>See the class note on why it returns {@code Object}.
     *
     * @param flavor the format, or null to ask in general
     * @param attributes the rest of what one intends to ask for, or null
     */
    Object getSupportedAttributeValues(Class<? extends Attribute> category, DocFlavor flavor,
                                       AttributeSet attributes);

    /** Whether it can give that value in that context. */
    boolean isAttributeValueSupported(Attribute attrval, DocFlavor flavor, AttributeSet attributes);

    /**
     * Which of those attributes it cannot meet, or null if it can with all.
     *
     * <p>It is the way of asking about the whole request at once instead of attribute by attribute.
     */
    AttributeSet getUnsupportedAttributes(DocFlavor flavor, AttributeSet attributes);

    /** The factory of this printer's own screens, or null. */
    ServiceUIFactory getServiceUIFactory();

    /** Equal if it is the same printer. See the class note. */
    boolean equals(Object obj);

    /** Consistent with {@link #equals}. */
    int hashCode();
}
