package java.lang.invoke;

import java.util.List;

// The bridge between the two ways this project already describes a type. A `ClassDesc` in
// `java.lang.constant` NAMES a type without loading it; a `Class` IS the loaded type. Both can
// answer "what is your class-file descriptor", and this interface is that single question — which
// is why `ClassDesc`, `MethodTypeDesc`, `Class` and `MethodType` all implement it.
//
// MODELLED RAW ON PURPOSE. The JDK declares these as self-bounded generics:
//
//     interface OfField<F extends TypeDescriptor.OfField<F>>
//     interface OfMethod<F extends TypeDescriptor.OfField<F>, M extends TypeDescriptor.OfMethod<F, M>>
//
// so that an implementation gets its OWN type back instead of the interface. Written that way
// here, every method's descriptor comes out with `Object` where the JDK has
// `TypeDescriptor$OfField` — our compiler erases a BOUNDED type variable to `Object` instead of
// to its leftmost bound (finding #100, JLS 4.6). Nine methods, nine mismatches.
//
// Declaring them raw with the methods returning the BOUND produces exactly the descriptors the
// JDK emits, because that is what the JDK's own erasure amounts to. Same trick as the
// self-bounded `Configuration<T extends Configuration<T>>` in jakarta.validation: the source
// loses the compile-time precision, the BINARY is faithful, and no allowlist entry is needed.
public interface TypeDescriptor {

    // The class-file spelling: `I`, `[[J`, `Ljava/lang/String;`, `(II)V`.
    String descriptorString();

    // A descriptor for a FIELD type — one type, possibly an array of one.
    public interface OfField extends TypeDescriptor {

        boolean isArray();

        boolean isPrimitive();

        OfField componentType();

        OfField arrayType();
    }

    // A descriptor for a METHOD type — a return type plus parameters, each of them a field
    // descriptor. That pairing is what ties the two interfaces together.
    public interface OfMethod extends TypeDescriptor {

        int parameterCount();

        OfField parameterType(int i);

        OfField returnType();

        OfField[] parameterArray();

        // The same parameters as `parameterArray`, as a list. Both exist because an array can be
        // handed out only by copying — it is mutable — while a list can be handed out shared, and
        // callers that only read want the cheap one.
        //
        // RAW `List` on purpose. The JDK writes `List<F> parameterList()` where `F` is the
        // self-bound field-descriptor variable; an implementor (`MethodTypeDesc`) then returns
        // `List<ClassDesc>`. Modelled raw here, `F` becomes the concrete `OfField`, and
        // `List<ClassDesc>` is not a `List<OfField>` (generic invariance) — so the override would
        // be rejected. The raw type erases to the same `()Ljava/util/List;` the JDK emits.
        List parameterList();

        OfMethod changeReturnType(OfField newReturn);

        OfMethod changeParameterType(int index, OfField newParameter);

        OfMethod dropParameterTypes(int start, int end);

        OfMethod insertParameterTypes(int pos, OfField[] parameterTypes);
    }
}
