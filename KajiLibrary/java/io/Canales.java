package java.io;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;

/**
 * Los canales que devuelven {@link FileInputStream#getChannel} y {@link FileOutputStream#getChannel}.
 *
 * <p>Package-private y sin nombre util: nadie deberia escribirlo. Estan aca y no en
 * `java.nio.channels` porque lo unico que los distingue de un canal cualquiera es que **comparten la
 * posicion con su flujo**, y esa posicion vive en el flujo.
 *
 * <h2>Lo unico que hay que entender de estas dos clases</h2>
 *
 * <p>El contrato de {@code getChannel()} no dice "un canal sobre el mismo archivo": dice que la
 * posicion del canal y la del flujo son **el mismo numero**. Leer del flujo mueve el canal; mover el
 * canal cambia desde donde lee el flujo. Es la parte que se equivoca sola si cada uno lleva su
 * cuenta, porque nada falla: simplemente se lee del lugar que no era, y el que llama recibe bytes
 * perfectamente creibles.
 *
 * <p>Por eso ninguno de los dos guarda una posicion propia. Hay un solo numero, y esta en el flujo.
 */
final class Canales {

    private Canales() {
    }

    /**
     * El canal de un {@link FileInputStream}.
     *
     * <p>Lee de **la misma foto** que el flujo, no del disco. Es la consecuencia directa de que el
     * flujo lea el archivo entero al construirse (ver su cabecera): si este canal fuera a buscar el
     * contenido al disco, los dos compartirian la posicion pero no lo que hay en ella, y un
     * {@code read} por el canal podria devolver algo distinto que el mismo {@code read} por el
     * flujo. Compartir la posicion y no el contenido seria peor que no compartir nada.
     *
     * <p>Es de solo lectura, como el del JDK: todo lo que escribiria levanta
     * {@link NonWritableChannelException}.
     */
    static final class DeEntrada extends FileChannel {

        private final FileInputStream duenio;

        DeEntrada(FileInputStream duenio) {
            this.duenio = duenio;
        }

        private byte[] exigirAbierto() throws IOException {
            if (this.duenio.cerrado || !this.isOpen()) {
                throw new ClosedChannelException();
            }
            return this.duenio.datos;
        }

        public int read(ByteBuffer dst) throws IOException {
            byte[] datos = this.exigirAbierto();
            int n = leerEn(datos, this.duenio.pos, dst);
            if (n > 0) {
                this.duenio.pos = this.duenio.pos + n;
            }
            return n;
        }

        public int read(ByteBuffer dst, long position) throws IOException {
            if (position < 0) {
                throw new IllegalArgumentException("posicion negativa");
            }
            return leerEn(this.exigirAbierto(), position, dst);
        }

        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            comprobarRango(dsts, offset, length);
            this.exigirAbierto();
            long total = 0;
            int i = offset;
            while (i < offset + length) {
                int n = this.read(dsts[i]);
                if (n < 0) {
                    // Fin de archivo: si ya se habia leido algo, el total vale; si no, -1.
                    return total > 0 ? total : -1L;
                }
                total = total + n;
                if (dsts[i].hasRemaining()) {
                    return total;
                }
                i = i + 1;
            }
            return total;
        }

        public int write(ByteBuffer src) throws IOException {
            throw new NonWritableChannelException();
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new NonWritableChannelException();
        }

        public int write(ByteBuffer src, long position) throws IOException {
            throw new NonWritableChannelException();
        }

        public long position() throws IOException {
            this.exigirAbierto();
            return this.duenio.pos;
        }

        public FileChannel position(long newPosition) throws IOException {
            if (newPosition < 0) {
                throw new IllegalArgumentException("posicion negativa");
            }
            this.exigirAbierto();
            // Pasarse del final es legal y no es un error: lo que sigue es que las lecturas dan -1.
            // Por eso no se recorta -- recortar haria que `position()` no devolviera lo que se puso.
            this.duenio.pos = newPosition;
            return this;
        }

        public long size() throws IOException {
            return this.exigirAbierto().length;
        }

        public FileChannel truncate(long size) throws IOException {
            throw new NonWritableChannelException();
        }

        public void force(boolean metaData) throws IOException {
            // No hay nada en vuelo que forzar: este canal no escribe.
            this.exigirAbierto();
        }

        public long transferTo(long position, long count, WritableByteChannel target)
                throws IOException {
            if (position < 0 || count < 0) {
                throw new IllegalArgumentException("posicion o cuenta negativa");
            }
            if (target == null) {
                throw new NullPointerException();
            }
            byte[] datos = this.exigirAbierto();
            if (!target.isOpen()) {
                throw new ClosedChannelException();
            }
            if (position >= datos.length) {
                return 0L;
            }
            int n = (int) Math.min(count, (long) datos.length - position);
            ByteBuffer bb = ByteBuffer.wrap(datos, (int) position, n);
            long escritos = 0;
            while (bb.hasRemaining()) {
                int w = target.write(bb);
                if (w <= 0) {
                    break;
                }
                escritos = escritos + w;
            }
            return escritos;
        }

        public long transferFrom(ReadableByteChannel src, long position, long count)
                throws IOException {
            throw new NonWritableChannelException();
        }

        protected void implCloseChannel() throws IOException {
            // Cerrar el canal cierra el flujo, como en el JDK: son la misma cosa vista de dos
            // maneras, y dejar uno abierto sugeriria que todavia se puede leer por ahi.
            this.duenio.cerrado = true;
        }
    }

    /**
     * El canal de un {@link FileOutputStream}.
     *
     * <p>El flujo junta lo escrito en un buffer y lo vuelca de a tandas; el canal escribe en una
     * posicion. Para que los dos sean el mismo numero, **toda operacion de este canal vacia primero
     * el buffer del flujo**, y a partir de que el canal existe el volcado del flujo pasa por el.
     * Asi la posicion es una sola --la del canal de abajo-- y no hay nada que sincronizar.
     *
     * <p>El precio de entrar en ese modo esta en {@link java.nio.channels.FileChannel}: escribir por
     * canal es leer-modificar-escribir el archivo entero, mientras que el volcado normal del flujo
     * es un agregado al final. Por eso el modo **no se activa hasta que alguien pide el canal**:
     * quien nunca llama a {@code getChannel()} escribe como antes.
     */
    static final class DeSalida extends FileChannel {

        private final FileOutputStream duenio;
        /** El canal de verdad sobre el archivo. Perezoso: abrirlo puede fallar y `getChannel` no tira. */
        private FileChannel abajo;

        DeSalida(FileOutputStream duenio) {
            this.duenio = duenio;
        }

        private FileChannel abajo() throws IOException {
            if (this.duenio.cerrado || !this.isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.duenio.ruta == null) {
                // Un flujo construido con un `FileDescriptor`. Esta VM no los modela, asi que no hay
                // archivo detras y no hay nada que este canal pueda hacer. Se dice, no se simula.
                throw new IOException("no file descriptor");
            }
            if (this.abajo == null) {
                this.abajo = FileChannel.open(new File(this.duenio.ruta).toPath(),
                        StandardOpenOption.WRITE);
                // Arranca donde termina lo escrito hasta ahora, que es lo que el contrato pide y
                // ademas lo unico compatible con el modo `append`.
                this.abajo.position(this.abajo.size());
            }
            return this.abajo;
        }

        /**
         * Manda al canal lo que el flujo tenga pendiente en su buffer.
         *
         * <p>Es lo que hace que la posicion sea una sola: despues de esto, lo que el flujo escribio
         * ya esta contado en la posicion del canal, y no hay bytes viviendo en dos lugares.
         */
        void vaciarPendiente() throws IOException {
            FileChannel c = this.abajo();
            if (this.duenio.usados == 0) {
                return;
            }
            ByteBuffer bb = ByteBuffer.wrap(this.duenio.buf, 0, this.duenio.usados);
            while (bb.hasRemaining()) {
                if (c.write(bb) <= 0) {
                    throw new IOException("Could not write to " + this.duenio.ruta);
                }
            }
            this.duenio.usados = 0;
        }

        public int read(ByteBuffer dst) throws IOException {
            throw new NonReadableChannelException();
        }

        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            throw new NonReadableChannelException();
        }

        public int read(ByteBuffer dst, long position) throws IOException {
            throw new NonReadableChannelException();
        }

        public int write(ByteBuffer src) throws IOException {
            this.vaciarPendiente();
            return this.abajo().write(src);
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            comprobarRango(srcs, offset, length);
            this.vaciarPendiente();
            return this.abajo().write(srcs, offset, length);
        }

        public int write(ByteBuffer src, long position) throws IOException {
            this.vaciarPendiente();
            return this.abajo().write(src, position);
        }

        public long position() throws IOException {
            this.vaciarPendiente();
            return this.abajo().position();
        }

        public FileChannel position(long newPosition) throws IOException {
            this.vaciarPendiente();
            this.abajo().position(newPosition);
            return this;
        }

        public long size() throws IOException {
            this.vaciarPendiente();
            return this.abajo().size();
        }

        public FileChannel truncate(long size) throws IOException {
            this.vaciarPendiente();
            this.abajo().truncate(size);
            return this;
        }

        public void force(boolean metaData) throws IOException {
            this.vaciarPendiente();
            this.abajo().force(metaData);
        }

        public long transferTo(long position, long count, WritableByteChannel target)
                throws IOException {
            throw new NonReadableChannelException();
        }

        public long transferFrom(ReadableByteChannel src, long position, long count)
                throws IOException {
            this.vaciarPendiente();
            return this.abajo().transferFrom(src, position, count);
        }

        protected void implCloseChannel() throws IOException {
            // Lo pendiente sale **antes** de cerrar: perderlo en el cierre seria la peor forma de
            // perderlo, porque cerrar es justamente lo que uno hace para asegurarse de que salio.
            if (this.duenio.ruta != null && !this.duenio.cerrado) {
                this.vaciarPendiente();
            }
            if (this.abajo != null) {
                this.abajo.close();
            }
            this.duenio.cerrado = true;
        }
    }

    private static void comprobarRango(ByteBuffer[] bufs, int offset, int length) {
        if (bufs == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset > bufs.length - length) {
            throw new IndexOutOfBoundsException();
        }
    }

    /** Copia en `dst` lo que haya en `datos` desde `desde`. -1 si `desde` ya paso el final. */
    private static int leerEn(byte[] datos, long desde, ByteBuffer dst) {
        if (dst == null) {
            throw new NullPointerException();
        }
        if (dst.isReadOnly()) {
            throw new java.nio.ReadOnlyBufferException();
        }
        if (desde >= datos.length) {
            // Fin de archivo es -1 aunque el buffer estuviera lleno; que no quede lugar es otra cosa
            // y va abajo, como cero.
            return -1;
        }
        int libres = dst.remaining();
        if (libres == 0) {
            return 0;
        }
        int n = (int) Math.min((long) libres, (long) datos.length - desde);
        dst.put(datos, (int) desde, n);
        return n;
    }
}
