package java.lang.constant;

import java.lang.constant.DirectMethodHandleDesc;


// The implementation of `DirectMethodHandleDesc`, in its own file rather than beside the
// interface: a top-level class cannot see a nested type declared by a sibling in the SAME file
// (finding #101), so `DirectMethodHandleDesc.Kind` has to arrive through an import — and an import needs its own
// compilation unit to be worth anything here. Importing the nested type directly is the form
// that resolves; the qualified `DirectMethodHandleDesc.Kind` does not.
final class ConstantMethodHandleDesc implements DirectMethodHandleDesc {

    private final DirectMethodHandleDesc.Kind kind;
    private final ClassDesc owner;
    private final String name;
    private final String lookupDescriptor;

    ConstantMethodHandleDesc(DirectMethodHandleDesc.Kind kind, ClassDesc owner, String name, String lookupDescriptor) {
        this.kind = kind;
        this.owner = owner;
        this.name = name;
        this.lookupDescriptor = lookupDescriptor;
    }

    // The factories of `MethodHandleDesc` delegate here instead of calling `new` themselves.
    // From that file the check "does ConstantMethodHandleDesc implement DirectMethodHandleDesc?"
    // compares a CLASSPATH class against a CLASSPATH interface and fails ("tipo de retorno
    // incompatible"), even though the emitted `interfaces[]` table is correct. Declaring the
    // factory here makes the same relation a source-to-classpath one, which the compiler does
    // accept, and the caller then only reads the return type off this method's descriptor.
    static DirectMethodHandleDesc make(DirectMethodHandleDesc.Kind kind, ClassDesc owner, String name, String lookupDescriptor) {
        return new ConstantMethodHandleDesc(kind, owner, name, lookupDescriptor);
    }

    public DirectMethodHandleDesc.Kind kind() {
        return kind;
    }

    public int refKind() {
        return kind.refKind;
    }

    public boolean isOwnerInterface() {
        return kind.isInterface;
    }

    public ClassDesc owner() {
        return owner;
    }

    public String methodName() {
        return name;
    }

    public String lookupDescriptor() {
        return lookupDescriptor;
    }

    // Where the lookup descriptor and the invocation type part ways. An instance method is
    // invoked with the receiver as an extra leading argument; a constructor is invoked to
    // *produce* its class, so `V` becomes the owner; and a field accessor has no descriptor of
    // its own at all — its signature is synthesised from the field's type and whether it is
    // static.
    public MethodTypeDesc invocationType() {
        ClassDesc[] justOwner = new ClassDesc[1];
        justOwner[0] = owner;
        // Dispatch on the JVMS reference kind rather than on the enum constants. That is not a
        // workaround dressed up as design — the `REF_*` byte IS the discriminator, and the
        // interface variants (INTERFACE_STATIC, INTERFACE_SPECIAL, INTERFACE_VIRTUAL) share both
        // the byte and the shape of their invocation type with their class counterparts. It also
        // sidesteps finding #101: an imported nested type resolves in a TYPE position, but
        // `DirectMethodHandleDesc.Kind.STATIC` in a VALUE position is read as a variable and does not resolve.
        int ref = kind.refKind;
        MethodTypeDesc type;
        if (ref == 1) {
            // REF_getField: (owner)fieldType
            type = MethodTypeDesc.of(ClassDesc.ofDescriptor(lookupDescriptor), justOwner);
        } else if (ref == 2) {
            // REF_getStatic: ()fieldType
            type = MethodTypeDesc.of(ClassDesc.ofDescriptor(lookupDescriptor));
        } else if (ref == 3) {
            // REF_putField: (owner, fieldType)void
            ClassDesc[] receiverAndValue = new ClassDesc[2];
            receiverAndValue[0] = owner;
            receiverAndValue[1] = ClassDesc.ofDescriptor(lookupDescriptor);
            type = MethodTypeDesc.of(ClassDesc.ofDescriptor("V"), receiverAndValue);
        } else if (ref == 4) {
            // REF_putStatic: (fieldType)void
            ClassDesc[] justValue = new ClassDesc[1];
            justValue[0] = ClassDesc.ofDescriptor(lookupDescriptor);
            type = MethodTypeDesc.of(ClassDesc.ofDescriptor("V"), justValue);
        } else if (ref == 6) {
            // REF_invokeStatic: the lookup descriptor already is the invocation type.
            type = MethodTypeDesc.ofDescriptor(lookupDescriptor);
        } else if (ref == 8) {
            // REF_newInvokeSpecial: a constructor is invoked to PRODUCE its class, so the `V`
            // of the lookup descriptor becomes the owner.
            type = MethodTypeDesc.ofDescriptor(lookupDescriptor).changeReturnType(owner);
        } else {
            // REF_invokeVirtual / Special / Interface: the receiver leads the argument list.
            type = MethodTypeDesc.ofDescriptor(lookupDescriptor).insertParameterTypes(0, justOwner);
        }
        return type;
    }

    public boolean equals(Object o) {
        boolean same = false;
        if (o instanceof ConstantMethodHandleDesc) {
            ConstantMethodHandleDesc other = (ConstantMethodHandleDesc) o;
            same = kind == other.kind
                    && owner.equals(other.owner)
                    && name.equals(other.name)
                    && lookupDescriptor.equals(other.lookupDescriptor);
        }
        return same;
    }

    public int hashCode() {
        return ((kind.refKind * 31 + owner.hashCode()) * 31 + name.hashCode()) * 31
                + lookupDescriptor.hashCode();
    }

    public String toString() {
        // Deliberately no `kind.toString()` in the concatenation: with the enum operand the
        // string-concat desugar reports `append` as ambiguous (finding #122 — a class's
        // declaration and its interface's re-declaration count as two candidates).
        return "MethodHandleDesc[" + owner.displayName() + "::" + name + lookupDescriptor + "]";
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
