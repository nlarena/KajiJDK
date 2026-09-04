package java.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Una llamada guardada para ejecutar despues: un objetivo, un nombre de metodo y sus argumentos.
// Es la unidad con la que la persistencia describe "como se rehace este objeto" — un grafo de
// objetos se guarda como la secuencia de llamadas que lo reconstruye, no como sus bytes.
//
// El nombre "new" es especial y significa constructor: `new Statement(Foo.class, "new", args)`
// ejecuta `new Foo(args)`.
//
// La resolucion del metodo no puede ser por descriptor exacto: los argumentos llegan como Object,
// asi que un `int` viene envuelto en Integer y hay que aceptarlo donde se declaro `int`. Por eso
// se recorren los candidatos y se elige el primero cuyos parametros ACEPTAN los argumentos dados.
public class Statement {

    private Object target;
    private String methodName;
    private Object[] arguments;

    public Statement(Object target, String methodName, Object[] arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments == null ? new Object[0] : arguments;
    }

    public Object getTarget() {
        return this.target;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public Object[] getArguments() {
        return this.arguments;
    }

    // Ejecuta la llamada y descarta el resultado. Expression la redefine para quedarselo.
    public void execute() throws Exception {
        this.invocar();
    }

    // El motor compartido con Expression.
    Object invocar() throws Exception {
        if (this.target == null) {
            throw new NullPointerException("target should not be null");
        }
        if (this.methodName == null) {
            throw new NullPointerException("method name should not be null");
        }

        Object resultado;
        if (this.target.getClass().isArray()
                && ("get".equals(this.methodName) || "set".equals(this.methodName))) {
            // Los arreglos no tienen metodos: `get`/`set` sobre un arreglo son acceso indexado.
            // Es el mismo caso especial que hace el JDK, y es el que le permite a la persistencia
            // describir "el elemento 3 de este arreglo" como una llamada mas.
            int indice = ((Integer) this.arguments[0]).intValue();
            if ("get".equals(this.methodName)) {
                resultado = elementoDeArreglo(this.target, indice);
            } else {
                ponerEnArreglo(this.target, indice, this.arguments[1]);
                resultado = null;
            }
        } else if ("new".equals(this.methodName)) {
            if (!(this.target instanceof Class)) {
                throw new NoSuchMethodException("\"new\" needs a Class target");
            }
            resultado = this.construir((Class<?>) this.target);
        } else if (this.target instanceof Class) {
            // Un objetivo Class puede ser tanto "llamar un estatico de esa clase" como "llamar un
            // metodo de la instancia Class". Se prueba primero el estatico, que es lo que quiso
            // decir quien escribio el Statement.
            Method m = this.buscar((Class<?>) this.target, true);
            if (m != null) {
                resultado = m.invoke(null, this.arguments);
            } else {
                Method mc = this.buscar(this.target.getClass(), false);
                if (mc == null) {
                    throw new NoSuchMethodException(this.descripcion());
                }
                resultado = mc.invoke(this.target, this.arguments);
            }
        } else {
            Method m = this.buscar(this.target.getClass(), false);
            if (m == null) {
                throw new NoSuchMethodException(this.descripcion());
            }
            resultado = m.invoke(this.target, this.arguments);
        }
        return resultado;
    }

    private Object construir(Class<?> c) throws Exception {
        // Character es el unico envoltorio sin constructor desde String. La persistencia describe
        // a todos los envoltorios igual —`new Integer("7")`, `new Boolean("true")`—, asi que en vez
        // de darle a Character un delegado aparte se finge aca ese constructor que no existe. Es el
        // mismo remiendo que hace el JDK y en el mismo lugar: si no estuviera, todo bean con una
        // propiedad `char` fallaria al releer su propio valor.
        if (c == Character.class && this.arguments.length == 1
                && this.arguments[0] instanceof String) {
            String s = (String) this.arguments[0];
            if (s.length() == 0) {
                throw new IllegalArgumentException("empty string for Character");
            }
            return Character.valueOf(s.charAt(0));
        }
        Constructor<?>[] cs = c.getConstructors();
        Constructor<?> elegido = null;
        for (int i = 0; i < cs.length; i++) {
            if (elegido == null && aceptan(cs[i].getParameterTypes(), this.arguments)) {
                elegido = cs[i];
            }
        }
        if (elegido == null) {
            throw new NoSuchMethodException(this.descripcion());
        }
        return elegido.newInstance(this.arguments);
    }

    // El primer metodo publico con ese nombre cuyos parametros aceptan los argumentos.
    private Method buscar(Class<?> c, boolean soloEstaticos) {
        Method elegido = null;
        Method[] ms = c.getMethods();
        for (int i = 0; i < ms.length; i++) {
            Method m = ms[i];
            if (elegido == null
                    && m.getName().equals(this.methodName)
                    && Modifier.isStatic(m.getModifiers()) == soloEstaticos
                    && aceptan(m.getParameterTypes(), this.arguments)) {
                elegido = m;
            }
        }
        return elegido;
    }

    private String descripcion() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.methodName).append('(');
        for (int i = 0; i < this.arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.arguments[i] == null ? "null" : this.arguments[i].getClass().getName());
        }
        sb.append(')');
        return sb.toString();
    }

    // Si esos parametros declarados admiten esos argumentos.
    static boolean aceptan(Class<?>[] params, Object[] args) {
        boolean ok = params.length == args.length;
        for (int i = 0; ok && i < params.length; i++) {
            ok = acepta(params[i], args[i]);
        }
        return ok;
    }

    // Un parametro primitivo acepta su envoltorio y nada mas —ni siquiera null—; un parametro de
    // referencia acepta null y cualquier instancia suya.
    static boolean acepta(Class<?> param, Object arg) {
        boolean ok;
        if (param.isPrimitive()) {
            ok = arg != null && envoltorioDe(param) == arg.getClass();
        } else if (arg == null) {
            ok = true;
        } else {
            ok = param.isAssignableFrom(arg.getClass());
        }
        return ok;
    }

    // Los tres accesos a arreglo van por despacho de tipo y no por `java.lang.reflect.Array`: en
    // esta VM `Array.get`, `Array.set` y `Array.getLength` son `native` sin implementacion
    // registrada y tiran UnsatisfiedLinkError. Con el despacho explicito la persistencia de
    // arreglos funciona igual y no depende de nativos que no estan.
    static int largoDeArreglo(Object a) {
        int n;
        if (a instanceof Object[]) { n = ((Object[]) a).length; }
        else if (a instanceof int[]) { n = ((int[]) a).length; }
        else if (a instanceof boolean[]) { n = ((boolean[]) a).length; }
        else if (a instanceof byte[]) { n = ((byte[]) a).length; }
        else if (a instanceof char[]) { n = ((char[]) a).length; }
        else if (a instanceof short[]) { n = ((short[]) a).length; }
        else if (a instanceof long[]) { n = ((long[]) a).length; }
        else if (a instanceof float[]) { n = ((float[]) a).length; }
        else if (a instanceof double[]) { n = ((double[]) a).length; }
        else { throw new IllegalArgumentException("Argument is not an array"); }
        return n;
    }

    static Object elementoDeArreglo(Object a, int i) {
        Object v;
        if (a instanceof Object[]) { v = ((Object[]) a)[i]; }
        else if (a instanceof int[]) { v = Integer.valueOf(((int[]) a)[i]); }
        else if (a instanceof boolean[]) { v = Boolean.valueOf(((boolean[]) a)[i]); }
        else if (a instanceof byte[]) { v = Byte.valueOf(((byte[]) a)[i]); }
        else if (a instanceof char[]) { v = Character.valueOf(((char[]) a)[i]); }
        else if (a instanceof short[]) { v = Short.valueOf(((short[]) a)[i]); }
        else if (a instanceof long[]) { v = Long.valueOf(((long[]) a)[i]); }
        else if (a instanceof float[]) { v = Float.valueOf(((float[]) a)[i]); }
        else if (a instanceof double[]) { v = Double.valueOf(((double[]) a)[i]); }
        else { throw new IllegalArgumentException("Argument is not an array"); }
        return v;
    }

    static void ponerEnArreglo(Object a, int i, Object v) {
        if (a instanceof Object[]) { ((Object[]) a)[i] = v; }
        else if (a instanceof int[]) { ((int[]) a)[i] = ((Number) v).intValue(); }
        else if (a instanceof boolean[]) { ((boolean[]) a)[i] = ((Boolean) v).booleanValue(); }
        else if (a instanceof byte[]) { ((byte[]) a)[i] = ((Number) v).byteValue(); }
        else if (a instanceof char[]) { ((char[]) a)[i] = ((Character) v).charValue(); }
        else if (a instanceof short[]) { ((short[]) a)[i] = ((Number) v).shortValue(); }
        else if (a instanceof long[]) { ((long[]) a)[i] = ((Number) v).longValue(); }
        else if (a instanceof float[]) { ((float[]) a)[i] = ((Number) v).floatValue(); }
        else if (a instanceof double[]) { ((double[]) a)[i] = ((Number) v).doubleValue(); }
        else { throw new IllegalArgumentException("Argument is not an array"); }
    }

    // El camino inverso de envoltorioDe: null si la clase no es un envoltorio.
    static Class<?> primitivoDelEnvoltorio(Class<?> envoltorio) {
        Class<?> r = null;
        if (envoltorio == Integer.class) { r = int.class; }
        else if (envoltorio == Boolean.class) { r = boolean.class; }
        else if (envoltorio == Long.class) { r = long.class; }
        else if (envoltorio == Double.class) { r = double.class; }
        else if (envoltorio == Float.class) { r = float.class; }
        else if (envoltorio == Short.class) { r = short.class; }
        else if (envoltorio == Byte.class) { r = byte.class; }
        else if (envoltorio == Character.class) { r = char.class; }
        return r;
    }

    static Class<?> envoltorioDe(Class<?> primitivo) {
        Class<?> r = null;
        if (primitivo == int.class) { r = Integer.class; }
        else if (primitivo == boolean.class) { r = Boolean.class; }
        else if (primitivo == long.class) { r = Long.class; }
        else if (primitivo == double.class) { r = Double.class; }
        else if (primitivo == float.class) { r = Float.class; }
        else if (primitivo == short.class) { r = Short.class; }
        else if (primitivo == byte.class) { r = Byte.class; }
        else if (primitivo == char.class) { r = Character.class; }
        return r;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.target == null ? "null" : EventSetDescriptor.nombreSimple(this.target.getClass()));
        sb.append('.').append(this.methodName).append('(');
        for (int i = 0; i < this.arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.arguments[i]);
        }
        sb.append(");");
        return sb.toString();
    }
}
