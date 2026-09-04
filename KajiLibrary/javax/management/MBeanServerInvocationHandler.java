package javax.management;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Traduce llamadas Java comunes a operaciones contra un MBean.
 *
 * <p>Es el reverso exacto de {@link StandardMBean}: alla una interfaz mas un objeto se convierten
 * en {@link DynamicMBean}; aca una interfaz mas un {@link ObjectName} se convierten en un objeto
 * que parece implementarla. Las dos usan la misma regla de nombres --`getX`/`isX`/`setX` son
 * atributos, el resto son operaciones--, cada una en un sentido.
 *
 * <p>Que se hace con lo que no es del MBean:
 *
 * <ul>
 *   <li>los tres metodos de `Object` --`equals`, `hashCode`, `toString`-- se contestan <b>aca</b>
 *       y no viajan. Mandar `hashCode()` al agente devolveria el del objeto remoto, que no tiene
 *       nada que ver con el proxy y romperia cualquier `HashMap` local;
 *   <li>los de {@link NotificationEmitter} se reenvian a los de la conexion, que llevan el
 *       `ObjectName` como primer argumento. Es lo que hace que un proxy pedido con
 *       `notificationBroadcaster` sirva de verdad para escuchar.
 * </ul>
 *
 * <h2>El modo MXBean</h2>
 *
 * <p>Con `isMXBean` en `true` el handler <b>convierte</b>: los argumentos van al servidor como tipos
 * abiertos y los resultados vuelven a los tipos Java de la interfaz. Esa es toda la diferencia entre
 * un proxy MBean y uno MXBean, y la hace {@link MXMapeo}.
 *
 * <p>La conversion se resuelve <b>al construir el proxy</b>, no en cada llamada: si algun tipo de la
 * interfaz no se puede mapear, el proxy no se crea. Es el momento en que quien escribe el codigo
 * puede hacer algo al respecto, y evita el peor caso -- un proxy que anda para la mitad de los
 * metodos. Que tipos entran y cuales no esta en la nota de {@link MXMapeo}.
 */
public class MBeanServerInvocationHandler implements InvocationHandler {

    private final MBeanServerConnection conexion;
    private final ObjectName nombre;
    private final boolean mxbean;

    /** Equivale a `isMXBean = false`. */
    public MBeanServerInvocationHandler(MBeanServerConnection connection, ObjectName objectName) {
        this(connection, objectName, false);
    }

    /**
     * @param isMXBean si hay que convertir a tipos abiertos; ver la nota de la clase
     */
    public MBeanServerInvocationHandler(MBeanServerConnection connection, ObjectName objectName,
                                        boolean isMXBean) {
        this.mxbean = isMXBean;
        if (connection == null) {
            throw new IllegalArgumentException("La conexion no puede ser null");
        }
        if (objectName == null) {
            throw new IllegalArgumentException("El ObjectName no puede ser null");
        }
        this.conexion = connection;
        this.nombre = objectName;
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return conexion;
    }

    public ObjectName getObjectName() {
        return nombre;
    }

    /** Si este proxy convierte a tipos abiertos. */
    public boolean isMXBean() {
        return this.mxbean;
    }

    /**
     * Arma el proxy.
     *
     * @param notificationBroadcaster si ademas de `interfaceClass` el proxy tiene que implementar
     *        {@link NotificationEmitter}. Es un `boolean` y no se deduce de la interfaz porque
     *        escuchar notificaciones es independiente de lo que el MBean expone como atributos.
     */
    public static <T> T newProxyInstance(MBeanServerConnection connection, ObjectName objectName,
                                         Class<T> interfaceClass,
                                         boolean notificationBroadcaster) {
        return newProxyInstance(connection, objectName, interfaceClass, notificationBroadcaster,
                                false);
    }

    // La forma general: la de arriba es esta con `mxbean` en false.
    @SuppressWarnings("unchecked")
    static <T> T newProxyInstance(MBeanServerConnection connection, ObjectName objectName,
                                  Class<T> interfaceClass, boolean notificationBroadcaster,
                                  boolean mxbean) {
        if (mxbean) {
            // Se resuelve el mapeo de toda la interfaz ahora: si algo no se puede mapear, el proxy
            // no llega a existir. Ver la nota de la clase.
            MBeanServerInvocationHandler.exigirMapeable(interfaceClass);
        }
        InvocationHandler h = new MBeanServerInvocationHandler(connection, objectName, mxbean);
        Class<?>[] interfaces;
        if (notificationBroadcaster) {
            interfaces = new Class<?>[] { interfaceClass, NotificationEmitter.class };
        } else {
            interfaces = new Class<?>[] { interfaceClass };
        }
        Object p = Proxy.newProxyInstance(interfaceClass.getClassLoader(), interfaces, h);
        return (T) p;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declara = method.getDeclaringClass();
        String nom = method.getName();
        Class<?>[] tipos = method.getParameterTypes();

        if (declara == Object.class) {
            return deObject(proxy, nom, args);
        }
        if (declara == NotificationBroadcaster.class || declara == NotificationEmitter.class) {
            return deNotificaciones(nom, tipos, args);
        }

        try {
            if (tipos.length == 0 && nom.startsWith("get") && nom.length() > 3
                    && method.getReturnType() != Void.TYPE) {
                return aJava(conexion.getAttribute(nombre, nom.substring(3)),
                             method.getReturnType());
            }
            if (tipos.length == 0 && nom.startsWith("is") && nom.length() > 2
                    && method.getReturnType() == Boolean.TYPE) {
                return aJava(conexion.getAttribute(nombre, nom.substring(2)),
                             method.getReturnType());
            }
            if (tipos.length == 1 && nom.startsWith("set") && nom.length() > 3
                    && method.getReturnType() == Void.TYPE) {
                conexion.setAttribute(nombre,
                        new Attribute(nom.substring(3), aAbierto(args[0], tipos[0])));
                return null;
            }
            // La firma que se manda es la de los tipos **abiertos** cuando el proxy es MXBean: es
            // lo que el servidor declara, y mandarle los tipos Java de la interfaz haria que no
            // encontrara la operacion.
            String[] firma = new String[tipos.length];
            Object[] pasados = args == null ? null : new Object[args.length];
            for (int i = 0; i < tipos.length; i++) {
                firma[i] = this.mxbean
                        ? MBeanServerInvocationHandler.nombreAbierto(tipos[i])
                        : tipos[i].getName();
                pasados[i] = aAbierto(args[i], tipos[i]);
            }
            return aJava(conexion.invoke(nombre, nom, pasados, firma), method.getReturnType());
        } catch (MBeanException e) {
            // Se desenvuelve: el que llama al proxy escribio una interfaz Java y espera **su**
            // excepcion, no el sobre en que JMX la transporto.
            throw e.getTargetException();
        } catch (RuntimeMBeanException e) {
            throw e.getTargetException();
        } catch (RuntimeErrorException e) {
            throw e.getTargetError();
        }
    }

    private Object deObject(Object proxy, String nom, Object[] args) {
        if (nom.equals("hashCode")) {
            return Integer.valueOf(nombre.hashCode());
        }
        if (nom.equals("toString")) {
            return getClass().getName() + "[" + nombre + "]";
        }
        // equals: dos proxies son iguales si apuntan al mismo MBean por la misma conexion.
        Object otro = args[0];
        if (otro == null || !Proxy.isProxyClass(otro.getClass())) {
            return Boolean.FALSE;
        }
        InvocationHandler h = Proxy.getInvocationHandler(otro);
        if (!(h instanceof MBeanServerInvocationHandler)) {
            return Boolean.FALSE;
        }
        MBeanServerInvocationHandler q = (MBeanServerInvocationHandler) h;
        return Boolean.valueOf(nombre.equals(q.nombre) && conexion == q.conexion);
    }

    private Object deNotificaciones(String nom, Class<?>[] tipos, Object[] args) throws Exception {
        if (nom.equals("getNotificationInfo")) {
            return conexion.getMBeanInfo(nombre).getNotifications();
        }
        if (nom.equals("addNotificationListener")) {
            conexion.addNotificationListener(nombre, (NotificationListener) args[0],
                                             (NotificationFilter) args[1], args[2]);
            return null;
        }
        if (nom.equals("removeNotificationListener")) {
            if (tipos.length == 1) {
                conexion.removeNotificationListener(nombre, (NotificationListener) args[0]);
            } else {
                conexion.removeNotificationListener(nombre, (NotificationListener) args[0],
                                                     (NotificationFilter) args[1], args[2]);
            }
            return null;
        }
        throw new UnsupportedOperationException(nom);
    }

    // ---- el modo MXBean ----------------------------------------------------------------------

    /**
     * Comprueba que todos los tipos que la interfaz menciona se puedan mapear.
     *
     * @throws IllegalArgumentException si alguno no; el mensaje nombra el metodo y el tipo
     */
    private static void exigirMapeable(Class<?> interfaceClass) {
        for (Method m : interfaceClass.getMethods()) {
            if (m.getDeclaringClass() == Object.class) {
                continue;
            }
            try {
                if (m.getReturnType() != Void.TYPE) {
                    MXMapeo.de(m.getReturnType());
                }
                for (Class<?> p : m.getParameterTypes()) {
                    MXMapeo.de(p);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(interfaceClass.getName() + "." + m.getName()
                        + ": " + e.getMessage());
            }
        }
    }

    /** El valor que va al servidor. */
    private Object aAbierto(Object v, Class<?> tipo) throws Exception {
        if (!this.mxbean || v == null) {
            return v;
        }
        return MXMapeo.de(tipo).aAbierto(v);
    }

    /** El valor que vuelve al que llamo. */
    private Object aJava(Object v, Class<?> tipo) throws Exception {
        if (!this.mxbean || v == null) {
            return v;
        }
        return MXMapeo.de(tipo).aJava(v);
    }

    // El nombre de clase con el que un tipo mapeado viaja en una firma.
    private static String nombreAbierto(Class<?> tipo) {
        MXMapeo m = MXMapeo.de(tipo);
        if (m.esIdentidad()) {
            return tipo.getName();
        }
        javax.management.openmbean.OpenType<?> t = m.tipoAbierto();
        return t == null ? tipo.getName() : t.getClassName();
    }
}
