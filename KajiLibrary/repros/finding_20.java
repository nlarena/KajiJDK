// Finding #20 — a `new` with a FULLY-QUALIFIED class name is miscompiled.
// `new java.lang.Object()` (qualified) is mishandled; `new Object()` (simple, via import or
// java.lang) works. The qualified type name in the instance-creation expression is not
// resolved for codegen.
//
// Two symptoms, same root:
//   A. Self-contained (no -cp), as below: a COMPILE ERROR
//      "el generador de bytecode todavía no soporta un `new` de un tipo que no se pudo resolver".
//   B. With -cp and a cross-package type (e.g. `new java.util.Formatter()` inside
//      java.lang.String): NO error — the whole method body is silently dropped to a bare
//      `0: areturn`. It passes javap and the API-shape gate but is broken bytecode. This is
//      the dangerous one; surfaced writing `String.format` in H6-T1.
//
// Workaround (KajiLibrary): import the class and use the simple name —
//   `import java.util.Formatter; ... new Formatter()` (done in String.format).
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_20.java
//        (errors on `qualified()`; comment it out to see `simple()` compile fine.)
public class finding_20 {

    // BUG: qualified name in `new` — errors "new de un tipo que no se pudo resolver".
    public static Object qualified() {
        return new java.lang.Object();
    }

    // OK: simple name resolves and codegens correctly.
    public static Object simple() {
        return new Object();
    }
}
