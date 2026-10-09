package java.util;

// KajiLibrary's java.util.Formattable — a type that renders itself for a Formatter's `%s`
// conversion. When `%s` receives a Formattable, the Formatter calls formatTo (passing the
// flags, width and precision) instead of toString(), so the object can honour them itself.
public interface Formattable {

    void formatTo(Formatter formatter, int flags, int width, int precision);
}
