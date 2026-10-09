package javax.annotation.processing;

import javax.lang.model.SourceVersion;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

// Support for APT's round loop (JSR 269): a concrete implementation of ProcessingEnvironment that the
// VM's driver reifies to hand to the processor's `init(env)`. `getFiler()` hands over a KajiFiler
// (phase 4), the piece a processor uses to manufacture the sources it generates: each
// `createSourceFile` pushes the (name, StringWriter) through the native bridge and the round loop
// drains it to feed back what was generated. `getMessager()` hands over an AptMessager, which writes
// through the same native bridge as AptTrace.
//
// The three remaining accessors answer what this compiler really knows, not a placeholder:
//
//   - `getOptions()` — the empty, immutable map. The round loop receives no `-A` options (`apt.rs`'s
//     driver does not parse them), so "there are none" is the truth. Immutable because the contract
//     does not allow a processor to add options.
//   - `getSourceVersion()` — `latest()`, that is RELEASE_25: the version this javac compiles.
//   - `getLocale()` — `null`, which the contract defines as "there is no locale", and that is exactly
//     the case (there is no `-locale` option to set it). Returning `Locale.getDefault()` would invent
//     a preference nobody expressed.
public class ProcessingEnvironmentImpl implements ProcessingEnvironment {

    public Messager getMessager() { return new AptMessager(); }

    public Filer getFiler() { return new KajiFiler(); }

    public Map<String, String> getOptions() { return Collections.emptyMap(); }

    public SourceVersion getSourceVersion() { return SourceVersion.latest(); }

    public Locale getLocale() { return null; }

    // The two that query the compiler's model return `null`, and that is the honest answer: there is
    // no implementation of `Elements`/`Types` in this library, because what is needed is `javac`'s
    // symbol table and type table, which live in `src/javac/` and not in Java.
    //
    // `null` and not an empty implementation: an `Elements` whose `getTypeElement` always returned
    // `null` would say "that type does not exist" of any type it was asked about, and a processor
    // would read that as an answer. `null` here says "there is no model", which is what is happening,
    // and the processor runs into the problem where the problem is.
    //
    // The day `apt.rs` exposes the model, these two are the points where it hooks in.

    public javax.lang.model.util.Elements getElementUtils() {
        return null;
    }

    public javax.lang.model.util.Types getTypeUtils() {
        return null;
    }
}
