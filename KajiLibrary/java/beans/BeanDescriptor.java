package java.beans;

// El bean visto como un todo: su clase y, opcionalmente, la clase del customizador que una
// herramienta usaria para editarlo. El nombre del descriptor es el nombre simple de la clase
// —verificado contra el JDK real: para `BeanRaro` da "BeanRaro", no el nombre calificado—.
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
            int punto = n.lastIndexOf('.');
            if (punto >= 0) {
                n = n.substring(punto + 1);
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
