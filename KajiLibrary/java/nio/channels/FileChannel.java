package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

/**
 * KajiLibrary's java.nio.channels.FileChannel — un canal sobre un archivo.
 *
 * <p>Es el unico canal que esta biblioteca sabe **fabricar**, y por eso el unico con
 * {@link #open(Path, OpenOption...)} de verdad: los de red necesitan un socket y esta VM no tiene
 * nativos de red, pero los archivos si se pueden leer y escribir.
 *
 * <h2>Como esta hecho, que es lo que hay que saber antes de usarlo</h2>
 *
 * <p>Esta VM no abre descriptores: lo unico que hay abajo es "leer el archivo entero" y "escribirlo
 * entero" (`jdk.internal.io.Fs`). Un canal sobre eso se puede construir de dos maneras, y la
 * eleccion se toma aca a la vista:
 *
 * <ul>
 *   <li><strong>Con cache</strong>: leer una vez al abrir, trabajar en memoria, volcar al cerrar.
 *       Rapido, y **mentiroso**: lo que se escribio no esta en el disco hasta cerrar, y un programa
 *       que se cae sin cerrar no dejo nada. Es el trato que ya hace `java.io.FileOutputStream` en
 *       este arbol, ahi documentado.
 *   <li><strong>Al vuelo</strong>: cada lectura lee el archivo y cada escritura lo reescribe.
 *       <strong>Es lo que hace este canal.</strong>
 * </ul>
 *
 * <p>La segunda es O(n) por operacion --escribir un byte al final de un archivo de un mega mueve un
 * mega-- y aun asi es la correcta, porque es la unica en la que el contrato se cumple: cuando
 * {@link #write} vuelve, los bytes **estan en el disco**; lo que otro proceso escriba se ve en la
 * lectura siguiente; y un corte de luz no borra lo que ya se habia escrito. Se paga velocidad por
 * no mentir, que es el cambio correcto. Quien necesite velocidad tiene
 * `java.io.BufferedOutputStream` sobre {@link Channels#newOutputStream}, donde el buffer es una
 * decision suya y no una sorpresa.
 *
 * <p>Consecuencia agradable de lo anterior: {@link #force} no tiene nada que hacer y `SYNC`/`DSYNC`
 * se cumplen solos. No es que se ignoren, es que ya estaban.
 *
 * <p>Lo que **no** se puede prometer es atomicidad: reescribir el archivo entero no es un solo paso,
 * asi que dos escritores simultaneos sobre el mismo archivo se pisan. El JDK tampoco garantiza nada
 * ahi sin bloqueos, pero su ventana es de bytes y la de aca es del archivo entero.
 *
 * <h2>Lo que quedo afuera a proposito</h2>
 *
 * <p>Tres metodos del JDK no estan, y ninguno por olvido. Los tres prometen algo que solo el sistema
 * operativo puede dar, y esta VM no tiene el nativo. Un {@code map()} que devolviera una copia en
 * memoria, o un {@code lock()} que solo excluyera a los hilos de esta VM, **compilarian igual y
 * fallarian en produccion**, que es exactamente el error que este arbol prefiere no cometer:
 *
 * <ul>
 *   <li>{@code map(MapMode, long, long)} — mapear en memoria. Un {@link java.nio.MappedByteBuffer}
 *       que fuera una copia haria que escribirle no llegara nunca al archivo, en silencio.
 *   <li>{@code lock(long, long, boolean)} y {@code lock()} — el candado del JDK se toma **a nombre
 *       de toda la VM** y su proposito es excluir a **otros procesos**. Lo unico implementable aca
 *       seria lo contrario exacto: excluir a los hilos de esta VM y a nadie mas. Mismo nombre,
 *       garantia opuesta.
 *   <li>{@code tryLock(long, long, boolean)} y {@code tryLock()} — idem.
 * </ul>
 *
 * <p>{@link MapMode} si esta, porque es un valor y no una promesa: sus tres constantes se pueden
 * nombrar, comparar y guardar sin que nada mienta. Que no haya {@code map()} al que pasarselas es
 * una carencia visible en compilacion, no una trampa en ejecucion.
 *
 * <p>{@link java.nio.channels.FileLock} tambien esta declarada, por lo mismo: es una clase abstracta
 * cuyo contrato se entiende solo, y sin {@code lock()} nadie puede obtener una instancia y creerse
 * protegido.
 */
public abstract class FileChannel extends AbstractInterruptibleChannel
        implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel {

    protected FileChannel() {
    }

    // ---- apertura --------------------------------------------------------------------------------

    /**
     * Abre un canal sobre `path`.
     *
     * <p>Sin ninguna opcion de escritura, se abre para lectura. `options` acepta lo mismo que
     * `java.nio.file.Files`: `READ`, `WRITE`, `APPEND`, `TRUNCATE_EXISTING`, `CREATE`, `CREATE_NEW`,
     * `DELETE_ON_CLOSE`, `SYNC`, `DSYNC` y `NOFOLLOW_LINKS`.
     *
     * <p>`SYNC` y `DSYNC` se aceptan porque **se cumplen**: este canal escribe al disco en cada
     * `write`. `SPARSE` se rechaza --no se hacen archivos ralos aca-- igual que en `Files`, en vez
     * de aceptarse como sugerencia: ignorar en silencio una opcion que el que llama puso por algo
     * es como se descubre tarde que el archivo ocupa lo que no debia.
     *
     * @throws IllegalArgumentException si las opciones se contradicen (`READ` con `APPEND`, o
     *         `APPEND` con `TRUNCATE_EXISTING`)
     * @throws UnsupportedOperationException si se pide una opcion que esta VM no puede honrar
     * @throws java.nio.file.NoSuchFileException si no existe y no se pidio crearlo
     * @throws java.nio.file.FileAlreadyExistsException con `CREATE_NEW` si ya estaba
     */
    public static FileChannel open(Path path, OpenOption... options) throws IOException {
        if (options == null) {
            throw new NullPointerException();
        }
        return KajiFileChannel.abrir(path, options);
    }

    /**
     * Como el otro, con las opciones en un conjunto y atributos iniciales.
     *
     * <p>`attrs` **tiene que venir vacio**: esta VM no sabe fijar permisos ni due&ntilde;o al crear,
     * y aceptar atributos que despues no se aplican dejaria un archivo con permisos distintos a los
     * pedidos sin que nadie se entere. Se rechaza en vez de ignorarse.
     *
     * @throws UnsupportedOperationException si `attrs` trae algo
     */
    public static FileChannel open(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        if (options == null || attrs == null) {
            throw new NullPointerException();
        }
        if (attrs.length > 0) {
            throw new UnsupportedOperationException("atributos iniciales no soportados");
        }
        OpenOption[] arr = new OpenOption[options.size()];
        int i = 0;
        for (OpenOption o : options) {
            arr[i] = o;
            i = i + 1;
        }
        return KajiFileChannel.abrir(path, arr);
    }

    // ---- lectura y escritura por posicion corriente -----------------------------------------------

    /** Lee desde la posicion corriente y la avanza. */
    public abstract int read(ByteBuffer dst) throws IOException;

    /**
     * Lee repartiendo en varios buffers, en orden.
     *
     * <p>Llenar uno antes de empezar el siguiente es el contrato, no un detalle: es lo que permite
     * leer una cabecera de tama&ntilde;o fijo y su cuerpo en una sola llamada.
     */
    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    /** Como el otro, con todos los buffers del arreglo. */
    public final long read(ByteBuffer[] dsts) throws IOException {
        return this.read(dsts, 0, dsts.length);
    }

    /** Escribe en la posicion corriente y la avanza. */
    public abstract int write(ByteBuffer src) throws IOException;

    /** Escribe juntando varios buffers, en orden. */
    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    /** Como el otro, con todos los buffers del arreglo. */
    public final long write(ByteBuffer[] srcs) throws IOException {
        return this.write(srcs, 0, srcs.length);
    }

    // ---- posicion y tamanio ----------------------------------------------------------------------

    /** La posicion corriente, en bytes desde el principio. */
    public abstract long position() throws IOException;

    /**
     * Mueve la posicion.
     *
     * <p>Se admite mas alla del final: leer ahi da -1 y escribir ahi deja un hueco de ceros.
     */
    public abstract FileChannel position(long newPosition) throws IOException;

    /** El tama&ntilde;o del archivo. */
    public abstract long size() throws IOException;

    /**
     * Corta el archivo a `size`.
     *
     * <p>Si ya era mas chico no pasa nada --no lo agranda-- y si la posicion quedaba mas alla del
     * nuevo final, pasa a ser el nuevo final.
     */
    public abstract FileChannel truncate(long size) throws IOException;

    /**
     * Fuerza los cambios al disco.
     *
     * <p>No hace nada, y no es una omision: este canal ya escribe al disco en cada `write`, asi que
     * cuando se llama a esto no queda nada pendiente. Ver la nota de la clase.
     *
     * @param metaData si `false`, no hace falta forzar los metadatos
     */
    public abstract void force(boolean metaData) throws IOException;

    // ---- transferencias --------------------------------------------------------------------------

    /**
     * Copia hasta `count` bytes desde `position` de este archivo hacia `target`.
     *
     * <p>No toca la posicion corriente de este canal --si la de `target`--, que es lo que permite
     * usarlo desde varios hilos sobre el mismo canal.
     */
    public abstract long transferTo(long position, long count, WritableByteChannel target)
            throws IOException;

    /** Copia hasta `count` bytes de `src` hacia `position` de este archivo. */
    public abstract long transferFrom(ReadableByteChannel src, long position, long count)
            throws IOException;

    // ---- lectura y escritura por posicion absoluta ------------------------------------------------

    /**
     * Lee desde `position` **sin mover** la posicion corriente.
     *
     * @throws IllegalArgumentException si `position` es negativa
     */
    public abstract int read(ByteBuffer dst, long position) throws IOException;

    /**
     * Escribe en `position` sin mover la posicion corriente.
     *
     * @throws IllegalArgumentException si `position` es negativa
     */
    public abstract int write(ByteBuffer src, long position) throws IOException;

    // ---- MapMode ---------------------------------------------------------------------------------

    /**
     * Los modos de un mapeo en memoria.
     *
     * <p>Esta aunque {@code map()} no este; ver la nota de la clase. No es un `enum` --tampoco en el
     * JDK-- porque la lista queda abierta: un proveedor de sistema de archivos puede agregar modos
     * propios, y un `enum` lo impediria para siempre.
     */
    public static class MapMode {

        /** Mapeo de solo lectura. */
        public static final MapMode READ_ONLY = new MapMode("READ_ONLY");

        /** Mapeo de lectura y escritura; los cambios llegan al archivo. */
        public static final MapMode READ_WRITE = new MapMode("READ_WRITE");

        /** Copia al escribir: los cambios quedan en el mapeo y no tocan el archivo. */
        public static final MapMode PRIVATE = new MapMode("PRIVATE");

        private final String nombre;

        // De paquete y no privado --el JDK lo tiene privado-- para que `FabricaMapMode` pueda
        // construir los modos de `jdk.nio.mapmode`. Ver el comentario de esa clase: es el mismo
        // puente que el JDK hace con `SharedSecrets`, sin la maquinaria. No es API: no cambia
        // ningun miembro publico ni protegido de `MapMode`.
        MapMode(String nombre) {
            this.nombre = nombre;
        }

        public String toString() {
            return this.nombre;
        }
    }
}
