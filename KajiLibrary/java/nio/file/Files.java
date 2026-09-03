package java.nio.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileTypeDetector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import jdk.internal.io.Fs;

// KajiLibrary's java.nio.file.Files -- las operaciones sobre archivos de NIO.2.
//
// ============================================================================================
// **Que hay y que no, y por que.** Esta es la clase donde mas se nota el techo de la VM, asi que
// vale la pena decirlo de entrada. Todo el acceso al disco pasa por las **seis** operaciones de
// `jdk.internal.io.Fs`: leer el archivo entero, escribirlo entero (pisando o anexando), `stat`
// (existe / es archivo / es directorio / se lee / se escribe), tamaño, borrar, y crear directorio.
// No hay descriptores abiertos, ni posicion dentro de un archivo, ni **listar un directorio**, ni
// metadatos de tiempo, ni permisos POSIX, ni enlaces.
//
// De los 70 metodos del JDK, aca estan los 58 que se pueden hacer honestamente. Los que faltan,
// agrupados por **que** es lo que falta:
//
//   1. **Falta un nativo, y la firma no tiene como decirlo.** No hay con que hacerlo, ni de a
//      pedazos, y el metodo no declara ninguna excepcion que signifique "esto no existe aca".
//
//      Los nueve que **enumeraban un directorio** --`newDirectoryStream` (x3), `list`, `walk` (x2),
//      `find`, `walkFileTree` (x2)-- estaban aca y ya no: `jdk.internal.io.Fs.list(String)` existe y
//      la VM lo implementa. La nota que decia que esa era "la ausencia mas grande de la lista" se
//      escribio antes de que ese nativo llegara, y quedo desactualizada sin que nadie la mirara --
//      que es lo que pasa con las notas de lo que falta cuando lo que falta deja de faltar.
//
//      - (`setLastModifiedTime` estaba aca. Ya no: `Fs.setMtime` existe.)
//      - (`getFileStore` estaba aca. Ya no: `Fs.diskTotal`/`diskUsable`/`diskUnallocated` existen
//        y `KajiFileStore` los usa.)
//
//   2. **Se podria escribir, pero seria mentir.**
//      - (`isSameFile` estaba aca, y el argumento era bueno: comparar rutas normalizadas daria
//        `false` para dos nombres del mismo archivo, y ahi `false` es una afirmacion y no un "no se".
//        Lo que faltaba era la identidad, y `Fs.canonical` la da: canonicalizar resuelve mayusculas,
//        `.`/`..`, enlaces y relativas contra el directorio actual. Dos rutas son el mismo archivo si
//        y solo si canonizan igual.)
//
//        Y no es un caso de borde: sobre Windows `C:\A.TXT` y `C:\a.txt` son el mismo archivo y no
//        son la misma cadena, y como `user.dir` es `null` en esta VM una ruta relativa **no** se
//        puede llevar a absoluta para compararla con una que si lo es. Los dos usos mas comunes de
//        `isSameFile` darian `false`. Ver `KajiFileSystemProvider.isSameFile`, que contesta el
//        subconjunto que si se puede y falla en el resto -- ahi el metodo es `abstract` y **hay que**
//        darle un cuerpo, asi que no existe la opcion de omitirlo.
//
// **Y una nota sobre los metodos que estan y solo saben fallar.** `createSymbolicLink`, `createLink`,
// `readSymbolicLink`, `getOwner`, `setOwner`, `getPosixFilePermissions`, `setPosixFilePermissions` y
// `setAttribute` existen y tiran `UnsupportedOperationException`. **No es un hueco tapado**: en los
// ocho casos la spec declara esa excepcion para exactamente esta situacion --"la implementacion no
// soporta enlaces simbolicos", "la vista de atributos no esta disponible"-- asi que tirarla es
// *contestar* el contrato, no esquivarlo. Un programa que las llama recibe la misma excepcion que
// recibiria de un JDK sobre un sistema de archivos que tampoco las soporta.
//
// **Y una sobre `isHidden`, que esta pero no contesta lo que contestaria un JDK de Windows.** La
// spec deja la definicion de "oculto" al proveedor, y la de KajiJDK es la de POSIX --el nombre
// empieza con un punto-- que es tambien la que ya usa `java.io.File.isHidden()` en esta biblioteca.
// No sale del bit de DOS porque `stat` no lo trae. Esta escrito en el metodo con las dos
// diferencias concretas que produce.
//
// **Los atributos que si se leen.** `readAttributes` (x2), `getAttribute` y `getLastModifiedTime`
// funcionan, sobre la vista `basic` y nada mas. `stat` y `size` contestan cuatro de los nueve
// atributos (`isRegularFile`, `isDirectory`, `isOther`, `size`); las tres marcas de tiempo salen
// **epoca**, que es lo que la spec de `BasicFileAttributes` manda devolver cuando el sistema de
// archivos no las soporta -- no un cero inventado; `fileKey()` da `null`, tambien por spec; y el
// noveno, `isSymbolicLink()`, da `false`. Ver `AtributosBasicos`, abajo, donde esta el detalle
// atributo por atributo.
//
// **Ojo con la asimetria, que es real.** Los atributos se **leen** y sin embargo
// `getFileAttributeView` devuelve `null` y `FileSystem.supportedFileAttributeViews()` da vacio. No
// es una contradiccion: una *vista* es un objeto que lee **y escribe**, y `BasicFileAttributeView`
// tiene `setTimes`, para lo que no hay nativo. Es la misma razon por la que falta
// `setLastModifiedTime`.
//
// **Y dos diferencias de comportamiento que hay que tener presentes en lo que si esta.**
//
// La primera: ninguna de las operaciones de creacion es **atomica**. `createFile` es un `stat`
// seguido de una escritura, y entre los dos alguien podria crear el archivo. El JDK lo hace en una
// sola llamada al sistema. Cada metodo afectado lo dice en su javadoc.
//
// La segunda: **no todos los metodos de apertura aceptan las mismas `OpenOption`**, y la diferencia
// es a proposito. La regla de esta clase es una sola --se acepta la opcion que se puede cumplir, y
// se rechaza la que solo se podria fingir-- pero da resultados distintos segun sobre que este
// construido cada metodo, porque no todos tienen las mismas capacidades abajo:
//
//   - `newInputStream`, `newOutputStream`, `newBufferedReader` y `newBufferedWriter` van a
//     `java.io.FileInputStream` / `FileOutputStream`, que **acumulan y vuelcan al cerrar**. Ahi
//     `SYNC` y `DSYNC` no se pueden cumplir, y `DELETE_ON_CLOSE` no esta implementado: los tres se
//     rechazan, que es lo que hace `resolver`.
//   - `newByteChannel` (x2) va a `java.nio.channels.FileChannel`, que **escribe al disco en cada
//     `write`**. Ahi `SYNC` y `DSYNC` ya se cumplen sin hacer nada y `DELETE_ON_CLOSE` esta
//     implementado, asi que las tres se aceptan. `newByteChannel` no pasa por `resolver`: delega la
//     resolucion entera en `FileChannel.open`.
//
// `SPARSE` se rechaza en los dos lados, porque en ninguno se hacen archivos ralos. Uniformar hacia
// el criterio estricto seria rechazar en `newByteChannel` opciones que ahi **si** se honran, y
// uniformar hacia el laxo seria aceptar en `newOutputStream` un `SYNC` que no sincroniza, que es
// justo la promesa falsa que esta biblioteca no hace.
// ============================================================================================
public final class Files {

    // Es una clase de utilidades: no hay nada que instanciar.
    private Files() {
    }

    // El generador de nombres temporales. Sembrado con el reloj de alta resolucion: no hace falta
    // que sea criptografico --el JDK usa `SecureRandom` porque un nombre adivinable es un vector de
    // ataque, y eso aca no cambia nada porque la creacion no es atomica de todos modos-- pero si
    // que dos VMs que arrancan juntas no generen la misma secuencia.
    private static final Random AZAR = new Random(System.nanoTime());

    // La ruta como cadena, comprobando que el Path sea de esta biblioteca.
    private static String ruta(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (!(path instanceof KajiPath)) {
            throw new ProviderMismatchException(path.getClass().getName());
        }
        return path.toString();
    }

    // Las opciones de apertura ya resueltas. Se pasa una sola vez por el array y despues se
    // consultan campos: recorrerlo en cada pregunta seria O(n) por consulta y --peor-- dejaria la
    // validacion desparramada.
    //
    // Esto vale para los metodos que abren un stream, **no** para `newByteChannel`: ver la nota de
    // la cabecera sobre por que los dos criterios de opciones difieren y cual manda en cada caso.
    private static final class Apertura {
        boolean leer;
        boolean escribir;
        boolean anexar;
        boolean crear;
        boolean crearNuevo;
        boolean truncar;
    }

    private static Apertura resolver(OpenOption[] options, boolean paraEscribir) {
        Apertura a = new Apertura();
        int i = 0;
        while (i < options.length) {
            OpenOption o = options[i];
            if (o == null) {
                throw new NullPointerException();
            }
            if (o == StandardOpenOption.READ) {
                a.leer = true;
            } else if (o == StandardOpenOption.WRITE) {
                a.escribir = true;
            } else if (o == StandardOpenOption.APPEND) {
                a.anexar = true;
            } else if (o == StandardOpenOption.TRUNCATE_EXISTING) {
                a.truncar = true;
            } else if (o == StandardOpenOption.CREATE) {
                a.crear = true;
            } else if (o == StandardOpenOption.CREATE_NEW) {
                a.crearNuevo = true;
            } else if (o == LinkOption.NOFOLLOW_LINKS) {
                // Sin enlaces en el modelo, no seguirlos es lo unico que se puede hacer: aceptarla
                // no promete nada que no se cumpla, y no cambia ninguna bandera.
            } else {
                // DELETE_ON_CLOSE, SPARSE, SYNC, DSYNC y cualquier OpenOption ajena. Se rechazan en
                // vez de ignorarse: un `SYNC` que no sincroniza es la clase de promesa falsa que
                // hace perder datos.
                throw new UnsupportedOperationException(String.valueOf(o) + " not supported");
            }
            i = i + 1;
        }
        if (a.anexar && a.leer) {
            throw new IllegalArgumentException("READ + APPEND not allowed");
        }
        if (a.anexar && a.truncar) {
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        }
        if (paraEscribir) {
            // Por omision: crear, truncar y escribir, que es lo que dice la spec cuando no se pasa
            // ninguna opcion.
            if (!a.escribir && !a.anexar) {
                a.escribir = true;
                if (options.length == 0) {
                    a.crear = true;
                    a.truncar = true;
                }
            }
        } else if (!a.leer && options.length == 0) {
            a.leer = true;
        }
        return a;
    }

    // Traduce el resultado de `stat` a la excepcion que corresponde cuando no se pudo leer.
    private static IOException porQueNoSeLeyo(String p) {
        int st = Fs.stat(p);
        if ((st & Fs.EXISTE) == 0) {
            return new NoSuchFileException(p);
        }
        if ((st & Fs.ES_DIRECTORIO) != 0) {
            return new IOException(p + " is a directory");
        }
        return new AccessDeniedException(p);
    }

    // ------------------------------------------------------------------------------------------
    // Abrir
    // ------------------------------------------------------------------------------------------

    /**
     * Un stream de bytes sobre `path`.
     *
     * <p>**El archivo se lee entero al abrir**, como todo en esta VM: lo que devuelve es un stream
     * sobre una copia en memoria, no una ventana viva al archivo. Ver la nota de
     * `java.io.FileInputStream`, que es lo que hay abajo.
     *
     * @throws UnsupportedOperationException si se pide una opcion que esta VM no puede honrar
     * @throws NoSuchFileException si el archivo no esta
     */
    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        String p = ruta(path);
        Apertura a = resolver(options, false);
        if (a.escribir || a.anexar || a.crear || a.crearNuevo || a.truncar) {
            throw new UnsupportedOperationException("write options on newInputStream");
        }
        // Se pregunta por `stat` antes de abrir para poder distinguir "no existe" de "no tengo
        // permiso": `FileInputStream` solo sabe decir `FileNotFoundException`, que junta las dos.
        int st = Fs.stat(p);
        if ((st & Fs.EXISTE) == 0) {
            throw new NoSuchFileException(p);
        }
        if ((st & Fs.ES_DIRECTORIO) != 0) {
            throw new IOException(p + " is a directory");
        }
        if ((st & Fs.SE_LEE) == 0) {
            throw new AccessDeniedException(p);
        }
        return new FileInputStream(p);
    }

    /**
     * Un stream para escribir en `path`.
     *
     * <p>Sin opciones equivale a `CREATE`, `TRUNCATE_EXISTING` y `WRITE`.
     *
     * <p>**El contenido llega al disco recien al cerrar**, no a medida que se escribe: abajo esta
     * `java.io.FileOutputStream`, que acumula y vuelca de una porque el nativo escribe el archivo
     * entero. Un programa que se cuelga sin cerrar el stream no deja nada escrito.
     *
     * @throws FileAlreadyExistsException con `CREATE_NEW` si el archivo ya estaba
     * @throws NoSuchFileException sin `CREATE` ni `CREATE_NEW` si el archivo no estaba
     */
    public static OutputStream newOutputStream(Path path, OpenOption... options)
            throws IOException {
        String p = ruta(path);
        Apertura a = resolver(options, true);
        if (a.leer) {
            throw new UnsupportedOperationException("READ on newOutputStream");
        }
        boolean existe = (Fs.stat(p) & Fs.EXISTE) != 0;
        if (a.crearNuevo && existe) {
            throw new FileAlreadyExistsException(p);
        }
        if (!existe && !a.crear && !a.crearNuevo) {
            throw new NoSuchFileException(p);
        }
        return new FileOutputStream(p, a.anexar);
    }

    /**
     * Un canal con posicion sobre `path`.
     *
     * <p>Es el unico metodo de apertura de esta clase que **no** vuelca en memoria: abajo esta
     * `java.nio.channels.FileChannel`, que va al disco en cada lectura y en cada escritura. Sale
     * caro --O(n) por operacion, porque el nativo solo sabe leer y escribir el archivo entero-- y a
     * cambio es el unico que cumple lo que promete: cuando `write` vuelve, los bytes estan en el
     * disco. La cabecera de `FileChannel` explica el trato completo.
     *
     * <p>**Las opciones las resuelve `FileChannel.open`, no esta clase.** Por eso aca `SYNC`,
     * `DSYNC` y `DELETE_ON_CLOSE` se aceptan, mientras que `newInputStream` y `newOutputStream` las
     * rechazan: ahi no se pueden cumplir y aca si. `SPARSE` se rechaza en los dos. El porque esta
     * en la cabecera de la clase.
     *
     * @throws ProviderMismatchException si `path` no es de esta biblioteca
     * @throws IllegalArgumentException si las opciones se contradicen
     * @throws UnsupportedOperationException si se pide una opcion que esta VM no puede honrar
     * @throws NoSuchFileException si no existe y no se pidio crearlo
     * @throws FileAlreadyExistsException con `CREATE_NEW` si ya estaba
     */
    public static SeekableByteChannel newByteChannel(Path path, OpenOption... options)
            throws IOException {
        // La comprobacion del proveedor se hace aca y no mas abajo: `FileChannel.open` trabaja con
        // la ruta como cadena y le da igual de donde salga, pero el contrato de `Files` dice que un
        // `Path` ajeno es `ProviderMismatchException`.
        ruta(path);
        return FileChannel.open(path, options);
    }

    /**
     * Como el otro, con las opciones en un conjunto y atributos iniciales.
     *
     * <p>`attrs` tiene que venir vacio, por la misma razon que en `createFile`: no hay nativo que
     * fije permisos al crear. Lo rechaza `FileChannel.open`, asi que no se repite la comprobacion
     * aca --duplicarla es como se termina con dos mensajes distintos para el mismo error.
     *
     * @throws UnsupportedOperationException si `attrs` trae algo
     */
    public static SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        ruta(path);
        return FileChannel.open(path, options, attrs);
    }

    /** Un lector con buffer sobre `path`, decodificando con `cs`. */
    public static BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
        return new BufferedReader(new InputStreamReader(newInputStream(path), cs));
    }

    /** Como el otro, en UTF-8. */
    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    /** Un escritor con buffer sobre `path`, codificando con `cs`. */
    public static BufferedWriter newBufferedWriter(Path path, Charset cs, OpenOption... options)
            throws IOException {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(path, options), cs));
    }

    /** Como el otro, en UTF-8. */
    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options)
            throws IOException {
        return newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }

    // ------------------------------------------------------------------------------------------
    // Crear
    // ------------------------------------------------------------------------------------------

    // Rechaza los atributos de creacion. Se hace en un solo lugar porque la razon es una sola: el
    // nativo que crea no toma ningun parametro de permisos.
    private static void sinAtributos(FileAttribute<?>[] attrs) {
        if (attrs.length > 0) {
            throw new UnsupportedOperationException(
                    "KajiJDK cannot set attributes when creating a file");
        }
    }

    /**
     * Crea un archivo vacio.
     *
     * <p>**No es atomico, a diferencia del JDK.** Aca son dos pasos --comprobar que no exista y
     * escribir cero bytes-- y entre los dos otro proceso podria crearlo; en ese caso este metodo
     * lo pisa en vez de fallar. El JDK lo hace en una sola llamada al sistema con `O_EXCL`. Cuando
     * haya un nativo de creacion exclusiva, esto se arregla aca adentro y nadie mas se entera.
     *
     * @throws FileAlreadyExistsException si ya existe
     * @throws UnsupportedOperationException si se pasa algun `FileAttribute`
     */
    public static Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        String p = ruta(path);
        sinAtributos(attrs);
        if ((Fs.stat(p) & Fs.EXISTE) != 0) {
            throw new FileAlreadyExistsException(p);
        }
        if (!Fs.writeAllBytes(p, new byte[0], false)) {
            throw new IOException("cannot create " + p);
        }
        return path;
    }

    /**
     * Crea un directorio. El padre tiene que existir.
     *
     * @throws FileAlreadyExistsException si ya hay algo con ese nombre
     * @throws NoSuchFileException si falta el directorio padre
     */
    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        String p = ruta(dir);
        sinAtributos(attrs);
        if ((Fs.stat(p) & Fs.EXISTE) != 0) {
            throw new FileAlreadyExistsException(p);
        }
        Path padre = dir.toAbsolutePath().getParent();
        if (padre != null && (Fs.stat(padre.toString()) & Fs.EXISTE) == 0) {
            throw new NoSuchFileException(padre.toString());
        }
        if (!Fs.mkdir(p, false)) {
            throw new IOException("cannot create directory " + p);
        }
        return dir;
    }

    /**
     * Crea el directorio y todos los padres que falten.
     *
     * <p>A diferencia de `createDirectory`, **no falla si ya existe** -- es la unica diferencia de
     * contrato entre las dos y es la que hace que esta sirva para asegurar una ruta.
     *
     * @throws FileAlreadyExistsException si la ruta existe pero no es un directorio
     */
    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        String p = ruta(dir);
        sinAtributos(attrs);
        int st = Fs.stat(p);
        if ((st & Fs.EXISTE) != 0) {
            if ((st & Fs.ES_DIRECTORIO) == 0) {
                throw new FileAlreadyExistsException(p);
            }
            return dir;
        }
        if (!Fs.mkdir(p, true)) {
            throw new IOException("cannot create directories " + p);
        }
        return dir;
    }

    // Un nombre candidato. El JDK usa un long aleatorio en decimal; se hace igual para que los
    // nombres tengan la misma pinta.
    private static String nombreTemporal(String prefix, String suffix, boolean esDirectorio) {
        String pre = (prefix == null) ? "" : prefix;
        String suf = suffix;
        if (suf == null) {
            suf = esDirectorio ? "" : ".tmp";
        }
        long n = AZAR.nextLong();
        // `Long.MIN_VALUE` no tiene valor absoluto: se lo trata aparte para no imprimir el signo.
        String medio = (n == Long.MIN_VALUE) ? "0" : Long.toString(Math.abs(n));
        return pre + medio + suf;
    }

    /**
     * Un archivo nuevo con nombre unico dentro de `dir`.
     *
     * <p>**No es atomico** por la misma razon que `createFile`; se reintenta hasta que un nombre no
     * este tomado, y despues de varios intentos se rinde en vez de girar para siempre.
     */
    public static Path createTempFile(Path dir, String prefix, String suffix,
            FileAttribute<?>... attrs) throws IOException {
        sinAtributos(attrs);
        int intentos = 0;
        while (intentos < 100) {
            Path p = dir.resolve(nombreTemporal(prefix, suffix, false));
            if ((Fs.stat(p.toString()) & Fs.EXISTE) == 0) {
                return createFile(p);
            }
            intentos = intentos + 1;
        }
        throw new IOException("cannot create a unique temporary file in " + dir);
    }

    /** Como el otro, en el directorio de `java.io.tmpdir`. */
    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs)
            throws IOException {
        return createTempFile(directorioTemporal(), prefix, suffix, attrs);
    }

    /** Un directorio nuevo con nombre unico dentro de `dir`. No es atomico. */
    public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs)
            throws IOException {
        sinAtributos(attrs);
        int intentos = 0;
        while (intentos < 100) {
            Path p = dir.resolve(nombreTemporal(prefix, null, true));
            if ((Fs.stat(p.toString()) & Fs.EXISTE) == 0) {
                return createDirectory(p);
            }
            intentos = intentos + 1;
        }
        throw new IOException("cannot create a unique temporary directory in " + dir);
    }

    /** Como el otro, en el directorio de `java.io.tmpdir`. */
    public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs)
            throws IOException {
        return createTempDirectory(directorioTemporal(), prefix, attrs);
    }

    private static Path directorioTemporal() {
        String t = System.getProperty("java.io.tmpdir");
        if (t == null || t.length() == 0) {
            t = ".";
        }
        return Path.of(t);
    }

    // ------------------------------------------------------------------------------------------
    // Borrar, copiar, mover
    // ------------------------------------------------------------------------------------------

    /**
     * Borra el archivo, o el directorio **si esta vacio**.
     *
     * <p>Que un directorio con contenido no se borre es del nativo y es deliberado: un borrado
     * recursivo escondido detras de un `delete()` convierte un error de ruta en una perdida de
     * datos.
     *
     * @throws NoSuchFileException si no existe
     * @throws DirectoryNotEmptyException si es un directorio con cosas adentro
     */
    public static void delete(Path path) throws IOException {
        String p = ruta(path);
        int st = Fs.stat(p);
        if ((st & Fs.EXISTE) == 0) {
            throw new NoSuchFileException(p);
        }
        if (Fs.delete(p)) {
            return;
        }
        if ((st & Fs.ES_DIRECTORIO) != 0) {
            throw new DirectoryNotEmptyException(p);
        }
        throw new AccessDeniedException(p);
    }

    /**
     * Borra si esta; devuelve si borro algo.
     *
     * <p>**No es atomico**: se comprueba y despues se borra. Sirve para el caso comun --no querer
     * escribir el `try`/`catch`-- no para arbitrar entre procesos.
     */
    public static boolean deleteIfExists(Path path) throws IOException {
        String p = ruta(path);
        if ((Fs.stat(p) & Fs.EXISTE) == 0) {
            return false;
        }
        delete(path);
        return true;
    }

    // Separa las opciones de copia. Devuelve si hay que pisar el destino; rechaza las que no se
    // pueden honrar, cada una con la excepcion que le corresponde por spec.
    private static boolean resolverCopia(CopyOption[] options) throws IOException {
        boolean pisar = false;
        int i = 0;
        while (i < options.length) {
            CopyOption o = options[i];
            if (o == StandardCopyOption.REPLACE_EXISTING) {
                pisar = true;
            } else if (o == StandardCopyOption.COPY_ATTRIBUTES) {
                // Ni leer ni escribir metadatos: aceptarla en silencio dejaria al destino con
                // fechas y permisos distintos de los del origen sin avisar.
                throw new UnsupportedOperationException("COPY_ATTRIBUTES: no attribute natives");
            } else if (o == StandardCopyOption.ATOMIC_MOVE) {
                throw new AtomicMoveNotSupportedException(null, null,
                        "move is copy+delete in KajiJDK; there is no rename native");
            } else if (o == LinkOption.NOFOLLOW_LINKS) {
                // Sin enlaces, no seguirlos es lo que ya pasa: no cambia nada.
            } else {
                throw new UnsupportedOperationException(String.valueOf(o) + " not supported");
            }
            i = i + 1;
        }
        return pisar;
    }

    /**
     * Copia `source` en `target`.
     *
     * <p>**Solo archivos comunes.** Copiar un directorio en el JDK crea el directorio vacio en el
     * destino; aca eso se puede hacer, pero recorrerlo para copiar el contenido no, y la spec dice
     * que la copia de un directorio **no** es recursiva -- asi que el caso se soporta.
     *
     * <p>Como todo lo demas, pasa por memoria: un archivo de un giga se copia leyendolo entero.
     *
     * @throws FileAlreadyExistsException si el destino existe y no se paso `REPLACE_EXISTING`
     * @throws UnsupportedOperationException con `COPY_ATTRIBUTES`
     */
    public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
        String s = ruta(source);
        String t = ruta(target);
        boolean pisar = resolverCopia(options);
        int stOrigen = Fs.stat(s);
        if ((stOrigen & Fs.EXISTE) == 0) {
            throw new NoSuchFileException(s);
        }
        int stDestino = Fs.stat(t);
        if ((stDestino & Fs.EXISTE) != 0) {
            if (!pisar) {
                throw new FileAlreadyExistsException(t);
            }
            if (!Fs.delete(t)) {
                throw new DirectoryNotEmptyException(t);
            }
        }
        if ((stOrigen & Fs.ES_DIRECTORIO) != 0) {
            // La copia de un directorio crea uno vacio en el destino; no baja. Es lo que dice la
            // spec, no una limitacion de aca.
            if (!Fs.mkdir(t, false)) {
                throw new IOException("cannot create directory " + t);
            }
            return target;
        }
        byte[] b = Fs.readAllBytes(s);
        if (b == null) {
            throw porQueNoSeLeyo(s);
        }
        if (!Fs.writeAllBytes(t, b, false)) {
            throw new IOException("cannot write " + t);
        }
        return target;
    }

    /**
     * Mueve `source` a `target`.
     *
     * <p>**Es copiar y borrar, no un rename.** No hay nativo de rename, asi que hay un instante en
     * el que el archivo esta en los dos lados. Las consecuencias, dichas de frente:
     *
     * <ul>
     *   <li>`ATOMIC_MOVE` levanta `AtomicMoveNotSupportedException`, siempre. Es la respuesta
     *       correcta: quien la pide la pide porque le importa.
     *   <li>Un corte en el medio puede dejar el archivo duplicado. Nunca lo pierde: primero se
     *       escribe el destino y recien despues se borra el origen.
     *   <li>Mover un directorio con contenido **falla**, porque el borrado del origen no puede
     *       borrar un directorio no vacio. En el JDK un rename dentro del mismo volumen lo mueve.
     * </ul>
     */
    public static Path move(Path source, Path target, CopyOption... options) throws IOException {
        String s = ruta(source);
        copy(source, target, options);
        if (!Fs.delete(s)) {
            // El destino ya esta escrito. Se avisa en vez de callar: el arbol quedo con una copia
            // de mas y quien llamo tiene que saberlo.
            throw new IOException("copied to " + ruta(target) + " but cannot delete " + s);
        }
        return target;
    }

    // ------------------------------------------------------------------------------------------
    // Preguntas
    // ------------------------------------------------------------------------------------------

    /**
     * Si la ruta existe.
     *
     * <p>`options` se acepta y no cambia nada: sin enlaces simbolicos en el modelo, seguirlos o no
     * da lo mismo.
     */
    public static boolean exists(Path path, LinkOption... options) {
        return (Fs.stat(ruta(path)) & Fs.EXISTE) != 0;
    }

    /**
     * Si la ruta **no** existe.
     *
     * <p>No es la negacion de `exists` en el JDK --alla las dos pueden dar `false` cuando no se
     * puede determinar-- pero aca si lo es: `stat` no distingue "no existe" de "no puedo mirar", y
     * fingir un tercer estado que el nativo no reporta seria inventarlo.
     */
    public static boolean notExists(Path path, LinkOption... options) {
        return (Fs.stat(ruta(path)) & Fs.EXISTE) == 0;
    }

    /** Si existe y es un archivo comun. */
    public static boolean isRegularFile(Path path, LinkOption... options) {
        return (Fs.stat(ruta(path)) & Fs.ES_ARCHIVO) != 0;
    }

    /** Si existe y es un directorio. */
    public static boolean isDirectory(Path path, LinkOption... options) {
        return (Fs.stat(ruta(path)) & Fs.ES_DIRECTORIO) != 0;
    }

    /** Si esta VM lo puede leer. */
    public static boolean isReadable(Path path) {
        return (Fs.stat(ruta(path)) & Fs.SE_LEE) != 0;
    }

    /** Si esta VM lo puede escribir. */
    public static boolean isWritable(Path path) {
        return (Fs.stat(ruta(path)) & Fs.SE_ESCRIBE) != 0;
    }

    /**
     * Si esta VM lo puede ejecutar -- **siempre `false` en KajiJDK**.
     *
     * <p>`stat` trae lectura y escritura, no ejecucion, asi que aca no se puede determinar. Y
     * `false` **es** la respuesta que manda la spec para ese caso: el contrato dice `false` "si el
     * archivo no existe, si el permiso de ejecucion seria denegado, **o si el acceso no se puede
     * determinar**". Los tres son el mismo `false`, y por eso no hace falta inventar nada.
     *
     * <p>Es el unico de los tres `is*able` que difiere de su equivalente en el proveedor:
     * `KajiFileSystemProvider.checkAccess(p, EXECUTE)` tira `UnsupportedOperationException`, porque
     * ahi la firma **si** permite decir "no se puede", y decirlo es mejor que un `false`.
     */
    public static boolean isExecutable(Path path) {
        ruta(path);
        return false;
    }

    /**
     * Si es un enlace simbolico -- **siempre `false` en KajiJDK**.
     *
     * <p>Como en `isExecutable`, `false` es lo que la spec pide cuando no se puede determinar. El
     * `stat` de esta VM sigue los enlaces y devuelve los datos del destino, asi que un enlace a un
     * archivo se ve como el archivo: el modelo de abajo es transparente a los enlaces y no tiene
     * con que distinguirlos.
     */
    public static boolean isSymbolicLink(Path path) {
        try {
            return readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                    .isSymbolicLink();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Si el archivo se considera oculto.
     *
     * <p>**La definicion es del proveedor, y esta es la de KajiJDK: el nombre empieza con un
     * punto.** No es una respuesta a medias --la spec dice textualmente que "la definicion exacta
     * de oculto depende de la plataforma o del proveedor"--, asi que un proveedor que elige una
     * regla y la publica esta contestando el contrato, no esquivandolo. La regla elegida es la
     * misma que ya usa `java.io.File.isHidden()` en esta biblioteca: dos respuestas distintas para
     * la misma pregunta sobre el mismo archivo seria la incoherencia de verdad.
     *
     * <p>**En que difiere del JDK sobre Windows.** Alla la respuesta sale del bit de DOS, que
     * `stat` no trae: `.gitignore` da `false` en un JDK de Windows y `true` aca, y un archivo
     * marcado oculto sin punto inicial da `true` alla y `false` aca. Es una diferencia de
     * definicion, no un error de lectura, y por eso esta escrita.
     *
     * <p>No mira el disco --no hace falta para contestar por el nombre-- asi que tampoco falla si
     * el archivo no existe. Es lo mismo que hace el proveedor de Unix del JDK.
     */
    public static boolean isHidden(Path path) throws IOException {
        ruta(path);
        Path nombre = path.getFileName();
        if (nombre == null) {
            return false;
        }
        String s = nombre.toString();
        return s.length() > 0 && s.charAt(0) == '.';
    }

    /**
     * El tamaño en bytes.
     *
     * @throws NoSuchFileException si no existe
     */
    public static long size(Path path) throws IOException {
        String p = ruta(path);
        if ((Fs.stat(p) & Fs.EXISTE) == 0) {
            throw new NoSuchFileException(p);
        }
        return Fs.size(p);
    }

    /**
     * La posicion del primer byte en que difieren los dos archivos, o -1 si son identicos.
     *
     * <p>Si uno es prefijo del otro, la respuesta es el tamaño del mas corto.
     *
     * <p>A diferencia del JDK **no** hay atajo por identidad de archivo: alla, dos rutas al mismo
     * inodo dan -1 sin leer nada. Aca se leen y se comparan los bytes, que da la misma respuesta
     * para el mismo contenido -- solo que trabajando de mas.
     */
    public static long mismatch(Path path, Path path2) throws IOException {
        byte[] a = readAllBytes(path);
        byte[] b = readAllBytes(path2);
        int n = Math.min(a.length, b.length);
        int i = 0;
        while (i < n) {
            if (a[i] != b[i]) {
                return i;
            }
            i = i + 1;
        }
        return (a.length == b.length) ? -1L : ((long) n);
    }

    /**
     * Una vista de atributos del archivo -- **siempre `null` en KajiJDK**.
     *
     * <p>`null` no es un hueco tapado: la spec dice que es lo que hay que devolver cuando la vista
     * pedida no esta disponible, y aca no hay **ninguna** disponible.
     *
     * <p>Y no es porque no se puedan leer los atributos --`readAttributes` los lee-- sino porque una
     * vista es un objeto que **lee y escribe**: `BasicFileAttributeView`, la unica que un JDK esta
     * obligado a ofrecer, tiene `setTimes`, y no hay nativo que escriba metadatos ni excepcion
     * declarada ahi con la que decirlo. Una vista cuyo `setTimes` mintiera seria peor que no tener
     * vista. Por eso `FileSystem.supportedFileAttributeViews()` tampoco nombra `"basic"`: las dos
     * respuestas dicen lo mismo. Ver `java.nio.file.attribute.BasicFileAttributes`.
     */
    public static <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options) {
        ruta(path);
        if (type == null) {
            throw new NullPointerException();
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------
    // Atributos
    // ------------------------------------------------------------------------------------------

    // Todas las marcas de tiempo. La epoca **no** es un relleno: la spec de
    // `BasicFileAttributes.lastAccessTime` y `creationTime` dicen textualmente que si el sistema de
    // archivos no soporta la marca, el metodo devuelve "un valor por omision especifico de la
    // implementacion, tipicamente un `FileTime` que representa la epoca". No hay nativo para
    // ninguna de las dos, asi que este es el valor **contestado por el contrato**, no un cero
    // disfrazado de fecha.
    //
    // `lastModifiedTime` **ya no** usa esto: `Fs.mtime` la lee de verdad. Tenerla en epoca mientras
    // `setLastModifiedTime` la escribia habria sido peor que las dos ausencias juntas -- se podria
    // fijar una fecha y leer otra.
    //
    // Una sola instancia compartida: `FileTime` es inmutable.
    private static final FileTime EPOCA = FileTime.fromMillis(0L);

    /**
     * Los nueve atributos basicos de un archivo, sacados de un `stat` y un `size`.
     *
     * <p>Atributo por atributo, de donde sale cada uno:
     *
     * <ul>
     *   <li>`isRegularFile`, `isDirectory` -- banderas de `stat`, directo.
     *   <li>`isOther` -- existe y no es ninguna de las dos. Es la definicion de la spec.
     *   <li>`size` -- el nativo `size`.
     *   <li>`lastModifiedTime` -- el nativo `mtime`.
     *   <li>`lastAccessTime`, `creationTime` -- la epoca; ver `EPOCA`.
     *   <li>`fileKey` -- `null`, que la spec declara valido cuando no hay algo como el inodo.
     *   <li>`isSymbolicLink` -- `false`. Es el unico donde la respuesta no esta respaldada por el
     *       contrato de este tipo sino por el de `Files.isSymbolicLink`, que hace `false` la
     *       respuesta de "no se puede determinar". `stat` sigue los enlaces, asi que el modelo de
     *       abajo no los ve.
     * </ul>
     *
     * <p>Se toma **una** foto en el constructor y despues solo se consultan campos: eso es lo que
     * hace que las nueve preguntas sean consistentes entre si. Preguntarle a `stat` en cada
     * accesor podria mezclar dos momentos --archivo en uno, borrado en el otro-- que es justo lo
     * que un `BasicFileAttributes` existe para evitar.
     */
    private static final class AtributosBasicos implements BasicFileAttributes {

        private final int banderas;
        private final long bytes;

        // La fecha de modificacion, leida en la **misma foto** que las banderas y el tamano. Es lo
        // que mantiene consistentes a las nueve preguntas entre si, que es para lo que
        // `BasicFileAttributes` existe.
        private final FileTime modificado;

        AtributosBasicos(int banderas, long bytes, long millis) {
            this.banderas = banderas;
            this.bytes = bytes;
            this.modificado = millis == Long.MIN_VALUE ? EPOCA : FileTime.fromMillis(millis);
        }

        public FileTime lastModifiedTime() {
            return this.modificado;
        }

        public FileTime lastAccessTime() {
            return EPOCA;
        }

        public FileTime creationTime() {
            return EPOCA;
        }

        public boolean isRegularFile() {
            return (this.banderas & Fs.ES_ARCHIVO) != 0;
        }

        public boolean isDirectory() {
            return (this.banderas & Fs.ES_DIRECTORIO) != 0;
        }

        public boolean isSymbolicLink() {
            return false;
        }

        public boolean isOther() {
            return (this.banderas & Fs.EXISTE) != 0
                    && (this.banderas & (Fs.ES_ARCHIVO | Fs.ES_DIRECTORIO)) == 0;
        }

        public long size() {
            return this.bytes;
        }

        public Object fileKey() {
            return null;
        }
    }

    // La foto de `p`, o la excepcion que corresponda si no se pudo mirar.
    private static BasicFileAttributes leerAtributos(String p) throws IOException {
        int st = Fs.stat(p);
        if ((st & Fs.EXISTE) == 0) {
            throw new NoSuchFileException(p);
        }
        return new AtributosBasicos(st, Fs.size(p), Fs.mtime(p));
    }

    // Los nueve nombres de la vista `basic`, en el orden en que los devuelve el JDK. El orden de un
    // `Map` no es parte de ningun contrato, pero coincidir sale gratis y hace comparables las dos
    // salidas cuando se prueba una contra la otra.
    private static final String[] NOMBRES_BASIC = {
        "lastModifiedTime", "lastAccessTime", "creationTime", "size",
        "isRegularFile", "isDirectory", "isSymbolicLink", "isOther", "fileKey"
    };

    // La mitad de antes de los dos puntos en "vista:atributos", con `basic` por omision.
    private static String vistaDe(String attribute) {
        int i = attribute.indexOf(':');
        return (i < 0) ? "basic" : attribute.substring(0, i);
    }

    // La mitad de despues.
    private static String nombresDe(String attribute) {
        int i = attribute.indexOf(':');
        return (i < 0) ? attribute : attribute.substring(i + 1);
    }

    // El valor de un atributo `basic` por nombre, o `null` si el nombre no es de los nueve.
    //
    // Devuelve `null` en vez de tirar para que quien llama elija la excepcion: `readAttributes`
    // pone el nombre completo con la vista en el mensaje, y `setAttribute` distingue "no existe" de
    // "existe pero es de solo lectura".
    private static Object atributoBasic(BasicFileAttributes a, String nombre) {
        if (nombre.equals("lastModifiedTime")) {
            return a.lastModifiedTime();
        }
        if (nombre.equals("lastAccessTime")) {
            return a.lastAccessTime();
        }
        if (nombre.equals("creationTime")) {
            return a.creationTime();
        }
        if (nombre.equals("size")) {
            return Long.valueOf(a.size());
        }
        if (nombre.equals("isRegularFile")) {
            return Boolean.valueOf(a.isRegularFile());
        }
        if (nombre.equals("isDirectory")) {
            return Boolean.valueOf(a.isDirectory());
        }
        if (nombre.equals("isSymbolicLink")) {
            return Boolean.valueOf(a.isSymbolicLink());
        }
        if (nombre.equals("isOther")) {
            return Boolean.valueOf(a.isOther());
        }
        if (nombre.equals("fileKey")) {
            return a.fileKey();
        }
        return null;
    }

    /**
     * Los atributos del archivo, del tipo pedido.
     *
     * <p>Solo `BasicFileAttributes.class`: es la unica vista que esta VM puede contestar. Para
     * `DosFileAttributes` o `PosixFileAttributes` tira `UnsupportedOperationException`, que es lo
     * que la spec declara para un tipo de atributos no soportado.
     *
     * <p>`options` se acepta y no cambia nada: sin enlaces en el modelo, seguirlos o no da lo
     * mismo. Ver `AtributosBasicos` para el detalle de que sale de donde.
     *
     * @throws UnsupportedOperationException si `type` no es `BasicFileAttributes.class`
     * @throws NoSuchFileException si el archivo no esta
     */
    public static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
            LinkOption... options) throws IOException {
        String p = ruta(path);
        if (type == null) {
            throw new NullPointerException();
        }
        if (type != BasicFileAttributes.class) {
            throw new UnsupportedOperationException(type.getName() + " is not supported");
        }
        return type.cast(leerAtributos(p));
    }

    /**
     * Los atributos nombrados, como mapa de nombre a valor.
     *
     * <p>`attributes` tiene la forma `[vista:]lista`, con la lista separada por comas y `*` para
     * pedir todos. La unica vista es `basic`, que es tambien la de omision.
     *
     * <p>Las claves del mapa van **sin** el prefijo de la vista, como en el JDK.
     *
     * @throws UnsupportedOperationException si se nombra una vista que no es `basic`
     * @throws IllegalArgumentException si no se nombra ningun atributo, o si alguno no existe
     * @throws NoSuchFileException si el archivo no esta
     */
    public static Map<String, Object> readAttributes(Path path, String attributes,
            LinkOption... options) throws IOException {
        String p = ruta(path);
        if (attributes == null) {
            throw new NullPointerException();
        }
        String vista = vistaDe(attributes);
        if (!vista.equals("basic")) {
            throw new UnsupportedOperationException("View '" + vista + "' not available");
        }
        String lista = nombresDe(attributes);
        if (lista.length() == 0) {
            throw new IllegalArgumentException("No attributes specified");
        }
        BasicFileAttributes a = leerAtributos(p);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        int inicio = 0;
        while (inicio <= lista.length()) {
            int coma = lista.indexOf(',', inicio);
            String nombre = (coma < 0) ? lista.substring(inicio) : lista.substring(inicio, coma);
            if (nombre.equals("*")) {
                // `*` gana sobre lo demas: pedir "size,*" da los nueve, igual que en el JDK.
                int i = 0;
                while (i < NOMBRES_BASIC.length) {
                    out.put(NOMBRES_BASIC[i], atributoBasic(a, NOMBRES_BASIC[i]));
                    i = i + 1;
                }
                return out;
            }
            if (nombre.length() > 0) {
                Object v = atributoBasic(a, nombre);
                if (v == null && !nombre.equals("fileKey")) {
                    throw new IllegalArgumentException("'basic:" + nombre + "' not recognized");
                }
                out.put(nombre, v);
            }
            if (coma < 0) {
                break;
            }
            inicio = coma + 1;
        }
        return out;
    }

    /**
     * El valor de **un** atributo.
     *
     * <p>Como `readAttributes(path, attributes, options)` pero para un solo nombre: ni `*` ni comas.
     *
     * @throws IllegalArgumentException si el nombre trae `*` o `,`, o si no existe
     * @throws UnsupportedOperationException si se nombra una vista que no es `basic`
     * @throws NoSuchFileException si el archivo no esta
     */
    public static Object getAttribute(Path path, String attribute, LinkOption... options)
            throws IOException {
        if (attribute == null) {
            throw new NullPointerException();
        }
        if (attribute.indexOf('*') >= 0 || attribute.indexOf(',') >= 0) {
            throw new IllegalArgumentException(attribute);
        }
        Map<String, Object> uno = readAttributes(path, attribute, options);
        return uno.get(nombresDe(attribute));
    }

    /**
     * Fija un atributo -- **nunca funciona en KajiJDK**.
     *
     * <p>No hay nativo que escriba metadatos, asi que ninguna vista de esta VM es escribible. La
     * excepcion depende de **por que** no se puede, para que el error diga algo:
     *
     * <ul>
     *   <li>vista que no es `basic`: `UnsupportedOperationException`, que es lo que la spec declara
     *       para "la vista de atributos no esta disponible".
     *   <li>nombre que no es de los nueve: `IllegalArgumentException`, igual que el JDK.
     *   <li>uno de los seis atributos de solo lectura (`size`, `isDirectory`, ...):
     *       `IllegalArgumentException`, tambien igual que el JDK -- no son escribibles en **ningun**
     *       sistema de archivos.
     *   <li>una de las tres marcas de tiempo, que en el JDK **si** se pueden fijar:
     *       `UnsupportedOperationException`. Es la unica de las cuatro donde la diferencia es de
     *       esta VM y no de la spec, y por eso la excepcion es la que dice "aca no se puede".
     * </ul>
     */
    public static Path setAttribute(Path path, String attribute, Object value,
            LinkOption... options) throws IOException {
        ruta(path);
        if (attribute == null) {
            throw new NullPointerException();
        }
        String vista = vistaDe(attribute);
        if (!vista.equals("basic")) {
            throw new UnsupportedOperationException("View '" + vista + "' not available");
        }
        String nombre = nombresDe(attribute);
        boolean esTiempo = nombre.equals("lastModifiedTime") || nombre.equals("lastAccessTime")
                || nombre.equals("creationTime");
        if (esTiempo) {
            throw new UnsupportedOperationException(
                    "KajiJDK cannot write 'basic:" + nombre + "': no native writes timestamps");
        }
        throw new IllegalArgumentException("'basic:" + nombre + "' not recognized");
    }

    /**
     * La fecha de ultima modificacion -- **siempre la epoca** en KajiJDK.
     *
     * <p>Ver `EPOCA`: es el valor que la spec manda devolver cuando el sistema de archivos no
     * guarda la marca, y `stat` no la guarda.
     *
     * @throws NoSuchFileException si el archivo no esta
     */
    public static FileTime getLastModifiedTime(Path path, LinkOption... options)
            throws IOException {
        return leerAtributos(ruta(path)).lastModifiedTime();
    }

    /**
     * Los permisos POSIX -- **falla siempre**.
     *
     * <p>`UnsupportedOperationException` es lo que la spec declara para cuando el sistema de
     * archivos no soporta `PosixFileAttributeView`, y este no la soporta: `stat` da cinco banderas y
     * ninguna es un modo de nueve bits. Ver `FileSystem.supportedFileAttributeViews`, que devuelve
     * el conjunto vacio y es de donde sale esta respuesta.
     */
    public static Set<PosixFilePermission> getPosixFilePermissions(Path path,
            LinkOption... options) throws IOException {
        ruta(path);
        throw new UnsupportedOperationException("PosixFileAttributeView is not supported");
    }

    /** Fija los permisos POSIX -- **falla siempre**, por lo mismo que el otro. */
    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> perms)
            throws IOException {
        ruta(path);
        if (perms == null) {
            throw new NullPointerException();
        }
        throw new UnsupportedOperationException("PosixFileAttributeView is not supported");
    }

    /**
     * El dueño del archivo -- **falla siempre**.
     *
     * <p>`UnsupportedOperationException` es lo que la spec declara para cuando no se soporta
     * `FileOwnerAttributeView`. No hay nativo que devuelva un uid, y tampoco hay con que convertir
     * un uid en un `UserPrincipal`: ver `FileSystem.getUserPrincipalLookupService`, que falla por lo
     * mismo.
     */
    public static UserPrincipal getOwner(Path path, LinkOption... options) throws IOException {
        ruta(path);
        throw new UnsupportedOperationException("FileOwnerAttributeView is not supported");
    }

    /** Fija el dueño -- **falla siempre**, por lo mismo que el otro. */
    public static Path setOwner(Path path, UserPrincipal owner) throws IOException {
        ruta(path);
        if (owner == null) {
            throw new NullPointerException();
        }
        throw new UnsupportedOperationException("FileOwnerAttributeView is not supported");
    }

    /**
     * El tipo MIME del archivo, o `null` si no se puede determinar.
     *
     * <p>**Hoy devuelve `null` para todo, y eso es la respuesta correcta, no un stub.** La spec dice
     * que el resultado sale de los `FileTypeDetector` **instalados** --que se descubren con
     * `ServiceLoader`-- mas un detector por omision del sistema. KajiJDK no trae detector por
     * omision, y el descubrimiento no encuentra nada porque `ClassLoader` no tiene recursos (ver la
     * cabecera de `java.util.ServiceLoader`). Con la cadena vacia, `null` --"no se pudo
     * determinar"-- es lo unico que se puede contestar.
     *
     * <p>El recorrido esta escrito de verdad y no cortocircuitado a `return null`: el dia que el
     * descubrimiento funcione, un detector puesto en el classpath por la aplicacion se usa sin
     * tocar esta clase. Adivinar por extension aca dentro seria contestar por un detector que nadie
     * registro.
     */
    public static String probeContentType(Path path) throws IOException {
        ruta(path);
        // El `ServiceLoader` va a una variable con el tipo escrito en vez de encadenar
        // `.iterator()` sobre la llamada: la inferencia de nuestro `javac` no propaga el parametro
        // de tipo a traves de la cadena. Ver el informe de esta sesion.
        ServiceLoader<FileTypeDetector> detectores = ServiceLoader.load(FileTypeDetector.class);
        Iterator<FileTypeDetector> it = detectores.iterator();
        while (it.hasNext()) {
            String tipo = it.next().probeContentType(path);
            if (tipo != null) {
                return tipo;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------
    // Enlaces
    // ------------------------------------------------------------------------------------------

    /**
     * Crea un enlace simbolico -- **falla siempre**.
     *
     * <p>`UnsupportedOperationException` es exactamente lo que la spec declara para "la
     * implementacion no soporta la creacion de enlaces simbolicos", y los seis nativos de
     * `jdk.internal.io.Fs` no incluyen ninguno que la haga. Un programa que llama a esto recibe la
     * misma excepcion que recibiria de un JDK sobre un sistema de archivos sin enlaces.
     */
    public static Path createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
            throws IOException {
        ruta(link);
        ruta(target);
        throw new UnsupportedOperationException("KajiJDK does not support symbolic links");
    }

    /**
     * Crea un enlace duro -- **falla siempre**.
     *
     * <p>La spec declara `UnsupportedOperationException` para "la implementacion no soporta agregar
     * un archivo existente a un directorio", que es lo que pasa aca.
     */
    public static Path createLink(Path link, Path existing) throws IOException {
        ruta(link);
        ruta(existing);
        throw new UnsupportedOperationException("KajiJDK does not support hard links");
    }

    /**
     * El destino de un enlace simbolico -- **falla siempre**.
     *
     * <p>Misma razon que `createSymbolicLink`, y la misma excepcion declarada por la spec. Notar que
     * **no** es `NotLinkException`: eso diria "esta ruta no es un enlace", que es una afirmacion
     * sobre el archivo, y aca lo que no existe es la operacion.
     */
    public static Path readSymbolicLink(Path link) throws IOException {
        ruta(link);
        throw new UnsupportedOperationException("KajiJDK does not support symbolic links");
    }

    // ------------------------------------------------------------------------------------------
    // Leer y escribir el contenido
    // ------------------------------------------------------------------------------------------

    /**
     * Todos los bytes del archivo.
     *
     * <p>Es la operacion nativa tal cual: en esta VM leer un archivo es leerlo entero, asi que este
     * metodo es el barato y los demas se arman encima.
     */
    public static byte[] readAllBytes(Path path) throws IOException {
        String p = ruta(path);
        byte[] b = Fs.readAllBytes(p);
        if (b == null) {
            throw porQueNoSeLeyo(p);
        }
        return b;
    }

    /** El archivo como texto, decodificado con `cs`. */
    public static String readString(Path path, Charset cs) throws IOException {
        if (cs == null) {
            throw new NullPointerException();
        }
        return new String(readAllBytes(path), cs);
    }

    /** El archivo como texto, en UTF-8. */
    public static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    // Corta en lineas por `\n`, `\r` o `\r\n`, sin dejar una linea vacia al final si el archivo
    // termina con salto. Es la regla de `BufferedReader.readLine`, escrita aca sobre la cadena ya
    // decodificada para no armar toda la cañeria de streams por algo que es un recorrido.
    private static List<String> cortarEnLineas(String texto) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        int inicio = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (c == '\n') {
                out.add(texto.substring(inicio, i));
                i = i + 1;
                inicio = i;
            } else if (c == '\r') {
                out.add(texto.substring(inicio, i));
                i = i + 1;
                if (i < texto.length() && texto.charAt(i) == '\n') {
                    i = i + 1;
                }
                inicio = i;
            } else {
                i = i + 1;
            }
        }
        if (inicio < texto.length()) {
            out.add(texto.substring(inicio));
        }
        return out;
    }

    /** Las lineas del archivo, decodificadas con `cs`. */
    public static List<String> readAllLines(Path path, Charset cs) throws IOException {
        return cortarEnLineas(readString(path, cs));
    }

    /** Las lineas del archivo, en UTF-8. */
    public static List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * Las lineas del archivo como un `Stream`.
     *
     * <p>**Diferencia con el JDK, y no es chica: aca no es perezoso.** Alla el stream lee a medida
     * que se consume y hay que cerrarlo; aca el archivo se lee entero primero --no hay otra forma
     * de leerlo-- y el stream sale de una lista en memoria. El resultado son las mismas lineas en
     * el mismo orden; lo que cambia es cuando se hace el trabajo y cuanta memoria ocupa. `close()`
     * sobre el stream sigue siendo valido y no hace falta.
     */
    public static Stream<String> lines(Path path, Charset cs) throws IOException {
        return readAllLines(path, cs).stream();
    }

    /** Como el otro, en UTF-8. */
    public static Stream<String> lines(Path path) throws IOException {
        return lines(path, StandardCharsets.UTF_8);
    }

    // Escribe `datos` respetando las opciones. Es el unico lugar que llama a `writeAllBytes`, para
    // que la comprobacion de existencia y la eleccion de anexar esten escritas una sola vez.
    private static Path escribir(Path path, byte[] datos, OpenOption[] options)
            throws IOException {
        String p = ruta(path);
        Apertura a = resolver(options, true);
        if (a.leer) {
            throw new UnsupportedOperationException("READ on write");
        }
        boolean existe = (Fs.stat(p) & Fs.EXISTE) != 0;
        if (a.crearNuevo && existe) {
            throw new FileAlreadyExistsException(p);
        }
        if (!existe && !a.crear && !a.crearNuevo && options.length > 0) {
            throw new NoSuchFileException(p);
        }
        if (!Fs.writeAllBytes(p, datos, a.anexar)) {
            throw new IOException("cannot write " + p);
        }
        return path;
    }

    /**
     * Escribe los bytes en el archivo.
     *
     * <p>Sin opciones: crea, trunca y escribe. Con `APPEND`: agrega al final.
     */
    public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException {
        if (bytes == null) {
            throw new NullPointerException();
        }
        return escribir(path, bytes, options);
    }

    /** Escribe el texto codificado con `cs`. */
    public static Path writeString(Path path, CharSequence csq, Charset cs, OpenOption... options)
            throws IOException {
        if (csq == null || cs == null) {
            throw new NullPointerException();
        }
        return escribir(path, csq.toString().getBytes(cs), options);
    }

    /** Escribe el texto en UTF-8. */
    public static Path writeString(Path path, CharSequence csq, OpenOption... options)
            throws IOException {
        return writeString(path, csq, StandardCharsets.UTF_8, options);
    }

    /**
     * Escribe las lineas, cada una seguida del separador de linea de la plataforma.
     *
     * <p>Se arma la cadena entera y se codifica de una sola vez, en vez de linea por linea: con un
     * nativo que escribe el archivo completo, escribir de a una seria releer y reescribir todo por
     * cada linea.
     */
    public static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs,
            OpenOption... options) throws IOException {
        if (lines == null || cs == null) {
            throw new NullPointerException();
        }
        String salto = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        Iterator<? extends CharSequence> it = lines.iterator();
        while (it.hasNext()) {
            CharSequence linea = it.next();
            sb.append(linea == null ? "null" : linea.toString());
            sb.append(salto);
        }
        return escribir(path, sb.toString().getBytes(cs), options);
    }

    /** Como el otro, en UTF-8. */
    public static Path write(Path path, Iterable<? extends CharSequence> lines,
            OpenOption... options) throws IOException {
        return write(path, lines, StandardCharsets.UTF_8, options);
    }

    /**
     * Vuelca `in` en `target` y devuelve cuantos bytes copio.
     *
     * <p>El stream se lee entero a memoria antes de escribir, porque el nativo escribe el archivo
     * completo de una. **`in` no se cierra**: lo abrio quien llamo, y cerrar algo que no se abrio
     * es la clase de sorpresa que rompe un `try`-con-recursos de afuera.
     *
     * @throws FileAlreadyExistsException si el destino existe y no se paso `REPLACE_EXISTING`
     */
    public static long copy(InputStream in, Path target, CopyOption... options) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        String t = ruta(target);
        boolean pisar = resolverCopia(options);
        if ((Fs.stat(t) & Fs.EXISTE) != 0 && !pisar) {
            throw new FileAlreadyExistsException(t);
        }
        byte[] datos = leerTodo(in);
        if (!Fs.writeAllBytes(t, datos, false)) {
            throw new IOException("cannot write " + t);
        }
        return (long) datos.length;
    }

    /**
     * Vuelca `source` en `out` y devuelve cuantos bytes copio.
     *
     * <p>**`out` no se cierra**, por la misma razon que `in` en la otra sobrecarga.
     */
    public static long copy(Path source, OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        byte[] b = readAllBytes(source);
        out.write(b, 0, b.length);
        return (long) b.length;
    }

    // Junta todo lo que quede en el stream. Duplica el buffer cuando se llena: crecer de a un
    // tamaño fijo seria cuadratico en la cantidad de copias para un stream grande.
    private static byte[] leerTodo(InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        int usado = 0;
        while (true) {
            if (usado == buf.length) {
                byte[] mas = new byte[buf.length * 2];
                System.arraycopy(buf, 0, mas, 0, usado);
                buf = mas;
            }
            int n = in.read(buf, usado, buf.length - usado);
            if (n < 0) {
                break;
            }
            usado = usado + n;
        }
        byte[] out = new byte[usado];
        System.arraycopy(buf, 0, out, 0, usado);
        return out;
    }

    // ---- enumerar un directorio ---------------------------------------------------------------------
    //
    // Los nueve se apoyan en `Fs.list`, y los nueve se apoyan en **uno**: `walkFileTree`. Los `walk`,
    // `find` y `list` son vistas de flujo sobre el mismo recorrido, y escribirlos aparte habria dado
    // cuatro recorridos que se pueden desincronizar.

    /** Las entradas **directas** de ese directorio. Sin filtrar, sin bajar. */
    public static DirectoryStream<Path> newDirectoryStream(Path dir) throws IOException {
        return new KajiDirectoryStream(dir, null);
    }

    /**
     * El de arriba filtrado por un **glob** contra el nombre de cada entrada.
     *
     * <p>El patron se compara contra el ultimo elemento del camino y no contra el camino entero, que
     * es lo que hace que `"*.java"` funcione como uno espera.
     */
    public static DirectoryStream<Path> newDirectoryStream(Path dir, String glob)
            throws IOException {
        if (glob == null) {
            throw new NullPointerException("glob");
        }
        // `"*"` es el caso comun y significa "todo": no hace falta armar un matcher para eso.
        if (glob.equals("*")) {
            return new KajiDirectoryStream(dir, null);
        }
        final PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + glob);
        return new KajiDirectoryStream(dir, new DirectoryStream.Filter<Path>() {
            public boolean accept(Path entry) {
                Path nombre = entry.getFileName();
                return nombre != null && matcher.matches(nombre);
            }
        });
    }

    /** El de arriba con un filtro propio. */
    public static DirectoryStream<Path> newDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException {
        if (filter == null) {
            throw new NullPointerException("filter");
        }
        return new KajiDirectoryStream(dir, filter);
    }

    /**
     * Recorre el arbol que cuelga de `start`, avisandole al visitante en cada paso.
     *
     * <p>Es el metodo del que salen los otros cuatro. El orden es el que el contrato fija: para un
     * directorio, `preVisitDirectory`, despues sus hijos, despues `postVisitDirectory`; para un
     * archivo, `visitFile`. Y `visitFileFailed` cuando no se pudo mirar una entrada -- que **no** es
     * un error del recorrido: el visitante decide si sigue.
     *
     * @param maxDepth cuantos niveles bajar; `0` visita solo `start`
     */
    public static Path walkFileTree(Path start, java.util.Set<FileVisitOption> options,
            int maxDepth, FileVisitor<? super Path> visitor) throws IOException {
        if (start == null || options == null || visitor == null) {
            throw new NullPointerException();
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth no puede ser negativo");
        }
        recorrer(start, 0, maxDepth, visitor);
        return start;
    }

    /** El de arriba sin opciones y sin limite de profundidad. */
    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor)
            throws IOException {
        return walkFileTree(start, java.util.Collections.<FileVisitOption>emptySet(),
                Integer.MAX_VALUE, visitor);
    }

    // El recorrido. Devuelve lo que el visitante contesto, para que un `TERMINATE` corte todo el
    // arbol y no solo la rama -- que es la diferencia entre `TERMINATE` y `SKIP_SUBTREE`.
    private static FileVisitResult recorrer(Path p, int nivel, int maxDepth,
            FileVisitor<? super Path> visitor) throws IOException {
        BasicFileAttributes attrs = null;
        try {
            attrs = readAttributes(p, BasicFileAttributes.class);
        } catch (IOException e) {
            return visitor.visitFileFailed(p, e);
        }
        if (!attrs.isDirectory() || nivel >= maxDepth) {
            return visitor.visitFile(p, attrs);
        }
        FileVisitResult r = visitor.preVisitDirectory(p, attrs);
        if (r == FileVisitResult.TERMINATE) {
            return r;
        }
        if (r == FileVisitResult.SKIP_SUBTREE || r == FileVisitResult.SKIP_SIBLINGS) {
            // `SKIP_SIBLINGS` sobre un directorio significa "ni entres ni sigas con sus hermanos":
            // no se baja, y el que corta a los hermanos es el bucle de arriba.
            return r == FileVisitResult.SKIP_SIBLINGS ? r : FileVisitResult.CONTINUE;
        }
        IOException fallo = null;
        try {
            DirectoryStream<Path> hijos = newDirectoryStream(p);
            try {
                java.util.Iterator<Path> it = hijos.iterator();
                while (it.hasNext()) {
                    FileVisitResult hr = recorrer(it.next(), nivel + 1, maxDepth, visitor);
                    if (hr == FileVisitResult.TERMINATE) {
                        return hr;
                    }
                    if (hr == FileVisitResult.SKIP_SIBLINGS) {
                        break;
                    }
                }
            } finally {
                hijos.close();
            }
        } catch (IOException e) {
            // El fallo al listar se le pasa a `postVisitDirectory`, que es donde el contrato dice
            // que llega. No se lanza: el visitante puede querer seguir con el resto del arbol.
            fallo = e;
        }
        return visitor.postVisitDirectory(p, fallo);
    }

    /** Las entradas directas de un directorio, como flujo. */
    public static java.util.stream.Stream<Path> list(Path dir) throws IOException {
        List<Path> out = new ArrayList<Path>();
        DirectoryStream<Path> s = newDirectoryStream(dir);
        try {
            java.util.Iterator<Path> it = s.iterator();
            while (it.hasNext()) {
                out.add(it.next());
            }
        } finally {
            s.close();
        }
        return out.stream();
    }

    /**
     * Todo el arbol que cuelga de `start`, como flujo, hasta `maxDepth` niveles.
     *
     * <p>El primer elemento es `start`. Una entrada que no se puede mirar **corta** el flujo con
     * `IOException`, que es lo que el JDK hace: `walk` no tiene por donde avisar de un fallo parcial.
     */
    public static java.util.stream.Stream<Path> walk(Path start, int maxDepth,
            FileVisitOption... options) throws IOException {
        final List<Path> out = new ArrayList<Path>();
        walkFileTree(start, opcionesDe(options), maxDepth, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                out.add(dir);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                out.add(file);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }
        });
        return out.stream();
    }

    /** El de arriba sin limite de profundidad. */
    public static java.util.stream.Stream<Path> walk(Path start, FileVisitOption... options)
            throws IOException {
        return walk(start, Integer.MAX_VALUE, options);
    }

    /**
     * Las entradas del arbol que cumplen el predicado.
     *
     * <p>El predicado recibe el camino **y sus atributos**, que ya se leyeron para recorrer: es lo
     * que evita que quien filtra por tamano o por fecha tenga que volver a mirar el disco.
     */
    public static java.util.stream.Stream<Path> find(Path start, int maxDepth,
            java.util.function.BiPredicate<Path, BasicFileAttributes> matcher,
            FileVisitOption... options) throws IOException {
        if (matcher == null) {
            throw new NullPointerException("matcher");
        }
        final List<Path> out = new ArrayList<Path>();
        final java.util.function.BiPredicate<Path, BasicFileAttributes> pred = matcher;
        walkFileTree(start, opcionesDe(options), maxDepth, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (pred.test(dir, attrs)) {
                    out.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (pred.test(file, attrs)) {
                    out.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }
        });
        return out.stream();
    }

    // Las opciones como conjunto. Hoy la unica que existe es `FOLLOW_LINKS`, y esta VM no tiene
    // enlaces simbolicos, asi que el recorrido sale igual con o sin ella. Se acepta igual porque la
    // firma la declara y rechazarla seria inventar un error.
    private static java.util.Set<FileVisitOption> opcionesDe(FileVisitOption[] options) {
        java.util.Set<FileVisitOption> out = new java.util.HashSet<FileVisitOption>();
        int i = 0;
        while (options != null && i < options.length) {
            if (options[i] == null) {
                throw new NullPointerException("una opcion es null");
            }
            out.add(options[i]);
            i = i + 1;
        }
        return out;
    }

    // ---- identidad y fecha de modificacion ------------------------------------------------------
    //
    // Los dos estuvieron afuera hasta que aparecieron sus nativos, y los dos son de la misma
    // familia: preguntas sobre el archivo que el nombre no puede contestar.

    /**
     * Si las dos rutas nombran **el mismo archivo**.
     *
     * <p>La pregunta no la puede contestar la cadena. En Windows `C:\A.TXT` y `C:.txt` son el
     * mismo archivo y no son el mismo texto; una ruta relativa y una absoluta tampoco; y `a/../b` y
     * `b` menos. Por eso se compara la forma **canonica**, que resuelve las tres cosas.
     *
     * <p>El atajo por igualdad va primero y no es solo una optimizacion: la spec dice que dos
     * caminos iguales son el mismo archivo **sin mirar el disco**, asi que dos rutas iguales que no
     * existen dan `true` igual.
     *
     * @throws NoSuchFileException si alguno de los dos no existe
     */
    public static boolean isSameFile(Path path, Path path2) throws IOException {
        if (path == null || path2 == null) {
            throw new NullPointerException();
        }
        if (path.equals(path2)) {
            return true;
        }
        String a = Fs.canonical(path.toString());
        if (a == null) {
            throw new NoSuchFileException(path.toString());
        }
        String b = Fs.canonical(path2.toString());
        if (b == null) {
            throw new NoSuchFileException(path2.toString());
        }
        return a.equals(b);
    }

    /**
     * Fija la fecha de ultima modificacion.
     *
     * <p>Devuelve el mismo `path`, que es lo que permite encadenarlo detras de un `write`.
     *
     * @throws NoSuchFileException si el archivo no existe
     */
    public static Path setLastModifiedTime(Path path, java.nio.file.attribute.FileTime time)
            throws IOException {
        if (path == null || time == null) {
            throw new NullPointerException();
        }
        if (!Fs.setMtime(path.toString(), time.toMillis())) {
            if ((Fs.stat(path.toString()) & Fs.EXISTE) == 0) {
                throw new NoSuchFileException(path.toString());
            }
            throw new IOException("no se pudo fijar la fecha de " + path);
        }
        return path;
    }

    /**
     * El volumen donde vive ese archivo.
     *
     * <p>El {@link FileStore} que sale contesta los tres espacios de verdad --total, utilizable y
     * sin asignar-- y da `"unknown"` como tipo, que es lo que el propio JDK contesta cuando no lo
     * puede determinar. Ver {@link KajiFileStore} sobre que se sabe y que no.
     *
     * @throws NullPointerException si `path` es `null`
     * @throws IOException si el archivo no existe o si no se pudo leer el volumen
     */
    public static FileStore getFileStore(Path path) throws IOException {
        if (path == null) {
            throw new NullPointerException("path");
        }
        String ruta = path.toAbsolutePath().toString();
        // Que el archivo exista se comprueba **antes** de preguntar por el volumen, y no despues: la
        // API de espacio contesta igual por una ruta que no existe --le alcanza con el volumen-- y
        // `getFileStore` de un archivo inexistente tiene que fallar, no devolver el volumen de su
        // directorio.
        if (jdk.internal.io.Fs.stat(ruta) == 0) {
            throw new NoSuchFileException(path.toString());
        }
        return new KajiFileStore(ruta, KajiFileStore.nombreDeVolumen(ruta));
    }
}
