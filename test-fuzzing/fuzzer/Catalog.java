import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Vuelca, por reflexion sobre el JDK de referencia, que se puede construir y que se puede llamar.
 *
 * Lee una lista de nombres de clase (una por linea) y escribe un JSON con, para cada clase que
 * existe en la referencia: sus constructores publicos, sus metodos estaticos y sus metodos de
 * instancia, con los tipos de sus parametros y su tipo de retorno.
 *
 * Se queda solo con lo que el generador sabe usar:
 *   - nada de `void`: una expresion que no devuelve nada no se puede observar;
 *   - nada de parametros ni retornos que el generador no pueda fabricar ni imprimir;
 *   - nada que sea obviamente del entorno (hilos, archivos, red, reloj, azar), porque el
 *     filtro de determinismo lo descartaria igual y conviene no gastar corridas en eso;
 *   - nada de `toString` ni `hashCode` heredados de Object: llevan el hash de identidad, que es
 *     distinto en cada VM.
 *
 * Lo que NO filtra es la pureza: de eso se encarga el arnes corriendo cada caso dos veces en la
 * referencia. Este catalogo solo dice que es *expresable*.
 *
 *   java Catalog <lista-de-clases.txt> <salida.json>
 */
public class Catalog {

    /** Tipos que el generador sabe fabricar como literal. */
    private static final List<String> LITERAL_TYPES = List.of(
            "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "java.lang.String", "java.lang.CharSequence", "java.lang.Object",
            "int[]", "long[]", "double[]", "char[]", "byte[]", "java.lang.String[]",
            "java.lang.Integer", "java.lang.Long", "java.lang.Double", "java.lang.Boolean",
            "java.lang.Character", "java.lang.Comparable", "java.lang.Number");

    /** Nombres que delatan dependencia del entorno: no se catalogan. */
    private static final List<String> ENVIRONMENT = List.of(
            "java.io.File", "java.io.InputStream", "java.io.OutputStream", "java.io.Reader",
            "java.io.Writer", "java.nio.file.Path", "java.net.Socket", "java.net.URL",
            "java.lang.Thread", "java.lang.Runnable", "java.util.Random", "java.lang.System",
            "java.lang.Runtime", "java.lang.Process", "java.security.SecureRandom",
            "java.util.concurrent", "java.lang.ref", "java.lang.ClassLoader");

    private static String typeName(Class<?> t) {
        return t.isArray() ? typeName(t.getComponentType()) + "[]" : t.getName();
    }

    private static boolean isLiteral(Class<?> t) {
        return LITERAL_TYPES.contains(typeName(t));
    }

    private static boolean isEnvironment(String name) {
        for (String p : ENVIRONMENT) {
            if (name.startsWith(p)) return true;
        }
        return false;
    }

    /** Un tipo sirve de parametro si se fabrica como literal o si es una clase del catalogo. */
    private static boolean isUsableParam(Class<?> t, List<String> catalog) {
        String n = typeName(t);
        return !isEnvironment(n) && (isLiteral(t) || catalog.contains(n));
    }

    /** Un retorno sirve si se puede imprimir o seguir usando. */
    private static boolean isObservable(Class<?> t) {
        String n = typeName(t);
        return !n.equals("void") && !isEnvironment(n);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void signature(StringBuilder b, String owner, String name, Class<?>[] ps, Class<?> ret) {
        b.append("{\"clase\":\"").append(escape(owner)).append("\",\"nombre\":\"").append(escape(name))
         .append("\",\"params\":[");
        for (int i = 0; i < ps.length; i++) {
            b.append(i > 0 ? "," : "").append('"').append(escape(typeName(ps[i]))).append('"');
        }
        b.append("],\"ret\":\"").append(escape(ret == null ? owner : typeName(ret))).append("\"}");
    }

    public static void main(String[] args) throws Exception {
        List<String> asked = Files.readAllLines(Path.of(args[0]));
        List<String> catalog = new ArrayList<>();
        List<Class<?>> classes = new ArrayList<>();
        for (String n : asked) {
            n = n.trim();
            if (n.isEmpty() || isEnvironment(n)) continue;
            try {
                Class<?> c = Class.forName(n, false, Catalog.class.getClassLoader());
                if (!Modifier.isPublic(c.getModifiers()) || c.isAnonymousClass()) continue;
                classes.add(c);
                catalog.add(n);
            } catch (Throwable t) {
                // La clase no existe en la referencia, o no se puede inicializar: no se cataloga.
            }
        }

        StringBuilder ctors = new StringBuilder("[");
        StringBuilder statics = new StringBuilder("[");
        StringBuilder methods = new StringBuilder("[");
        StringBuilder withToString = new StringBuilder("[");
        int nc = 0, ns = 0, nm = 0, nt = 0;

        for (Class<?> c : classes) {
            String cn = typeName(c);
            if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                for (Constructor<?> k : c.getConstructors()) {
                    Class<?>[] ps = k.getParameterTypes();
                    if (ps.length > 3) continue;
                    boolean ok = true;
                    for (Class<?> p : ps) ok &= isUsableParam(p, catalog);
                    if (!ok) continue;
                    ctors.append(nc++ > 0 ? "," : "");
                    signature(ctors, cn, "<init>", ps, null);
                }
            }
            for (Method m : c.getMethods()) {
                if (m.isSynthetic() || m.isBridge()) continue;
                if (!isObservable(m.getReturnType())) continue;
                // `toString` y `hashCode` heredados de Object llevan el hash de identidad, que es
                // distinto en cada VM y hasta en cada corrida. No se catalogan.
                boolean fromObject = m.getDeclaringClass().equals(Object.class);
                if (fromObject && (m.getName().equals("toString") || m.getName().equals("hashCode"))) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length > 3) continue;
                boolean ok = true;
                for (Class<?> p : ps) ok &= isUsableParam(p, catalog);
                if (!ok) continue;
                boolean isStatic = Modifier.isStatic(m.getModifiers());
                if (isStatic && !m.getDeclaringClass().equals(c)) continue;      // heredados: una vez
                StringBuilder where = isStatic ? statics : methods;
                where.append((isStatic ? ns++ : nm++) > 0 ? "," : "");
                signature(where, cn, m.getName(), ps, m.getReturnType());
            }
            try {
                Method ts = c.getMethod("toString");
                if (!ts.getDeclaringClass().equals(Object.class)) {
                    withToString.append(nt++ > 0 ? "," : "").append('"').append(escape(cn)).append('"');
                }
            } catch (NoSuchMethodException e) {
                // sin toString propio: imprimirlo mostraria el hash de identidad
            }
        }

        String json = "{\n  \"clases\": " + classes.size()
                + ",\n  \"constructores\": " + ctors + "]"
                + ",\n  \"estaticos\": " + statics + "]"
                + ",\n  \"metodos\": " + methods + "]"
                + ",\n  \"conToString\": " + withToString + "]\n}\n";
        Files.writeString(Path.of(args[1]), json);
        System.out.println("clases catalogadas: " + classes.size()
                + " · constructores: " + nc + " · estaticos: " + ns + " · metodos: " + nm
                + " · con toString propio: " + nt);
    }
}
