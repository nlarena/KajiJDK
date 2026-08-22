package java.lang.constant;

// The root of the nominal-descriptor hierarchy: a value that can live in the constant pool and
// describes something WITHOUT resolving it. Every descriptor type here implements it, so it is
// what lets a bootstrap argument list be typed at all.
//
// The JDK declares exactly one method on it:
//
//     Object resolveConstantDesc(MethodHandles.Lookup) throws ReflectiveOperationException
//
// which is OMITTED here, and with it the interface's only member. Resolution is the step that
// turns a *description* into the live thing, and it needs `java.lang.invoke` — a package
// KajiLibrary does not have, because its content is the VM's method-handle machinery rather
// than library code. The gate accepts the omission (our surface must be a SUBSET of the JDK's).
//
// What survives is still the useful half: the whole package can be built, compared and printed
// without ever resolving anything, which is precisely the property that makes these descriptors
// usable at compile time — see `ClassDesc` for why that matters.
public interface ConstantDesc {
}
