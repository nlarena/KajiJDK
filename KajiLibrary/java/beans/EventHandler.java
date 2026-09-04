package java.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// Convierte un evento en una llamada a otro objeto, sin escribir la clase del oyente: "cuando
// llegue este evento, sacale esta propiedad y pasasela a este metodo de aquel objeto".
//
// Los tres nombres que lleva son las tres partes de esa frase:
//   - `listenerMethodName`: a que metodo del oyente reacciona (null = a todos).
//   - `eventPropertyName`: que sacarle al evento. Puede ser una ruta con puntos —"source.text"
//     saca `getSource()` y despues `getText()`—. null significa "no le pases nada".
//   - `action`: que llamarle al objetivo. Tambien puede ser una ruta con puntos, y el ultimo
//     tramo es el metodo o la propiedad a escribir.
//
// Los tres `create(...)` estaticos son la cara que se usa: envuelven este InvocationHandler en un
// proxy que implementa la interfaz de oyente pedida, asi el llamador puede pasarlo a un
// `addFooListener` sin escribir la clase. Dependen de `java.lang.reflect.Proxy`, que **ahora si**
// esta en este arbol; una version anterior de esta clase los declaraba omitidos por esa falta y esa
// nota quedo vieja.
public class EventHandler implements InvocationHandler {

    private Object target;
    private String action;
    private String eventPropertyName;
    private String listenerMethodName;

    public EventHandler(Object target, String action, String eventPropertyName, String listenerMethodName) {
        if (target == null) {
            throw new NullPointerException("target must be non-null");
        }
        if (action == null) {
            throw new NullPointerException("action must be non-null");
        }
        this.target = target;
        this.action = action;
        this.eventPropertyName = eventPropertyName;
        this.listenerMethodName = listenerMethodName;
    }

    public Object getTarget() {
        return this.target;
    }

    public String getAction() {
        return this.action;
    }

    public String getEventPropertyName() {
        return this.eventPropertyName;
    }

    // A que metodo del oyente reacciona. null significa a todos.
    public String getListenerMethodName() {
        return this.listenerMethodName;
    }

    // La llamada que llega desde el oyente. Si no es el metodo al que este handler reacciona, se
    // contesta lo minimo que la interfaz espera y no se hace nada.
    //
    // Los tres metodos de Object van aparte y no como "el metodo del oyente": con
    // `listenerMethodName == null` este handler reacciona a TODO, y un `hashCode()` sobre el proxy
    // —que es lo que hace cualquier coleccion donde se guarde el oyente— terminaria ejecutando la
    // accion. Se contestan por identidad del proxy, que es lo que un oyente sin estado propio es.
    public Object invoke(Object proxy, Method method, Object[] arguments) {
        Object resultado = null;
        if (method != null) {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                if (name.equals("hashCode")) {
                    resultado = Integer.valueOf(System.identityHashCode(proxy));
                } else if (name.equals("equals")) {
                    resultado = Boolean.valueOf(proxy == arguments[0]);
                } else if (name.equals("toString")) {
                    resultado = proxy.getClass().getName() + '@'
                        + Integer.toHexString(System.identityHashCode(proxy));
                }
            } else if (this.listenerMethodName == null || this.listenerMethodName.equals(name)) {
                resultado = this.aplicar(arguments);
            }
        }
        return resultado;
    }

    private Object aplicar(Object[] arguments) {
        Object resultado = null;
        try {
            // Que pasarle al objetivo: lo que diga eventPropertyName sobre el evento, o nada.
            Object[] args;
            if (this.eventPropertyName == null) {
                args = new Object[0];
            } else {
                Object evento = arguments != null && arguments.length > 0 ? arguments[0] : null;
                args = new Object[] { this.seguirRuta(evento, this.eventPropertyName) };
            }

            // El action tambien puede ser una ruta: se camina hasta el anteultimo tramo y el
            // ultimo es lo que se llama.
            Object destino = this.target;
            String ultimo = this.action;
            int punto = this.action.lastIndexOf('.');
            if (punto >= 0) {
                destino = this.seguirRuta(this.target, this.action.substring(0, punto));
                ultimo = this.action.substring(punto + 1);
            }
            resultado = this.llamar(destino, ultimo, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resultado;
    }

    // Camina "a.b.c" aplicando cada tramo como propiedad de lectura.
    private Object seguirRuta(Object base, String ruta) throws Exception {
        Object actual = base;
        int desde = 0;
        while (desde <= ruta.length() && actual != null) {
            int punto = ruta.indexOf('.', desde);
            String tramo = punto < 0 ? ruta.substring(desde) : ruta.substring(desde, punto);
            if (tramo.length() > 0) {
                actual = this.leerPropiedad(actual, tramo);
            }
            desde = punto < 0 ? ruta.length() + 1 : punto + 1;
        }
        return actual;
    }

    // Lee una propiedad probando `getX`, `isX` y, por ultimo, un metodo que se llame igual.
    private Object leerPropiedad(Object o, String nombre) throws Exception {
        String cap = PropertyDescriptor.capitalizar(nombre);
        Method m = PropertyDescriptor.buscarMetodo(o.getClass(), "get" + cap, 0);
        if (m == null) {
            m = PropertyDescriptor.buscarMetodo(o.getClass(), "is" + cap, 0);
        }
        if (m == null) {
            m = PropertyDescriptor.buscarMetodo(o.getClass(), nombre, 0);
        }
        if (m == null) {
            throw new NoSuchMethodException("No property " + nombre + " on " + o.getClass().getName());
        }
        return m.invoke(o);
    }

    // Llama al metodo del objetivo, aceptando tanto el nombre directo como la forma `setX`.
    private Object llamar(Object destino, String nombre, Object[] args) throws Exception {
        Method elegido = this.buscarCompatible(destino.getClass(), nombre, args);
        if (elegido == null) {
            String cap = "set" + PropertyDescriptor.capitalizar(nombre);
            elegido = this.buscarCompatible(destino.getClass(), cap, args);
        }
        if (elegido == null) {
            throw new NoSuchMethodException("No method " + nombre + " on " + destino.getClass().getName());
        }
        return elegido.invoke(destino, args);
    }

    // Un oyente de `listenerInterface` que, ante cualquiera de sus metodos, le llama `action` al
    // objetivo sin pasarle nada del evento.
    public static <T> T create(Class<T> listenerInterface, Object target, String action) {
        return makeProxy(listenerInterface, target, action, null, null);
    }

    // Igual, pero pasandole al objetivo lo que `eventPropertyName` saque del evento.
    public static <T> T create(Class<T> listenerInterface, Object target, String action,
            String eventPropertyName) {
        return makeProxy(listenerInterface, target, action, eventPropertyName, null);
    }

    // Igual, y ademas solo reacciona al metodo de oyente que se nombre.
    public static <T> T create(Class<T> listenerInterface, Object target, String action,
            String eventPropertyName, String listenerMethodName) {
        return makeProxy(listenerInterface, target, action, eventPropertyName, listenerMethodName);
    }

    // El cargador que se le pide al proxy es el de la interfaz y no el del contexto: la clase que
    // se fabrica tiene que VER a `listenerInterface` para poder implementarla, y el unico cargador
    // del que eso se sabe seguro es el que la cargo a ella.
    private static <T> T makeProxy(Class<T> listenerInterface, Object target, String action,
            String eventPropertyName, String listenerMethodName) {
        if (listenerInterface == null) {
            throw new NullPointerException("listenerInterface must be non-null");
        }
        EventHandler eh = new EventHandler(target, action, eventPropertyName, listenerMethodName);
        Object proxy = Proxy.newProxyInstance(listenerInterface.getClassLoader(),
            new Class<?>[] { listenerInterface }, eh);
        return listenerInterface.cast(proxy);
    }

    private Method buscarCompatible(Class<?> c, String nombre, Object[] args) {
        Method elegido = null;
        Method[] ms = c.getMethods();
        for (int i = 0; i < ms.length; i++) {
            if (elegido == null
                    && ms[i].getName().equals(nombre)
                    && Statement.aceptan(ms[i].getParameterTypes(), args)) {
                elegido = ms[i];
            }
        }
        return elegido;
    }
}
