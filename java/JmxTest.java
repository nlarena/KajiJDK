import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeChangeNotificationFilter;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanPermission;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerNotification;
import javax.management.MBeanServerPermission;
import javax.management.MBeanTrustPermission;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.StandardMBean;

/**
 * Prueba de `javax.management`. Las dos maquinas virtuales tienen que devolver lo mismo.
 *
 * <p>`run()` devuelve -1 si pasa todo, o el indice del primer caso que falla. El indice es un
 * <b>identificador</b>, no una posicion: una vez que las dos maquinas virtuales se pusieron de
 * acuerdo sobre un numero, ese numero no se mueve aunque el caso quede en el medio del archivo. Los
 * casos nuevos toman el siguiente numero libre.
 *
 * <p>No se prueba nada cuya respuesta correcta dependa de quien implementa --el
 * `getImplementationName` del delegado, el `MBeanServerId`, los textos de descripcion que
 * `StandardMBean` pone por omision--: ahi las dos VMs tienen que diferir y probarlo seria probar
 * que una copia a la otra.
 */
public class JmxTest {

    // ---- soporte -------------------------------------------------------------------------------

    public interface ContadorMBean {
        int getValor();
        void setValor(int v);
        boolean isVivo();
        String getNombre();
        int sumar(int a, int b);
        void fallar() throws Exception;
    }

    public static class Contador implements ContadorMBean {
        private int valor = 3;
        public int getValor() { return valor; }
        public void setValor(int v) { valor = v; }
        public boolean isVivo() { return true; }
        public String getNombre() { return "c"; }
        public int sumar(int a, int b) { return a + b; }
        public void fallar() throws Exception { throw new java.io.IOException("boom"); }
    }

    public static class Cazador implements NotificationListener {
        final List<Notification> vistas = new ArrayList<Notification>();
        final List<Object> fuentes = new ArrayList<Object>();
        public void handleNotification(Notification n, Object handback) {
            vistas.add(n);
            fuentes.add(n.getSource());
        }
    }

    @MXBean(false)
    public interface NoEsMXBean { int getX(); }

    public interface AlgoMXBean { int getX(); }

    public interface Plano { int getX(); }

    /** Con `Class` crudo: con el generico, el compilador no deja ni escribir el caso. */
    private static boolean noCumple(Object impl, Class iface) {
        try {
            new StandardMBean(impl, iface);
            return false;
        } catch (javax.management.NotCompliantMBeanException e) {
            return true;
        }
    }

    private static boolean nombreInvalido(String s) {
        try {
            new ObjectName(s);
            return false;
        } catch (MalformedObjectNameException e) {
            return true;
        } catch (NullPointerException e) {
            return true;
        }
    }

    // ---- la prueba -----------------------------------------------------------------------------

    public static int run() {
        try {
            return correr();
        } catch (Throwable t) {
            System.out.println("EXCEPCION INESPERADA: " + t);
            return 999;
        }
    }

    private static int correr() throws Exception {

        // --- ObjectName: forma canonica y orden de las claves ---

        // 0: las claves se ordenan alfabeticamente en la forma canonica, no en toString
        ObjectName n1 = new ObjectName("d:z=1,a=2,m=3");
        if (!n1.getCanonicalName().equals("d:a=2,m=3,z=1")) return 0;

        // 1: toString conserva el orden en que se escribio
        if (!n1.toString().equals("d:z=1,a=2,m=3")) return 1;

        // 2: la lista de claves canonica sin el dominio
        if (!n1.getCanonicalKeyPropertyListString().equals("a=2,m=3,z=1")) return 2;

        // 3: la lista tal cual se escribio
        if (!n1.getKeyPropertyListString().equals("z=1,a=2,m=3")) return 3;

        // 4: dos nombres con las mismas claves en distinto orden son iguales
        if (!n1.equals(new ObjectName("d:a=2,z=1,m=3"))) return 4;

        // 5: y tienen el mismo hashCode
        if (n1.hashCode() != new ObjectName("d:a=2,z=1,m=3").hashCode()) return 5;

        // 6: dominio vacio es legal y queda vacio
        if (!new ObjectName(":a=1").getDomain().equals("")) return 6;

        // 7: getKeyProperty por nombre
        if (!"2".equals(n1.getKeyProperty("a"))) return 7;

        // 8: una clave que no esta da null
        if (n1.getKeyProperty("nada") != null) return 8;

        // 9: la tabla tiene las tres
        Hashtable<String, String> t = n1.getKeyPropertyList();
        if (t.size() != 3 || !"3".equals(t.get("m"))) return 9;

        // 10: el constructor de tres partes
        if (!new ObjectName("d", "a", "1").getCanonicalName().equals("d:a=1")) return 10;

        // --- ObjectName: patrones ---

        // 11: sin comodines no es patron
        if (n1.isPattern()) return 11;

        // 12: dominio con comodin
        ObjectName p1 = new ObjectName("*:a=1");
        if (!p1.isPattern() || !p1.isDomainPattern() || p1.isPropertyPattern()) return 12;

        // 13: lista de propiedades con comodin
        ObjectName p2 = new ObjectName("d:a=1,*");
        if (!p2.isPropertyPattern() || !p2.isPropertyListPattern()) return 13;

        // 14: comodin adentro de un valor
        ObjectName p3 = new ObjectName("d:a=1*");
        if (!p3.isPropertyValuePattern() || p3.isPropertyListPattern()) return 14;

        // 15: y se puede preguntar por cual propiedad
        if (!p3.isPropertyValuePattern("a")) return 15;

        // 16: WILDCARD es patron en las dos dimensiones
        if (!ObjectName.WILDCARD.isPattern() || !ObjectName.WILDCARD.isDomainPattern()) return 16;

        // --- ObjectName: apply ---

        // 17: el comodin de dominio acepta cualquier dominio con las mismas claves
        if (!p1.apply(new ObjectName("otro:a=1"))) return 17;

        // 18: pero no si las claves no son exactamente esas
        if (p1.apply(new ObjectName("otro:a=1,b=2"))) return 18;

        // 19: la lista con `*` acepta claves de mas
        if (!p2.apply(new ObjectName("d:a=1,b=2,c=3"))) return 19;

        // 20: pero exige las que nombra
        if (p2.apply(new ObjectName("d:b=2"))) return 20;

        // 21: comodin de valor: `1*` acepta `123`
        if (!p3.apply(new ObjectName("d:a=123"))) return 21;

        // 22: y rechaza lo que no empieza igual
        if (p3.apply(new ObjectName("d:a=23"))) return 22;

        // 23: `?` es exactamente un caracter
        ObjectName p4 = new ObjectName("d:a=1?3");
        if (!p4.apply(new ObjectName("d:a=123"))) return 23;

        // 24: y no dos
        if (p4.apply(new ObjectName("d:a=1223"))) return 24;

        // 25: `*` en el dominio tambien es prefijo/sufijo, no solo el dominio entero
        if (!new ObjectName("do*:a=1").apply(new ObjectName("dominio:a=1"))) return 25;

        // 26: apply contra un patron devuelve false (un patron no es una instancia)
        if (ObjectName.WILDCARD.apply(new ObjectName("d:*"))) return 26;

        // 27: WILDCARD acepta cualquier nombre concreto
        if (!ObjectName.WILDCARD.apply(new ObjectName("d:a=1,b=2"))) return 27;

        // --- ObjectName: valores citados ---

        // 28: quote escapa las comillas y agrega las de afuera
        if (!ObjectName.quote("a\"b").equals("\"a\\\"b\"")) return 28;

        // 29: quote de un texto simple
        if (!ObjectName.quote("hola").equals("\"hola\"")) return 29;

        // 30: unquote deshace quote
        if (!ObjectName.unquote(ObjectName.quote("a\"b")).equals("a\"b")) return 30;

        // 31: quote escapa el comodin, que por eso deja de serlo
        ObjectName q1 = new ObjectName("d:a=" + ObjectName.quote("x*y"));
        if (q1.isPropertyValuePattern()) return 31;

        // 32: quote escapa tambien el comodin, y getKeyProperty devuelve el valor tal cual
        if (!"\"x\\*y\"".equals(q1.getKeyProperty("a"))) return 32;

        // 33: y el nombre citado coincide consigo mismo
        if (!q1.equals(new ObjectName("d:a=" + ObjectName.quote("x*y")))) return 33;

        // 34: quote de un salto de linea usa la secuencia de escape
        if (!ObjectName.quote("a\nb").equals("\"a\\nb\"")) return 34;

        // --- ObjectName: nombres invalidos ---

        // 35: dos puntos de mas en el dominio
        if (!nombreInvalido("a:b:c=1")) return 35;

        // 36: sin la parte de propiedades
        if (!nombreInvalido("d")) return 36;

        // 37: una clave repetida
        if (!nombreInvalido("d:a=1,a=2")) return 37;

        // 38: una propiedad sin `=`
        if (!nombreInvalido("d:a")) return 38;

        // 39: un `*` suelto, sin dominio ni propiedades
        if (!nombreInvalido("*")) return 39;

        // 40: coma suelta
        if (!nombreInvalido("d:a=1,")) return 40;

        // 41: una clave vacia
        if (!nombreInvalido("d:=1")) return 41;

        // 42: null
        if (!nombreInvalido(null)) return 42;

        // 43: comilla sin cerrar
        if (!nombreInvalido("d:a=\"x")) return 43;

        // 44: un nombre con solo `:` y nada mas
        if (!nombreInvalido("d:")) return 44;

        // --- ObjectName: comparacion y getInstance ---

        // 45: compareTo ordena primero por dominio
        if (new ObjectName("a:x=1").compareTo(new ObjectName("b:x=1")) >= 0) return 45;

        // 46: getInstance(String) da un nombre equivalente al del constructor
        if (!ObjectName.getInstance("d:a=1").equals(new ObjectName("d:a=1"))) return 46;

        // 47: getInstance sobre una tabla ordena igual
        Hashtable<String, String> tt = new Hashtable<String, String>();
        tt.put("z", "1");
        tt.put("a", "2");
        if (!ObjectName.getInstance("d", tt).getCanonicalName().equals("d:a=2,z=1")) return 47;

        // --- NotificationFilterSupport ---

        // 48: recien construido no deja pasar nada
        NotificationFilterSupport f = new NotificationFilterSupport();
        if (f.isNotificationEnabled(new Notification("a.b.c", "o", 1L))) return 48;

        // 49: habilita por prefijo
        f.enableType("a.b");
        if (!f.isNotificationEnabled(new Notification("a.b.c", "o", 1L))) return 49;

        // 50: y el prefijo no alcanza para otro tipo
        if (f.isNotificationEnabled(new Notification("a.c", "o", 1L))) return 50;

        // 51: la lista de habilitados
        if (f.getEnabledTypes().size() != 1) return 51;

        // 52: disableAllTypes vuelve a bloquear todo
        f.disableAllTypes();
        if (f.isNotificationEnabled(new Notification("a.b.c", "o", 1L))) return 52;

        // 53: enableType(null) tira IllegalArgumentException
        try {
            f.enableType(null);
            return 53;
        } catch (IllegalArgumentException e) { /* esperado */ }

        // 54: el prefijo vacio habilita todo
        f.enableType("");
        if (!f.isNotificationEnabled(new Notification("cualquiera", "o", 1L))) return 54;

        // --- AttributeChangeNotificationFilter ---

        // 55: recien construido no deja pasar nada
        AttributeChangeNotificationFilter af = new AttributeChangeNotificationFilter();
        AttributeChangeNotification acn = new AttributeChangeNotification(
            "o", 1L, 0L, "cambio", "Valor", "int", Integer.valueOf(1), Integer.valueOf(2));
        if (af.isNotificationEnabled(acn)) return 55;

        // 56: habilitado por nombre exacto
        af.enableAttribute("Valor");
        if (!af.isNotificationEnabled(acn)) return 56;

        // 57: la comparacion es exacta, no por prefijo
        AttributeChangeNotificationFilter af2 = new AttributeChangeNotificationFilter();
        af2.enableAttribute("Val");
        if (af2.isNotificationEnabled(acn)) return 57;

        // 58: una notificacion que no es de cambio de atributo nunca pasa
        if (af.isNotificationEnabled(new Notification("cambio", "o", 1L))) return 58;

        // 59: la lista de habilitados
        if (af.getEnabledAttributes().size() != 1) return 59;

        // --- NotificationBroadcasterSupport ---

        // 60: entrega al oyente registrado
        NotificationBroadcasterSupport b = new NotificationBroadcasterSupport();
        Cazador c1 = new Cazador();
        b.addNotificationListener(c1, null, "hb");
        b.sendNotification(new Notification("t", b, 7L));
        if (c1.vistas.size() != 1) return 60;

        // 61: el filtro descarta antes de entregar
        NotificationFilterSupport f2 = new NotificationFilterSupport();
        f2.enableType("si");
        Cazador c2 = new Cazador();
        b.addNotificationListener(c2, f2, null);
        b.sendNotification(new Notification("no", b, 8L));
        if (c2.vistas.size() != 0) return 61;

        // 62: y deja pasar lo que habilita
        b.sendNotification(new Notification("si", b, 9L));
        if (c2.vistas.size() != 1) return 62;

        // 63: sacar por identidad de oyente, filtro y handback
        b.removeNotificationListener(c2, f2, null);
        b.sendNotification(new Notification("si", b, 10L));
        if (c2.vistas.size() != 1) return 63;

        // 64: sacar un oyente que no esta tira ListenerNotFoundException
        try {
            b.removeNotificationListener(new Cazador());
            return 64;
        } catch (javax.management.ListenerNotFoundException e) { /* esperado */ }

        // 65: sin info declarada, el arreglo es vacio y no null
        if (b.getNotificationInfo() == null || b.getNotificationInfo().length != 0) return 65;

        // --- StandardMBean: introspeccion ---

        Contador cont = new Contador();
        StandardMBean sm = new StandardMBean(cont, ContadorMBean.class);

        // 66: tres atributos: Valor, Vivo, Nombre
        MBeanInfo mi = sm.getMBeanInfo();
        if (mi.getAttributes().length != 3) return 66;

        // 67: dos operaciones: sumar y fallar
        if (mi.getOperations().length != 2) return 67;

        // 68: el className es el de la implementacion, no el de la interfaz
        if (!mi.getClassName().equals(Contador.class.getName())) return 68;

        // 69: Valor es de lectura y escritura
        MBeanAttributeInfo aiValor = null;
        MBeanAttributeInfo aiVivo = null;
        MBeanAttributeInfo aiNombre = null;
        for (MBeanAttributeInfo a : mi.getAttributes()) {
            if (a.getName().equals("Valor")) aiValor = a;
            if (a.getName().equals("Vivo")) aiVivo = a;
            if (a.getName().equals("Nombre")) aiNombre = a;
        }
        if (aiValor == null || !aiValor.isReadable() || !aiValor.isWritable()) return 69;

        // 70: y su tipo es `int`
        if (!aiValor.getType().equals("int")) return 70;

        // 71: Vivo es de solo lectura y con la forma `is`
        if (aiVivo == null || !aiVivo.isReadable() || aiVivo.isWritable() || !aiVivo.isIs()) {
            return 71;
        }

        // 72: Nombre es de solo lectura y NO tiene la forma `is`
        if (aiNombre == null || aiNombre.isWritable() || aiNombre.isIs()) return 72;

        // 73: el impacto de una operacion introspeccionada por reflexion es UNKNOWN
        MBeanOperationInfo opSumar = null;
        for (MBeanOperationInfo o : mi.getOperations()) {
            if (o.getName().equals("sumar")) opSumar = o;
        }
        if (opSumar == null || opSumar.getImpact() != MBeanOperationInfo.UNKNOWN) return 73;

        // 74: la firma de sumar son dos int
        if (opSumar.getSignature().length != 2
                || !opSumar.getSignature()[0].getType().equals("int")) return 74;

        // 75: envolviendo a otro objeto no se publican sus constructores
        if (mi.getConstructors().length != 0) return 75;

        // 76: la interfaz de administracion es la que se dio
        if (sm.getMBeanInterface() != ContadorMBean.class) return 76;

        // 77: y la clase de la implementacion, la del objeto
        if (sm.getImplementationClass() != Contador.class) return 77;

        // --- StandardMBean: despacho ---

        // 78: lee un atributo
        if (((Integer) sm.getAttribute("Valor")).intValue() != 3) return 78;

        // 79: lo escribe
        sm.setAttribute(new Attribute("Valor", Integer.valueOf(9)));
        if (cont.getValor() != 9) return 79;

        // 80: un atributo `is`
        if (!((Boolean) sm.getAttribute("Vivo")).booleanValue()) return 80;

        // 81: invoca una operacion resolviendo por firma
        Object r = sm.invoke("sumar", new Object[] { Integer.valueOf(2), Integer.valueOf(5) },
                             new String[] { "int", "int" });
        if (((Integer) r).intValue() != 7) return 81;

        // 82: un atributo que no existe tira AttributeNotFoundException
        try {
            sm.getAttribute("Nada");
            return 82;
        } catch (AttributeNotFoundException e) { /* esperado */ }

        // 83: escribir uno de solo lectura tambien
        try {
            sm.setAttribute(new Attribute("Vivo", Boolean.FALSE));
            return 83;
        } catch (AttributeNotFoundException e) { /* esperado */ }

        // 84: lo que tira el MBean llega envuelto en MBeanException
        try {
            sm.invoke("fallar", null, null);
            return 84;
        } catch (MBeanException e) {
            if (!(e.getTargetException() instanceof java.io.IOException)) return 84;
        }

        // 85: getAttributes es al mejor esfuerzo y omite lo que falla
        AttributeList al = sm.getAttributes(new String[] { "Valor", "Nada", "Vivo" });
        if (al.size() != 2) return 85;

        // 86: setAttributes devuelve solo lo que escribio
        AttributeList entrada = new AttributeList();
        entrada.add(new Attribute("Valor", Integer.valueOf(11)));
        entrada.add(new Attribute("Nada", "x"));
        if (sm.setAttributes(entrada).size() != 1) return 86;

        // 87: y el valor quedo escrito
        if (cont.getValor() != 11) return 87;

        // 88: setImplementation cambia el objeto sin rehacer la interfaz
        Contador otro = new Contador();
        sm.setImplementation(otro);
        if (((Integer) sm.getAttribute("Valor")).intValue() != 3) return 88;

        // 89: un objeto que no implementa la interfaz no se acepta
        if (!noCumple(new Object(), ContadorMBean.class)) return 89;

        // --- JMX ---

        // 90: la convencion del sufijo
        if (!JMX.isMXBeanInterface(AlgoMXBean.class)) return 90;

        // 91: una interfaz sin sufijo ni anotacion no lo es
        if (JMX.isMXBeanInterface(Plano.class)) return 91;

        // 92: RETIRADO. Comprobaba que `@MXBean(false)` mande sobre la convencion del sufijo.
        // Es lo que hace la implementacion, pero no se puede probar aca: nuestro javac no emite las
        // meta-anotaciones de una declaracion `@interface` (finding #467), asi que `@MXBean` queda
        // con retencion CLASS al leerse del `.class` y la anotacion del usuario nunca llega a
        // ejecucion. El indice queda retirado y no se reusa.

        // 93: las constantes de campo de descriptor
        if (!JMX.MXBEAN_FIELD.equals("mxbean")
                || !JMX.DEFAULT_VALUE_FIELD.equals("defaultValue")
                || !JMX.IMMUTABLE_INFO_FIELD.equals("immutableInfo")
                || !JMX.INTERFACE_CLASS_NAME_FIELD.equals("interfaceClassName")) return 93;

        // 94: las otras cinco
        if (!JMX.LEGAL_VALUES_FIELD.equals("legalValues")
                || !JMX.MAX_VALUE_FIELD.equals("maxValue")
                || !JMX.MIN_VALUE_FIELD.equals("minValue")
                || !JMX.OPEN_TYPE_FIELD.equals("openType")
                || !JMX.ORIGINAL_TYPE_FIELD.equals("originalType")) return 94;

        // --- permisos ---

        // 95: `*` cubre todo
        MBeanPermission todo = new MBeanPermission("*", "*");
        if (!todo.implies(new MBeanPermission("com.foo.Bar#Attr[d:a=1]", "getAttribute"))) {
            return 95;
        }

        // 96: el comodin de sufijo en la clase cubre el paquete
        MBeanPermission paq = new MBeanPermission("com.foo.*", "getAttribute");
        if (!paq.implies(new MBeanPermission("com.foo.Bar#X[d:a=1]", "getAttribute"))) return 96;

        // 97: y no cubre otro paquete
        if (paq.implies(new MBeanPermission("com.baz.Bar#X[d:a=1]", "getAttribute"))) return 97;

        // 98: una accion de mas no esta cubierta
        if (paq.implies(new MBeanPermission("com.foo.Bar#X[d:a=1]", "setAttribute"))) return 98;

        // 99: el ObjectName se compara como patron
        MBeanPermission conNombre = new MBeanPermission("*#*[d:*]", "invoke");
        if (!conNombre.implies(new MBeanPermission("com.foo.Bar#f[d:a=1]", "invoke"))) return 99;

        // 100: y un dominio distinto no entra
        if (conNombre.implies(new MBeanPermission("com.foo.Bar#f[e:a=1]", "invoke"))) return 100;

        // 101: queryMBeans implica queryNames
        MBeanPermission qm = new MBeanPermission("*", "queryMBeans");
        if (!qm.implies(new MBeanPermission("*", "queryNames"))) return 101;

        // 102: pero no al reves
        if (new MBeanPermission("*", "queryNames")
                .implies(new MBeanPermission("*", "queryMBeans"))) return 102;

        // 103: una accion inexistente no se acepta
        try {
            new MBeanPermission("*", "volar");
            return 103;
        } catch (IllegalArgumentException e) { /* esperado */ }

        // 157: dentro de los corchetes, `*` no significa "cualquiera": tiene que ser un
        // ObjectName legal, y `*` solo no lo es
        try {
            new MBeanPermission("com.foo.Bar#X[*]", "getAttribute");
            return 157;
        } catch (IllegalArgumentException e) { /* esperado */ }

        // 158: el vacio si significa "cualquiera"
        if (!new MBeanPermission("com.foo.Bar#X[]", "getAttribute")
                .implies(new MBeanPermission("com.foo.Bar#X[z:a=9]", "getAttribute"))) {
            return 158;
        }

        // 159: el constructor de cuatro partes escribe el guion donde recibio null
        if (!new MBeanPermission("com.foo.Bar", "X", null, "getAttribute").getName()
                .equals("com.foo.Bar#X[-]")) {
            return 159;
        }

        // 104: createMBeanServer implica newMBeanServer
        MBeanServerPermission crear = new MBeanServerPermission("createMBeanServer");
        if (!crear.implies(new MBeanServerPermission("newMBeanServer"))) return 104;

        // 105: y no al reves
        if (new MBeanServerPermission("newMBeanServer").implies(crear)) return 105;

        // 106: `*` cubre las cuatro
        if (!new MBeanServerPermission("*").implies(
                new MBeanServerPermission("releaseMBeanServer"))) return 106;

        // 107: el nombre puede ser una lista con comas
        MBeanServerPermission dos = new MBeanServerPermission("findMBeanServer,newMBeanServer");
        if (!dos.implies(new MBeanServerPermission("findMBeanServer"))) return 107;

        // 108: un nombre invalido no se acepta
        try {
            new MBeanServerPermission("volar");
            return 108;
        } catch (IllegalArgumentException e) { /* esperado */ }

        // 109: MBeanTrustPermission solo acepta `register` y `*`
        new MBeanTrustPermission("register");
        try {
            new MBeanTrustPermission("otra");
            return 109;
        } catch (IllegalArgumentException e) { /* esperado */ }

        // 110: y no lleva acciones
        try {
            new MBeanTrustPermission("register", "leer");
            return 110;
        } catch (IllegalArgumentException e) { /* esperado */ }

        // --- MBeanServer ---

        MBeanServer mbs = MBeanServerFactory.newMBeanServer("Kaji");

        // 111: el dominio por omision es el que se pidio
        if (!mbs.getDefaultDomain().equals("Kaji")) return 111;

        // 112: el delegado ya esta registrado
        if (!mbs.isRegistered(MBeanServerDelegate.DELEGATE_NAME)) return 112;

        // 113: y es el unico MBean
        if (mbs.getMBeanCount().intValue() != 1) return 113;

        // 114: registrar devuelve la instancia con la clase de la implementacion
        ObjectName on = new ObjectName("prueba:tipo=Contador");
        Contador cc = new Contador();
        ObjectInstance oi = mbs.registerMBean(cc, on);
        if (!oi.getClassName().equals(Contador.class.getName())) return 114;

        // 115: y el ObjectName
        if (!oi.getObjectName().equals(on)) return 115;

        // 116: ahora hay dos
        if (mbs.getMBeanCount().intValue() != 2) return 116;

        // 117: lee un atributo a traves del agente
        if (((Integer) mbs.getAttribute(on, "Valor")).intValue() != 3) return 117;

        // 118: lo escribe
        mbs.setAttribute(on, new Attribute("Valor", Integer.valueOf(42)));
        if (cc.getValor() != 42) return 118;

        // 119: invoca una operacion
        Object r2 = mbs.invoke(on, "sumar", new Object[] { Integer.valueOf(4), Integer.valueOf(6) },
                               new String[] { "int", "int" });
        if (((Integer) r2).intValue() != 10) return 119;

        // 120: el MBeanInfo viene del agente
        if (mbs.getMBeanInfo(on).getAttributes().length != 3) return 120;

        // 121: isInstanceOf mira la clase administrada, no el envoltorio
        if (!mbs.isInstanceOf(on, Contador.class.getName())) return 121;

        // 122: y tambien sus interfaces
        if (!mbs.isInstanceOf(on, ContadorMBean.class.getName())) return 122;

        // 123: registrar dos veces el mismo nombre falla
        try {
            mbs.registerMBean(new Contador(), on);
            return 123;
        } catch (InstanceAlreadyExistsException e) { /* esperado */ }

        // 124: un objeto que no es MBean tampoco entra
        try {
            mbs.registerMBean(new Object(), new ObjectName("prueba:tipo=Nada"));
            return 124;
        } catch (javax.management.NotCompliantMBeanException e) { /* esperado */ }

        // 125: consultar un MBean que no esta
        try {
            mbs.getAttribute(new ObjectName("prueba:tipo=Fantasma"), "Valor");
            return 125;
        } catch (InstanceNotFoundException e) { /* esperado */ }

        // 126: queryNames con patron de dominio
        Set<ObjectName> encontrados = mbs.queryNames(new ObjectName("prueba:*"), null);
        if (encontrados.size() != 1 || !encontrados.contains(on)) return 126;

        // 127: queryNames sin patron los trae todos
        if (mbs.queryNames(null, null).size() != 2) return 127;

        // 128: queryMBeans devuelve instancias
        Set<ObjectInstance> insts = mbs.queryMBeans(new ObjectName("prueba:*"), null);
        if (insts.size() != 1) return 128;

        // 129: una QueryExp filtra por atributo
        QueryExp q = Query.eq(Query.attr("Valor"), Query.value(42));
        if (mbs.queryNames(new ObjectName("prueba:*"), q).size() != 1) return 129;

        // 130: y descarta si no coincide
        QueryExp q2 = Query.eq(Query.attr("Valor"), Query.value(7));
        if (mbs.queryNames(new ObjectName("prueba:*"), q2).size() != 0) return 130;

        // 131: getDomains lista los dominios en uso
        List<String> doms = new ArrayList<String>();
        for (String d : mbs.getDomains()) doms.add(d);
        if (!doms.contains("prueba") || !doms.contains("JMImplementation")) return 131;

        // 132: el delegado avisa las altas, con el ObjectName adentro
        Cazador cd = new Cazador();
        mbs.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, cd, null, null);
        ObjectName on2 = new ObjectName("prueba:tipo=Otro");
        mbs.registerMBean(new Contador(), on2);
        if (cd.vistas.size() != 1) return 132;

        // 133: y es una notificacion de registro
        Notification nn = cd.vistas.get(0);
        if (!(nn instanceof MBeanServerNotification)) return 133;

        // 134: que nombra al MBean que entro
        if (!((MBeanServerNotification) nn).getMBeanName().equals(on2)) return 134;

        // 135: el tipo es el de alta
        if (!nn.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) return 135;

        // 136: y el agente reemplazo la fuente por el ObjectName del delegado
        if (!MBeanServerDelegate.DELEGATE_NAME.equals(cd.fuentes.get(0))) return 136;

        // 137: la baja tambien avisa
        mbs.unregisterMBean(on2);
        if (cd.vistas.size() != 2) return 137;

        // 138: con el tipo de baja
        if (!cd.vistas.get(1).getType()
                .equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) return 138;

        // 139: y ya no esta registrado
        if (mbs.isRegistered(on2)) return 139;

        // 140: sacado el oyente, no llegan mas
        mbs.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, cd);
        mbs.registerMBean(new Contador(), on2);
        if (cd.vistas.size() != 2) return 140;

        // 141: instantiate arma un objeto por nombre de clase
        Object hecho = mbs.instantiate(Contador.class.getName());
        if (!(hecho instanceof Contador)) return 141;

        // 142: createMBean instancia y registra de una
        ObjectName on3 = new ObjectName("prueba:tipo=Creado");
        ObjectInstance oi3 = mbs.createMBean(Contador.class.getName(), on3);
        if (!oi3.getObjectName().equals(on3) || !mbs.isRegistered(on3)) return 142;

        // 143: y responde como cualquier otro
        if (((Integer) mbs.getAttribute(on3, "Valor")).intValue() != 3) return 143;

        // 144: una clase que no existe
        try {
            mbs.instantiate("no.existe.Clase");
            return 144;
        } catch (javax.management.ReflectionException e) { /* esperado */ }

        // 145: un agente creado con newMBeanServer no es encontrable
        int antes = MBeanServerFactory.findMBeanServer(null).size();
        MBeanServerFactory.newMBeanServer();
        if (MBeanServerFactory.findMBeanServer(null).size() != antes) return 145;

        // 146: uno creado con createMBeanServer si
        MBeanServer enc = MBeanServerFactory.createMBeanServer("Encontrable");
        if (MBeanServerFactory.findMBeanServer(null).size() != antes + 1) return 146;

        // 147: y se lo encuentra por su MBeanServerId
        String id = (String) enc.getAttribute(MBeanServerDelegate.DELEGATE_NAME, "MBeanServerId");
        List<MBeanServer> hallados = MBeanServerFactory.findMBeanServer(id);
        if (hallados.size() != 1 || hallados.get(0) != enc) return 147;

        // 148: releaseMBeanServer lo saca
        MBeanServerFactory.releaseMBeanServer(enc);
        if (MBeanServerFactory.findMBeanServer(null).size() != antes) return 148;

        // 149: soltar dos veces falla
        try {
            MBeanServerFactory.releaseMBeanServer(enc);
            return 149;
        } catch (IllegalArgumentException e) { /* esperado */ }

        // 150: un DynamicMBean se registra tal cual, sin envolver
        DynamicMBean dm = new StandardMBean(new Contador(), ContadorMBean.class);
        ObjectName on4 = new ObjectName("prueba:tipo=Dinamico");
        mbs.registerMBean(dm, on4);
        if (((Integer) mbs.getAttribute(on4, "Valor")).intValue() != 3) return 150;

        // 151: la cadena vacia SI es legal: equivale a `*:*`
        ObjectName vacio = new ObjectName("");
        if (!vacio.isPattern() || !vacio.getCanonicalName().equals("*:*")) return 151;

        // 152: y por lo tanto acepta cualquier nombre
        if (!vacio.apply(new ObjectName("d:a=1"))) return 152;

        // 153: el `*` de la lista se normaliza al final de la forma canonica
        if (!new ObjectName("d:*,a=1").getCanonicalName().equals("d:a=1,*")) return 153;

        // 154: un valor vacio es legal y no es patron
        if (new ObjectName("d:a=,b=1").isPattern()) return 154;

        // 155: un punto y coma no separa propiedades
        if (!nombreInvalido("d:a=1;b=2")) return 155;

        // 156: el delegado contesta el nombre de la especificacion
        if (!"Java Management Extensions".equals(
                mbs.getAttribute(MBeanServerDelegate.DELEGATE_NAME, "SpecificationName"))) {
            return 156;
        }

        return -1;
    }

    public static void main(String[] args) {
        System.out.println("run=" + run());
    }
}
