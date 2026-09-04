package java.nio.channels;

import java.io.IOException;

/**
 * KajiLibrary's java.nio.channels.FileLock — un candado sobre un tramo de archivo.
 *
 * <p>Un candado es un **rango**, no un archivo: `[position, position+size)`, y puede ser compartido
 * --varios lectores a la vez-- o exclusivo. Que el rango pueda pasarse del final del archivo no es
 * un descuido del dise&ntilde;o sino lo que permite reservar de antemano la zona donde uno va a
 * escribir, antes de que exista.
 *
 * <p>El candado lo toma **la VM entera**, no el hilo: dos hilos de este proceso que pidan el mismo
 * rango no se excluyen entre si, se excluyen de **otros procesos**. Es la fuente clasica de
 * confusion con esta clase y por eso esta escrito primero.
 *
 * <h2>Por que esta declarada si nadie puede conseguir una</h2>
 *
 * <p>{@link FileChannel} de esta biblioteca **no trae `lock()` ni `tryLock()`**, y el porque esta en
 * su cabecera: lo unico implementable sobre esta VM seria un candado entre hilos de este proceso, es
 * decir la garantia **opuesta** a la que el nombre promete. Sin esos dos metodos no hay forma de
 * obtener una instancia de esta clase, asi que nadie puede creerse protegido por algo que no lo
 * protege.
 *
 * <p>Lo que si aporta declararla: es un tipo abstracto cuyo contrato se lee y se entiende, y sus
 * partes calculables --{@link #overlaps}, {@link #position()}, {@link #size()},
 * {@link #isShared()}-- estan implementadas de verdad, porque son aritmetica sobre los campos y no
 * dependen de ningun nativo. Quien implemente un sistema de archivos propio hereda de aca y solo
 * tiene que poner {@link #isValid()} y {@link #release()}.
 *
 * <h2>Lo que quedo afuera</h2>
 *
 * <p><strong>Nada.</strong> Los dos constructores y los nueve metodos publicos estan.
 */
public abstract class FileLock implements AutoCloseable {

    // El canal se guarda como `Channel` y no como `FileChannel` porque los dos constructores
    // aceptan jerarquias distintas; `channel()` y `acquiredBy()` son las dos vistas de este campo.
    private final Channel canal;
    private final long posicion;
    private final long largo;
    private final boolean compartido;

    /**
     * Para un candado sobre un canal de archivo.
     *
     * @throws IllegalArgumentException si el rango es negativo o se desborda
     */
    protected FileLock(FileChannel channel, long position, long size, boolean shared) {
        comprobar(position, size);
        if (channel == null) {
            throw new NullPointerException();
        }
        this.canal = channel;
        this.posicion = position;
        this.largo = size;
        this.compartido = shared;

    }

    /**
     * Para un candado sobre un canal de archivo asincronico.
     *
     * <p>Existe por separado del otro porque {@link AsynchronousFileChannel} no hereda de
     * {@link FileChannel}: son dos jerarquias distintas que dan sobre el mismo archivo.
     */
    protected FileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
        comprobar(position, size);
        if (channel == null) {
            throw new NullPointerException();
        }
        this.canal = channel;
        this.posicion = position;
        this.largo = size;
        this.compartido = shared;

    }

    private static void comprobar(long position, long size) {
        if (position < 0) {
            throw new IllegalArgumentException("posicion negativa");
        }
        if (size < 0) {
            throw new IllegalArgumentException("tamanio negativo");
        }
        // El desborde se ataja aca y no al comparar rangos: un `position+size` que da vuelta el
        // signo convertiria un candado enorme en uno que no solapa con nada.
        if (position + size < 0) {
            throw new IllegalArgumentException("el rango se desborda");
        }
    }

    /**
     * El canal sobre el que se tomo, o `null` si fue un {@link AsynchronousFileChannel}.
     *
     * <p>Devolver `null` en ese caso es lo que hace el JDK, y es incomodo pero coherente: el tipo de
     * retorno es {@link FileChannel} y un canal asincronico no lo es. {@link #acquiredBy()} es la
     * forma sin sorpresas de preguntar lo mismo.
     */
    public final FileChannel channel() {
        if (this.canal instanceof FileChannel) {
            return (FileChannel) this.canal;
        }
        return null;
    }

    /** El canal sobre el que se tomo, sea del tipo que sea. */
    public Channel acquiredBy() {
        return this.canal;
    }

    /** Donde empieza el tramo trabado. */
    public final long position() {
        return this.posicion;
    }

    /**
     * Cuantos bytes abarca.
     *
     * <p>Puede pasarse del final del archivo, y entonces el tama&ntilde;o del candado no cambia
     * aunque el archivo crezca: lo que se reservo se reservo.
     */
    public final long size() {
        return this.largo;
    }

    /** Si es compartido; si no, es exclusivo. */
    public final boolean isShared() {
        return this.compartido;
    }

    /** Si este candado y el rango dado pisan aunque sea un byte en comun. */
    public final boolean overlaps(long position, long size) {
        if (position + size <= this.posicion) {
            return false;
        }
        if (this.posicion + this.largo <= position) {
            return false;
        }
        return true;
    }

    /**
     * Si el candado sigue valido.
     *
     * <p>Deja de serlo al soltarlo, al cerrar el canal, o al apagarse la VM.
     */
    public abstract boolean isValid();

    /** Suelta el candado. Sobre uno ya invalido no hace nada. */
    public abstract void release() throws IOException;

    /**
     * Lo mismo que {@link #release()}, para `try`-con-recursos.
     *
     * <p>Es la razon de que la clase implemente `AutoCloseable`: un candado que se olvida de soltar
     * traba a los demas hasta que muere el proceso.
     */
    public final void close() throws IOException {
        this.release();
    }

    public final String toString() {
        String modo;
        if (this.compartido) {
            modo = "shared";
        } else {
            modo = "exclusive";
        }
        String estado;
        if (this.isValid()) {
            estado = "valid";
        } else {
            estado = "invalid";
        }
        return "FileLock[" + modo + " " + this.posicion + ":" + this.largo + " " + estado + "]";
    }
}
