package java.beans;

// Lo que una clase puede declarar explicitamente sobre si misma como bean, en vez de dejar que
// Introspector lo deduzca. Introspector busca una clase `<Bean>BeanInfo`, y lo que esta encuentre
// pisa lo deducido.
//
// **Omitido a proposito: `java.awt.Image getIcon(int)`.** En este arbol no existe `java.awt`, asi
// que el metodo no se puede declarar con su tipo real. Declararlo devolviendo otra cosa —Object,
// o un Image propio— seria una firma que miente: quien compile contra el JDK real y corra contra
// este no encontraria el metodo. Las cuatro constantes ICON_* si estan, porque son int y no
// arrastran a awt: sirven para que el codigo que las nombra siga compilando.
public interface BeanInfo {

    int ICON_COLOR_16x16 = 1;
    int ICON_COLOR_32x32 = 2;
    int ICON_MONO_16x16 = 3;
    int ICON_MONO_32x32 = 4;

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
