package java.beans;

// What a class can declare explicitly about itself as a bean, instead of letting Introspector work
// it out. Introspector looks for a `<Bean>BeanInfo` class, and whatever that one has overrides what
// was worked out.
public interface BeanInfo {

    int ICON_COLOR_16x16 = 1;
    int ICON_COLOR_32x32 = 2;
    int ICON_MONO_16x16 = 3;
    int ICON_MONO_32x32 = 4;

    /**
     * The icon a tool shows the bean with in its palette.
     *
     * @param iconKind one of the four `ICON_*` constants: colour or monochrome, 16 or 32 pixels
     * @return the image, or `null` if the bean offers no icon of that kind
     */
    java.awt.Image getIcon(int iconKind);

    BeanDescriptor getBeanDescriptor();

    PropertyDescriptor[] getPropertyDescriptors();

    // The index, within getPropertyDescriptors(), of the property a tool ought to highlight. -1
    // means "none in particular".
    int getDefaultPropertyIndex();

    EventSetDescriptor[] getEventSetDescriptors();

    int getDefaultEventIndex();

    MethodDescriptor[] getMethodDescriptors();

    // Other BeanInfos whose descriptors add to this one's. Returning null —not an empty array— is
    // how "there are none" is said.
    BeanInfo[] getAdditionalBeanInfo();
}
