package java.beans;

import java.lang.reflect.Method;

// Un metodo del bean que vale la pena exponer, con descriptores opcionales para sus parametros.
// El nombre del descriptor es el del metodo.
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

    // Puede ser null: describir los parametros es opcional, y no describirlos no es lo mismo que
    // decir que no tiene.
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
