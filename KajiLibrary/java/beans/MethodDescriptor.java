package java.beans;

import java.lang.reflect.Method;

// A method of the bean worth exposing, with optional descriptors for its parameters. The
// descriptor's name is the method's.
public class MethodDescriptor extends FeatureDescriptor {

    private Method method;
    private ParameterDescriptor[] parameterDescriptors;

    public MethodDescriptor(Method method) {
        this.method = method;
        if (method != null) {
            this.setName(method.getName());
        }
    }

    public MethodDescriptor(Method method, ParameterDescriptor[] parameterDescriptors) {
        this(method);
        this.parameterDescriptors = parameterDescriptors;
    }

    public synchronized Method getMethod() {
        return this.method;
    }

    // It may be null: describing the parameters is optional, and not describing them is not the
    // same as saying it has none.
    public ParameterDescriptor[] getParameterDescriptors() {
        ParameterDescriptor[] r = null;
        if (this.parameterDescriptors != null) {
            r = new ParameterDescriptor[this.parameterDescriptors.length];
            for (int i = 0; i < this.parameterDescriptors.length; i++) {
                r[i] = this.parameterDescriptors[i];
            }
        }
        return r;
    }
}
