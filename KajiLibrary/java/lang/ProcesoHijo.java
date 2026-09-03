package java.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jdk.internal.proc.Proc;

// La implementacion de `Process` sobre un proceso del sistema. De paquete: se llega por
// `ProcessBuilder.start()`, que es la unica forma de fabricar uno -- igual que en el JDK.
//
// Todo el estado es el `handle`, un indice en la tabla de procesos de la VM. Los tres flujos son
// vistas sobre ese handle, no buffers propios: leer de `getInputStream()` lee de la tuberia del hijo
// en ese momento, que es lo que un flujo promete.
//
// Por que los flujos son clases anidadas y no una sola parametrizada por "cual": porque la entrada
// escribe y las otras dos leen, y una clase que hiciera las dos cosas tendria la mitad de sus metodos
// tirando. Dos clases chicas dicen mejor lo que cada una es.
final class ProcesoHijo extends Process {

    private final int handle;
    private final OutputStream entrada;
    private final InputStream salida;
    private final InputStream error;

    ProcesoHijo(int handle) {
        this.handle = handle;
        this.entrada = new Entrada(handle);
        this.salida = new Salida(handle, true);
        this.error = new Salida(handle, false);
    }

    // Ojo con los nombres, que estan al reves de lo que uno esperaria y es asi en el JDK: el
    // "OutputStream" del proceso es SU ENTRADA -- se escribe ahi para mandarle datos--, y el
    // "InputStream" es SU SALIDA. Estan nombrados desde el punto de vista del que llama.
    public OutputStream getOutputStream() {
        return this.entrada;
    }

    public InputStream getInputStream() {
        return this.salida;
    }

    public InputStream getErrorStream() {
        return this.error;
    }

    public int waitFor() throws InterruptedException {
        return Proc.waitFor(this.handle);
    }

    /**
     * @throws IllegalThreadStateException si todavia no termino
     */
    public int exitValue() {
        int c = Proc.exitValue(this.handle);
        if (c == Integer.MIN_VALUE) {
            throw new IllegalThreadStateException("el proceso todavia no termino");
        }
        return c;
    }

    public void destroy() {
        Proc.destroy(this.handle, false);
    }

    /**
     * Lo mata sin darle oportunidad de terminar.
     *
     * <p>En Windows es lo mismo que {@link #destroy()}: el sistema no tiene una senial "amable" que
     * un proceso pueda atender, asi que los dos matan igual. Se dice aca en vez de fingir dos
     * comportamientos, y por eso {@link #supportsNormalTermination()} devuelve `false` alli.
     */
    public Process destroyForcibly() {
        Proc.destroy(this.handle, true);
        return this;
    }

    public boolean isAlive() {
        return Proc.isAlive(this.handle);
    }

    public long pid() {
        long p = Proc.pid(this.handle);
        if (p < 0L) {
            throw new UnsupportedOperationException("no se pudo obtener el pid");
        }
        return p;
    }

    /**
     * Si {@link #destroy()} pide la terminacion en vez de forzarla.
     *
     * <p>`false` en Windows y `true` en el resto, que es exactamente lo que el JDK contesta. No es una
     * limitacion de esta biblioteca: es una diferencia entre sistemas operativos.
     */
    public boolean supportsNormalTermination() {
        return !System.getProperty("os.name", "").startsWith("Windows");
    }

    // La entrada del hijo. Cada escritura va derecho al nativo y se vacia: no se acumula nada de este
    // lado, porque un buffer propio haria que el hijo no viera lo que ya se le escribio hasta que
    // alguien llamara a `flush()`, y el que escribe no tiene por que saber eso.
    private static final class Entrada extends OutputStream {

        private final int handle;
        private boolean cerrada;

        Entrada(int handle) {
            this.handle = handle;
        }

        public void write(int b) throws IOException {
            byte[] uno = new byte[] { (byte) b };
            this.write(uno, 0, 1);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (this.cerrada) {
                throw new IOException("la entrada del proceso esta cerrada");
            }
            if (len == 0) {
                return;
            }
            if (!Proc.writeIn(this.handle, b, off, len)) {
                throw new IOException("no se pudo escribir en el proceso");
            }
        }

        public void close() throws IOException {
            if (!this.cerrada) {
                this.cerrada = true;
                Proc.closeIn(this.handle);
            }
        }
    }

    // La salida o el error del hijo, segun `esSalida`. Lee directo de la tuberia.
    private static final class Salida extends InputStream {

        private final int handle;
        private final boolean esSalida;

        Salida(int handle, boolean esSalida) {
            this.handle = handle;
            this.esSalida = esSalida;
        }

        public int read() throws IOException {
            byte[] uno = new byte[1];
            int n = this.read(uno, 0, 1);
            if (n <= 0) {
                return -1;
            }
            // A 0..255, porque `read()` devuelve un byte sin signo y -1 solo significa fin.
            return uno[0] & 0xFF;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            // El nativo llena desde el principio del arreglo que se le pasa, asi que cuando el
            // llamador pide un tramo se usa un arreglo propio y se copia. Es una copia de mas y evita
            // que el nativo tenga que saber de offsets.
            byte[] buf = off == 0 && len == b.length ? b : new byte[len];
            int n = this.esSalida ? Proc.readOut(this.handle, buf) : Proc.readErr(this.handle, buf);
            if (n > 0 && buf != b) {
                System.arraycopy(buf, 0, b, off, n);
            }
            return n;
        }
    }
}
