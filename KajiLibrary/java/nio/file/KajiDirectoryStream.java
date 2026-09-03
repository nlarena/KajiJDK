package java.nio.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * KajiLibrary's java.nio.file.KajiDirectoryStream -- el {@link DirectoryStream} que devuelve
 * {@link Files#newDirectoryStream}.
 *
 * <p>Lee el directorio **una sola vez, al construirse**, y despues itera sobre lo leido. El JDK lo
 * hace perezoso --mantiene abierto un descriptor y pide entradas de a poco-- y la diferencia se nota
 * en un directorio enorme; la razon de no imitarlo es que el nativo que hay,
 * {@code jdk.internal.io.Fs.list}, devuelve el arreglo entero y no un cursor. Fingir pereza sobre un
 * arreglo ya leido seria fingir.
 *
 * <p>Lo que **si** se respeta es el contrato observable: `iterator()` una sola vez
 * ({@link IllegalStateException} la segunda), el filtro aplicado al avanzar, y `close()` idempotente
 * que corta la iteracion.
 */
final class KajiDirectoryStream implements DirectoryStream<Path> {

    private final List<Path> entradas;
    private final DirectoryStream.Filter<? super Path> filtro;
    private boolean iteradorPedido;
    private boolean cerrado;

    KajiDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filtro) throws IOException {
        this.filtro = filtro;
        String[] nombres = jdk.internal.io.Fs.list(dir.toString());
        if (nombres == null) {
            // El nativo devuelve `null` cuando no pudo listar. Hay que distinguir por que, porque
            // las dos respuestas son excepciones distintas y quien llama las trata distinto.
            if (!Files.exists(dir)) {
                throw new NoSuchFileException(dir.toString());
            }
            if (!Files.isDirectory(dir)) {
                throw new NotDirectoryException(dir.toString());
            }
            throw new IOException("no se pudo listar " + dir);
        }
        this.entradas = new ArrayList<Path>();
        int i = 0;
        while (i < nombres.length) {
            this.entradas.add(dir.resolve(nombres[i]));
            i = i + 1;
        }
    }

    public Iterator<Path> iterator() {
        if (this.cerrado) {
            throw new IllegalStateException("el stream esta cerrado");
        }
        if (this.iteradorPedido) {
            throw new IllegalStateException("ya se pidio el iterador de este stream");
        }
        this.iteradorPedido = true;
        return new Recorrido();
    }

    public void close() {
        this.cerrado = true;
    }

    // El filtro se aplica **al avanzar** y no al construir, que es lo que el contrato pide: un filtro
    // que falle tiene que salir por `DirectoryIteratorException` durante la iteracion, no antes.
    private final class Recorrido implements Iterator<Path> {

        private int i;
        private Path siguiente;
        private boolean listo;

        public boolean hasNext() {
            if (this.listo) {
                return this.siguiente != null;
            }
            this.listo = true;
            this.siguiente = null;
            while (this.i < KajiDirectoryStream.this.entradas.size()) {
                if (KajiDirectoryStream.this.cerrado) {
                    return false;
                }
                Path p = KajiDirectoryStream.this.entradas.get(this.i);
                this.i = this.i + 1;
                boolean pasa = true;
                if (KajiDirectoryStream.this.filtro != null) {
                    try {
                        pasa = KajiDirectoryStream.this.filtro.accept(p);
                    } catch (IOException e) {
                        throw new DirectoryIteratorException(e);
                    }
                }
                if (pasa) {
                    this.siguiente = p;
                    return true;
                }
            }
            return false;
        }

        public Path next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            Path p = this.siguiente;
            this.listo = false;
            this.siguiente = null;
            return p;
        }
    }
}
