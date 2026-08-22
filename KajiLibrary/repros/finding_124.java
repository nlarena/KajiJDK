// Finding #124 — a field initializer in an INTERFACE is lowered as if it belonged to a class: the
// compiler synthesizes a constructor for the interface and puts the initialization inside it.
//
// `javap` of the emitted interface shows:
//
//     public default finding_124_Iface();
//       Code:
//          0: aload_0
//          1: invokespecial #x   // Method java/lang/Object."<init>":()V
//          4: ldc           #y
//          6: putstatic     #z   // Field VALUE:I
//          9: return
//
// Two distinct defects in one method:
//
//   (1) An interface must never declare `<init>` at all. Its fields are implicitly
//       `public static final` (JLS §9.3) and are initialized in `<clinit>` (JVMS §2.9.2 —
//       "a class or interface initialization method"). The emitted method is also marked
//       `default`, i.e. an interface method WITH A BODY, and its first instruction is
//       `aload_0; invokespecial Object.<init>` against a `this` that cannot exist.
//
//   (2) Because the initialization landed there instead of in `<clinit>`, and nothing ever calls
//       that constructor, the field is NEVER ASSIGNED. This compounds #112: the value survives
//       only in the field's ConstantValue attribute, which our VM does not apply.
//
// The API-shape gate DOES catch this one, as an EXTRA `<init>()V` — which is how it was found,
// while writing `java.text.CharacterIterator` (whose `char DONE` is part of the JDK contract).
//
// Library handling: `CharacterIterator.DONE` is OMITTED (a subset is legal, an extra is not) and
// KajiLibrary's implementations use the literal directly. It returns once this is fixed.
//
// Fix direction: the lowering that moves field initializers into constructors has to check the
// enclosing type's kind — for an interface (and for `static` fields of a class) the initializer
// belongs in `<clinit>`, and no `<init>` may be synthesized.
public class finding_124 {
}

interface finding_124_Iface {
    // Any initialized field triggers it — the type does not matter.
    int VALUE = 7;

    int get();
}

// A second shape: an interface whose field is initialized with a non-constant expression. Here the
// value cannot even live in ConstantValue, so the field is unconditionally zero at runtime.
interface finding_124_Computed {
    int COMPUTED = "abc".length();

    int get();
}
