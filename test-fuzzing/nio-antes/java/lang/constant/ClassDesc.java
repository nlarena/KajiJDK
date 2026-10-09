package java.lang.constant;

// A *nominal* descriptor for a class: it names the type without resolving it. That distinction
// is the whole point — a `ClassDesc` can be produced and compared at a stage where loading the
// class would be wrong or impossible, which is what makes it usable as a `condy` argument.
//
// In KajiLibrary this backs the lowering of a `switch` over an enum: each case label becomes an
// `Enum.EnumDesc` holding one of these plus the constant's name, and the VM matches by name on
// both sides instead of ever calling `getstatic` on the constant.
//
// Extends `TypeDescriptor.OfField` so that `arrayType()`/`componentType()` become the field-type
// descriptor operations the interface family expects — the covariant narrowing to `ClassDesc`
// makes the compiler synthesize the `OfField`-returning bridges. `resolveConstantDesc` is present
// but degrades honestly: KajiLibrary's `java.lang.invoke` cannot load a class from a descriptor.
public interface ClassDesc extends ConstantDesc, java.lang.invoke.TypeDescriptor.OfField {

    // A descriptor for the class with this binary name (dotted, e.g. `java.lang.Thread$State`).
    // `public` is spelled out although interface members are implicitly public: our compiler
    // applies that implicit modifier to abstract methods but not to static ones (finding #116).
    public static ClassDesc of(String name) {
        return new ConstantClassDesc(name, "L" + DescNames.swap(name, '.', '/') + ";");
    }

    // A descriptor built from a package and a simple name. An empty package means the default
    // one, where the simple name is already the binary name.
    public static ClassDesc of(String packageName, String className) {
        String binary = className;
        if (packageName.length() != 0) {
            binary = packageName + "." + className;
        }
        return of(binary);
    }

    // A descriptor for the class with this *internal* name (`java/lang/Thread$State`).
    public static ClassDesc ofInternalName(String name) {
        return of(DescNames.swap(name, '/', '.'));
    }

    // A descriptor parsed from its class-file spelling: `I`, `[[J`, `Ljava/lang/String;`.
    public static ClassDesc ofDescriptor(String descriptor) {
        ClassDesc result;
        if (DescNames.isPrimitiveDescriptor(descriptor)) {
            result = new ConstantClassDesc(DescNames.primitiveName(descriptor.charAt(0)), descriptor);
        } else if (descriptor.charAt(0) == '[') {
            // An array keeps the descriptor verbatim; its binary name is only ever derived.
            result = new ConstantClassDesc(descriptor, descriptor);
        } else {
            String internal = descriptor.substring(1, descriptor.length() - 1);
            result = of(DescNames.swap(internal, '/', '.'));
        }
        return result;
    }

    // The descriptor of an array of this type — one more dimension.
    ClassDesc arrayType();

    // The descriptor of an array of this type with `rank` extra dimensions.
    ClassDesc arrayType(int rank);

    // The descriptor of a type nested inside this one (`Outer` -> `Outer$Inner`).
    default ClassDesc nested(String nestedName) {
        return ClassDesc.of(DescNames.binaryNameOf(descriptorString()) + "$" + nestedName);
    }

    default ClassDesc nested(String firstNestedName, String... moreNestedNames) {
        ClassDesc current = nested(firstNestedName);
        int i = 0;
        while (i < moreNestedNames.length) {
            current = current.nested(moreNestedNames[i]);
            i = i + 1;
        }
        return current;
    }

    default boolean isArray() {
        return descriptorString().charAt(0) == '[';
    }

    default boolean isPrimitive() {
        return DescNames.isPrimitiveDescriptor(descriptorString());
    }

    default boolean isClassOrInterface() {
        return descriptorString().charAt(0) == 'L';
    }

    // The element type of an array, or null when this is not one — the JDK's own contract.
    default ClassDesc componentType() {
        ClassDesc component = null;
        if (isArray()) {
            component = ClassDesc.ofDescriptor(DescNames.substringFrom(descriptorString(), 1));
        }
        return component;
    }

    // The package of a class-or-interface descriptor; empty for the default package, and empty
    // for primitives and arrays, which belong to none.
    default String packageName() {
        String pkg = "";
        if (isClassOrInterface()) {
            String binary = DescNames.binaryNameOf(descriptorString());
            int dot = DescNames.lastIndexOf(binary, '.');
            if (dot >= 0) {
                pkg = binary.substring(0, dot);
            }
        }
        return pkg;
    }

    // The human-facing name: the simple name for a class, the keyword for a primitive, and the
    // component's display name plus `[]` for an array.
    String displayName();

    // The class-file spelling.
    String descriptorString();

    // Resolve this descriptor to a live `Class` using the given lookup. Covariantly narrows the
    // `Object`-returning method of `ConstantDesc`, which is what makes the compiler emit the
    // `Object` bridge the interface family expects.
    Class<?> resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup) throws java.lang.ReflectiveOperationException;

    boolean equals(Object o);
}

// The only implementation. It shares the file with the interface because the two reference each
// other and our compiler builds one file per invocation; same-file top-level classes are the
// project's idiom for such a cycle.
//
// CROSS-TRACK CONTRACT — do not rename the field `name`, and keep it holding the BINARY name
// with dots. The interpreter reads it back by name when it matches an enum-`switch` label
// against the selector's runtime class (`matches_enum_constant` in invokedynamic.rs, which does
// the dots-to-slashes conversion on its side). `descriptorString()` computes the class-file
// spelling on top of it instead of replacing it, precisely so that contract stays intact.
final class ConstantClassDesc implements ClassDesc {

    private final String name;
    private final String descriptor;

    ConstantClassDesc(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    public String descriptorString() {
        return descriptor;
    }

    public String displayName() {
        String display = name;
        if (isArray()) {
            ClassDesc component = componentType();
            display = component.displayName() + "[]";
        } else if (isClassOrInterface()) {
            int dot = DescNames.lastIndexOf(name, '.');
            if (dot >= 0) {
                display = DescNames.substringFrom(name, dot + 1);
            }
        }
        return display;
    }

    public ClassDesc arrayType() {
        return ClassDesc.ofDescriptor("[" + descriptor);
    }

    public ClassDesc arrayType(int rank) {
        ClassDesc current = this;
        int i = 0;
        while (i < rank) {
            current = current.arrayType();
            i = i + 1;
        }
        return current;
    }

    public boolean equals(Object o) {
        boolean same = false;
        if (o instanceof ConstantClassDesc) {
            ConstantClassDesc other = (ConstantClassDesc) o;
            same = descriptor.equals(other.descriptor);
        }
        return same;
    }

    public int hashCode() {
        return descriptor.hashCode();
    }

    public String toString() {
        return "ClassDesc[" + displayName() + "]";
    }

    /**
     * Unsupported: resolving a descriptor needs `java.lang.invoke`, which this library does not
     * have. Everything else about this type works without it.
     *
     * @param lookup the lookup that would perform the resolution
     * @throws UnsupportedOperationException always
     */
    public Class<?> resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup) {
        throw new UnsupportedOperationException("resolution needs java.lang.invoke");
    }
}
