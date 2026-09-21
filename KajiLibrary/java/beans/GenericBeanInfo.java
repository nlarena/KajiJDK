package java.beans;

// The BeanInfo Introspector puts together out of what it worked out by reflection, letting an
// explicit BeanInfo -- a `<Bean>BeanInfo` class -- override whatever it likes.
//
// The precedence rule is the JDK's and it rests on returning null meaning "no opinion": if the
// explicit one returns something non-null for a category, the explicit one wins and what was worked
// out for that category is discarded; if it returns null, what was worked out stays. It is all or
// nothing per category, not an element-by-element merge.
//
// Top-level and package-private on purpose: in this tree a class nested inside another has its
// history of miscompiles (#13), and this class gains nothing by being nested.
class GenericBeanInfo implements BeanInfo {

    private BeanDescriptor beanDescriptor;
    private PropertyDescriptor[] properties;
    private EventSetDescriptor[] events;
    private MethodDescriptor[] methods;
    private BeanInfo explicit;

    GenericBeanInfo(BeanDescriptor beanDescriptor, PropertyDescriptor[] properties,
                     EventSetDescriptor[] events, MethodDescriptor[] methods, BeanInfo explicit) {
        this.beanDescriptor = beanDescriptor;
        this.properties = properties;
        this.events = events;
        this.methods = methods;
        this.explicit = explicit;
    }

    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor r = null;
        if (this.explicit != null) {
            r = this.explicit.getBeanDescriptor();
        }
        if (r == null) {
            r = this.beanDescriptor;
        }
        return r;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] r = null;
        if (this.explicit != null) {
            r = this.explicit.getPropertyDescriptors();
        }
        if (r == null) {
            r = this.properties;
        }
        return copyProperties(r);
    }

    public EventSetDescriptor[] getEventSetDescriptors() {
        EventSetDescriptor[] r = null;
        if (this.explicit != null) {
            r = this.explicit.getEventSetDescriptors();
        }
        if (r == null) {
            r = this.events;
        }
        return copyEvents(r);
    }

    public MethodDescriptor[] getMethodDescriptors() {
        MethodDescriptor[] r = null;
        if (this.explicit != null) {
            r = this.explicit.getMethodDescriptors();
        }
        if (r == null) {
            r = this.methods;
        }
        return copyMethods(r);
    }

    public int getDefaultPropertyIndex() {
        int r = -1;
        if (this.explicit != null) {
            r = this.explicit.getDefaultPropertyIndex();
        }
        return r;
    }

    public int getDefaultEventIndex() {
        int r = -1;
        if (this.explicit != null) {
            r = this.explicit.getDefaultEventIndex();
        }
        return r;
    }

    // Only the explicit one supplies the icon: there is nothing to work out by reflection about a
    // drawing.
    public java.awt.Image getIcon(int iconKind) {
        if (this.explicit != null) {
            return this.explicit.getIcon(iconKind);
        }
        return null;
    }

    public BeanInfo[] getAdditionalBeanInfo() {
        BeanInfo[] r = null;
        if (this.explicit != null) {
            r = this.explicit.getAdditionalBeanInfo();
        }
        return r;
    }

    // It is copied on the way out: the cached array cannot be left exposed to the caller sorting it
    // or overwriting it, because the next query would see the damage.
    private static PropertyDescriptor[] copyProperties(PropertyDescriptor[] a) {
        PropertyDescriptor[] r = null;
        if (a != null) {
            r = new PropertyDescriptor[a.length];
            for (int i = 0; i < a.length; i++) { r[i] = a[i]; }
        }
        return r;
    }

    private static EventSetDescriptor[] copyEvents(EventSetDescriptor[] a) {
        EventSetDescriptor[] r = null;
        if (a != null) {
            r = new EventSetDescriptor[a.length];
            for (int i = 0; i < a.length; i++) { r[i] = a[i]; }
        }
        return r;
    }

    private static MethodDescriptor[] copyMethods(MethodDescriptor[] a) {
        MethodDescriptor[] r = null;
        if (a != null) {
            r = new MethodDescriptor[a.length];
            for (int i = 0; i < a.length; i++) { r[i] = a[i]; }
        }
        return r;
    }
}
