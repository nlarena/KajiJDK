package java.beans;

// The bean seen as a whole: its class and, optionally, the class of the customizer a tool would
// use to edit it. The descriptor's name is the class's simple name —checked against the real JDK:
// for `BeanRaro` it gives "BeanRaro", not the qualified name.
public class BeanDescriptor extends FeatureDescriptor {

    private Class<?> beanClass;
    private Class<?> customizerClass;

    public BeanDescriptor(Class<?> beanClass) {
        this(beanClass, null);
    }

    public BeanDescriptor(Class<?> beanClass, Class<?> customizerClass) {
        this.beanClass = beanClass;
        this.customizerClass = customizerClass;
        if (beanClass != null) {
            String n = beanClass.getName();
            int dot = n.lastIndexOf('.');
            if (dot >= 0) {
                n = n.substring(dot + 1);
            }
            this.setName(n);
        }
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    public Class<?> getCustomizerClass() {
        return this.customizerClass;
    }
}
