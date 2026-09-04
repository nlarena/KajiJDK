import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * TCP de verdad: `ServerSocket` escucha, `Socket` conecta, y los bytes cruzan.
 *
 * <p>Todo contra `127.0.0.1` y con el puerto que elija el sistema (`new ServerSocket(0)`): la prueba
 * no toca la red ni pisa un puerto que alguien mas pueda estar usando. Es lo unico que la vuelve
 * repetible en cualquier maquina.
 *
 * <p>Lo que se comprueba es lo que hace util a un socket y lo que hasta hace poco esta biblioteca no
 * podia prometer: que lo que se escribe de un lado se lee del otro, que cerrar una punta le da un
 * fin de flujo a la otra, que el par se sabe quien es, y que conectar a un puerto donde no hay nadie
 * falla en vez de colgarse.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `java.net`.
 */
public class SocketTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /** El servidor: acepta uno, hace eco de lo que le llegue, y cierra. */
    static final class Eco implements Runnable {

        final ServerSocket servidor;
        volatile String recibido;
        volatile int puertoDelPar = -1;
        volatile Exception falla;

        Eco(ServerSocket s) {
            this.servidor = s;
        }

        public void run() {
            try {
                Socket c = this.servidor.accept();
                this.puertoDelPar = c.getPort();
                InputStream in = c.getInputStream();
                byte[] buf = new byte[64];
                int n = in.read(buf);
                this.recibido = n > 0 ? new String(buf, 0, n, "UTF-8") : "";
                OutputStream out = c.getOutputStream();
                out.write(("eco:" + this.recibido).getBytes("UTF-8"));
                out.flush();
                // Cerrar la escritura le da un fin de flujo al cliente sin cerrar todo el socket.
                c.shutdownOutput();
                c.close();
            } catch (Exception e) {
                this.falla = e;
            }
        }
    }

    /** Lee todo lo que quede del flujo, hasta el fin. */
    static String drenar(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[64];
        int n = in.read(buf);
        while (n > 0) {
            sb.append(new String(buf, 0, n, "UTF-8"));
            n = in.read(buf);
        }
        return sb.toString();
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- atar en el puerto que el sistema elija
        ServerSocket servidor = new ServerSocket(0);
        ok("el servidor queda atado", servidor.isBound());
        ok("no esta cerrado", !servidor.isClosed());
        int puerto = servidor.getLocalPort();
        ok("el sistema dio un puerto", puerto > 0);

        Eco eco = new Eco(servidor);
        Thread hilo = new Thread(eco);
        hilo.start();

        // ---- conectar y hablar
        Socket cliente = new Socket("127.0.0.1", puerto);
        ok("el cliente queda conectado", cliente.isConnected());
        ok("y atado", cliente.isBound());
        ok("y no cerrado", !cliente.isClosed());
        ok("sabe a que puerto se conecto", cliente.getPort() == puerto);
        ok("sabe con quien habla",
                "127.0.0.1".equals(cliente.getInetAddress().getHostAddress()));
        ok("tiene un puerto local propio",
                cliente.getLocalPort() > 0 && cliente.getLocalPort() != puerto);

        OutputStream out = cliente.getOutputStream();
        // El mismo objeto siempre; se comprueba **antes** del shutdown porque despues
        // `getOutputStream()` tira, que es lo que hace el JDK y lo que esta prueba aprendio de el.
        ok("getOutputStream da siempre el mismo", cliente.getOutputStream() == out);
        out.write("hola".getBytes("UTF-8"));
        out.flush();
        cliente.shutdownOutput();

        String vuelta = SocketTest.drenar(cliente.getInputStream());
        ok("lo escrito llega al otro lado y vuelve", "eco:hola".equals(vuelta));

        hilo.join();
        ok("el servidor no fallo", eco.falla == null);
        if (eco.falla != null) {
            System.out.println("  (" + eco.falla + ")");
        }
        ok("el servidor recibio lo que se mando", "hola".equals(eco.recibido));
        ok("y vio el puerto del cliente", eco.puertoDelPar > 0);

        // ---- despues del shutdown de escritura, no hay flujo de salida
        boolean tiroTrasShutdown = false;
        try {
            cliente.getOutputStream();
        } catch (SocketException e) {
            tiroTrasShutdown = true;
        }
        ok("tras shutdownOutput no hay flujo de salida", tiroTrasShutdown);

        cliente.close();
        ok("despues de cerrar, esta cerrado", cliente.isClosed());
        boolean tiroCerrado = false;
        try {
            cliente.getOutputStream();
        } catch (SocketException e) {
            tiroCerrado = true;
        }
        ok("un socket cerrado no da flujos", tiroCerrado);

        servidor.close();
        ok("el servidor queda cerrado", servidor.isClosed());

        // ---- conectar donde no hay nadie falla, no cuelga
        //
        // Se usa el puerto que el servidor acaba de liberar: nadie lo tiene, y no hace falta
        // adivinar uno que pueda estar ocupado en la maquina de otro.
        boolean tiroConexion = false;
        try {
            Socket perdido = new Socket();
            perdido.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), puerto), 500);
            perdido.close();
        } catch (IOException e) {
            tiroConexion = true;
        }
        ok("conectar a un puerto vacio falla", tiroConexion);

        // ---- isReachable prueba de verdad
        //
        // El loopback siempre contesta: nadie escucha en el puerto 7, pero el rechazo lo manda la
        // maquina, y un rechazo es una respuesta. La 192.0.2.1 es de la red que la RFC 5737 reserva
        // para documentacion: no existe, y por eso el silencio es seguro y no depende de la red.
        //
        // Tres segundos y no uno porque Windows tarda ~2s en reportar un rechazo de TCP, y esta
        // biblioteca prueba con TCP: el JDK manda un ICMP y contesta en el acto. Es la unica
        // diferencia observable entre los dos, y esta dicha en el javadoc de `isReachable`.
        ok("el loopback esta accesible",
                InetAddress.getByName("127.0.0.1").isReachable(3000));
        ok("una direccion reservada para documentacion no",
                !InetAddress.getByName("192.0.2.1").isReachable(400));
        boolean tiroPlazoNegativo = false;
        try {
            InetAddress.getByName("127.0.0.1").isReachable(-1);
        } catch (IllegalArgumentException e) {
            tiroPlazoNegativo = true;
        }
        ok("un plazo negativo tira", tiroPlazoNegativo);

        // ---- elegir la punta local
        //
        // Sale por el loopback y por un puerto que elige el sistema. Se comprueba que la conexion
        // salio **de ahi**: es lo unico que distingue a estos constructores de los de dos
        // argumentos, y lo unico que hay que probar.
        ServerSocket segundo = new ServerSocket(0);
        int puerto2 = segundo.getLocalPort();
        Socket desde = new Socket(InetAddress.getByName("127.0.0.1"), puerto2,
                InetAddress.getByName("127.0.0.1"), 0);
        ok("conecta eligiendo la punta local", desde.isConnected());
        ok("y salio por la que se pidio",
                "127.0.0.1".equals(desde.getLocalAddress().getHostAddress()));
        ok("con un puerto local de verdad", desde.getLocalPort() > 0);
        Socket atendido = segundo.accept();
        ok("el servidor lo ve venir de ese puerto", atendido.getPort() == desde.getLocalPort());

        // ---- un byte fuera de banda
        desde.sendUrgentData(65);
        ok("mandar fuera de banda no falla", true);

        // ---- el nombre tambien vale, y un puerto local fuera de rango se rechaza
        boolean tiroPuertoLocal = false;
        try {
            Socket malo = new Socket("127.0.0.1", puerto2, InetAddress.getByName("127.0.0.1"),
                    70000);
            malo.close();
        } catch (IllegalArgumentException e) {
            tiroPuertoLocal = true;
        }
        ok("un puerto local fuera de rango tira", tiroPuertoLocal);

        atendido.close();
        desde.close();
        segundo.close();

        // ---- atar primero y conectar despues
        //
        // Lo que se afirma es lo unico que las dos implementaciones prometen igual: que despues de
        // `bind` el socket **esta atado**, que atar dos veces falla, y que la conexion que venga
        // sale por la direccion que se pidio. El puerto local no se compara: el JDK lo reserva en el
        // `bind` y esta biblioteca recien al conectar, y eso esta dicho en el javadoc de `bind`.
        ServerSocket tercero = new ServerSocket(0);
        Socket atado = new Socket();
        atado.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        ok("despues de bind esta atado", atado.isBound());
        boolean tiroDosVeces = false;
        try {
            atado.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        } catch (SocketException e) {
            tiroDosVeces = true;
        }
        ok("atar dos veces falla", tiroDosVeces);
        atado.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),
                tercero.getLocalPort()));
        ok("el socket atado conecta", atado.isConnected());
        ok("y salio por donde se ato",
                "127.0.0.1".equals(atado.getLocalAddress().getHostAddress()));
        Socket delOtroLado = tercero.accept();
        delOtroLado.close();
        atado.close();
        tercero.close();

        // ---- la bandera `stream`
        //
        // Con `false` prometia un socket UDP con cara de `Socket`. El JDK dejo de sostenerlo y tira
        // `IllegalArgumentException`; se comprobo contra el JDK 25.
        boolean tiroDatagrama = false;
        try {
            Socket udp = new Socket("127.0.0.1", puerto2, false);
            udp.close();
        } catch (IllegalArgumentException e) {
            tiroDatagrama = true;
        }
        ok("stream=false ya no da un socket de datagramas", tiroDatagrama);

        // ---- isReachable por una placa
        //
        // Se busca la placa de loopback y se prueba por ella. Si la maquina no la expone, no hay
        // nada que probar y la prueba no falla por eso: lo que se afirma es que **si** hay placa,
        // la respuesta es la misma que sin elegirla.
        NetworkInterface loopback = null;
        try {
            java.util.Enumeration<NetworkInterface> placas =
                    NetworkInterface.getNetworkInterfaces();
            while (placas != null && placas.hasMoreElements()) {
                NetworkInterface n = placas.nextElement();
                if (n.isLoopback()) {
                    loopback = n;
                    break;
                }
            }
        } catch (SocketException e) {
            // Enumerar las placas es una llamada al sistema que no todas las VM exponen --KajiJDK
            // todavia no--, y el contrato la declara. No hay placa que probar y no hay nada que
            // afirmar; lo que sigue, con placa null, se prueba igual en las dos.
            loopback = null;
        }
        if (loopback != null) {
            ok("el loopback esta accesible por su placa",
                    InetAddress.getByName("127.0.0.1").isReachable(loopback, 0, 3000));
        }
        ok("con placa null es la de un parametro",
                InetAddress.getByName("127.0.0.1").isReachable(null, 0, 3000));
        boolean tiroTtl = false;
        try {
            InetAddress.getByName("127.0.0.1").isReachable(null, -1, 100);
        } catch (IllegalArgumentException e) {
            tiroTtl = true;
        }
        ok("un ttl negativo tira", tiroTtl);

        // ---- las validaciones de connect
        Socket suelto = new Socket();
        ok("un socket nuevo no esta conectado", !suelto.isConnected());
        boolean tiroNulo = false;
        try {
            suelto.connect(null);
        } catch (IllegalArgumentException e) {
            tiroNulo = true;
        }
        ok("connect(null) tira", tiroNulo);
        boolean tiroPlazo = false;
        try {
            suelto.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), puerto), -1);
        } catch (IllegalArgumentException e) {
            tiroPlazo = true;
        }
        ok("un plazo negativo tira", tiroPlazo);
        boolean tiroSinConectar = false;
        try {
            suelto.getInputStream();
        } catch (SocketException e) {
            tiroSinConectar = true;
        }
        ok("un socket sin conectar no da flujos", tiroSinConectar);
        suelto.close();

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("SocketTest " + SocketTest.run());
    }
}
