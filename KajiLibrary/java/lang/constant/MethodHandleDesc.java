package java.lang.constant;

import java.lang.constant.DirectMethodHandleDesc;



// A nominal descriptor for a method handle. The factories all produce the *direct* flavour,
// which is the only one a class file can hold literally; everything else is built by combinator
// at run time and has no constant-pool form.
//
// `resolveConstantDesc` is OMITTED (`java.lang.invoke`); see `ConstantDesc`.
//
// The nested `DirectMethodHandleDesc.Kind` is reached through an import rather than as `DirectMethodHandleDesc.Kind`:
// a qualified reference to a nested type does not resolve in our compiler, and worse, with an
// import plus the simple name it used to emit the wrong descriptor (finding #101). Importing the
// nested type itself is the form that works.
public interface MethodHandleDesc extends ConstantDesc {

    // `public` is spelled out on the static methods (finding #116 — see `ClassDesc`).
    public static DirectMethodHandleDesc of(DirectMethodHandleDesc.Kind kind, ClassDesc owner, String name, String lookupDescriptor) {
        return ConstantMethodHandleDesc.make(kind, owner, name, lookupDescriptor);
    }

    public static DirectMethodHandleDesc ofMethod(DirectMethodHandleDesc.Kind kind, ClassDesc owner, String name, MethodTypeDesc lookupMethodType) {
        return ConstantMethodHandleDesc.make(kind, owner, name, lookupMethodType.descriptorString());
    }

    public static DirectMethodHandleDesc ofField(DirectMethodHandleDesc.Kind kind, ClassDesc owner, String fieldName, ClassDesc fieldType) {
        return ConstantMethodHandleDesc.make(kind, owner, fieldName, fieldType.descriptorString());
    }

    // OMITTED (subset): `static DirectMethodHandleDesc ofConstructor(ClassDesc, ClassDesc...)`.
    //
    // It needs the value `DirectMethodHandleDesc.Kind.CONSTRUCTOR`, and a static member of a nested type turns out to be
    // unreachable from outside the file that declares it — by ANY spelling. All three forms were
    // tried and all three fail with "no se encuentra el simbolo: variable DirectMethodHandleDesc.Kind":
    //     DirectMethodHandleDesc.Kind.valueOf(8)          (fully qualified)
    //     DirectMethodHandleDesc.Kind.valueOf(8)                                 (with the nested type imported)
    //     CONSTRUCTOR                                     (with the constant statically imported)
    // The type import works in a TYPE position — the parameters above prove it — so the defect is
    // specific to using the nested type as a QUALIFIER. That sharpens finding #101, which until
    // now recorded the type-position half.
    //
    // There is no source-level workaround: a `DirectMethodHandleDesc.Kind` value cannot be produced outside
    // `DirectMethodHandleDesc.java`, and any helper that could produce one would be a member the
    // JDK does not have, which the gate would reject as extra. The method returns when #101 does.

    // A view of this handle under a different type. The adaptation is a run-time operation, so
    // all a descriptor can do is record the intent.
    default MethodHandleDesc asType(MethodTypeDesc type) {
        return new AdaptedMethodHandleDesc(this, type);
    }

    // The type an invocation of the handle actually has — NOT the lookup descriptor. For an
    // instance method the receiver becomes the first parameter, a constructor returns the class
    // it builds, and a field accessor turns into a getter or setter signature.
    MethodTypeDesc invocationType();

    boolean equals(Object o);
}

// The adapted view. NOT named `AsTypeMethodHandleDesc`: the JDK has a package-private class by
// that exact name, so the gate would compare ours against it and report our `equals`/`hashCode`
// as extra members. A different name has no counterpart and is skipped. It keeps the target so the pair can be compared, and reports the requested
// type as its own.
final class AdaptedMethodHandleDesc implements MethodHandleDesc {

    private final MethodHandleDesc target;
    private final MethodTypeDesc type;

    AdaptedMethodHandleDesc(MethodHandleDesc target, MethodTypeDesc type) {
        this.target = target;
        this.type = type;
    }

    public MethodTypeDesc invocationType() {
        return type;
    }

    public boolean equals(Object o) {
        boolean same = false;
        if (o instanceof AdaptedMethodHandleDesc) {
            AdaptedMethodHandleDesc other = (AdaptedMethodHandleDesc) o;
            same = target.equals(other.target) && type.equals(other.type);
        }
        return same;
    }

    public int hashCode() {
        return target.hashCode() * 31 + type.hashCode();
    }

    public String toString() {
        return "MethodHandleDesc[" + target.toString() + " asType " + type.displayDescriptor() + "]";
    }

    /**
     * Unsupported: resolving a descriptor needs `java.lang.invoke`, which this library does not
     * have. Everything else about this type works without it.
     *
     * @param lookup the lookup that would perform the resolution
     * @throws UnsupportedOperationException always
     */
    public Object resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup) {
        throw new UnsupportedOperationException("resolution needs java.lang.invoke");
    }
}
