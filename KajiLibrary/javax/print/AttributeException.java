package javax.print;

import javax.print.attribute.Attribute;

/**
 * KajiLibrary's javax.print.AttributeException -- the failure was because of attributes.
 *
 * <p>An interface, not an exception; see the note of {@link PrintException} on why.
 *
 * <p>It tells apart two things that get confused:
 *
 * <ul>
 *   <li>{@link #getUnsupportedAttributes} are whole <b>categories</b> the printer does not
 *       understand --it does not know what duplex is--;
 *   <li>{@link #getUnsupportedValues} are attributes it does understand with a value it cannot give
 *       --it understands duplex, it does not have it--.
 * </ul>
 *
 * <p>Both may return null if there is nothing of that kind.
 */
public interface AttributeException {

    /** The categories it does not understand, or null. */
    Class<?>[] getUnsupportedAttributes();

    /** The values it cannot give, or null. */
    Attribute[] getUnsupportedValues();
}
