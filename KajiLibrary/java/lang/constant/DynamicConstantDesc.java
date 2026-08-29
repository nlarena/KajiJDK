package java.lang.constant;

import java.util.ArrayList;
import java.util.List;

// A nominal descriptor for a `condy` constant: a value the JVM produces by CALLING a bootstrap
// method the first time the constant is used, instead of reading it out of the pool. Everything
// here describes that call — which bootstrap, under what name, of what type, with which static
// arguments — and none of it performs the call, which is what keeps it usable at compile time.
//
// `resolveConstantDesc` is OMITTED (`java.lang.invoke`); see `ConstantDesc`.
public abstract class DynamicConstantDesc<T> implements ConstantDesc {

    private final DirectMethodHandleDesc bootstrapMethod;
    private final String constantName;
    private final ClassDesc constantType;
    private final ConstantDesc[] bootstrapArgs;

    protected DynamicConstantDesc(DirectMethodHandleDesc bootstrapMethod, String constantName,
            ClassDesc constantType, ConstantDesc[] bootstrapArgs) {
        this.bootstrapMethod = bootstrapMethod;
        this.constantName = constantName;
        this.constantType = constantType;
        this.bootstrapArgs = bootstrapArgs;
    }

    // Un `condy` sobre un bootstrap bien conocido describe algo que YA tiene un descriptor mas
    // simple, y devolver el simple es lo que hace que dos descripciones de la misma constante se
    // comparen iguales. La nota que estaba acá decia que no se podia porque los `ConstantDescs.
    // BSM_*` estaban bloqueados por #101 -- #101 esta cerrado y los BSM existen, asi que el
    // pliegue del constante nulo se hace. Los otros que el JDK pliega (la clase primitiva, la
    // constante de enum, los VarHandle) necesitan descriptores que la biblioteca todavia no
    // tiene, y esos siguen devolviendose como estan.
    public static ConstantDesc ofCanonical(DirectMethodHandleDesc bootstrapMethod, String constantName,
            ClassDesc constantType, ConstantDesc... bootstrapArgs) {
        if (bootstrapArgs.length == 0 && bootstrapMethod.equals(ConstantDescs.BSM_NULL_CONSTANT)) {
            return ConstantDescs.NULL;
        }
        return ofNamed(bootstrapMethod, constantName, constantType, bootstrapArgs);
    }

    public static DynamicConstantDesc ofNamed(DirectMethodHandleDesc bootstrapMethod, String constantName,
            ClassDesc constantType, ConstantDesc... bootstrapArgs) {
        return new AnonymousDynamicConstantDesc(bootstrapMethod, constantName, constantType, bootstrapArgs);
    }

    // The common case: the name carries no meaning, so the JVM's default `_` is used.
    public static DynamicConstantDesc of(DirectMethodHandleDesc bootstrapMethod, ConstantDesc... bootstrapArgs) {
        // The cast is finding #120: `invocationType()` is declared on `MethodHandleDesc` and only
        // INHERITED by `DirectMethodHandleDesc`, and our compiler does not read a classpath
        // supertype's method table — so the call is not found unless the receiver is spelled as
        // the type that declares it.
        MethodHandleDesc bsm = (MethodHandleDesc) bootstrapMethod;
        return ofNamed(bootstrapMethod, "_", bsm.invocationType().returnType(), bootstrapArgs);
    }

    public static DynamicConstantDesc of(DirectMethodHandleDesc bootstrapMethod) {
        return of(bootstrapMethod, new ConstantDesc[0]);
    }

    public String constantName() {
        return constantName;
    }

    public ClassDesc constantType() {
        return constantType;
    }

    public DirectMethodHandleDesc bootstrapMethod() {
        return bootstrapMethod;
    }

    public ConstantDesc[] bootstrapArgs() {
        ConstantDesc[] copy = new ConstantDesc[bootstrapArgs.length];
        int i = 0;
        while (i < bootstrapArgs.length) {
            copy[i] = bootstrapArgs[i];
            i = i + 1;
        }
        return copy;
    }

    public List<ConstantDesc> bootstrapArgsList() {
        List<ConstantDesc> list = new ArrayList<ConstantDesc>();
        int i = 0;
        while (i < bootstrapArgs.length) {
            list.add(bootstrapArgs[i]);
            i = i + 1;
        }
        return list;
    }

    public final boolean equals(Object o) {
        boolean same = false;
        if (o instanceof DynamicConstantDesc) {
            DynamicConstantDesc other = (DynamicConstantDesc) o;
            same = constantName.equals(other.constantName())
                    && constantType.equals(other.constantType())
                    && bootstrapMethod.equals(other.bootstrapMethod());
        }
        return same;
    }

    public final int hashCode() {
        return (constantName.hashCode() * 31 + constantType.hashCode()) * 31
                + bootstrapMethod.hashCode();
    }

    public String toString() {
        return "DynamicConstantDesc[" + constantName + ":" + constantType.displayName() + "]";
    }
}

// The concrete subclass the factories hand out. The JDK uses an anonymous class for this; ours
// is named and package-private, because our compiler's anonymous classes carry captures we do
// not need here, and the gate skips a class the JDK has no counterpart for.
final class AnonymousDynamicConstantDesc extends DynamicConstantDesc {

    AnonymousDynamicConstantDesc(DirectMethodHandleDesc bootstrapMethod, String constantName,
            ClassDesc constantType, ConstantDesc[] bootstrapArgs) {
        super(bootstrapMethod, constantName, constantType, bootstrapArgs);
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
