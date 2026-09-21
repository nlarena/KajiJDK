package javax.xml.validation;

import org.w3c.dom.TypeInfo;

/**
 * KajiLibrary's javax.xml.validation.TypeInfoProvider -- what type what just went by had.
 *
 * <p>It is what makes validating with {@link ValidatorHandler} good for more than saying whether
 * the document is fine: as validation advances, this says of which <b>schema type</b> each element
 * and each attribute is. With that, whoever listens can convert the text to the type that
 * corresponds instead of guessing by its shape.
 *
 * <h2>It is only valid inside the call</h2>
 *
 * <p>It is the rule to keep in mind and the one that breaks whoever uses it wrongly: the element
 * methods can only be called from {@code startElement} or {@code endElement}, and the attribute
 * ones only from {@code startElement}. Keeping the {@code TypeInfoProvider} and asking it later
 * does not give an old value: it gives an {@code IllegalStateException}.
 *
 * <p>It makes sense -- nothing is kept. The provider is a window onto the validator's state at that
 * instant, and that is why having it costs nothing: if it had to keep the types of the whole
 * document to be able to answer later, it would be precisely what the SAX route wants to avoid.
 *
 * <p>Attributes are asked for <b>by index</b>, the same as in the {@code Attributes} that arrived
 * at {@code startElement}.
 */
public abstract class TypeInfoProvider {

    /** For the subclasses. */
    protected TypeInfoProvider() {
    }

    /**
     * The type of the current element.
     *
     * @throws IllegalStateException outside {@code startElement} or {@code endElement}
     */
    public abstract TypeInfo getElementTypeInfo();

    /**
     * The type of that attribute of the current element.
     *
     * @param index the index in {@code startElement}'s {@code Attributes}
     * @throws IllegalStateException outside {@code startElement}
     * @throws IndexOutOfBoundsException if the index does not exist
     */
    public abstract TypeInfo getAttributeTypeInfo(int index);

    /**
     * Whether that attribute is of identifier type.
     *
     * <p>It is the question that decides whether the value serves for {@code getElementById}, and
     * it cannot be answered without the schema: in a document without DTD or schema, no attribute
     * is an identifier however much it is called {@code id}.
     */
    public abstract boolean isIdAttribute(int index);

    /**
     * Whether the attribute was <b>written</b> in the document.
     *
     * <p>False means the schema put it there as a default value. The difference matters when the
     * document has to be rewritten: emitting the default values changes it, and emitting them with
     * another schema can change its meaning.
     */
    public abstract boolean isSpecified(int index);
}
