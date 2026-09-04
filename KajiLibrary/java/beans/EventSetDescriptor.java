package java.beans;

import java.lang.reflect.Method;

// Un grupo de eventos que el bean dispara: la interfaz de oyente, los metodos que se le llaman, y
// el par add/remove con el que uno se suscribe.
//
// El nombre del conjunto sale de la interfaz, no del metodo: para `addFooListener(FooListener)` el
// evento se llama "foo" — se le saca el sufijo "Listener" y se decapitaliza. Y esta comprobado
// contra el JDK real que **el sufijo "Listener" es obligatorio**: un bean con
// `addBarOyente(BarOyente)`, con BarOyente extendiendo EventListener y todo, NO produce ningun
// conjunto de eventos. Es una regla de nombre, no de tipo.
public class EventSetDescriptor extends FeatureDescriptor {

    private Class<?> listenerType;
    private MethodDescriptor[] listenerMethodDescriptors;
    private Method addMethod;
    private Method removeMethod;
    private Method getMethod;
    private boolean unicast;
    private boolean inDefaultEventSet = true;

    // La forma mas corta: se deducen `add<Listener>` y `remove<Listener>` del nombre de la interfaz.
    public EventSetDescriptor(Class<?> sourceClass, String eventSetName,
                              Class<?> listenerType, String listenerMethodName)
            throws IntrospectionException {
        this(sourceClass, eventSetName, listenerType,
             new String[] { listenerMethodName },
             "add" + nombreSimple(listenerType),
             "remove" + nombreSimple(listenerType));
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
            Method m = buscarPorNombre(listenerType, listenerMethodNames[i]);
            if (m == null) {
                throw new IntrospectionException("Method not found: " + listenerMethodNames[i]
                    + " on class " + listenerType.getName());
            }
            ms[i] = m;
        }
        this.fijarMetodosDeOyente(ms);

        this.addMethod = exigir(sourceClass, addListenerMethodName, listenerType);
        this.removeMethod = exigir(sourceClass, removeListenerMethodName, listenerType);
        if (getListenerMethodName != null) {
            this.getMethod = PropertyDescriptor.buscarMetodo(sourceClass, getListenerMethodName, 0);
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
        this.fijarMetodosDeOyente(listenerMethods);
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

    private void fijarMetodosDeOyente(Method[] ms) {
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

    // El metodo que devuelve los oyentes ya registrados. Suele faltar: es opcional en la convencion.
    public synchronized Method getGetListenerMethod() {
        return this.getMethod;
    }

    // Unicast: el bean admite un solo oyente y el add tira TooManyListenersException.
    public void setUnicast(boolean unicast) {
        this.unicast = unicast;
    }

    public boolean isUnicast() {
        return this.unicast;
    }

    // Si una herramienta deberia mostrarlo por defecto.
    public void setInDefaultEventSet(boolean inDefaultEventSet) {
        this.inDefaultEventSet = inDefaultEventSet;
    }

    public boolean isInDefaultEventSet() {
        return this.inDefaultEventSet;
    }

    private static Method exigir(Class<?> c, String nombre, Class<?> tipoOyente)
            throws IntrospectionException {
        Method m = PropertyDescriptor.buscarMetodo(c, nombre, 1);
        if (m == null) {
            throw new IntrospectionException("Method not found: " + nombre + " on class " + c.getName());
        }
        return m;
    }

    private static Method buscarPorNombre(Class<?> c, String nombre) {
        Method encontrado = null;
        Method[] ms = c.getMethods();
        for (int i = 0; i < ms.length; i++) {
            if (encontrado == null && ms[i].getName().equals(nombre)) {
                encontrado = ms[i];
            }
        }
        return encontrado;
    }

    // El nombre sin paquete ni clase envolvente: `FooListener` para `com.x.Outer$FooListener`.
    static String nombreSimple(Class<?> c) {
        String n = c.getName();
        int punto = n.lastIndexOf('.');
        if (punto >= 0) {
            n = n.substring(punto + 1);
        }
        int peso = n.lastIndexOf('$');
        if (peso >= 0) {
            n = n.substring(peso + 1);
        }
        return n;
    }
}
