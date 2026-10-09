package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.Attribute -- what makes an object a print attribute.
 *
 * <h2>The package's central idea: category and value are different things</h2>
 *
 * <p>A print attribute is a **typed value** --"three copies", "two-sided", "A4 paper"-- and each
 * value belongs to a **category**, which is the question that value answers. The category is not
 * kept as a string or a number: it is a {@code Class}, the one {@link #getCategory()} returns.
 *
 * <p>That is what governs the whole package. In an {@link AttributeSet} the key is the category,
 * not the object's class nor the object itself: putting {@code new Copies(3)} where there was
 * {@code new Copies(2)} **replaces**, because both answer the same question. And that is why there
 * are two methods and not one --without the explicit category, a subclass of an attribute would be
 * filed under a key different from its parent's and the set would have two answers to the same
 * question.
 *
 * <p>Normally the category is the class itself ({@code Copies.getCategory()} is {@code
 * Copies.class}). The exceptions are exactly the ones that explain what the indirection is for:
 * {@code MediaSizeName}, {@code MediaTray} and {@code MediaName} are three different classes and
 * all three report {@code Media.class}, because "which paper" is **one** question that can be
 * answered by size, by tray or by name, and a job cannot have the three answers at once.
 *
 * <p>{@link #getName()} returns the IPP protocol name (RFC 2911) --{@code "copies"},
 * {@code "sides"}-- and does not change with the language: it is for the wire, not for the user.
 */
public interface Attribute extends Serializable {

    /**
     * The question this value answers.
     *
     * <p>It is the key under which an {@link AttributeSet} files it, and that is why it matters
     * that it is not always {@code getClass()}: see the note about {@code Media} in the header.
     */
    Class<? extends Attribute> getCategory();

    /** The IPP name of the category; it is not translated. */
    String getName();
}
