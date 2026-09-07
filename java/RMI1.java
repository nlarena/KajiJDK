import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectionImpl_Stub;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

/**
 * Comprueba {@code javax.management.remote.rmi} contra el JDK 25.
 *
 * <p>Arma el camino completo dentro de un mismo proceso --servidor, conector, conexion, oyente de
 * notificaciones-- y compara linea por linea con lo que hizo el JDK real, que esta escrito abajo.
 * {@link #donde()} devuelve el indice de la primera que no coincide, o -1: un entero alcanza porque
 * {@code run-headless} no vacia la consola.
 *
 * <p>Ese camino no necesita el transporte de RMI. Es a proposito: lo que se puede comprobar es
 * justamente lo que esta biblioteca implementa de verdad, y lo que no --exportar, encontrar el
 * objeto remoto-- se comprueba por el error que da, que tambien esta en la comparacion.
 */
public class RMI1 {

    static final ObjectName DELEGADO = nombre("JMImplementation:type=MBeanServerDelegate");



    static final String[] ESPERADO = {
        "version-prefijo|true",
        "cl|null|mbs|null",
        "toStub|java.rmi.NoSuchObjectException: object not exported",
        "newClient-sin-mbs|java.lang.IllegalStateException: Not attached to an MBean server",
        "puerto-negativo|java.lang.IllegalArgumentException: Negative port: -1",
        "atributos|[jmx.remote.jndi.rebind, jmx.remote.x.cualquiera, otra.cosa]",
        "activo|false|direccion|null",
        "toJMXConnector|java.lang.IllegalStateException: Connector is not active",
        "activo-tras-parar|false",
        "arrancar-tras-parar|java.io.IOException: The server has been stopped.",
        "protocolo-malo|java.net.MalformedURLException: Invalid protocol type: jmxmp",
        "conector|javax.management.remote.rmi.RMIConnector: jmxServiceURL=service:jmx:rmi://localhost",
        "conector-direccion|service:jmx:rmi://localhost",
        "id-sin-conectar|java.io.IOException: Not connected",
        "mbsc-sin-conectar|java.io.IOException: Not connected",
        "conectar-cerrado|java.io.IOException: Connector closed",
        "oyente-nulo|java.lang.NullPointerException: listener",
    };

    static ObjectName nombre(String s) {
        try {
            return new ObjectName(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String falla(Throwable e) {
        return e.getClass().getName() + ": " + e.getMessage();
    }

    /** Lo que hace el paquete, escrito como una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        // ---- el servidor, antes de arrancar ----
        final RMIJRMPServerImpl srv = new RMIJRMPServerImpl(0, null, null, null);
        a.add("version-prefijo|" + srv.getVersion().startsWith("1.0 "));
        a.add("cl|" + srv.getDefaultClassLoader() + "|mbs|" + srv.getMBeanServer());
        try {
            srv.toStub();
            a.add("toStub|sin error");
        } catch (Throwable e) {
            a.add("toStub|" + falla(e));
        }
        try {
            srv.newClient(null);
            a.add("newClient-sin-mbs|sin error");
        } catch (Throwable e) {
            a.add("newClient-sin-mbs|" + falla(e));
        }
        try {
            new RMIJRMPServerImpl(-1, null, null, null);
            a.add("puerto-negativo|sin error");
        } catch (Throwable e) {
            a.add("puerto-negativo|" + falla(e));
        }

        // ---- el servidor conector ----
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost");
        final Map<String, Object> env = new HashMap<String, Object>();
        env.put("jmx.remote.jndi.rebind", "true");
        env.put("jmx.remote.x.password.file", "clave.txt");
        env.put("java.naming.security.principal", "quien");
        env.put("jmx.remote.x.cualquiera", "2");
        env.put("otra.cosa", "3");
        final RMIConnectorServer cs = new RMIConnectorServer(url, env);
        a.add("atributos|" + new TreeSet<String>(cs.getAttributes().keySet()));
        a.add("activo|" + cs.isActive() + "|direccion|" + cs.getAddress());
        try {
            cs.toJMXConnector(null);
            a.add("toJMXConnector|sin error");
        } catch (Throwable e) {
            a.add("toJMXConnector|" + falla(e));
        }
        cs.stop();
        a.add("activo-tras-parar|" + cs.isActive());
        try {
            cs.start();
            a.add("arrancar-tras-parar|sin error");
        } catch (Throwable e) {
            a.add("arrancar-tras-parar|" + falla(e));
        }
        try {
            new RMIConnectorServer(new JMXServiceURL("service:jmx:jmxmp://localhost"), null);
            a.add("protocolo-malo|sin error");
        } catch (Throwable e) {
            a.add("protocolo-malo|" + falla(e));
        }

        // ---- el conector, antes de conectarse ----
        final RMIConnector antes = new RMIConnector(url, null);
        a.add("conector|" + antes);
        a.add("conector-direccion|" + antes.getAddress());
        try {
            antes.getConnectionId();
            a.add("id-sin-conectar|sin error");
        } catch (Throwable e) {
            a.add("id-sin-conectar|" + falla(e));
        }
        try {
            antes.getMBeanServerConnection();
            a.add("mbsc-sin-conectar|" + "sin error");
        } catch (Throwable e) {
            a.add("mbsc-sin-conectar|" + falla(e));
        }
        antes.close();
        try {
            antes.connect();
            a.add("conectar-cerrado|sin error");
        } catch (Throwable e) {
            a.add("conectar-cerrado|" + falla(e));
        }
        try {
            antes.addConnectionNotificationListener(null, null, null);
            a.add("oyente-nulo|sin error");
        } catch (Throwable e) {
            a.add("oyente-nulo|" + falla(e));
        }

        return a.toArray(new String[a.size()]);
    }
    /**
     * Los numeros de los 27 metodos, tal como estan en el stub del JDK 25.
     *
     * <p>Los saco `javap -c` del archivo compilado del JDK, donde son literales que puso
     * `rmic`. Aca se calculan al cargar la clase; que las dos listas ordenadas sean
     * iguales es lo que prueba que el calculo es el de la especificacion.
     */
    static final long[] HASHES_JDK = {
        -8679469989872508324L, -8578317696269497109L, -7404813916326233354L, -6662314179953625551L,
        -6604955182088909937L, -5321691879380783377L, -5037523307973544478L, -4742752445160157748L,
        -2147516868461740814L, -2042362057335820635L, -1089783104982388203L, -230470228399681820L,
        -159498580868721452L, -67907180346059933L, 1434350937885235744L, 2510753813974665446L,
        2549120024456183446L, 2578029900065214857L, 2915881009400597976L, 4867822117947806114L,
        6047668923998658472L, 6285293806596348999L, 6604721169198089513L, 6738606893952597516L,
        6950095694996159938L, 8325683335228268564L, 9152567528369059802L,
    };

    /**
     * Comprueba lo que el JDK no puede hacer por el mismo camino, mas los numeros del stub.
     *
     * <p>El JDK exige que el {@code RMIServerImpl} este atado a un {@code RMIConnectorServer} antes
     * de dar conexiones, y atarlo pasa por exportar. Asi que el camino completo no se puede correr
     * de los dos lados y compararlo; lo que si se puede es correrlo de este y exigir que de lo mismo
     * que preguntarle directamente al {@code MBeanServer}, que es la unica respuesta correcta.
     *
     * @return 0 si todo dio bien, o el numero de la comprobacion que fallo
     */
    public static int interno() {
        try {
            return interior();
        } catch (Throwable e) {
            return 9000;
        }
    }

    private static int interior() throws Exception {
        // Los numeros del stub: los 27 calculados tienen que ser exactamente los del JDK.
        final TreeSet<Long> nuestros = new TreeSet<Long>();
        for (final Field f : RMIConnectionImpl_Stub.class.getDeclaredFields()) {
            if (f.getName().startsWith("$hash_")) {
                f.setAccessible(true);
                nuestros.add(Long.valueOf(f.getLong(null)));
            }
        }
        if (nuestros.size() != HASHES_JDK.length) {
            return 1;
        }
        int k = 0;
        for (final Long h : nuestros) {
            if (h.longValue() != HASHES_JDK[k++]) {
                return 2;
            }
        }

        // El camino completo: servidor, conector, conexion.
        final MBeanServer mbs = MBeanServerFactory.newMBeanServer("prueba");
        final RMIJRMPServerImpl srv = new RMIJRMPServerImpl(0, null, null, null);
        srv.setMBeanServer(mbs);
        final RMIConnector con = new RMIConnector(srv, null);
        con.connect();
        if (!con.getConnectionId().equals("rmi:  1")) {
            return 3;
        }
        final MBeanServerConnection c = con.getMBeanServerConnection();

        // Cada respuesta se compara con la que da el MBeanServer local, que es la correcta.
        if (!c.getMBeanCount().equals(mbs.getMBeanCount())) {
            return 10;
        }
        if (!c.getDefaultDomain().equals(mbs.getDefaultDomain())) {
            return 11;
        }
        if (!Arrays.toString(c.getDomains()).equals(Arrays.toString(mbs.getDomains()))) {
            return 12;
        }
        if (c.isRegistered(DELEGADO) != mbs.isRegistered(DELEGADO)) {
            return 13;
        }
        if (!c.getObjectInstance(DELEGADO).equals(mbs.getObjectInstance(DELEGADO))) {
            return 14;
        }
        if (!c.getAttribute(DELEGADO, "ImplementationName")
                .equals(mbs.getAttribute(DELEGADO, "ImplementationName"))) {
            return 15;
        }
        if (!new TreeSet<Object>(c.queryNames(null, null))
                .equals(new TreeSet<Object>(mbs.queryNames(null, null)))) {
            return 16;
        }
        if (!c.getMBeanInfo(DELEGADO).getClassName()
                .equals(mbs.getMBeanInfo(DELEGADO).getClassName())) {
            return 17;
        }
        if (!c.isInstanceOf(DELEGADO, "javax.management.MBeanServerDelegate")) {
            return 18;
        }
        final AttributeList al = c.getAttributes(DELEGADO,
                new String[] {"ImplementationName", "SpecificationName"});
        if (al.size() != 2) {
            return 19;
        }

        // Un MBean nuevo, que ademas dispara la notificacion de registro.
        final StringBuilder recibidas = new StringBuilder();
        final NotificationListener oyente = new NotificationListener() {
            public void handleNotification(Notification n, Object handback) {
                synchronized (recibidas) {
                    recibidas.append(n.getType()).append('/').append(handback).append(';');
                    recibidas.notifyAll();
                }
            }
        };
        c.addNotificationListener(DELEGADO, oyente, null, "marca");
        final ObjectName nuevo = nombre("prueba:type=Uno");
        c.createMBean("javax.management.timer.Timer", nuevo);
        synchronized (recibidas) {
            final long limite = System.currentTimeMillis() + 5000;
            while (recibidas.length() == 0 && System.currentTimeMillis() < limite) {
                recibidas.wait(200);
            }
        }
        if (!recibidas.toString().equals("JMX.mbean.registered/marca;")) {
            return 20;
        }
        c.removeNotificationListener(DELEGADO, oyente);
        try {
            c.removeNotificationListener(DELEGADO, oyente);
            return 21;
        } catch (ListenerNotFoundException e) {
            // Es lo que tiene que pasar: sacar dos veces el mismo oyente no es sacar dos.
        }
        c.unregisterMBean(nuevo);
        if (mbs.isRegistered(nuevo)) {
            return 22;
        }

        con.close();
        try {
            con.getMBeanServerConnection();
            return 23;
        } catch (IOException e) {
            if (!"Connector closed".equals(e.getMessage())) {
                return 24;
            }
        }
        return 0;
    }

    /**
     * El indice de la primera comprobacion que no coincide con el JDK, o -1 si coinciden todas.
     *
     * @return el indice, o -1
     */
    public static int donde() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != ESPERADO.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = donde();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
