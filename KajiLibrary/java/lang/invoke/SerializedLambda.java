package java.lang.invoke;

import java.io.Serializable;

// The serialized form of a lambda. A lambda has no name and no stable class — its implementing
// class is spun at run time — so it cannot be serialized as itself. What travels instead is this
// RECIPE: which interface it implements, which method implements it, and the captured values.
// The receiving side re-creates an equivalent lambda from the recipe.
//
// It is a plain data holder, which is why it is one of the few classes in this package that is
// complete rather than a shell.
public final class SerializedLambda implements Serializable {

    private final String capturingClass;
    private final String functionalInterfaceClass;
    private final String functionalInterfaceMethodName;
    private final String functionalInterfaceMethodSignature;
    private final String implClass;
    private final String implMethodName;
    private final String implMethodSignature;
    private final int implMethodKind;
    private final String instantiatedMethodType;
    private final Object[] capturedArgs;

    public SerializedLambda(Class<?> capturingClass, String functionalInterfaceClass,
            String functionalInterfaceMethodName, String functionalInterfaceMethodSignature,
            int implMethodKind, String implClass, String implMethodName,
            String implMethodSignature, String instantiatedMethodType, Object[] capturedArgs) {
        this.capturingClass = capturingClass.getName();
        this.functionalInterfaceClass = functionalInterfaceClass;
        this.functionalInterfaceMethodName = functionalInterfaceMethodName;
        this.functionalInterfaceMethodSignature = functionalInterfaceMethodSignature;
        this.implMethodKind = implMethodKind;
        this.implClass = implClass;
        this.implMethodName = implMethodName;
        this.implMethodSignature = implMethodSignature;
        this.instantiatedMethodType = instantiatedMethodType;
        this.capturedArgs = capturedArgs;
    }

    public String getCapturingClass() {
        return capturingClass;
    }

    public String getFunctionalInterfaceClass() {
        return functionalInterfaceClass;
    }

    public String getFunctionalInterfaceMethodName() {
        return functionalInterfaceMethodName;
    }

    public String getFunctionalInterfaceMethodSignature() {
        return functionalInterfaceMethodSignature;
    }

    public String getImplClass() {
        return implClass;
    }

    public String getImplMethodName() {
        return implMethodName;
    }

    public String getImplMethodSignature() {
        return implMethodSignature;
    }

    public int getImplMethodKind() {
        return implMethodKind;
    }

    // The type the lambda was INSTANTIATED as, which can be more specific than the interface
    // method's own signature — that is where a generic functional interface gets pinned down.
    public final String getInstantiatedMethodType() {
        return instantiatedMethodType;
    }

    public int getCapturedArgCount() {
        return capturedArgs.length;
    }

    public Object getCapturedArg(int i) {
        return capturedArgs[i];
    }

    public String toString() {
        return "SerializedLambda[" + implClass + "." + implMethodName + "]";
    }
}
