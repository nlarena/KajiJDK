package java.beans;

// Lo que una clase puede declarar explicitamente sobre si misma como bean, en vez de dejar que
// Introspector lo deduzca. Introspector busca una clase `<Bean>BeanInfo`, y lo que esta encuentre
// pisa lo deducido.
public interface BeanInfo {

    int ICON_COLOR_16x16 = 1;
    int ICON_COLOR_32x32 = 2;
    int ICON_MONO_16x16 = 3;
    int ICON_MONO_32x32 = 4;

    /**
     * El ícono con que una herramienta muestra al bean en su paleta.
     *
     * @param iconKind una de las cuatro constantes `ICON_*`: color o monocromo, 16 o 32 píxeles
     * @return la imagen, o `null` si el bean no ofrece ícono de ese tipo
     */
    java.awt.Image getIcon(int iconKind);

    BeanDescriptor getBeanDescriptor();

    PropertyDescriptor[] getPropertyDescriptors();

    // El indice, dentro de getPropertyDescriptors(), de la propiedad que una herramienta deberia
    // resaltar. -1 significa "ninguna en particular".
    int getDefaultPropertyIndex();

    EventSetDescriptor[] getEventSetDescriptors();

    int getDefaultEventIndex();

    MethodDescriptor[] getMethodDescriptors();

    // Otros BeanInfo cuyos descriptores se suman a los de este. Devolver null —no un arreglo
    // vacio— es como se dice "no hay".
    BeanInfo[] getAdditionalBeanInfo();
}
