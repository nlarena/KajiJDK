package java.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Descubre la forma de un bean mirando sus metodos publicos: que propiedades tiene, que eventos
// dispara, y que metodos vale la pena exponer.
//
// Todas las reglas de abajo estan VERIFICADAS contra el JDK real (H:/jdk-25.0.2) con un bean que
// toca cada caso dudoso a la vez; no son deducciones del contrato escrito. Las que mas se suelen
// implementar mal:
//
//  - **decapitalize NO baja la primera letra siempre.** Si las dos primeras son mayusculas se deja
//    el nombre tal cual: `getURL` da la propiedad "URL", no "uRL". `getXCoord` da "XCoord", porque
//    X y C son las dos mayusculas. `getX` si da "x".
//  - **`is` vale solo para el boolean primitivo.** `isEnvuelto()` devolviendo `Boolean` NO es un
//    getter; la propiedad queda de solo escritura si tiene setter.
//  - **El setter tiene que devolver void.** `String setRaro(String)` no engancha, y la propiedad
//    "raro" directamente no existe.
//  - **Un setter con tipo que no coincide se descarta, y gana el getter.** `getDesparejo():String`
//    con `setDesparejo(int)` da una propiedad String de solo lectura, no dos propiedades ni un error.
//  - **Los estaticos no cuentan.** Un `public static String getEstatico()` no produce propiedad.
//  - **Una propiedad solo indexada tiene tipo null.** Ver IndexedPropertyDescriptor.
//  - **Las propiedades salen ordenadas por nombre.**
//  - **La propiedad "class" aparece** (de Object.getClass) salvo que se corte con un stopClass.
//  - `bound` se prende cuando el bean tiene addPropertyChangeListener; `constrained` NO se deduce
//    de addVetoableChangeListener.
//
// **Las anotaciones NO se leen.** `@BeanProperty`, `@JavaBean` y `@Transient` estan declaradas en
// el paquete, pero este Introspector no las consulta, y es deliberado: en este arbol el javac
// pierde `@Retention(RUNTIME)` cuando el tipo anotacion viene del classpath, asi que en tiempo de
// ejecucion no se ven; y ademas `Method.invoke` sobre la instancia de una anotacion voltea la VM.
// Un Introspector que dijera leerlas y no pudiera daria descriptores incompletos sin avisar, que
// es peor que no leerlas y decirlo.
public class Introspector {

    public static final int USE_ALL_BEANINFO = 1;
    public static final int IGNORE_IMMEDIATE_BEANINFO = 2;
    public static final int IGNORE_ALL_BEANINFO = 3;

    private static String[] searchPath = new String[] { "sun.beans.infos" };

    // Cache de lo ya introspeccionado, con la clave incluyendo stopClass y flags: el mismo bean
    // cortado en distinto lugar da distinto resultado.
    private static Map<String, BeanInfo> cache = new HashMap<String, BeanInfo>();

    private Introspector() {
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        return getBeanInfo(beanClass, null, USE_ALL_BEANINFO);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, int flags) throws IntrospectionException {
        return getBeanInfo(beanClass, null, flags);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass)
            throws IntrospectionException {
        return getBeanInfo(beanClass, stopClass, USE_ALL_BEANINFO);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass, int flags)
            throws IntrospectionException {
        if (beanClass == null) {
            throw new IntrospectionException("null bean class");
        }
        String clave = beanClass.getName() + "|" + (stopClass == null ? "-" : stopClass.getName()) + "|" + flags;
        BeanInfo bi = leerCache(clave);
        if (bi == null) {
            bi = analizar(beanClass, stopClass, flags);
            guardarCache(clave, bi);
        }
        return bi;
    }

    private static synchronized BeanInfo leerCache(String clave) {
        return cache.get(clave);
    }

    private static synchronized void guardarCache(String clave, BeanInfo bi) {
        cache.put(clave, bi);
    }

    public static synchronized void flushCaches() {
        cache.clear();
    }

    // Tira la entrada de una clase. La clave lleva el nombre adelante, asi que se barren todas las
    // combinaciones de stopClass/flags de esa clase.
    public static synchronized void flushFromCaches(Class<?> clz) {
        if (clz == null) {
            throw new NullPointerException();
        }
        String prefijo = clz.getName() + "|";
        Object[] claves = cache.keySet().toArray();
        for (int i = 0; i < claves.length; i++) {
            String c = (String) claves[i];
            if (c.startsWith(prefijo)) {
                cache.remove(c);
            }
        }
    }

    public static synchronized String[] getBeanInfoSearchPath() {
        String[] r = new String[searchPath.length];
        for (int i = 0; i < searchPath.length; i++) {
            r[i] = searchPath[i];
        }
        return r;
    }

    public static synchronized void setBeanInfoSearchPath(String[] path) {
        if (path == null) {
            searchPath = new String[0];
        } else {
            String[] r = new String[path.length];
            for (int i = 0; i < path.length; i++) {
                r[i] = path[i];
            }
            searchPath = r;
        }
    }

    // La regla de nombre que casi todo el mundo implementa mal. Verificada contra el JDK real:
    //   "URL" -> "URL"      "Name"   -> "name"    "XCoord" -> "XCoord"
    //   "X"   -> "x"        "aB"     -> "aB"      "ABc"    -> "ABc"
    // El caso raro —dos mayusculas al principio se dejan— existe para que las siglas sobrevivan:
    // bajar solo la primera letra de "URL" daria "uRL", que no es el nombre de nada.
    public static String decapitalize(String name) {
        String r = name;
        if (name != null && name.length() != 0) {
            boolean dosMayusculas = name.length() > 1
                && Character.isUpperCase(name.charAt(1))
                && Character.isUpperCase(name.charAt(0));
            if (!dosMayusculas) {
                char[] c = name.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                r = new String(c);
            }
        }
        return r;
    }

    // --- el analisis ----------------------------------------------------------------

    private static BeanInfo analizar(Class<?> beanClass, Class<?> stopClass, int flags)
            throws IntrospectionException {

        // Los metodos a considerar: los publicos de instancia declarados entre beanClass y
        // stopClass (sin incluir stopClass). Sin stopClass entran tambien los de Object, que es
        // de donde sale la propiedad "class".
        List<Method> metodos = metodosVisibles(beanClass, stopClass);

        List<PropertyDescriptor> props = descubrirPropiedades(metodos, beanClass);
        List<EventSetDescriptor> eventos = descubrirEventos(metodos);

        MethodDescriptor[] mds = new MethodDescriptor[metodos.size()];
        for (int i = 0; i < metodos.size(); i++) {
            mds[i] = new MethodDescriptor(metodos.get(i));
        }

        PropertyDescriptor[] pds = new PropertyDescriptor[props.size()];
        for (int i = 0; i < props.size(); i++) {
            pds[i] = props.get(i);
        }
        EventSetDescriptor[] esds = new EventSetDescriptor[eventos.size()];
        for (int i = 0; i < eventos.size(); i++) {
            esds[i] = eventos.get(i);
        }

        BeanInfo explicito = buscarBeanInfoExplicito(beanClass, flags);
        return new BeanInfoGenerico(new BeanDescriptor(beanClass), pds, esds, mds, explicito);
    }

    // Recorre la jerarquia desde beanClass hacia arriba, parando en stopClass. Se toman los
    // declarados de cada nivel y se filtran a publicos de instancia; asi un metodo redefinido
    // aparece una sola vez, con la version mas derivada.
    private static List<Method> metodosVisibles(Class<?> beanClass, Class<?> stopClass) {
        List<Method> salida = new ArrayList<Method>();
        List<String> vistos = new ArrayList<String>();
        Class<?> c = beanClass;
        while (c != null && c != stopClass) {
            Method[] ms = c.getDeclaredMethods();
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                int mods = m.getModifiers();
                if (Modifier.isPublic(mods) && !Modifier.isStatic(mods) && !m.isSynthetic()) {
                    String firma = firmaDe(m);
                    if (!vistos.contains(firma)) {
                        vistos.add(firma);
                        salida.add(m);
                    }
                }
            }
            c = c.getSuperclass();
        }
        return salida;
    }

    private static String firmaDe(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getName()).append('(');
        Class<?>[] args = m.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].getName()).append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    // El descubrimiento de propiedades: se clasifica cada metodo en uno de los cuatro roles y
    // recien al final se arman los descriptores, porque el tipo de una propiedad depende de
    // haberlos visto a todos.
    private static List<PropertyDescriptor> descubrirPropiedades(List<Method> metodos, Class<?> beanClass)
            throws IntrospectionException {

        List<String> nombres = new ArrayList<String>();
        List<Method> lectores = new ArrayList<Method>();
        List<Method> escritores = new ArrayList<Method>();
        List<Method> lectoresIdx = new ArrayList<Method>();
        List<Method> escritoresIdx = new ArrayList<Method>();

        for (int i = 0; i < metodos.size(); i++) {
            Method m = metodos.get(i);
            String nombreMetodo = m.getName();
            Class<?>[] args = m.getParameterTypes();
            Class<?> ret = m.getReturnType();

            String prop = null;
            int rol = -1;   // 0 lector, 1 escritor, 2 lector indexado, 3 escritor indexado

            if (nombreMetodo.startsWith("get") && nombreMetodo.length() > 3) {
                if (args.length == 0 && ret != void.class) {
                    prop = decapitalize(nombreMetodo.substring(3));
                    rol = 0;
                } else if (args.length == 1 && args[0] == int.class && ret != void.class) {
                    prop = decapitalize(nombreMetodo.substring(3));
                    rol = 2;
                }
            } else if (nombreMetodo.startsWith("is") && nombreMetodo.length() > 2) {
                // Solo el boolean primitivo. `Boolean` NO califica: verificado contra el JDK real.
                if (args.length == 0 && ret == boolean.class) {
                    prop = decapitalize(nombreMetodo.substring(2));
                    rol = 0;
                }
            } else if (nombreMetodo.startsWith("set") && nombreMetodo.length() > 3) {
                // El escritor tiene que devolver void: verificado contra el JDK real.
                if (ret == void.class) {
                    if (args.length == 1) {
                        prop = decapitalize(nombreMetodo.substring(3));
                        rol = 1;
                    } else if (args.length == 2 && args[0] == int.class) {
                        prop = decapitalize(nombreMetodo.substring(3));
                        rol = 3;
                    }
                }
            }

            if (prop != null && prop.length() > 0) {
                int idx = nombres.indexOf(prop);
                if (idx < 0) {
                    nombres.add(prop);
                    lectores.add(null);
                    escritores.add(null);
                    lectoresIdx.add(null);
                    escritoresIdx.add(null);
                    idx = nombres.size() - 1;
                }
                if (rol == 0 && lectores.get(idx) == null) {
                    lectores.set(idx, m);
                } else if (rol == 1 && escritores.get(idx) == null) {
                    escritores.set(idx, m);
                } else if (rol == 2 && lectoresIdx.get(idx) == null) {
                    lectoresIdx.set(idx, m);
                } else if (rol == 3 && escritoresIdx.get(idx) == null) {
                    escritoresIdx.set(idx, m);
                }
            }
        }

        boolean ligadas = PropertyDescriptor.buscarMetodo(beanClass, "addPropertyChangeListener", 1) != null;

        List<PropertyDescriptor> salida = new ArrayList<PropertyDescriptor>();
        for (int i = 0; i < nombres.size(); i++) {
            PropertyDescriptor pd = armar(nombres.get(i), lectores.get(i), escritores.get(i),
                                          lectoresIdx.get(i), escritoresIdx.get(i));
            if (pd != null) {
                pd.setBound(ligadas);
                salida.add(pd);
            }
        }
        ordenarPorNombre(salida);
        return salida;
    }

    // Arma el descriptor de una propiedad conciliando los cuatro accesores posibles. Aca viven las
    // reglas de emparejado, que son las que deciden si algo es propiedad y de que tipo.
    private static PropertyDescriptor armar(String nombre, Method lector, Method escritor,
                                            Method lectorIdx, Method escritorIdx) {
        PropertyDescriptor pd = null;

        // El tipo no indexado lo fija el lector si esta; si no, el escritor.
        Method l = lector;
        Method e = escritor;
        if (l != null && e != null) {
            // Tipos que no cierran: se descarta el escritor y gana el lector. Verificado contra el
            // JDK real con getDesparejo():String / setDesparejo(int).
            if (l.getReturnType() != e.getParameterTypes()[0]) {
                e = null;
            }
        }

        boolean hayIdx = lectorIdx != null || escritorIdx != null;
        if (hayIdx) {
            Class<?> tipoIdx = null;
            Method li = lectorIdx;
            Method ei = escritorIdx;
            if (li != null) {
                tipoIdx = li.getReturnType();
            }
            if (ei != null) {
                Class<?> t = ei.getParameterTypes()[1];
                if (tipoIdx == null) {
                    tipoIdx = t;
                } else if (tipoIdx != t) {
                    ei = null;
                }
            }
            // Si tambien hay accesores de arreglo, el componente tiene que coincidir; si no, la
            // mitad no indexada no pertenece a esta propiedad.
            Class<?> tipoArreglo = null;
            if (l != null) {
                tipoArreglo = l.getReturnType();
            } else if (e != null) {
                tipoArreglo = e.getParameterTypes()[0];
            }
            if (tipoArreglo != null) {
                if (!tipoArreglo.isArray() || tipoArreglo.getComponentType() != tipoIdx) {
                    l = null;
                    e = null;
                }
            }
            pd = new IndexedPropertyDescriptor(nombre, l, e, li, ei, true);
        } else if (l != null || e != null) {
            pd = new PropertyDescriptor(nombre, l, e, true);
        }
        return pd;
    }

    // Inserción directa: son pocas propiedades y evita depender de un sort de Object[], que en
    // este arbol java.util.Arrays no ofrece (solo tiene los primitivos).
    private static void ordenarPorNombre(List<PropertyDescriptor> l) {
        for (int i = 1; i < l.size(); i++) {
            PropertyDescriptor actual = l.get(i);
            int j = i - 1;
            while (j >= 0 && l.get(j).getName().compareTo(actual.getName()) > 0) {
                l.set(j + 1, l.get(j));
                j = j - 1;
            }
            l.set(j + 1, actual);
        }
    }

    // Los conjuntos de eventos: pares add/remove que toman un oyente. El sufijo "Listener" en el
    // nombre del tipo es obligatorio — verificado contra el JDK real, un `addBarOyente(BarOyente)`
    // con BarOyente extendiendo EventListener no produce nada.
    private static List<EventSetDescriptor> descubrirEventos(List<Method> metodos)
            throws IntrospectionException {

        List<String> nombres = new ArrayList<String>();
        List<Class<?>> tipos = new ArrayList<Class<?>>();
        List<Method> adds = new ArrayList<Method>();
        List<Method> removes = new ArrayList<Method>();

        for (int i = 0; i < metodos.size(); i++) {
            Method m = metodos.get(i);
            String n = m.getName();
            Class<?>[] args = m.getParameterTypes();
            boolean esAdd = n.startsWith("add") && n.length() > 3;
            boolean esRemove = n.startsWith("remove") && n.length() > 6;
            if ((esAdd || esRemove) && args.length == 1 && m.getReturnType() == void.class) {
                Class<?> tipo = args[0];
                if (java.util.EventListener.class.isAssignableFrom(tipo)) {
                    String simple = EventSetDescriptor.nombreSimple(tipo);
                    String sufijoDelMetodo = esAdd ? n.substring(3) : n.substring(6);
                    if (simple.endsWith("Listener") && simple.equals(sufijoDelMetodo)) {
                        String evento = decapitalize(simple.substring(0, simple.length() - 8));
                        int idx = nombres.indexOf(evento);
                        if (idx < 0) {
                            nombres.add(evento);
                            tipos.add(tipo);
                            adds.add(null);
                            removes.add(null);
                            idx = nombres.size() - 1;
                        }
                        if (esAdd) {
                            adds.set(idx, m);
                        } else {
                            removes.set(idx, m);
                        }
                    }
                }
            }
        }

        List<EventSetDescriptor> salida = new ArrayList<EventSetDescriptor>();
        for (int i = 0; i < nombres.size(); i++) {
            // Hacen falta los dos: poder suscribirse y no poder desuscribirse no es un conjunto
            // de eventos utilizable.
            if (adds.get(i) != null && removes.get(i) != null) {
                Class<?> tipo = tipos.get(i);
                Method[] delOyente = metodosDelOyente(tipo);
                salida.add(new EventSetDescriptor(nombres.get(i), tipo, delOyente,
                                                  adds.get(i), removes.get(i)));
            }
        }
        return salida;
    }

    private static Method[] metodosDelOyente(Class<?> tipo) {
        Method[] todos = tipo.getMethods();
        int n = 0;
        for (int i = 0; i < todos.length; i++) {
            if (!Modifier.isStatic(todos[i].getModifiers())) {
                n = n + 1;
            }
        }
        Method[] r = new Method[n];
        int k = 0;
        for (int i = 0; i < todos.length; i++) {
            if (!Modifier.isStatic(todos[i].getModifiers())) {
                r[k] = todos[i];
                k = k + 1;
            }
        }
        return r;
    }

    // Busca la clase `<Bean>BeanInfo` al lado del bean. Si no esta —el caso normal— se devuelve
    // null y todo sale de la reflexion.
    private static BeanInfo buscarBeanInfoExplicito(Class<?> beanClass, int flags) {
        BeanInfo bi = null;
        if (flags != IGNORE_ALL_BEANINFO && flags != IGNORE_IMMEDIATE_BEANINFO) {
            try {
                Class<?> c = Class.forName(beanClass.getName() + "BeanInfo");
                Object o = c.newInstance();
                if (o instanceof BeanInfo) {
                    bi = (BeanInfo) o;
                }
            } catch (Throwable noHay) {
                bi = null;
            }
        }
        return bi;
    }
}
