package javax.lang.model.element;

// KajiLibrary's javax.lang.model.element.Name — an immutable sequence of characters used as
// the name of a program element.
//
// The re-declared equals/hashCode are the point of the interface, not boilerplate: two Names
// compare equal only if they came from the same implementation *and* denote the same
// sequence, so a Name must never be compared with a String. contentEquals is the way to
// compare a Name against arbitrary character content.
public interface Name extends CharSequence {

    boolean equals(Object obj);

    int hashCode();

    boolean contentEquals(CharSequence cs);
}
