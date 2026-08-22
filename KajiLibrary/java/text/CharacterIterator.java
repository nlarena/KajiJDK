package java.text;

// KajiLibrary's java.text.CharacterIterator — bidirectional iteration over text.
//
// Unlike java.util.Iterator, this one goes BOTH WAYS and exposes an index, because text scanning
// backtracks: a break iterator or a collator needs to look one character back as often as forward.
// That is why it is a separate abstraction rather than a use of the collections iterator.
public interface CharacterIterator extends Cloneable {

    // OMITTED: `char DONE = '￿'`, the value returned when the iterator moves past either end.
    //
    // Declaring it triggers finding #124: a field initializer in an interface is lowered as if it
    // belonged to a class, so the compiler synthesizes a `default` CONSTRUCTOR on the interface
    // (calling Object.<init> on a `this` that cannot exist) and puts the assignment there instead
    // of in `<clinit>`. That is an EXTRA `<init>()V` on the public surface, which the API-shape
    // gate rejects — correctly, since an interface has no constructor.
    //
    // A missing member is a legal subset; a spurious one is not. So the constant is left out and
    // KajiLibrary's implementations use the literal '￿' directly. It returns once #124 is fixed.

    char first();

    char last();

    char current();

    char next();

    char previous();

    char setIndex(int position);

    int getBeginIndex();

    int getEndIndex();

    int getIndex();

    Object clone();
}
