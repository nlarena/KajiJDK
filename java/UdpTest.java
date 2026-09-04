import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

/**
 * UDP de verdad: un datagrama sale de un socket y entra en otro.
 *
 * <p>Todo contra `127.0.0.1` y con los puertos que elija el sistema (`new DatagramSocket(0)`): la
 * prueba no toca la red ni pisa un puerto que alguien mas pueda estar usando.
 *
 * <p>Lo que se comprueba es lo que hasta hace poco esta biblioteca no podia prometer: que lo que se
 * manda llega, que el receptor sabe de quien vino, que `setSoTimeout` corta una espera que no llega,
 * y que un socket conectado filtra. El multicast se prueba hasta donde se puede sin depender de la
 * red de la maquina: que entrar y salir de un grupo no falla.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `java.net`.
 */
public class UdpTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- atar en el puerto que el sistema elija
        DatagramSocket receptor = new DatagramSocket(0);
        ok("el receptor queda atado", receptor.isBound());
        ok("no esta cerrado", !receptor.isClosed());
        int puerto = receptor.getLocalPort();
        ok("el sistema dio un puerto", puerto > 0);

        DatagramSocket emisor = new DatagramSocket(0);
        ok("el emisor tiene su propio puerto",
                emisor.getLocalPort() > 0 && emisor.getLocalPort() != puerto);

        // ---- un datagrama cruza
        byte[] carga = "hola".getBytes("UTF-8");
        InetAddress local = InetAddress.getByName("127.0.0.1");
        emisor.send(new DatagramPacket(carga, carga.length, local, puerto));

        receptor.setSoTimeout(3000);
        byte[] buf = new byte[64];
        DatagramPacket recibido = new DatagramPacket(buf, buf.length);
        receptor.receive(recibido);
        ok("lo mandado llega entero", 4 == recibido.getLength());
        ok("y es lo mismo",
                "hola".equals(new String(recibido.getData(), recibido.getOffset(),
                        recibido.getLength(), "UTF-8")));
        ok("el receptor sabe de quien vino",
                "127.0.0.1".equals(recibido.getAddress().getHostAddress()));
        ok("y de que puerto", recibido.getPort() == emisor.getLocalPort());

        // ---- el plazo corta una espera que no llega
        receptor.setSoTimeout(300);
        long t0 = System.currentTimeMillis();
        boolean vencio = false;
        try {
            receptor.receive(new DatagramPacket(new byte[16], 16));
        } catch (SocketTimeoutException e) {
            vencio = true;
        }
        ok("el plazo corta la espera", vencio);
        ok("y no antes de tiempo", System.currentTimeMillis() - t0 >= 250);

        // ---- connect fija el destino: se puede mandar sin decirlo en cada paquete
        emisor.connect(local, puerto);
        ok("queda conectado", emisor.isConnected());
        ok("y sabe a donde", emisor.getPort() == puerto);
        byte[] otra = "chau".getBytes("UTF-8");
        emisor.send(new DatagramPacket(otra, otra.length));
        receptor.setSoTimeout(3000);
        DatagramPacket segundo = new DatagramPacket(new byte[64], 64);
        receptor.receive(segundo);
        ok("el datagrama de un socket conectado llega",
                "chau".equals(new String(segundo.getData(), segundo.getOffset(),
                        segundo.getLength(), "UTF-8")));

        // Un paquete con otro destino sobre un socket conectado se rechaza, y eso es del contrato.
        boolean tiroDestino = false;
        try {
            emisor.send(new DatagramPacket(otra, otra.length, local, puerto + 1));
        } catch (IllegalArgumentException e) {
            tiroDestino = true;
        }
        ok("un destino distinto del fijado se rechaza", tiroDestino);

        emisor.disconnect();
        ok("desconectar deja de estar conectado", !emisor.isConnected());

        // ---- cerrar
        emisor.close();
        ok("cerrado esta cerrado", emisor.isClosed());
        receptor.close();

        // ---- un socket sin atar se puede tener y configurar
        // El cast es por el #485: nuestro compilador todavia no descarta el constructor
        // `protected` --invisible desde aca-- antes de decidir que `null` es ambiguo.
        DatagramSocket suelto = new DatagramSocket((java.net.SocketAddress) null);
        ok("un socket sin atar no esta atado", !suelto.isBound());
        suelto.setSoTimeout(50);
        ok("y las opciones andan igual", suelto.getSoTimeout() == 50);
        suelto.close();

        // ---- multicast: entrar y salir de un grupo
        //
        // No se comprueba que un datagrama multicast llegue: eso depende de que la maquina tenga una
        // placa que lo enrute, y una prueba que falla segun donde corra no prueba nada. Lo que si se
        // puede comprobar --y es lo que fallaba antes-- es que la membresia se pida de verdad.
        MulticastSocket grupo = new MulticastSocket(0);
        ok("el multicast queda atado", grupo.isBound());
        grupo.setTimeToLive(1);
        ok("el ttl se lee como se fijo", grupo.getTimeToLive() == 1);
        InetAddress dirGrupo = InetAddress.getByName("230.0.0.1");
        ok("es una direccion multicast", dirGrupo.isMulticastAddress());
        boolean entro = true;
        try {
            grupo.joinGroup(new InetSocketAddress(dirGrupo, 0), null);
            grupo.leaveGroup(new InetSocketAddress(dirGrupo, 0), null);
        } catch (IOException e) {
            entro = false;
            System.out.println("  (" + e + ")");
        }
        ok("entrar y salir de un grupo no falla", entro);

        // Una direccion que no es multicast se rechaza. El JDK tira `SocketException` aunque su
        // javadoc prometa `IllegalArgumentException`; esta prueba sigue lo que hace.
        boolean tiroNoMulticast = false;
        try {
            grupo.joinGroup(new InetSocketAddress(local, 0), null);
        } catch (java.net.SocketException e) {
            tiroNoMulticast = true;
        }
        ok("una direccion que no es multicast se rechaza", tiroNoMulticast);
        grupo.close();

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("UdpTest " + UdpTest.run());
    }
}
