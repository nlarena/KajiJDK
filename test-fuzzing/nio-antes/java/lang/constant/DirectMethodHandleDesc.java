package java.lang.constant;

// A nominal descriptor for a method handle that points *directly* at a field, method or
// constructor — as opposed to one built by combinator. It is the shape a `MethodHandle` constant
// takes in the constant pool, which is why it carries exactly what a `CONSTANT_MethodHandle`
// entry does: a reference kind, an owner, a name and a descriptor.
public interface DirectMethodHandleDesc extends MethodHandleDesc {

    // What the handle does, and which `REF_*` byte encodes it (JVMS 4.4.8). The enum exists
    // because the raw reference kind is not enough on its own: `REF_invokeStatic` on an
    // interface and on a class are the same byte but different lookups, so the flag rides along.
    public enum Kind {
        STATIC(6),
        INTERFACE_STATIC(6, true),
        VIRTUAL(5),
        INTERFACE_VIRTUAL(9, true),
        SPECIAL(7),
        INTERFACE_SPECIAL(7, true),
        CONSTRUCTOR(8),
        GETTER(1),
        SETTER(3),
        STATIC_GETTER(2),
        STATIC_SETTER(4);

        public final int refKind;
        public final boolean isInterface;

        Kind(int refKind) {
            this.refKind = refKind;
            this.isInterface = false;
        }

        Kind(int refKind, boolean isInterface) {
            this.refKind = refKind;
            this.isInterface = isInterface;
        }

        // The kind for a reference byte, taking the class interpretation where it is ambiguous.
        public static Kind valueOf(int refKind) {
            return valueOf(refKind, false);
        }

        public static Kind valueOf(int refKind, boolean isInterface) {
            Kind found = null;
            Kind[] all = Kind.values();
            int i = 0;
            while (i < all.length) {
                Kind k = all[i];
                if (k.refKind == refKind && k.isInterface == isInterface) {
                    found = k;
                }
                i = i + 1;
            }
            return found;
        }
    }

    Kind kind();

    int refKind();

    boolean isOwnerInterface();

    ClassDesc owner();

    String methodName();

    // The descriptor as the *lookup* wants it: a method descriptor for a method, a field
    // descriptor for a field. Deliberately not the invocation type — see `invocationType()`.
    String lookupDescriptor();
}
