import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

// Prueba de comportamiento de java.nio.channels. Corre igual en las dos VMs: la del arbol y el JDK
// 25 de verdad. Nada de red -- solo archivos, arreglos de bytes y aritmetica de FileLock, que es
// todo lo que esta biblioteca implementa con logica propia.
//
// `run()` devuelve -1 si pasa todo, o el indice de la primera comprobacion que falla. Cada `if` que
// devuelve un numero es una comprobacion, y los numeros no se reordenan: si mañana se agrega una, va
// al final, para que un numero viejo en un informe viejo siga significando lo mismo.
public class ChanTest {

    // Un FileLock concreto: la clase es abstracta y sus partes calculables --overlaps, position,
    // size, isShared-- no dependen de ningun nativo. Es la unica forma de probarlas.
    static class Candado extends FileLock {
        Candado(FileChannel ch, long pos, long size, boolean shared) {
            super(ch, pos, size, shared);
        }

        public boolean isValid() {
            return true;
        }

        public void release() {
        }
    }

    static byte[] leerTodo(Path p) throws Exception {
        return Files.readAllBytes(p);
    }

    public static int run() {
        try {
            return correr();
        } catch (Throwable t) {
            // Una excepcion que se escapa es un fallo del que hay que enterarse, no un "no paso
            // nada": 999 no es ningun indice valido, asi que no se confunde con una comprobacion.
            return 999;
        }
    }

    static int correr() throws Exception {
        Path p = Path.of("chantest.tmp");
        Path q = Path.of("chantest2.tmp");
        Files.deleteIfExists(p);
        Files.deleteIfExists(q);

        // ---- Channels: de stream a canal -------------------------------------------------------
        byte[] fuente = new byte[] {10, 20, 30, 40, 50};
        ReadableByteChannel rc = Channels.newChannel(new ByteArrayInputStream(fuente));
        ByteBuffer bb = ByteBuffer.allocate(3);
        int n = rc.read(bb);
        if (n != 3) {
            return 0;
        }
        if (bb.position() != 3) {
            return 1;
        }
        bb.clear();
        n = rc.read(bb);
        if (n != 2) {
            return 2;
        }
        bb.clear();
        n = rc.read(bb);
        if (n != -1) {
            return 3;
        }
        if (!rc.isOpen()) {
            return 4;
        }
        rc.close();
        if (rc.isOpen()) {
            return 5;
        }

        // ---- Channels: de canal a stream -------------------------------------------------------
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WritableByteChannel wc = Channels.newChannel(bos);
        int escritos = wc.write(ByteBuffer.wrap(fuente));
        if (escritos != 5) {
            return 6;
        }
        wc.close();
        byte[] salida = bos.toByteArray();
        if (salida.length != 5 || salida[0] != 10 || salida[4] != 50) {
            return 7;
        }

        // Ida y vuelta por los dos adaptadores a la vez.
        InputStream is = Channels.newInputStream(Channels.newChannel(
                new ByteArrayInputStream(fuente)));
        int suma = 0;
        int b = is.read();
        while (b >= 0) {
            suma = suma + b;
            b = is.read();
        }
        if (suma != 150) {
            return 8;
        }

        // ---- FileChannel: escribir -------------------------------------------------------------
        FileChannel fc = FileChannel.open(p, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        n = fc.write(ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5}));
        if (n != 5) {
            return 9;
        }
        if (fc.position() != 5) {
            return 10;
        }
        if (fc.size() != 5) {
            return 11;
        }
        fc.force(true);
        fc.close();
        if (fc.isOpen()) {
            return 12;
        }
        byte[] enDisco = leerTodo(p);
        if (enDisco.length != 5 || enDisco[0] != 1 || enDisco[4] != 5) {
            return 13;
        }

        // ---- FileChannel: leer -----------------------------------------------------------------
        fc = FileChannel.open(p);
        ByteBuffer diez = ByteBuffer.allocate(10);
        n = fc.read(diez);
        if (n != 5) {
            return 14;
        }
        if (fc.position() != 5) {
            return 15;
        }
        n = fc.read(diez);
        if (n != -1) {
            return 16;
        }
        // Lectura por posicion absoluta: no mueve la posicion corriente.
        ByteBuffer tres = ByteBuffer.allocate(10);
        n = fc.read(tres, 2);
        if (n != 3) {
            return 17;
        }
        if (fc.position() != 5) {
            return 18;
        }
        if (tres.array()[0] != 3) {
            return 19;
        }
        // Leer mas alla del final es -1, no 0.
        n = fc.read(ByteBuffer.allocate(4), 99);
        if (n != -1) {
            return 20;
        }
        // Escribir en un canal de solo lectura.
        boolean tiro = false;
        try {
            fc.write(ByteBuffer.wrap(new byte[] {9}));
        } catch (java.nio.channels.NonWritableChannelException e) {
            tiro = true;
        }
        if (!tiro) {
            return 21;
        }
        fc.close();
        // Cerrado: cualquier operacion es ClosedChannelException.
        tiro = false;
        try {
            fc.position();
        } catch (java.nio.channels.ClosedChannelException e) {
            tiro = true;
        }
        if (!tiro) {
            return 22;
        }

        // ---- FileChannel: hueco de ceros y truncado --------------------------------------------
        fc = FileChannel.open(p, StandardOpenOption.WRITE);
        fc.position(8);
        fc.write(ByteBuffer.wrap(new byte[] {7, 7}));
        if (fc.size() != 10) {
            return 23;
        }
        fc.close();
        enDisco = leerTodo(p);
        if (enDisco.length != 10) {
            return 24;
        }
        if (enDisco[5] != 0 || enDisco[6] != 0 || enDisco[7] != 0) {
            return 25;
        }
        if (enDisco[8] != 7 || enDisco[9] != 7) {
            return 26;
        }

        fc = FileChannel.open(p, StandardOpenOption.WRITE);
        fc.position(9);
        fc.truncate(4);
        if (fc.size() != 4) {
            return 27;
        }
        // La posicion no puede quedar mas alla del nuevo final.
        if (fc.position() != 4) {
            return 28;
        }
        // Truncar a mas de lo que hay no agranda.
        fc.truncate(100);
        if (fc.size() != 4) {
            return 29;
        }
        fc.close();

        // ---- FileChannel: APPEND ---------------------------------------------------------------
        fc = FileChannel.open(p, StandardOpenOption.APPEND);
        fc.position(0);
        fc.write(ByteBuffer.wrap(new byte[] {99}));
        if (fc.size() != 5) {
            return 30;
        }
        fc.close();
        enDisco = leerTodo(p);
        if (enDisco[4] != 99) {
            return 31;
        }
        // READ con APPEND es contradictorio.
        tiro = false;
        try {
            FileChannel.open(p, StandardOpenOption.READ, StandardOpenOption.APPEND);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        if (!tiro) {
            return 32;
        }

        // ---- FileChannel: apertura -------------------------------------------------------------
        tiro = false;
        try {
            FileChannel.open(q);
        } catch (java.nio.file.NoSuchFileException e) {
            tiro = true;
        }
        if (!tiro) {
            return 33;
        }
        tiro = false;
        try {
            FileChannel.open(p, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (java.nio.file.FileAlreadyExistsException e) {
            tiro = true;
        }
        if (!tiro) {
            return 34;
        }
        // Solo escritura: leer es NonReadableChannelException.
        fc = FileChannel.open(p, StandardOpenOption.WRITE);
        tiro = false;
        try {
            fc.read(ByteBuffer.allocate(4));
        } catch (java.nio.channels.NonReadableChannelException e) {
            tiro = true;
        }
        if (!tiro) {
            return 35;
        }
        fc.close();

        // ---- FileChannel: transferencias -------------------------------------------------------
        // El archivo tiene 5 bytes: {1,2,3,4,99}.
        fc = FileChannel.open(p);
        ByteArrayOutputStream destino = new ByteArrayOutputStream();
        long t = fc.transferTo(1, 3, Channels.newChannel(destino));
        if (t != 3) {
            return 36;
        }
        // transferTo no mueve la posicion corriente.
        if (fc.position() != 0) {
            return 37;
        }
        byte[] transferido = destino.toByteArray();
        if (transferido.length != 3 || transferido[0] != 2 || transferido[2] != 4) {
            return 38;
        }
        // Desde mas alla del final no se transfiere nada.
        if (fc.transferTo(50, 10, Channels.newChannel(new ByteArrayOutputStream())) != 0) {
            return 39;
        }
        fc.close();

        fc = FileChannel.open(q, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        long tf = fc.transferFrom(Channels.newChannel(
                new ByteArrayInputStream(new byte[] {8, 8, 8})), 0, 3);
        if (tf != 3) {
            return 40;
        }
        fc.close();
        if (leerTodo(q).length != 3) {
            return 41;
        }

        // ---- FileChannel: varios buffers -------------------------------------------------------
        fc = FileChannel.open(p);
        ByteBuffer b1 = ByteBuffer.allocate(2);
        ByteBuffer b2 = ByteBuffer.allocate(2);
        long leidos = fc.read(new ByteBuffer[] {b1, b2});
        if (leidos != 4) {
            return 42;
        }
        if (b1.array()[0] != 1 || b2.array()[0] != 3) {
            return 43;
        }
        fc.close();

        fc = FileChannel.open(q, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        long puestos = fc.write(new ByteBuffer[] {
            ByteBuffer.wrap(new byte[] {1, 2}),
            ByteBuffer.wrap(new byte[] {3, 4, 5})});
        if (puestos != 5) {
            return 44;
        }
        fc.close();
        enDisco = leerTodo(q);
        if (enDisco.length != 5 || enDisco[2] != 3 || enDisco[4] != 5) {
            return 45;
        }

        // ---- Channels: texto -------------------------------------------------------------------
        ByteArrayOutputStream texto = new ByteArrayOutputStream();
        Writer w = Channels.newWriter(Channels.newChannel(texto), StandardCharsets.UTF_8);
        w.write("hola");
        w.close();
        if (texto.toByteArray().length != 4) {
            return 46;
        }
        Reader r = Channels.newReader(
                Channels.newChannel(new ByteArrayInputStream(texto.toByteArray())),
                StandardCharsets.UTF_8);
        char[] buf = new char[10];
        int cuantos = r.read(buf, 0, 10);
        if (cuantos != 4) {
            return 47;
        }
        if (buf[0] != 'h' || buf[3] != 'a') {
            return 48;
        }
        r.close();

        // ---- SelectionKey: las constantes no son consecutivas ----------------------------------
        if (SelectionKey.OP_READ != 1) {
            return 49;
        }
        if (SelectionKey.OP_WRITE != 4) {
            return 50;
        }
        if (SelectionKey.OP_CONNECT != 8) {
            return 51;
        }
        if (SelectionKey.OP_ACCEPT != 16) {
            return 52;
        }

        // ---- FileLock: la aritmetica de rangos -------------------------------------------------
        fc = FileChannel.open(p);
        FileLock l = new Candado(fc, 10, 5, false);
        if (l.position() != 10 || l.size() != 5 || l.isShared()) {
            return 53;
        }
        if (l.channel() != fc) {
            return 54;
        }
        if (l.acquiredBy() != fc) {
            return 55;
        }
        // [10,15) contra [0,10): pegados, no solapan.
        if (l.overlaps(0, 10)) {
            return 56;
        }
        // [10,15) contra [0,11): un byte en comun.
        if (!l.overlaps(0, 11)) {
            return 57;
        }
        // [10,15) contra [15,5): pegados del otro lado.
        if (l.overlaps(15, 5)) {
            return 58;
        }
        // Contenido entero.
        if (!l.overlaps(11, 2)) {
            return 59;
        }
        tiro = false;
        try {
            new Candado(fc, -1, 5, false);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        if (!tiro) {
            return 60;
        }
        fc.close();

        Files.deleteIfExists(p);
        Files.deleteIfExists(q);
        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
