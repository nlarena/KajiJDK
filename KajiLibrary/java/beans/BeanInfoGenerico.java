package java.beans;

// El BeanInfo que arma Introspector con lo deducido por reflexion, dejando que un BeanInfo
// explicito —una clase `<Bean>BeanInfo`— pise lo que quiera.
//
// La regla de precedencia es la del JDK y se apoya en que devolver null significa "no opino":
// si el explicito devuelve algo no-null para una categoria, gana el explicito y se descarta lo
// deducido para esa categoria; si devuelve null, queda lo deducido. Es todo o nada por categoria,
// no una fusion elemento por elemento.
//
// Top-level y package-private a proposito: en este arbol una clase anidada adentro de otra tiene
// su historia de miscompilados (#13), y esta clase no gana nada por estar anidada.
class BeanInfoGenerico implements BeanInfo {

    private BeanDescriptor beanDescriptor;
    private PropertyDescriptor[] properties;
    private EventSetDescriptor[] events;
    private MethodDescriptor[] methods;
    private BeanInfo explicito;

    BeanInfoGenerico(BeanDescriptor beanDescriptor, PropertyDescriptor[] properties,
                     EventSetDescriptor[] events, MethodDescriptor[] methods, BeanInfo explicito) {
        this.beanDescriptor = beanDescriptor;
        this.properties = properties;
        this.events = events;
        this.methods = methods;
        this.explicito = explicito;
    }

    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor r = null;
        if (this.explicito != null) {
            r = this.explicito.getBeanDescriptor();
        }
        if (r == null) {
            r = this.beanDescriptor;
        }
        return r;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] r = null;
        if (this.explicito != null) {
            r = this.explicito.getPropertyDescriptors();
        }
        if (r == null) {
            r = this.properties;
        }
        return copiaP(r);
    }

    public EventSetDescriptor[] getEventSetDescriptors() {
        EventSetDescriptor[] r = null;
        if (this.explicito != null) {
            r = this.explicito.getEventSetDescriptors();
        }
        if (r == null) {
            r = this.events;
        }
        return copiaE(r);
    }

    public MethodDescriptor[] getMethodDescriptors() {
        MethodDescriptor[] r = null;
        if (this.explicito != null) {
            r = this.explicito.getMethodDescriptors();
        }
        if (r == null) {
            r = this.methods;
        }
        return copiaM(r);
    }

    public int getDefaultPropertyIndex() {
        int r = -1;
        if (this.explicito != null) {
            r = this.explicito.getDefaultPropertyIndex();
        }
        return r;
    }

    public int getDefaultEventIndex() {
        int r = -1;
        if (this.explicito != null) {
            r = this.explicito.getDefaultEventIndex();
        }
        return r;
    }

    // El icono lo pone solo el explicito: no hay nada que deducir por reflexion sobre un dibujo.
    public java.awt.Image getIcon(int iconKind) {
        if (this.explicito != null) {
            return this.explicito.getIcon(iconKind);
        }
        return null;
    }

    public BeanInfo[] getAdditionalBeanInfo() {
        BeanInfo[] r = null;
        if (this.explicito != null) {
            r = this.explicito.getAdditionalBeanInfo();
        }
        return r;
    }

    // Se copia al salir: el arreglo cacheado no puede quedar expuesto a que el llamador lo ordene
    // o lo pise, porque la proxima consulta veria el destrozo.
    private static PropertyDescriptor[] copiaP(PropertyDescriptor[] a) {
        PropertyDescriptor[] r = null;
        if (a != null) {
            r = new PropertyDescriptor[a.length];
            for (int i = 0; i < a.length; i++) { r[i] = a[i]; }
        }
        return r;
    }

    private static EventSetDescriptor[] copiaE(EventSetDescriptor[] a) {
        EventSetDescriptor[] r = null;
        if (a != null) {
            r = new EventSetDescriptor[a.length];
            for (int i = 0; i < a.length; i++) { r[i] = a[i]; }
        }
        return r;
    }

    private static MethodDescriptor[] copiaM(MethodDescriptor[] a) {
        MethodDescriptor[] r = null;
        if (a != null) {
            r = new MethodDescriptor[a.length];
            for (int i = 0; i < a.length; i++) { r[i] = a[i]; }
        }
        return r;
    }
}
