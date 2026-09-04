package java.beans;

// La implementacion vacia de BeanInfo, para heredar y sobrescribir solo lo que interesa. Devolver
// null en todo no es pereza: es la senal convenida de "no aporto nada aca, deducilo por reflexion",
// y por eso los metodos no devuelven arreglos vacios.
//
// **Omitidos a proposito: `getIcon(int)` y `loadImage(String)`.** Los dos devuelven
// `java.awt.Image`, que no existe en este arbol. Ver la nota en BeanInfo.
public class SimpleBeanInfo implements BeanInfo {

    public SimpleBeanInfo() {
    }

    public BeanDescriptor getBeanDescriptor() {
        return null;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        return null;
    }

    public int getDefaultPropertyIndex() {
        return -1;
    }

    public EventSetDescriptor[] getEventSetDescriptors() {
        return null;
    }

    public int getDefaultEventIndex() {
        return -1;
    }

    public MethodDescriptor[] getMethodDescriptors() {
        return null;
    }

    public BeanInfo[] getAdditionalBeanInfo() {
        return null;
    }
}
