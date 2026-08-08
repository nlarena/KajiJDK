package java.time.chrono;

// KajiLibrary's java.time.chrono.Era — an era of a calendar system (e.g. the ISO BCE/CE eras).
// A KajiLibrary subset: only the numeric getValue() is modelled (the JDK also makes Era a
// TemporalAccessor/TemporalAdjuster).
public interface Era {

    int getValue();
}
