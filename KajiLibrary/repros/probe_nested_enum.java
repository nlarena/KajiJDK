// Can a class declare a NESTED enum and take it as a parameter, referring to it by simple name in
// the same file? Finding #101 says a qualified `Outer.Nested` does not resolve and a cross-file
// import emits the wrong descriptor, but records that the same-file simple name is fine.
//
// This is the deciding experiment for java.text.Normalizer, whose whole public API is
// `normalize(CharSequence, Normalizer.Form)` with Form nested. If the emitted descriptor is
// `(Ljava/lang/CharSequence;Lprobe_nested_enum$Form;)Ljava/lang/String;` the class is buildable;
// if Form erases to Object, it is not.
public final class probe_nested_enum {

    public enum Form {
        NFD,
        NFC
    }

    public static String take(CharSequence src, Form form) {
        if (form == Form.NFD) {
            return "d";
        }
        return "c";
    }
}
