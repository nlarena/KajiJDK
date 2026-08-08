package java.lang;

// KajiLibrary's java.lang.CharSequence — a readable sequence of char values (the common
// abstraction over String, StringBuilder, etc.): a length, indexed access, and slicing.
public interface CharSequence {

    int length();

    char charAt(int index);

    CharSequence subSequence(int start, int end);

    String toString();
}
