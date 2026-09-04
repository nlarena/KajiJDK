package java.beans;

// La implementacion vacia de BeanInfo, para heredar y sobrescribir solo lo que interesa. Devolver
// null en todo no es pereza: es la senal convenida de "no aporto nada aca, deducilo por reflexion",
// y por eso los metodos no devuelven arreglos vacios.
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

    /**
     * El ícono del bean.
     *
     * @return `null`: el de base no ofrece ninguno. Un `BeanInfo` concreto lo redefine llamando a
     *     {@link #loadImage} con el nombre de su archivo.
     */
    public java.awt.Image getIcon(int iconKind) {
        return null;
    }

    /**
     * Carga una imagen que está como recurso al lado de la clase del `BeanInfo`.
     *
     * <p>Es la ayuda pensada para escribir {@link #getIcon}: el nombre es relativo a la clase, como en
     * {@link Class#getResource}, así que `"MiBeanIcon16.gif"` busca al lado del `.class`.
     *
     * <p>Acá devuelve `null` aunque el recurso exista: los bytes se leen bien, pero esta biblioteca
     * no tiene decodificador de imágenes y {@link java.awt.Toolkit#createImage(byte[])} lo dice
     * devolviendo `null`. Es la misma respuesta que da el JDK cuando el recurso no está, y es honesta
     * en los dos casos: no hay imagen que dar.
     *
     * @return la imagen, o `null` si el recurso no está o no se puede decodificar
     */
    public java.awt.Image loadImage(String resourceName) {
        java.io.InputStream in = this.getClass().getResourceAsStream(resourceName);
        if (in == null) {
            return null;
        }
        try {
            byte[] bytes = in.readAllBytes();
            return java.awt.Toolkit.getDefaultToolkit().createImage(bytes);
        } catch (java.io.IOException e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (java.io.IOException e) {
                // Cerrar un recurso que ya se leyo no puede fallar de forma que importe.
            }
        }
    }
}
