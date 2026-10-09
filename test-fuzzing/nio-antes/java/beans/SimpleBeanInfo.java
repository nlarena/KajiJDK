package java.beans;

// BeanInfo's empty implementation, for inheriting from and overriding only what matters. Returning
// null everywhere is not laziness: it is the agreed signal for "I contribute nothing here, work it
// out by reflection", and that is why the methods do not return empty arrays.
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
     * The bean's icon.
     *
     * @return `null`: the base one offers none. A concrete `BeanInfo` overrides it by calling
     *     {@link #loadImage} with the name of its file.
     */
    public java.awt.Image getIcon(int iconKind) {
        return null;
    }

    /**
     * Loads an image sitting as a resource next to the `BeanInfo`'s class.
     *
     * <p>It is the help meant for writing {@link #getIcon}: the name is relative to the class, as in
     * {@link Class#getResource}, so `"MyBeanIcon16.gif"` looks next to the `.class`.
     *
     * <p>Here it returns `null` even if the resource exists: the bytes are read fine, but this
     * library has no image decoder and {@link java.awt.Toolkit#createImage(byte[])} says so by
     * returning `null`. It is the same answer the JDK gives when the resource is not there, and it
     * is honest in both cases: there is no image to give.
     *
     * @return the image, or `null` if the resource is not there or cannot be decoded
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
                // Closing a resource that has already been read cannot fail in a way that
                // matters.
            }
        }
    }
}
