package javax.swing.text.html.parser;

/**
 * The numbers the DTD names types and modifiers with.
 *
 * <h2>Three families of numbers in a single list</h2>
 *
 * <p>The values repeat on purpose: {@code CDATA} and {@code FIXED} are both worth 1, and
 * {@code ENTITY} and {@code REQUIRED} are both worth 2. It is not an oversight: they are
 * different families that are never compared with each other. The first nineteen are attribute or
 * content <em>types</em>; {@code FIXED} to {@code IMPLIED} are an attribute's <em>modifiers</em>;
 * and from {@code PUBLIC} to {@code SYSTEM} they are <em>entity</em> types.
 *
 * <p>The last three are bit flags, not numbers from a list: they are combined with a {@code |}
 * with the entity type to say whether it is general or a parameter one.
 *
 * <p>It is an interface with no methods and the package's classes implement it so as to name the
 * constants without qualifying them. It is a form that is not used today, but changing it would
 * change the public signature of {@link Element}, {@link Entity} and {@link AttributeList}.
 */
public interface DTDConstants {

    int CDATA = 1;
    int ENTITY = 2;
    int ENTITIES = 3;
    int ID = 4;
    int IDREF = 5;
    int IDREFS = 6;
    int NAME = 7;
    int NAMES = 8;
    int NMTOKEN = 9;
    int NMTOKENS = 10;
    int NOTATION = 11;
    int NUMBER = 12;
    int NUMBERS = 13;
    int NUTOKEN = 14;
    int NUTOKENS = 15;

    int RCDATA = 16;
    int EMPTY = 17;
    int MODEL = 18;
    int ANY = 19;

    int FIXED = 1;
    int REQUIRED = 2;
    int CURRENT = 3;
    int CONREF = 4;
    int IMPLIED = 5;

    int PUBLIC = 10;
    int SDATA = 11;
    int PI = 12;
    int STARTTAG = 13;
    int ENDTAG = 14;
    int MS = 15;
    int MD = 16;
    int SYSTEM = 17;

    int GENERAL = 1 << 16;
    int DEFAULT = 1 << 17;
    int PARAMETER = 1 << 18;
}
