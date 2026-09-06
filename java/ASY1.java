import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Comprueba los nueve estaticos asincronicos de {@code java.nio.channels} contra el JDK 25.
 *
 * <h2>Que se compara</h2>
 *
 * <p>Que los grupos se armen, se apaguen en sus dos formas y esperen; que un canal de archivo
 * asincronico lea y escriba de verdad por las dos vias --{@code Future} y {@code CompletionHandler}--
 * y que su lectura vea lo que su escritura dejo; y que un servidor y un cliente asincronicos se
 * conecten sobre {@code localhost} y se pasen bytes.
 *
 * <p>Lo que no se compara son los tiempos ni el paralelismo, ni cuando termina un grupo que quedo
 * con operaciones pedidas: ahi las dos implementaciones esperan cosas distintas --el JDK al sistema,
 * esta al pool-- y comparar eso seria comparar el modelo de espera y no la API. Abajo de esta biblioteca hay canales
 * bloqueantes en un pool de hilos --como el JDK en las plataformas sin entrada y salida asincronica
 * del sistema-- asi que las respuestas son las mismas pero el costo no. Eso esta documentado en las
 * clases, no medido aca.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class ASY1 {

    static final String[] ESPERADO = {
        "fijo|true|false|false|true",
        "cache|true|false",
        "pool|true|false",
        "mismo-proveedor|true|true",
        "apagado|true|true|true",
        "apagado-ya|true|true",
        "malos|IllegalArgumentException|NullPointerException|NullPointerException|NullPointerException",
        "escritura|8|8",
        "lectura|8|0a141e28323c4650",
        "lectura-parcial|3|3c4650",
        "manejador|ok:4:true|1e28323c",
        "mas-alla-del-fin|-1",
        "posicion-negativa|IllegalArgumentException",
        "truncar|4",
        "cerrado|false",
        "servidor|true|true",
        "conectados|true|true",
        "enviado|5",
        "recibido|5|0102030405",
        "dos-lecturas|ReadPendingException",
        "dos-aceptaciones|AcceptPendingException",
        "ya-conectado|AlreadyConnectedException",
        "cerrados|false|false",
        "grupo-limpio|true|true",
        "grupo-apagado|ShutdownChannelGroupException",
    };

    /** Lo que hacen los nueve estaticos, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        // Los tres grupos.
        final AsynchronousChannelGroup g1 =
                AsynchronousChannelGroup.withFixedThreadPool(2, Executors.defaultThreadFactory());
        a.add("fijo|" + (g1 != null) + "|" + g1.isShutdown() + "|" + g1.isTerminated()
                + "|" + (g1.provider() != null));
        final ExecutorService p2 = Executors.newCachedThreadPool();
        final AsynchronousChannelGroup g2 = AsynchronousChannelGroup.withCachedThreadPool(p2, 1);
        a.add("cache|" + (g2 != null) + "|" + g2.isShutdown());
        final ExecutorService p3 = Executors.newFixedThreadPool(2);
        final AsynchronousChannelGroup g3 = AsynchronousChannelGroup.withThreadPool(p3);
        a.add("pool|" + (g3 != null) + "|" + g3.isShutdown());
        a.add("mismo-proveedor|" + (g1.provider() == g2.provider())
                + "|" + (g2.provider() == g3.provider()));

        // Un grupo vacio se apaga y termina.
        g1.shutdown();
        a.add("apagado|" + g1.isShutdown() + "|" + g1.awaitTermination(5, TimeUnit.SECONDS)
                + "|" + g1.isTerminated());
        g2.shutdownNow();
        a.add("apagado-ya|" + g2.isShutdown() + "|" + g2.awaitTermination(5, TimeUnit.SECONDS));

        // Los argumentos que no sirven.
        a.add("malos|" + intentar(new FijoCero()) + "|" + intentar(new FijoSinFabrica())
                + "|" + intentar(new PoolNulo()) + "|" + intentar(new CacheNulo()));

        // Un canal de archivo asincronico.
        final Path tmp = Files.createTempFile("asy1", ".bin");
        try {
            final AsynchronousFileChannel af = AsynchronousFileChannel.open(tmp,
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
            final byte[] datos = {10, 20, 30, 40, 50, 60, 70, 80};
            final Future<Integer> fw = af.write(ByteBuffer.wrap(datos), 0L);
            a.add("escritura|" + fw.get(10, TimeUnit.SECONDS) + "|" + af.size());
            final ByteBuffer leido = ByteBuffer.allocate(8);
            final Future<Integer> fr = af.read(leido, 0L);
            a.add("lectura|" + fr.get(10, TimeUnit.SECONDS) + "|" + hex(leido.array()));
            final ByteBuffer parcial = ByteBuffer.allocate(3);
            a.add("lectura-parcial|" + af.read(parcial, 5L).get(10, TimeUnit.SECONDS)
                    + "|" + hex(parcial.array()));

            // Por manejador, que corre en el pool y no en este hilo.
            final Caja caja = new Caja();
            final ByteBuffer otro = ByteBuffer.allocate(4);
            af.read(otro, 2L, caja, new Anota(caja));
            a.add("manejador|" + caja.esperar() + "|" + hex(otro.array()));

            a.add("mas-alla-del-fin|"
                    + af.read(ByteBuffer.allocate(4), 100L).get(10, TimeUnit.SECONDS));
            a.add("posicion-negativa|" + intentar(new PosNegativa(af)));
            a.add("truncar|" + af.truncate(4).size());
            af.force(true);
            af.close();
            a.add("cerrado|" + af.isOpen());
        } finally {
            Files.deleteIfExists(tmp);
        }

        // Un servidor y un cliente asincronicos que se hablan.
        final AsynchronousChannelGroup g4 =
                AsynchronousChannelGroup.withFixedThreadPool(4, Executors.defaultThreadFactory());
        final AsynchronousServerSocketChannel srv = AsynchronousServerSocketChannel.open(g4);
        srv.bind(new InetSocketAddress("127.0.0.1", 0));
        final int puerto = ((InetSocketAddress) srv.getLocalAddress()).getPort();
        a.add("servidor|" + (puerto > 0) + "|" + srv.isOpen());

        final Future<AsynchronousSocketChannel> entrante = srv.accept();
        final AsynchronousSocketChannel cli = AsynchronousSocketChannel.open(g4);
        cli.connect(new InetSocketAddress("127.0.0.1", puerto)).get(10, TimeUnit.SECONDS);
        final AsynchronousSocketChannel servidorLado = entrante.get(10, TimeUnit.SECONDS);
        a.add("conectados|" + (servidorLado != null) + "|" + cli.isOpen());

        final byte[] mensaje = {1, 2, 3, 4, 5};
        a.add("enviado|" + cli.write(ByteBuffer.wrap(mensaje)).get(10, TimeUnit.SECONDS));
        final ByteBuffer recibido = ByteBuffer.allocate(5);
        int total = 0;
        while (total < 5) {
            final int n = servidorLado.read(recibido).get(10, TimeUnit.SECONDS);
            if (n < 0) {
                break;
            }
            total = total + n;
        }
        a.add("recibido|" + total + "|" + hex(recibido.array()));
        a.add("dos-lecturas|" + intentar(new DosLecturas(servidorLado)));
        a.add("dos-aceptaciones|" + intentar(new DosAceptaciones(srv)));
        a.add("ya-conectado|" + intentar(new YaConectado(cli, puerto)));

        cli.close();
        servidorLado.close();
        srv.close();
        a.add("cerrados|" + cli.isOpen() + "|" + srv.isOpen());
        g4.shutdown();

        // Un grupo cuyo unico canal se cerro termina. No se comprueba `g4`, que quedo con las dos
        // operaciones que `dos-lecturas` y `dos-aceptaciones` dejaron pedidas: ahi las dos
        // implementaciones difieren por como esperan --el JDK por el sistema, esta por el pool-- y
        // comparar eso seria comparar el modelo de espera y no la API.
        final AsynchronousChannelGroup g6 =
                AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory());
        final AsynchronousSocketChannel suelto = AsynchronousSocketChannel.open(g6);
        suelto.close();
        g6.shutdown();
        a.add("grupo-limpio|" + g6.awaitTermination(10, TimeUnit.SECONDS)
                + "|" + g6.isTerminated());

        // Un canal abierto en un grupo ya apagado no se acepta.
        final AsynchronousChannelGroup g5 =
                AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory());
        g5.shutdown();
        a.add("grupo-apagado|" + intentar(new AbrirEnApagado(g5)));

        p3.shutdown();
        return a.toArray(new String[a.size()]);
    }

    /** Los bytes en hexadecimal. */
    static String hex(byte[] b) {
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            final int v = b[i] & 0xff;
            s.append("0123456789abcdef".charAt(v >> 4));
            s.append("0123456789abcdef".charAt(v & 0xf));
        }
        return s.toString();
    }

    /** Donde el manejador deja lo que le avisaron. */
    static class Caja {
        private String valor;

        synchronized void poner(String v) {
            this.valor = v;
            notifyAll();
        }

        synchronized String esperar() throws InterruptedException {
            long queda = 10000L;
            while (this.valor == null && queda > 0) {
                final long antes = System.currentTimeMillis();
                wait(queda);
                queda = queda - (System.currentTimeMillis() - antes);
            }
            return this.valor == null ? "sin aviso" : this.valor;
        }
    }

    /** Un manejador que anota lo que le pasaron. */
    static class Anota implements CompletionHandler<Integer, Caja> {
        private final Caja caja;

        Anota(Caja caja) {
            this.caja = caja;
        }

        public void completed(Integer resultado, Caja adjunto) {
            this.caja.poner("ok:" + resultado + ":" + (adjunto == this.caja));
        }

        public void failed(Throwable t, Caja adjunto) {
            final String n = t.getClass().getName();
            this.caja.poner("falla:" + n.substring(n.lastIndexOf('.') + 1));
        }
    }

    /** Algo que se corre para ver con que falla. */
    interface Tiro {
        void correr() throws Exception;
    }

    /** Corre eso y devuelve "ok" o el nombre simple de lo que haya tirado. */
    static String intentar(Tiro r) {
        try {
            r.correr();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class FijoCero implements Tiro {
        public void correr() throws Exception {
            AsynchronousChannelGroup.withFixedThreadPool(0, Executors.defaultThreadFactory());
        }
    }

    static class FijoSinFabrica implements Tiro {
        public void correr() throws Exception {
            AsynchronousChannelGroup.withFixedThreadPool(1, null);
        }
    }

    static class PoolNulo implements Tiro {
        public void correr() throws Exception {
            AsynchronousChannelGroup.withThreadPool(null);
        }
    }

    static class CacheNulo implements Tiro {
        public void correr() throws Exception {
            AsynchronousChannelGroup.withCachedThreadPool(null, 1);
        }
    }

    static class PosNegativa implements Tiro {
        private final AsynchronousFileChannel af;

        PosNegativa(AsynchronousFileChannel af) {
            this.af = af;
        }

        public void correr() throws Exception {
            af.read(ByteBuffer.allocate(4), -1L);
        }
    }

    static class DosLecturas implements Tiro {
        private final AsynchronousSocketChannel c;

        DosLecturas(AsynchronousSocketChannel c) {
            this.c = c;
        }

        public void correr() throws Exception {
            c.read(ByteBuffer.allocate(4));
            c.read(ByteBuffer.allocate(4));
        }
    }

    static class DosAceptaciones implements Tiro {
        private final AsynchronousServerSocketChannel s;

        DosAceptaciones(AsynchronousServerSocketChannel s) {
            this.s = s;
        }

        public void correr() throws Exception {
            s.accept();
            s.accept();
        }
    }

    static class YaConectado implements Tiro {
        private final AsynchronousSocketChannel c;
        private final int puerto;

        YaConectado(AsynchronousSocketChannel c, int puerto) {
            this.c = c;
            this.puerto = puerto;
        }

        public void correr() throws Exception {
            c.connect(new InetSocketAddress("127.0.0.1", this.puerto));
        }
    }

    static class AbrirEnApagado implements Tiro {
        private final AsynchronousChannelGroup g;

        AbrirEnApagado(AsynchronousChannelGroup g) {
            this.g = g;
        }

        public void correr() throws Exception {
            AsynchronousSocketChannel.open(this.g);
        }
    }

    /**
     * El indice de la primera respuesta que no coincide con la del JDK, o -1.
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
