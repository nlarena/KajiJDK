package java.text;

// KajiLibrary's java.text.CharacterIterator — bidirectional iteration over text.
//
// Unlike java.util.Iterator, this one goes BOTH WAYS and exposes an index, because text scanning
// backtracks: a break iterator or a collator needs to look one character back as often as forward.
// That is why it is a separate abstraction rather than a use of the collections iterator.
public interface CharacterIterator extends Cloneable {

    // What first/last/next/previous/current return when the iterator runs past either end. It is
    // U+FFFF, which Unicode reserves as a non-character for exactly this: no legal text contains it,
    // so the sentinel cannot be mistaken for a datum.
    //
    // It was omitted because of finding #124 (a field initialiser in an interface made the compiler
    // synthesise an `<init>` on the interface, that is, one public member too many). #124 is closed
    // and verified: today the field comes out as `public static final char` and with no spurious
    // constructor.
    char DONE = '\uffff';

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
