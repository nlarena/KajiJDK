package java.lang;

// KajiLibrary's java.lang.Appendable — something you can append characters to: the common
// contract behind StringBuilder and the character Writers. Each append returns the same
// Appendable so calls can be chained. (The JDK's methods are declared to throw IOException;
// we don't model IOException yet.)
public interface Appendable {

    Appendable append(CharSequence csq) throws java.io.IOException;

    Appendable append(CharSequence csq, int start, int end) throws java.io.IOException;

    Appendable append(char c) throws java.io.IOException;
}
