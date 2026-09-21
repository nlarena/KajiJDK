package jdk.internal.apt;

import javax.lang.model.element.Name;

// The reification of a **name** of the `javax.lang.model` model
// (`javax.lang.model.element.Name`): a minimal wrapper over the `String` the VM has already
// interned. The contract of `Element` returns `Name` (a `CharSequence` with `contentEquals`), not
// `String`, so `SymElement.getSimpleName` / `getQualifiedName` build one of these: the native
// allocates the object and writes the field `value` (the interned `String`) into it, and the
// accessors of `CharSequence` delegate to that `String`.
public final class SymName implements Name {
    // The text of the name (an interned `String`). The VM writes it when building the object.
    String value;

    public int length() {
        return value.length();
    }

    public char charAt(int index) {
        return value.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    public String toString() {
        return value;
    }

    // Equality of contents with any `CharSequence` (the contract of `Name`): it compares the text.
    // There is no `String.contentEquals` in KajiLibrary, so the other sequence is materialised with
    // `toString()` (part of the contract of `CharSequence`) and compared with `String.equals`.
    public boolean contentEquals(CharSequence cs) {
        return value.equals(cs.toString());
    }
}
