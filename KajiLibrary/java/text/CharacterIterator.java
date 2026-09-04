package java.text;

// KajiLibrary's java.text.CharacterIterator — bidirectional iteration over text.
//
// Unlike java.util.Iterator, this one goes BOTH WAYS and exposes an index, because text scanning
// backtracks: a break iterator or a collator needs to look one character back as often as forward.
// That is why it is a separate abstraction rather than a use of the collections iterator.
public interface CharacterIterator extends Cloneable {

    // Lo que devuelven first/last/next/previous/current cuando el iterador se pasa de cualquiera
    // de los dos extremos. Vale U+FFFF, que Unicode reserva como no-carácter justamente para esto:
    // ningún texto legal lo contiene, así que el centinela no puede confundirse con un dato.
    //
    // Estuvo omitida por el finding #124 (un inicializador de campo en una interfaz hacía que el
    // compilador sintetizara un `<init>` sobre la interfaz, o sea un miembro público de más). #124
    // está cerrado y verificado: hoy el campo sale como `public static final char` y sin
    // constructor espurio.
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
