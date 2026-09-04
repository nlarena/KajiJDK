import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

/**
 * Los canales de red: `ServerSocketChannel` escucha, `SocketChannel` conecta, `DatagramChannel`
 * manda datagramas.
 *
 * <p>Todo contra `127.0.0.1` y con los puertos que elija el sistema: la prueba no toca la red ni pisa
 * un puerto que alguien mas pueda estar usando.
 *
 * <p>Lo que se comprueba, ademas de que los bytes crucen, es **lo que separa a un canal de un
 * socket**: que en modo no bloqueante `accept()` devuelva `null` en vez de esperar, que `receive()`
 * haga lo mismo, y que `socket()` entregue un objeto de `java.net` sobre el mismo descriptor.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `java.nio.channels`.
 */
public class ChannelNetTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    static String texto(ByteBuffer b) {
        byte[] bs = new byte[b.remaining()];
        b.get(bs, 0, bs.length);
        try {
            return new String(bs, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }

    static ByteBuffer bytes(String s) throws IOException {
        return ByteBuffer.wrap(s.getBytes("UTF-8"));
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- escuchar
        ServerSocketChannel escucha = ServerSocketChannel.open();
        ok("nace abierto", escucha.isOpen());
        ok("y bloqueante", escucha.isBlocking());
        escucha.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        InetSocketAddress local = (InetSocketAddress) escucha.getLocalAddress();
        ok("queda atado a un puerto", local != null && local.getPort() > 0);
        boolean tiroDosVeces = false;
        try {
            escucha.bind(new InetSocketAddress(0));
        } catch (AlreadyBoundException e) {
            tiroDosVeces = true;
        }
        ok("atar dos veces tira", tiroDosVeces);

        // ---- sin bloquear y sin nadie, accept devuelve null
        escucha.configureBlocking(false);
        ok("quedo no bloqueante", !escucha.isBlocking());
        ok("accept sin nadie da null", escucha.accept() == null);

        // ---- conectar y hablar
        SocketChannel cliente = SocketChannel.open(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), local.getPort()));
        ok("el cliente queda conectado", cliente.isConnected());
        ok("y no queda nada pendiente", !cliente.isConnectionPending());

        // El accept del otro lado. Sigue en modo no bloqueante, asi que se insiste un rato: la
        // conexion puede tardar un instante en aparecer en la cola del escucha.
        SocketChannel atendido = null;
        for (int i = 0; i < 3000 && atendido == null; i++) {
            atendido = escucha.accept();
            if (atendido == null) {
                Thread.sleep(1);
            }
        }
        ok("el escucha acepta la conexion", atendido != null);
        if (atendido == null) {
            return failures;
        }

        int escritos = cliente.write(ChannelNetTest.bytes("hola"));
        ok("se escriben los cuatro bytes", escritos == 4);

        ByteBuffer buf = ByteBuffer.allocate(64);
        int leidos = atendido.read(buf);
        ok("y llegan los cuatro", leidos == 4);
        buf.flip();
        ok("y son los mismos", "hola".equals(ChannelNetTest.texto(buf)));

        // ---- las direcciones
        ok("el atendido sabe con quien habla",
                ((InetSocketAddress) atendido.getRemoteAddress()).getPort()
                        == ((InetSocketAddress) cliente.getLocalAddress()).getPort());

        // ---- el socket que envuelve al canal comparte el descriptor
        java.net.Socket envoltorio = cliente.socket();
        ok("socket() da un java.net.Socket", envoltorio != null);
        ok("y esta conectado al mismo lugar", envoltorio.getPort() == local.getPort());
        java.net.ServerSocket envoltorioEscucha = escucha.socket();
        ok("el escucha tambien da el suyo",
                envoltorioEscucha != null && envoltorioEscucha.getLocalPort() == local.getPort());

        // ---- cerrar
        cliente.close();
        ok("cerrado esta cerrado", !cliente.isOpen());
        atendido.close();
        escucha.close();
        ok("el escucha queda cerrado", !escucha.isOpen());

        // ---- una direccion sin resolver se rechaza antes de tocar la red
        boolean tiroSinResolver = false;
        SocketChannel suelto = SocketChannel.open();
        try {
            suelto.connect(InetSocketAddress.createUnresolved("no.existe.invalido", 80));
        } catch (UnresolvedAddressException e) {
            tiroSinResolver = true;
        }
        ok("una direccion sin resolver tira", tiroSinResolver);
        suelto.close();

        // ---- datagramas
        DatagramChannel receptor = DatagramChannel.open();
        receptor.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        InetSocketAddress dirReceptor = (InetSocketAddress) receptor.getLocalAddress();
        ok("el receptor queda atado", dirReceptor != null && dirReceptor.getPort() > 0);

        receptor.configureBlocking(false);
        ok("receive sin nada da null", receptor.receive(ByteBuffer.allocate(16)) == null);

        DatagramChannel emisor = DatagramChannel.open(StandardProtocolFamily.INET);
        emisor.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        int mandados = emisor.send(ChannelNetTest.bytes("chau"), dirReceptor);
        ok("se mandan los cuatro bytes", mandados == 4);

        receptor.configureBlocking(true);
        ByteBuffer dbuf = ByteBuffer.allocate(64);
        java.net.SocketAddress fuente = receptor.receive(dbuf);
        ok("el datagrama llega", fuente != null);
        dbuf.flip();
        ok("y es el que se mando", "chau".equals(ChannelNetTest.texto(dbuf)));
        ok("y se sabe de quien vino",
                ((InetSocketAddress) fuente).getPort()
                        == ((InetSocketAddress) emisor.getLocalAddress()).getPort());

        // ---- un canal de datagramas sin conectar no tiene forma de flujo
        boolean tiroSinConectar = false;
        try {
            emisor.write(ChannelNetTest.bytes("x"));
        } catch (NotYetConnectedException e) {
            tiroSinConectar = true;
        }
        ok("write sin conectar tira", tiroSinConectar);

        // ---- conectar fija el destino
        emisor.connect(dirReceptor);
        ok("queda conectado", emisor.isConnected());
        emisor.write(ChannelNetTest.bytes("otra"));
        ByteBuffer d2 = ByteBuffer.allocate(64);
        receptor.receive(d2);
        d2.flip();
        ok("el datagrama de un canal conectado llega", "otra".equals(ChannelNetTest.texto(d2)));
        emisor.disconnect();
        ok("desconectar deja de estar conectado", !emisor.isConnected());

        // ---- el socket que envuelve al canal de datagramas
        java.net.DatagramSocket envoltorioUdp = receptor.socket();
        ok("socket() da un java.net.DatagramSocket",
                envoltorioUdp != null && envoltorioUdp.getLocalPort() == dirReceptor.getPort());

        // ---- multidifusion
        //
        // No se comprueba que un datagrama multicast llegue: eso depende de que la maquina tenga una
        // placa que lo enrute, y una prueba que falla segun donde corra no prueba nada. Lo que si se
        // comprueba es que la membresia se pida de verdad y que la llave diga lo que tiene que decir.
        DatagramChannel grupo = DatagramChannel.open(StandardProtocolFamily.INET);
        grupo.bind(new InetSocketAddress(0));
        InetAddress dirGrupo = InetAddress.getByName("230.0.0.1");
        NetworkInterface placa = ChannelNetTest.unaPlaca();
        if (placa != null) {
            MembershipKey llave = grupo.join(dirGrupo, placa);
            ok("la llave nace vigente", llave.isValid());
            ok("y sabe de que grupo", dirGrupo.equals(llave.group()));
            ok("y por que placa", placa.equals(llave.networkInterface()));
            ok("y de que canal", llave.channel() == grupo);
            ok("sin emisor es null", llave.sourceAddress() == null);
            llave.drop();
            ok("dada de baja deja de ser vigente", !llave.isValid());
            llave.drop();
            ok("darla de baja dos veces no hace nada", !llave.isValid());
        }
        boolean tiroNoMulticast = false;
        try {
            grupo.join(InetAddress.getByName("127.0.0.1"), placa);
        } catch (IllegalArgumentException e) {
            tiroNoMulticast = true;
        }
        ok("una direccion que no es multicast se rechaza", tiroNoMulticast);

        grupo.close();
        emisor.close();
        receptor.close();

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    /** Una placa cualquiera que sirva para multidifusion, o null si esta VM no las enumera. */
    static NetworkInterface unaPlaca() {
        try {
            java.util.Enumeration<NetworkInterface> placas =
                    NetworkInterface.getNetworkInterfaces();
            while (placas != null && placas.hasMoreElements()) {
                NetworkInterface n = placas.nextElement();
                if (n.isLoopback() && n.getInetAddresses().hasMoreElements()) {
                    return n;
                }
            }
        } catch (java.net.SocketException e) {
            // Enumerar placas es una llamada al sistema que no todas las VM exponen. No hay placa
            // que probar y no hay nada que afirmar; lo demas se prueba igual.
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("ChannelNetTest " + ChannelNetTest.run());
    }
}
