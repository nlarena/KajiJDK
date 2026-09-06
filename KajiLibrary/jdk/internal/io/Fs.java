package jdk.internal.io;

// La costura entre Java y el disco: seis nativos y nada mas.
//
// **Por que son tan pocos.** Todo lo que se puede escribir en Java se escribe en Java --
// `File.getParent`, `Scanner`, `Formatter`, los streams-- porque ahi se lee, se prueba y se corrige
// sin recompilar la VM. Bajar a Rust es la excepcion, y esta lista es exactamente donde no hay
// alternativa: un programa Java no puede abrir un archivo por sus propios medios.
//
// **El archivo se lee o escribe entero de una.** No hay descriptor abierto, ni posicion, ni `close`
// que pueda faltar. Es una limitacion real --un archivo de un giga entra en memoria dos veces-- y a
// cambio no hay ningun estado que se pueda quedar colgado, que es la clase de error mas dificil de
// encontrar en una VM. Cuando haga falta streaming de verdad, la puerta es agregar un handle aca
// abajo; nada de lo que esta arriba tiene que enterarse.
//
// **Ninguno tira.** Devuelven `null`, `false` o cero, y quien llama decide que excepcion
// corresponde: el nativo no tiene con que distinguir "no existe" de "no tengo permiso", y adivinar
// mal seria peor que no decir nada.
//
// Esta clase es `jdk.internal` y no API: nadie de afuera deberia nombrarla.
public final class Fs {

    private Fs() {
    }

    /** Bandera de `stat`: la ruta existe. */
    public static final int EXISTE = 1;

    /** Es un archivo comun. */
    public static final int ES_ARCHIVO = 2;

    /** Es un directorio. */
    public static final int ES_DIRECTORIO = 4;

    /** Se puede leer. */
    public static final int SE_LEE = 8;

    /** Se puede escribir. */
    public static final int SE_ESCRIBE = 16;

    /** Los bytes del archivo, o `null` si no se pudo leer. */
    public static native byte[] readAllBytes(String path);

    /** Escribe los bytes; `append` decide si agrega o pisa. `true` si se pudo. */
    public static native boolean writeAllBytes(String path, byte[] bytes, boolean append);

    /**
     * Los metadatos, en las banderas de arriba.
     *
     * <p>Van juntos y no en cinco llamadas porque salen de **una sola** consulta al sistema:
     * preguntarlos por separado tocaria el disco cinco veces y --peor-- podria dar respuestas de
     * momentos distintos si algo cambia en el medio.
     */
    public static native int stat(String path);

    /** El tamaño en bytes, o 0 si no se puede saber. */
    public static native long size(String path);

    /**
     * Borra un archivo o un directorio **vacio**.
     *
     * <p>Vacio a proposito: `File.delete()` no borra recursivamente, y hacerlo aca convertiria un
     * `delete()` sobre el directorio equivocado en una perdida de datos.
     */
    public static native boolean delete(String path);

    /** Crea un directorio; `todos` decide si tambien los padres que falten. */
    public static native boolean mkdir(String path, boolean todos);

    /**
     * Los nombres **simples** de las entradas de un directorio, o `null` si no se pudo leer.
     *
     * <p>Es el nativo que faltaba para poder **recorrer** el disco y no solo tocar archivos sueltos.
     * Con el entran los nueve metodos de `java.nio.file` que enumeran --`list`, `walk`, `find`,
     * `walkFileTree`, los tres `newDirectoryStream`-- y los cinco `list`/`listFiles` de
     * `java.io.File`, que hasta ahora devolvian `null` siempre.
     *
     * <p>Nombres simples y no rutas completas, como hace `File.list()`: quien quiera la ruta la arma
     * con el directorio que ya tiene. Devolverla armada obligaria al nativo a elegir un separador y a
     * decidir si normaliza, y esas dos son decisiones del lado Java.
     *
     * <p>`null` --y no un arreglo vacio-- cuando falla, para que se distinga de un directorio que
     * existe y esta vacio. Es la misma distincion que `File.list()` hace, y perderla convertiria un
     * error en un resultado.
     *
     * <p>El orden es el que da el sistema de archivos y **no se ordena**: el contrato dice
     * explicitamente que no hay garantia de orden.
     */
    public static native String[] list(String ruta);

    /**
     * El camino **canonico** de esa ruta: absoluto, resuelto y sin enlaces; `null` si no existe.
     *
     * <p>Es lo unico que contesta si dos rutas distintas nombran el mismo archivo. Comparar las
     * cadenas no alcanza: en Windows `C:\A.TXT` y `C:.txt` son el mismo archivo.
     */
    public static native String canonical(String ruta);

    /**
     * La fecha de ultima modificacion, en milisegundos desde la epoca; `Long.MIN_VALUE` si no se
     * pudo leer.
     *
     * <p>El centinela no es cero a proposito: cero **es** una fecha valida --la epoca-- y era la que
     * se devolvia cuando no habia con que leer la de verdad. Confundir "no se" con "1 de enero de
     * 1970" es exactamente el error que este valor evita.
     */
    public static native long mtime(String ruta);

    /** Fija la fecha de ultima modificacion, en milisegundos desde la epoca. */
    public static native boolean setMtime(String ruta, long millis);

    /**
     * El tamano total, en bytes, del volumen que contiene a esa ruta; **-1 si no se pudo saber**.
     *
     * <p>El centinela no es cero por lo mismo que en {@link #mtime}: cero **es** una respuesta
     * valida --un volumen sin espacio-- y confundirla con "no se" es el error que este valor evita.
     * El lado Java traduce el -1 a la `IOException` que `FileStore` declara.
     */
    public static native long diskTotal(String ruta);

    /**
     * Lo que **este usuario** puede escribir en ese volumen, en bytes; -1 si no se pudo saber.
     *
     * <p>No es lo mismo que {@link #diskUnallocated}, y la diferencia importa donde hay cuotas: lo
     * utilizable es lo que la cuota deja, lo sin asignar es lo que el volumen tiene. Sin cuota los
     * dos coinciden.
     */
    public static native long diskUsable(String ruta);

    /** Los bytes sin asignar del volumen; -1 si no se pudo saber. Ver {@link #diskUsable}. */
    public static native long diskUnallocated(String ruta);

    /**
     * Las raices del sistema de archivos: `C:\`, `D:\`, ... en Windows; `/` en el resto.
     *
     * <p>Se pregunta al sistema en cada llamada y no se guarda: una unidad que se conecta agrega una
     * raiz, y una lista cacheada estaria vieja justo cuando alguien la mira para ver que hay.
     */
    public static native String[] roots();

    /**
     * Takes a system lock over that region of the file.
     *
     * <p>It is a lock **between processes**, which is what a file lock is for: two different
     * virtual machines over the same file exclude each other. It is not a lock between the threads
     * of one machine; `java.util.concurrent` is where those live.
     *
     * <p>The region may run past the end of the file, and it may be open-ended: a `size` of zero
     * means "from `position` to wherever the file grows", which is how the JDK spells a lock over
     * everything still to come.
     *
     * @param path the file
     * @param position the first byte
     * @param size how many bytes, or 0 for however far the file grows
     * @param shared true for a lock other readers may share
     * @param wait true to wait until the region frees up, false to return at once
     * @return the lock token, zero or greater; -1 when the lock could not be taken, and -2 when
     *     this system has no file locks
     */
    public static native int lock(String path, long position, long size, boolean shared,
            boolean wait);

    /**
     * Releases a lock.
     *
     * @param token the identifier {@link #lock} returned
     * @return whether there was a lock to release
     */
    public static native boolean unlock(int token);

    /** {@link #mapOpen} mode: read only. */
    public static final int MAP_READ_ONLY = 0;

    /** {@link #mapOpen} mode: writes reach the file and other mappers. */
    public static final int MAP_READ_WRITE = 1;

    /** {@link #mapOpen} mode: writes stay in this process, copy-on-write. */
    public static final int MAP_PRIVATE = 2;

    /**
     * Maps a region of a file into memory.
     *
     * <p>The region may start anywhere; the system can only begin a mapping at a multiple of its
     * allocation granularity, so the base is rounded down and the difference added back. What comes
     * out addresses exactly the region that was asked for.
     *
     * @param path the file
     * @param mode one of {@link #MAP_READ_ONLY}, {@link #MAP_READ_WRITE}, {@link #MAP_PRIVATE}
     * @param position the first byte of the file to map
     * @param size how many bytes
     * @return the mapping token, zero or greater; -1 when it could not be mapped, -2 when this
     *     platform cannot map at all
     */
    public static native int mapOpen(String path, int mode, long position, int size);

    /**
     * Writes the mapped bytes back to the file and waits for them to land.
     *
     * @param token the mapping
     * @return whether it worked
     */
    public static native boolean mapForce(int token);

    /**
     * Takes the mapping down, flushing a writable one first.
     *
     * @param token the mapping
     * @return whether there was a mapping to close
     */
    public static native boolean mapClose(int token);

    /**
     * One byte of the mapping.
     *
     * @param token the mapping
     * @param index the byte, counted from the start of the mapped region
     * @return the byte, 0 to 255, or -1 if the token or the index is not right
     */
    public static native int mapGet(int token, int index);

    /**
     * Writes one byte of the mapping.
     *
     * @param token the mapping
     * @param index the byte, counted from the start of the mapped region
     * @param value the byte to write; only its low eight bits are used
     * @return whether it was written
     */
    public static native boolean mapPut(int token, int index, int value);

    /**
     * Copies a run of the mapping into an array.
     *
     * <p>It exists so that reading a mapped file is one native call per buffer instead of one per
     * byte, which is the whole cost of {@link #mapGet} in a loop.
     *
     * @param token the mapping
     * @param index the first byte of the mapping to read
     * @param dst where to put them
     * @param off the first element of `dst` to write
     * @param len how many bytes
     * @return whether it worked
     */
    public static native boolean mapRead(int token, int index, byte[] dst, int off, int len);

    /**
     * Copies a run of an array into the mapping.
     *
     * @param token the mapping
     * @param index the first byte of the mapping to write
     * @param src where to take them from
     * @param off the first element of `src` to read
     * @param len how many bytes
     * @return whether it worked
     */
    public static native boolean mapWrite(int token, int index, byte[] src, int off, int len);
}
