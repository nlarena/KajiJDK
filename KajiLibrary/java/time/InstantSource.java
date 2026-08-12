package java.time;

// KajiLibrary's java.time.InstantSource — a source of the current instant, the time-zone-free
// supertype of Clock (a Clock is an InstantSource that also knows a zone). A KajiLibrary subset: the
// functional contract instant() plus the millis() convenience; the static factories
// (system/tick/fixed/offset) are omitted.
@FunctionalInterface
public interface InstantSource {

    Instant instant();

    default long millis() {
        return instant().toEpochMilli();
    }
}
