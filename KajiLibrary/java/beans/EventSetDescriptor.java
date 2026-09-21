package java.beans;

import java.lang.reflect.Method;

// A group of events the bean fires: the listener interface, the methods called on it, and the
// add/remove pair one subscribes with.
//
// The set's name comes from the interface, not from the method: for `addFooListener(FooListener)`
// the event is called "foo" -- the "Listener" suffix is taken off and it is decapitalized. And it is
// checked against the real JDK that **the "Listener" suffix is compulsory**: a bean with
// `addBarOyente(BarOyente)`, with BarOyente extending EventListener and all, produces NO event set
// at all. It is a rule about the name, not about the type.
public class EventSetDescriptor extends FeatureDescriptor {

    private Class<?> listenerType;
    private MethodDescriptor[] listenerMethodDescriptors;
    private Method addMethod;
    private Method removeMethod;
    private Method getMethod;
    private boolean unicast;
    private boolean inDefaultEventSet = true;

    // The shortest form: `add<Listener>` and `remove<Listener>` are worked out from the
    // interface's name.
    public EventSetDescriptor(Class<?> sourceClass, String eventSetName,
                              Class<?> listenerType, String listenerMethodName)
            throws IntrospectionException {
        this(sourceClass, eventSetName, listenerType,
             new String[] { listenerMethodName },
             "add" + simpleName(listenerType),
             "remove" + simpleName(listenerType));
    }

    public EventSetDescriptor(Class<?> sourceClass, String eventSetName,
                              Class<?> listenerType, String[] listenerMethodNames,
                              String addListenerMethodName, String removeListenerMethodName)
            throws IntrospectionException {
        this(sourceClass, eventSetName, listenerType, listenerMethodNames,
             addListenerMethodName, removeListenerMethodName, null);
    }

    public EventSetDescriptor(Class<?> sourceClass, String eventSetName,
                              Class<?> listenerType, String[] listenerMethodNames,
                              String addListenerMethodName, String removeListenerMethodName,
                              String getListenerMethodName)
            throws IntrospectionException {
        if (sourceClass == null || eventSetName == null || listenerType == null) {
            throw new IntrospectionException("null arg in event set descriptor");
        }
        this.setName(eventSetName);
        this.listenerType = listenerType;

        Method[] ms = new Method[listenerMethodNames.length];
        for (int i = 0; i < listenerMethodNames.length; i++) {
            Method m = findByName(listenerType, listenerMethodNames[i]);
            if (m == null) {
                throw new IntrospectionException("Method not found: " + listenerMethodNames[i]
                    + " on class " + listenerType.getName());
            }
            ms[i] = m;
        }
        this.setListenerMethods(ms);

        this.addMethod = require(sourceClass, addListenerMethodName, listenerType);
        this.removeMethod = require(sourceClass, removeListenerMethodName, listenerType);
        if (getListenerMethodName != null) {
            this.getMethod = PropertyDescriptor.findMethod(sourceClass, getListenerMethodName, 0);
        }
    }

    public EventSetDescriptor(String eventSetName, Class<?> listenerType,
                              Method[] listenerMethods, Method addListenerMethod,
                              Method removeListenerMethod)
            throws IntrospectionException {
        this(eventSetName, listenerType, listenerMethods, addListenerMethod, removeListenerMethod, null);
    }

    public EventSetDescriptor(String eventSetName, Class<?> listenerType,
                              Method[] listenerMethods, Method addListenerMethod,
                              Method removeListenerMethod, Method getListenerMethod)
            throws IntrospectionException {
        this.setName(eventSetName);
        this.listenerType = listenerType;
        this.setListenerMethods(listenerMethods);
        this.addMethod = addListenerMethod;
        this.removeMethod = removeListenerMethod;
        this.getMethod = getListenerMethod;
    }

    public EventSetDescriptor(String eventSetName, Class<?> listenerType,
                              MethodDescriptor[] listenerMethodDescriptors,
                              Method addListenerMethod, Method removeListenerMethod)
            throws IntrospectionException {
        this.setName(eventSetName);
        this.listenerType = listenerType;
        if (listenerMethodDescriptors != null) {
            this.listenerMethodDescriptors = new MethodDescriptor[listenerMethodDescriptors.length];
            for (int i = 0; i < listenerMethodDescriptors.length; i++) {
                this.listenerMethodDescriptors[i] = listenerMethodDescriptors[i];
            }
        }
        this.addMethod = addListenerMethod;
        this.removeMethod = removeListenerMethod;
    }

    private void setListenerMethods(Method[] ms) {
        if (ms != null) {
            this.listenerMethodDescriptors = new MethodDescriptor[ms.length];
            for (int i = 0; i < ms.length; i++) {
                this.listenerMethodDescriptors[i] = new MethodDescriptor(ms[i]);
            }
        }
    }

    public Class<?> getListenerType() {
        return this.listenerType;
    }

    public synchronized Method[] getListenerMethods() {
        Method[] r = null;
        if (this.listenerMethodDescriptors != null) {
            r = new Method[this.listenerMethodDescriptors.length];
            for (int i = 0; i < this.listenerMethodDescriptors.length; i++) {
                r[i] = this.listenerMethodDescriptors[i].getMethod();
            }
        }
        return r;
    }

    public synchronized MethodDescriptor[] getListenerMethodDescriptors() {
        MethodDescriptor[] r = null;
        if (this.listenerMethodDescriptors != null) {
            r = new MethodDescriptor[this.listenerMethodDescriptors.length];
            for (int i = 0; i < this.listenerMethodDescriptors.length; i++) {
                r[i] = this.listenerMethodDescriptors[i];
            }
        }
        return r;
    }

    public synchronized Method getAddListenerMethod() {
        return this.addMethod;
    }

    public synchronized Method getRemoveListenerMethod() {
        return this.removeMethod;
    }

    // The method returning the already registered listeners. It is often missing: it is optional
    // in the convention.
    public synchronized Method getGetListenerMethod() {
        return this.getMethod;
    }

    // Unicast: the bean admits a single listener and the add throws
    // TooManyListenersException.
    public void setUnicast(boolean unicast) {
        this.unicast = unicast;
    }

    public boolean isUnicast() {
        return this.unicast;
    }

    // Whether a tool ought to show it by default.
    public void setInDefaultEventSet(boolean inDefaultEventSet) {
        this.inDefaultEventSet = inDefaultEventSet;
    }

    public boolean isInDefaultEventSet() {
        return this.inDefaultEventSet;
    }

    private static Method require(Class<?> c, String name, Class<?> listenerClass)
            throws IntrospectionException {
        Method m = PropertyDescriptor.findMethod(c, name, 1);
        if (m == null) {
            throw new IntrospectionException("Method not found: " + name + " on class " + c.getName());
        }
        return m;
    }

    private static Method findByName(Class<?> c, String name) {
        Method hit = null;
        Method[] ms = c.getMethods();
        for (int i = 0; i < ms.length; i++) {
            if (hit == null && ms[i].getName().equals(name)) {
                hit = ms[i];
            }
        }
        return hit;
    }

    // The name with no package and no enclosing class: `FooListener` for
    // `com.x.Outer$FooListener`.
    static String simpleName(Class<?> c) {
        String n = c.getName();
        int dot = n.lastIndexOf('.');
        if (dot >= 0) {
            n = n.substring(dot + 1);
        }
        int peso = n.lastIndexOf('$');
        if (peso >= 0) {
            n = n.substring(peso + 1);
        }
        return n;
    }
}
