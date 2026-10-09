// Finding #21 — CRITICAL: the compiler drops ALL enum machinery.
//
// A plain `enum { A, B, C }` compiles to a `final class extends Enum` with NONE of the enum
// machinery: no `public static final <E> A/B/C` constant fields, no `values()`, no `valueOf()`,
// no synthetic `$VALUES`, no `<clinit>`. So `Repro21.A` does not resolve, and the enum is
// unusable at runtime. Affects EVERY enum (even this default-package, no-constructor one).
//
// This is a REGRESSION from finding #18's state — there the note explicitly said "the .class
// still has the constants / values() / valueOf()"; now they are all gone. BOTH
// bin/javac-frozen.exe and target/debug/javac.exe reproduce it.
//
// It silently broke H4 / java.time: ChronoField, ChronoUnit, Month and DayOfWeek are all
// degenerate (0 constants). The API-shape gate can't catch it because missing members are still
// a subset of the JDK's. It blocks H6-T5 %t (needs ChronoField.YEAR etc.).
//
// Verify: cargo run -- --emit KajiLibrary/repros/finding_21.java
//         javap -p finding_21/Repro21.class   -> no A/B/C fields, no values()/valueOf()/$VALUES
package finding_21;

public enum Repro21 {
    A, B, C
}
