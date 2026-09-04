import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Prueba de comportamiento de java.lang.reflect.Proxy, escrita para correr **igual** en esta VM y
 * en el JDK real.
 *
 * <p>Cada comprobacion tiene un indice. {@code run()} devuelve -1 si pasaron todas, o el indice de
 * la primera que fallo: un solo int alcanza para comparar las dos VMs sin depender de que la salida
 * por consola coincida caracter por caracter.
 *
 * <p>Ninguna comprobacion mira el nombre ni el paquete de la clase generada: el JDK los cambio dos
 * veces (com.sun.proxy, despues jdk.proxy1) y no son parte del contrato.
 */
public class ProxyTest {

    // ---- interfaces de prueba ----

    public interface Saludo {
        String hola();
    }

    public interface Prims {
        int enteros(int a, long b, double c, boolean d);
        char masPrims(char a, byte b, short c, float d);
        void nada();
        long largo();
        double doble();
        float flotante();
        boolean bool();
        byte octeto();
        short corto();
        char caracter();
    }

    /** Declara `nombre()` primero: el Method que le llega al manejador tiene que ser el de aca. */
    public interface PrimeraA {
        String nombre();
    }

    public interface SegundaB {
        String nombre();
    }

    /** Retornos covariantes: `Object` y `String` son compatibles, los dos metodos se generan. */
    public interface CovarObj {
        Object valor();
    }

    public interface CovarStr {
        String valor();
    }

    /** Retornos incompatibles: ni String es Integer ni al reves. */
    public interface MalA {
        String choque();
    }

    public interface MalB {
        Integer choque();
    }

    public interface Cheque {
        void declarada() throws java.io.IOException;
        void noDeclarada();
    }

    public interface Arreglos {
        int[] duplicar(int[] entrada);
        String[][] matriz(String[] fila);
    }

    public interface ConDefault {
        String base();
        default String derivado() {
            return "default:" + this.base();
        }
    }

    /** Una interfaz que hereda su metodo: el Method que llega es el de quien lo DECLARA. */
    public interface Base {
        String heredado();
    }

    public interface Derivada extends Base {
        String propio();
    }

    /** Generica: el compilador le pone un puente `cmp(Object)` al lado de `cmp(Integer)`. */
    public interface Comparadora<T> {
        int cmp(T x);
    }

    public interface CompInt extends Comparadora<Integer> {
    }

    /** Redeclara `toString()`: misma firma completa que la de Object, no se duplica. */
    public interface ConToString {
        String toString();
    }

    /** No publica: la clase generada tiene que caer en este mismo paquete y no ser publica. */
    interface Reservada {
        String secreto();
    }

    // ---- manejadores ----

    /** Guarda lo ultimo que recibio y devuelve lo que se le programo. */
    static final class Espia implements InvocationHandler {
        Object respuesta;
        Throwable tirar;
        Object ultimoProxy;
        Method ultimoMetodo;
        Object[] ultimosArgs;

        public Object invoke(Object proxy, Method metodo, Object[] args) throws Throwable {
            this.ultimoProxy = proxy;
            this.ultimoMetodo = metodo;
            this.ultimosArgs = args;
            if (this.tirar != null) {
                Throwable t = this.tirar;
                this.tirar = null;
                throw t;
            }
            return this.respuesta;
        }
    }

    private static Object nuevo(Espia e, Class<?>[] ifaces) {
        return Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), ifaces, e);
    }

    public static int run() {
        Espia e = new Espia();

        // 0: la llamada llega al manejador y su retorno sale por el metodo.
        e.respuesta = "che";
        Object p = nuevo(e, new Class<?>[] { Saludo.class });
        if (!"che".equals(((Saludo) p).hola())) {
            return 0;
        }

        // 1: el proxy ES la interfaz.
        if (!(p instanceof Saludo)) {
            return 1;
        }

        // 2 y 3: la clase se reconoce como proxy y devuelve su manejador.
        if (!Proxy.isProxyClass(p.getClass())) {
            return 2;
        }
        if (Proxy.getInvocationHandler(p) != e) {
            return 3;
        }

        // 4: una clase cualquiera no es proxy.
        if (Proxy.isProxyClass(String.class)) {
            return 4;
        }

        // 5: un metodo sin parametros recibe `null`, no un arreglo vacio.
        if (e.ultimosArgs != null) {
            return 5;
        }

        // 6: el proxy que llega al manejador es el mismo objeto.
        if (e.ultimoProxy != p) {
            return 6;
        }

        // 7: el Method es el de la interfaz que lo declara.
        if (e.ultimoMetodo.getDeclaringClass() != Saludo.class) {
            return 7;
        }
        if (!"hola".equals(e.ultimoMetodo.getName())) {
            return 8;
        }

        // 9..14: los primitivos se boxean al entrar y se desboxean al salir.
        Espia ep = new Espia();
        Prims pr = (Prims) nuevo(ep, new Class<?>[] { Prims.class });
        ep.respuesta = Integer.valueOf(42);
        if (pr.enteros(1, 2L, 3.5d, true) != 42) {
            return 9;
        }
        Object[] a = ep.ultimosArgs;
        if (a == null || a.length != 4) {
            return 10;
        }
        if (!(a[0] instanceof Integer) || ((Integer) a[0]).intValue() != 1) {
            return 11;
        }
        if (!(a[1] instanceof Long) || ((Long) a[1]).longValue() != 2L) {
            return 12;
        }
        if (!(a[2] instanceof Double) || ((Double) a[2]).doubleValue() != 3.5d) {
            return 13;
        }
        if (!(a[3] instanceof Boolean) || !((Boolean) a[3]).booleanValue()) {
            return 14;
        }

        ep.respuesta = Character.valueOf('z');
        if (pr.masPrims('q', (byte) 7, (short) 300, 1.5f) != 'z') {
            return 15;
        }
        a = ep.ultimosArgs;
        if (!(a[0] instanceof Character) || ((Character) a[0]).charValue() != 'q') {
            return 16;
        }
        if (!(a[1] instanceof Byte) || ((Byte) a[1]).byteValue() != (byte) 7) {
            return 17;
        }
        if (!(a[2] instanceof Short) || ((Short) a[2]).shortValue() != (short) 300) {
            return 18;
        }
        if (!(a[3] instanceof Float) || ((Float) a[3]).floatValue() != 1.5f) {
            return 19;
        }

        // 20: un metodo void descarta lo que el manejador devuelva, aunque no sea null.
        ep.respuesta = "basura";
        pr.nada();
        if (!"nada".equals(ep.ultimoMetodo.getName())) {
            return 20;
        }

        // 21..27: cada retorno primitivo por separado.
        ep.respuesta = Long.valueOf(9000000000L);
        if (pr.largo() != 9000000000L) {
            return 21;
        }
        ep.respuesta = Double.valueOf(2.25d);
        if (pr.doble() != 2.25d) {
            return 22;
        }
        ep.respuesta = Float.valueOf(0.5f);
        if (pr.flotante() != 0.5f) {
            return 23;
        }
        ep.respuesta = Boolean.TRUE;
        if (!pr.bool()) {
            return 24;
        }
        ep.respuesta = Byte.valueOf((byte) -3);
        if (pr.octeto() != (byte) -3) {
            return 25;
        }
        ep.respuesta = Short.valueOf((short) -300);
        if (pr.corto() != (short) -300) {
            return 26;
        }
        ep.respuesta = Character.valueOf('k');
        if (pr.caracter() != 'k') {
            return 27;
        }

        // 28..33: hashCode, equals y toString van al manejador, con los Method de Object.
        Espia eo = new Espia();
        Object po = nuevo(eo, new Class<?>[] { Saludo.class });
        eo.respuesta = "yo soy el proxy";
        if (!"yo soy el proxy".equals(po.toString())) {
            return 28;
        }
        if (eo.ultimoMetodo.getDeclaringClass() != Object.class) {
            return 29;
        }
        if (!"toString".equals(eo.ultimoMetodo.getName())) {
            return 30;
        }
        eo.respuesta = Integer.valueOf(777);
        if (po.hashCode() != 777) {
            return 31;
        }
        if (eo.ultimoMetodo.getDeclaringClass() != Object.class
                || !"hashCode".equals(eo.ultimoMetodo.getName())) {
            return 32;
        }
        eo.respuesta = Boolean.TRUE;
        if (!po.equals("cualquiera")) {
            return 33;
        }
        if (eo.ultimoMetodo.getDeclaringClass() != Object.class
                || !"equals".equals(eo.ultimoMetodo.getName())
                || eo.ultimosArgs == null || eo.ultimosArgs.length != 1
                || !"cualquiera".equals(eo.ultimosArgs[0])) {
            return 34;
        }

        // 35: el mismo metodo en dos interfaces se implementa una vez; el Method es el de la
        // primera que lo declara.
        Espia ed = new Espia();
        ed.respuesta = "uno";
        Object pd = nuevo(ed, new Class<?>[] { PrimeraA.class, SegundaB.class });
        ((SegundaB) pd).nombre();
        if (ed.ultimoMetodo.getDeclaringClass() != PrimeraA.class) {
            return 35;
        }
        ((PrimeraA) pd).nombre();
        if (ed.ultimoMetodo.getDeclaringClass() != PrimeraA.class) {
            return 36;
        }

        // 37: retornos covariantes -> las dos firmas existen y las dos despachan.
        Espia ec = new Espia();
        ec.respuesta = "cov";
        Object pc = nuevo(ec, new Class<?>[] { CovarObj.class, CovarStr.class });
        if (!"cov".equals(((CovarStr) pc).valor())) {
            return 37;
        }
        if (!"cov".equals(((CovarObj) pc).valor())) {
            return 38;
        }

        // 39: retornos incompatibles -> newProxyInstance falla.
        try {
            nuevo(new Espia(), new Class<?>[] { MalA.class, MalB.class });
            return 39;
        } catch (IllegalArgumentException ok) {
            // esperado
        }

        // 40: null para un metodo de retorno primitivo -> NullPointerException.
        Espia en = new Espia();
        Prims pn = (Prims) nuevo(en, new Class<?>[] { Prims.class });
        en.respuesta = null;
        try {
            pn.largo();
            return 40;
        } catch (NullPointerException ok) {
            // esperado
        }

        // 41: tipo incompatible -> ClassCastException.
        Espia et = new Espia();
        Saludo ps = (Saludo) nuevo(et, new Class<?>[] { Saludo.class });
        et.respuesta = Integer.valueOf(5);
        try {
            ps.hola();
            return 41;
        } catch (ClassCastException ok) {
            // esperado
        }

        // 42: tipo incompatible para un retorno primitivo -> tambien ClassCastException.
        et.respuesta = "no soy un long";
        try {
            ((Prims) nuevo(et, new Class<?>[] { Prims.class })).largo();
            return 42;
        } catch (ClassCastException ok) {
            // esperado
        }

        // 43..45: una chequeada que el metodo declara pasa; una que no declara sale envuelta.
        Espia ex = new Espia();
        Cheque pq = (Cheque) nuevo(ex, new Class<?>[] { Cheque.class });
        java.io.IOException io = new java.io.IOException("io");
        ex.tirar = io;
        try {
            pq.declarada();
            return 43;
        } catch (java.io.IOException ok) {
            if (ok != io) {
                return 44;
            }
        }
        ex.tirar = io;
        try {
            pq.noDeclarada();
            return 45;
        } catch (UndeclaredThrowableException ok) {
            if (ok.getUndeclaredThrowable() != io) {
                return 46;
            }
        }

        // 47..48: RuntimeException y Error nunca se envuelven.
        RuntimeException re = new IllegalStateException("re");
        ex.tirar = re;
        try {
            pq.noDeclarada();
            return 47;
        } catch (RuntimeException ok) {
            if (ok != re) {
                return 48;
            }
        }
        Error er = new StackOverflowError("er");
        ex.tirar = er;
        try {
            pq.noDeclarada();
            return 49;
        } catch (Error ok) {
            if (ok != er) {
                return 50;
            }
        }

        // 51: dos pedidos con las mismas interfaces dan LA MISMA clase.
        Class<?> c1 = Proxy.getProxyClass(ProxyTest.class.getClassLoader(),
                new Class<?>[] { Saludo.class });
        Class<?> c2 = Proxy.getProxyClass(ProxyTest.class.getClassLoader(),
                new Class<?>[] { Saludo.class });
        if (c1 != c2) {
            return 51;
        }
        if (c1 != p.getClass()) {
            return 52;
        }
        if (!Proxy.isProxyClass(c1)) {
            return 53;
        }

        // 54: getInterfaces() de la clase generada es exactamente lo pedido, en orden.
        Class<?>[] ifs = pd.getClass().getInterfaces();
        if (ifs.length != 2 || ifs[0] != PrimeraA.class || ifs[1] != SegundaB.class) {
            return 54;
        }

        // 55: la clase generada extiende Proxy.
        if (!Proxy.class.isAssignableFrom(c1)) {
            return 55;
        }

        // 56..57: arreglos como parametro y como retorno.
        Espia ea = new Espia();
        Arreglos pa = (Arreglos) nuevo(ea, new Class<?>[] { Arreglos.class });
        int[] dado = new int[] { 1, 2, 3 };
        int[] devuelto = new int[] { 4, 5 };
        ea.respuesta = devuelto;
        if (pa.duplicar(dado) != devuelto) {
            return 56;
        }
        if (ea.ultimosArgs.length != 1 || ea.ultimosArgs[0] != dado) {
            return 57;
        }
        String[][] m = new String[][] { { "a" } };
        ea.respuesta = m;
        if (pa.matriz(new String[] { "x" }) != m) {
            return 58;
        }

        // 59: un metodo `default` tambien se intercepta -- el proxy no hereda su cuerpo.
        Espia eg = new Espia();
        ConDefault pg = (ConDefault) nuevo(eg, new Class<?>[] { ConDefault.class });
        eg.respuesta = "interceptado";
        if (!"interceptado".equals(pg.derivado())) {
            return 59;
        }

        // 60: una clase que no es interfaz es un error.
        try {
            nuevo(new Espia(), new Class<?>[] { String.class });
            return 60;
        } catch (IllegalArgumentException ok) {
            // esperado
        }

        // 61: la misma interfaz dos veces es un error.
        try {
            nuevo(new Espia(), new Class<?>[] { Saludo.class, Saludo.class });
            return 61;
        } catch (IllegalArgumentException ok) {
            // esperado
        }

        // 62: manejador nulo es NullPointerException.
        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(),
                    new Class<?>[] { Saludo.class }, null);
            return 62;
        } catch (NullPointerException ok) {
            // esperado
        }

        // 63: getInvocationHandler sobre algo que no es proxy es IllegalArgumentException.
        try {
            Proxy.getInvocationHandler("no soy proxy");
            return 63;
        } catch (IllegalArgumentException ok) {
            // esperado
        }

        // 64: un proxy sin ninguna interfaz es legal y sigue respondiendo a los metodos de Object.
        Espia ez = new Espia();
        Object pz = nuevo(ez, new Class<?>[0]);
        ez.respuesta = "vacio";
        if (!"vacio".equals(pz.toString())) {
            return 64;
        }

        // 65: los metodos finales de Object NO se interceptan.
        ez.respuesta = "no deberia usarse";
        if (pz.getClass() != pz.getClass()) {
            return 65;
        }

        // 66: el Method que llega para un metodo de interfaz declara sus excepciones.
        ex.respuesta = null;
        try {
            pq.declarada();
        } catch (Throwable ignorado) {
            return 66;
        }
        Class<?>[] decl = ex.ultimoMetodo.getExceptionTypes();
        if (decl.length != 1 || decl[0] != java.io.IOException.class) {
            return 67;
        }

        // 68: un metodo heredado de una superinterfaz trae el Method de quien lo declara.
        Espia eh = new Espia();
        eh.respuesta = "her";
        Derivada pdv = (Derivada) nuevo(eh, new Class<?>[] { Derivada.class });
        pdv.heredado();
        if (eh.ultimoMetodo.getDeclaringClass() != Base.class) {
            return 68;
        }
        pdv.propio();
        if (eh.ultimoMetodo.getDeclaringClass() != Derivada.class) {
            return 69;
        }

        // 70..72: el puente de una interfaz generica es OTRO metodo del proxy, no un reenvio.
        Espia eb = new Espia();
        eb.respuesta = Integer.valueOf(1);
        Object pb = nuevo(eb, new Class<?>[] { CompInt.class });
        if (((CompInt) pb).cmp(Integer.valueOf(4)) != 1) {
            return 70;
        }
        Class<?>[] pars = eb.ultimoMetodo.getParameterTypes();
        if (pars.length != 1 || pars[0] != Object.class) {
            // El puente `cmp(Object)` es el unico metodo publico que CompInt hereda; la version
            // con Integer solo existe si el compilador la emite ademas.
            if (pars.length != 1 || pars[0] != Integer.class) {
                return 71;
            }
        }
        if (((Comparadora) pb).cmp("cualquier cosa") != 1) {
            return 72;
        }

        // 73: una interfaz que redeclara toString() no agrega un segundo metodo; sigue llegando
        // el Method de Object.
        Espia es = new Espia();
        es.respuesta = "ts";
        Object pt = nuevo(es, new Class<?>[] { ConToString.class });
        if (!"ts".equals(pt.toString())) {
            return 73;
        }
        if (es.ultimoMetodo.getDeclaringClass() != Object.class) {
            return 74;
        }

        // 75..76: una interfaz no publica se puede proxiar, y la clase generada no es publica.
        Espia er2 = new Espia();
        er2.respuesta = "shh";
        Object prv = nuevo(er2, new Class<?>[] { Reservada.class });
        if (!"shh".equals(((Reservada) prv).secreto())) {
            return 75;
        }
        if (java.lang.reflect.Modifier.isPublic(prv.getClass().getModifiers())) {
            return 76;
        }

        // 77: todo proxy es Serializable, porque Proxy lo es.
        if (!(prv instanceof java.io.Serializable)) {
            return 77;
        }

        // 78: dos interfaces donde la segunda extiende a la primera es legal.
        Espia ee = new Espia();
        ee.respuesta = "ok";
        Object pe = nuevo(ee, new Class<?>[] { Base.class, Derivada.class });
        if (!"ok".equals(((Base) pe).heredado())) {
            return 78;
        }
        if (ee.ultimoMetodo.getDeclaringClass() != Base.class) {
            return 79;
        }

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
