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
}
