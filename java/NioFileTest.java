// `java.nio.file` y `java.nio.file.attribute` de punta a punta.
//
// **Se comprueba contra `java` real corriendo lo mismo**, asi que la prueba solo toca lo que las dos
// VMs pueden contestar igual: crea sus propios archivos --nada del arbol, que podria diferir en los
// finales de linea-- y no usa ninguno de los metodos que KajiJDK deja afuera a proposito.
//
// Hay tres cosas que **no** se comprueban aunque compilarian en las dos, y vale decir por que:
//
//   - `Files.getFileAttributeView`: aca devuelve siempre `null` (no hay vistas disponibles) y en el
//     JDK devuelve una vista real. La diferencia es honesta y esta documentada, pero no es
//     comparable.
//   - `AclEntry.toString()`: recorre conjuntos, y el orden de iteracion de un `HashSet` no es parte
//     de ningun contrato. Se comprueban los accesores, que si lo son.
//   - El **rechazo** de `SYNC`, `DSYNC` y `DELETE_ON_CLOSE` en `newOutputStream`: KajiJDK las tira
//     porque abajo hay un stream que acumula y no las puede cumplir, y el JDK las acepta porque el
//     suyo si. La diferencia esta documentada en la cabecera de `Files`. Lo que si se comprueba, en
//     `canales()`, es que `newByteChannel` las **acepta** en las dos VMs, que es el otro lado de esa
//     misma decision y ahi si son comparables.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NioFileTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static final String BASE = "_kaji_niotest_";

    // ---------------------------------------------------------------------------------------
    // FileTime: puro valor, sin tocar el disco.
    // ---------------------------------------------------------------------------------------
    static void tiempos() {
        FileTime t = FileTime.fromMillis(1234567890000L);
        ok(t.toMillis() == 1234567890000L);
        ok("2009-02-13T23:31:30Z".equals(t.toString()));

        FileTime conFraccion = FileTime.fromMillis(1234567890123L);
        ok("2009-02-13T23:31:30.123Z".equals(conFraccion.toString()));

        // La misma marca en dos granularidades distintas: iguales, y con el mismo hash.
        FileTime porSegundos = FileTime.from(1234567890L, TimeUnit.SECONDS);
        ok(porSegundos.equals(t));
        ok(t.equals(porSegundos));
        ok(porSegundos.hashCode() == t.hashCode());
        ok(porSegundos.compareTo(t) == 0);

        // Y el orden.
        ok(t.compareTo(conFraccion) < 0);
        ok(conFraccion.compareTo(t) > 0);

        // Conversiones.
        ok(t.to(TimeUnit.SECONDS) == 1234567890L);
        ok(conFraccion.to(TimeUnit.MILLISECONDS) == 1234567890123L);
        ok(t.to(TimeUnit.DAYS) == 14288L);

        // Ida y vuelta por Instant.
        Instant i = t.toInstant();
        ok(i.getEpochSecond() == 1234567890L);
        ok(i.getNano() == 0);
        ok(FileTime.from(i).equals(t));

        // La epoca, y una fecha anterior.
        ok("1970-01-01T00:00:00Z".equals(FileTime.fromMillis(0L).toString()));
        ok("1969-12-31T23:59:59Z".equals(FileTime.fromMillis(-1000L).toString()));

        // Saturacion en vez de dar vuelta el signo: los dias de Long.MAX_VALUE no entran en nanos.
        FileTime lejos = FileTime.from(Long.MAX_VALUE, TimeUnit.DAYS);
        ok(lejos.to(TimeUnit.NANOSECONDS) == Long.MAX_VALUE);
        ok(lejos.toMillis() == Long.MAX_VALUE);
    }

    // ---------------------------------------------------------------------------------------
    // Los permisos POSIX, que son puro texto.
    // ---------------------------------------------------------------------------------------
    static void permisos() {
        Set<PosixFilePermission> p = PosixFilePermissions.fromString("rwxr-x---");
        ok(p.size() == 5);
        ok(p.contains(PosixFilePermission.OWNER_READ));
        ok(p.contains(PosixFilePermission.OWNER_WRITE));
        ok(p.contains(PosixFilePermission.OWNER_EXECUTE));
        ok(p.contains(PosixFilePermission.GROUP_READ));
        ok(p.contains(PosixFilePermission.GROUP_EXECUTE));
        ok(!p.contains(PosixFilePermission.GROUP_WRITE));
        ok(!p.contains(PosixFilePermission.OTHERS_READ));

        // Ida y vuelta.
        ok("rwxr-x---".equals(PosixFilePermissions.toString(p)));
        ok("---------".equals(
                PosixFilePermissions.toString(new HashSet<PosixFilePermission>())));
        ok("rwxrwxrwx".equals(
                PosixFilePermissions.toString(PosixFilePermissions.fromString("rwxrwxrwx"))));

        // Una cadena mal escrita falla fuerte en vez de adivinar.
        ok(rompe("rwx"));
        ok(rompe("xwrxwrxwr"));
        ok(rompe("rwxrwxrwxx"));

        ok("posix:permissions".equals(PosixFilePermissions.asFileAttribute(p).name()));
        ok(PosixFilePermissions.asFileAttribute(p).value().size() == 5);
    }

    static boolean rompe(String modo) {
        try {
            PosixFilePermissions.fromString(modo);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------
    // AclEntry: puro valor tambien. Necesita un UserPrincipal, que se puede escribir aca.
    // ---------------------------------------------------------------------------------------
    static final class Usuario implements UserPrincipal {

        private final String nombre;

        Usuario(String nombre) {
            this.nombre = nombre;
        }

        public String getName() {
            return this.nombre;
        }

        public boolean equals(Object o) {
            return (o instanceof Usuario) && ((Usuario) o).nombre.equals(this.nombre);
        }

        public int hashCode() {
            return this.nombre.hashCode();
        }

        public String toString() {
            return this.nombre;
        }
    }

    static void acl() {
        Usuario u = new Usuario("kaji");
        AclEntry e = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(u)
                .setPermissions(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA)
                .setFlags(AclEntryFlag.FILE_INHERIT)
                .build();
        ok(e.type() == AclEntryType.ALLOW);
        ok(e.principal().getName().equals("kaji"));
        ok(e.permissions().size() == 2);
        ok(e.permissions().contains(AclEntryPermission.READ_DATA));
        ok(e.flags().size() == 1);
        ok(e.flags().contains(AclEntryFlag.FILE_INHERIT));

        // Los alias de directorio --`LIST_DIRECTORY`, `ADD_FILE`, `ADD_SUBDIRECTORY`-- son la
        // **misma** constante que `READ_DATA`, `WRITE_DATA` y `APPEND_DATA`, no constantes nuevas:
        // por eso `values()` devuelve 14 y no 17.
        //
        // La identidad **no se comprueba aca** porque hoy no se cumple sobre esta VM, y no por
        // culpa de la biblioteca: el `javac` propio emite los inicializadores de campo `static` de
        // un enum **antes** de construir las constantes, asi que un alias queda en `null`. El
        // fuente es correcto --el `javac` del JDK lo compila bien y esta misma prueba pasa alla--
        // y el bug esta reportado aparte. Cuando se arregle, se agregan las tres lineas.
        ok(AclEntryPermission.values().length == 14);

        // Copiar cambiando una sola cosa.
        AclEntry negada = AclEntry.newBuilder(e).setType(AclEntryType.DENY).build();
        ok(negada.type() == AclEntryType.DENY);
        ok(negada.permissions().size() == 2);
        ok(!negada.equals(e));

        // Igualdad por valor, no por identidad.
        AclEntry otra = AclEntry.newBuilder(e).build();
        ok(otra.equals(e));
        ok(otra.hashCode() == e.hashCode());

        // El conjunto devuelto es una copia: tocarlo no toca la entrada.
        Set<AclEntryPermission> copia = e.permissions();
        copia.clear();
        ok(e.permissions().size() == 2);

        // Sin principal no se puede construir.
        boolean tiro = false;
        try {
            AclEntry.newBuilder().setType(AclEntryType.ALLOW).build();
        } catch (IllegalStateException x) {
            tiro = true;
        }
        ok(tiro);
    }

    // ---------------------------------------------------------------------------------------
    // Rutas y excepciones, sin tocar el disco.
    // ---------------------------------------------------------------------------------------
    static void rutas() {
        String sep = FileSystems.getDefault().getSeparator();
        ok(sep.length() == 1);

        Path p = Paths.get("a", "b", "c");
        ok(p.getNameCount() == 3);
        ok(p.getFileName().toString().equals("c"));
        ok(p.equals(Path.of("a", "b", "c")));
        ok(!p.isAbsolute());
        ok(p.toAbsolutePath().isAbsolute());

        // El mensaje de InvalidPathException junta razon, indice y entrada.
        InvalidPathException ipe = new InvalidPathException("a?b", "Illegal char", 1);
        ok(ipe.getInput().equals("a?b"));
        ok(ipe.getReason().equals("Illegal char"));
        ok(ipe.getIndex() == 1);
        ok("Illegal char at index 1: a?b".equals(ipe.getMessage()));
        ok("Illegal char: a?b".equals(new InvalidPathException("a?b", "Illegal char").getMessage()));

        // Y el de FileSystemException, con y sin segundo archivo.
        ok("x: no anda".equals(new NoSuchFileException("x", null, "no anda").getMessage()));
        ok("x -> y: no anda".equals(new NoSuchFileException("x", "y", "no anda").getMessage()));
        ok("x".equals(new NoSuchFileException("x").getMessage()));

        ok(StandardOpenOption.values().length == 10);
        ok(StandardCopyOption.values().length == 3);
    }

    // ---------------------------------------------------------------------------------------
    // El disco.
    // ---------------------------------------------------------------------------------------
    static void archivos() throws Exception {
        Path f = Path.of(BASE + "a.txt");
        Files.deleteIfExists(f);
        ok(Files.notExists(f));
        ok(!Files.exists(f));

        // Crear.
        ok(Files.createFile(f).equals(f));
        ok(Files.exists(f));
        ok(Files.isRegularFile(f));
        ok(!Files.isDirectory(f));
        ok(Files.size(f) == 0L);
        ok(Files.isReadable(f));
        ok(Files.isWritable(f));

        // Crearlo de nuevo falla.
        boolean tiro = false;
        try {
            Files.createFile(f);
        } catch (FileAlreadyExistsException e) {
            tiro = true;
        }
        ok(tiro);

        // Escribir y leer bytes.
        byte[] datos = "hola".getBytes(StandardCharsets.UTF_8);
        ok(Files.write(f, datos).equals(f));
        ok(Files.size(f) == 4L);
        byte[] leidos = Files.readAllBytes(f);
        ok(leidos.length == 4);
        ok(leidos[0] == (byte) 'h' && leidos[3] == (byte) 'a');

        // Texto.
        Files.writeString(f, "hola mundo");
        ok("hola mundo".equals(Files.readString(f)));
        ok(Files.size(f) == 10L);

        // Anexar.
        Files.write(f, "!!".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        ok("hola mundo!!".equals(Files.readString(f)));

        // Lineas.
        List<String> lineas = new ArrayList<String>();
        lineas.add("uno");
        lineas.add("dos");
        Files.write(f, lineas);
        List<String> vuelta = Files.readAllLines(f);
        ok(vuelta.size() == 2);
        ok("uno".equals(vuelta.get(0)));
        ok("dos".equals(vuelta.get(1)));
        ok(Files.lines(f).count() == 2L);

        // Una linea sin salto final tambien cuenta.
        Files.writeString(f, "a\nb");
        ok(Files.readAllLines(f).size() == 2);
        Files.writeString(f, "a\n\nb\n");
        List<String> conVacia = Files.readAllLines(f);
        ok(conVacia.size() == 3);
        ok(conVacia.get(1).isEmpty());

        // Streams.
        Files.writeString(f, "streams");
        InputStream in = Files.newInputStream(f);
        ok(in.read() == (int) 's');
        in.close();
        OutputStream out = Files.newOutputStream(f);
        out.write("porStream".getBytes(StandardCharsets.UTF_8));
        out.close();
        ok("porStream".equals(Files.readString(f)));

        // El lector con buffer.
        Files.writeString(f, "linea1\nlinea2\n");
        java.io.BufferedReader br = Files.newBufferedReader(f);
        ok("linea1".equals(br.readLine()));
        ok("linea2".equals(br.readLine()));
        ok(br.readLine() == null);
        br.close();

        // Copiar a un OutputStream.
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ok(Files.copy(f, bao) == 14L);
        ok(bao.toByteArray().length == 14);

        // Copiar y mover archivos.
        Path g = Path.of(BASE + "b.txt");
        Files.deleteIfExists(g);
        Files.writeString(f, "contenido");
        ok(Files.copy(f, g).equals(g));
        ok("contenido".equals(Files.readString(g)));

        // Copiar sobre algo que ya esta falla, salvo con REPLACE_EXISTING.
        tiro = false;
        try {
            Files.copy(f, g);
        } catch (FileAlreadyExistsException e) {
            tiro = true;
        }
        ok(tiro);
        Files.writeString(f, "nuevo");
        Files.copy(f, g, StandardCopyOption.REPLACE_EXISTING);
        ok("nuevo".equals(Files.readString(g)));

        // mismatch.
        Files.writeString(f, "nuevo");
        ok(Files.mismatch(f, g) == -1L);
        Files.writeString(g, "nuevx");
        ok(Files.mismatch(f, g) == 4L);
        Files.writeString(g, "nuev");
        ok(Files.mismatch(f, g) == 4L);

        // Mover: el origen deja de estar.
        Path h = Path.of(BASE + "c.txt");
        Files.deleteIfExists(h);
        Files.writeString(f, "movido");
        Files.move(f, h);
        ok(Files.notExists(f));
        ok("movido".equals(Files.readString(h)));

        // Copiar desde un InputStream.
        InputStream desde = Files.newInputStream(h);
        Files.deleteIfExists(f);
        ok(Files.copy(desde, f) == 6L);
        desde.close();
        ok("movido".equals(Files.readString(f)));

        // Borrar de verdad.
        ok(Files.deleteIfExists(f));
        ok(!Files.deleteIfExists(f));
        Files.deleteIfExists(g);
        Files.deleteIfExists(h);
        ok(Files.notExists(f));

        // Borrar lo que no esta falla.
        tiro = false;
        try {
            Files.delete(f);
        } catch (NoSuchFileException e) {
            tiro = true;
        }
        ok(tiro);

        // Leer lo que no esta, tambien.
        tiro = false;
        try {
            Files.readAllBytes(f);
        } catch (NoSuchFileException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void directorios() throws Exception {
        Path d = Path.of(BASE + "dir");
        Path hondo = d.resolve("x").resolve("y");
        // De abajo hacia arriba, porque solo se borran directorios vacios.
        Files.deleteIfExists(hondo);
        Files.deleteIfExists(d.resolve("x"));
        Files.deleteIfExists(d);

        ok(Files.createDirectory(d).equals(d));
        ok(Files.isDirectory(d));
        ok(!Files.isRegularFile(d));

        // Crearlo de nuevo falla...
        boolean tiro = false;
        try {
            Files.createDirectory(d);
        } catch (FileAlreadyExistsException e) {
            tiro = true;
        }
        ok(tiro);
        // ...pero createDirectories no: es idempotente, y es la unica diferencia entre las dos.
        ok(Files.createDirectories(d).equals(d));

        // Y crea los padres que falten.
        ok(Files.createDirectories(hondo).equals(hondo));
        ok(Files.isDirectory(hondo));
        ok(Files.isDirectory(d.resolve("x")));

        // Un directorio con cosas adentro no se borra.
        tiro = false;
        try {
            Files.delete(d);
        } catch (java.nio.file.DirectoryNotEmptyException e) {
            tiro = true;
        }
        ok(tiro);

        Files.delete(hondo);
        Files.delete(d.resolve("x"));
        Files.delete(d);
        ok(Files.notExists(d));
    }

    static void temporales() throws Exception {
        Path t = Files.createTempFile(BASE, ".tmp");
        ok(Files.exists(t));
        ok(Files.isRegularFile(t));
        ok(Files.size(t) == 0L);
        ok(t.getFileName().toString().startsWith(BASE));
        ok(t.getFileName().toString().endsWith(".tmp"));
        Files.delete(t);

        Path td = Files.createTempDirectory(BASE);
        ok(Files.isDirectory(td));
        Files.delete(td);

        // Y en un directorio elegido.
        Path base = Path.of(BASE + "tdir");
        Files.createDirectories(base);
        Path dentro = Files.createTempFile(base, "p", ".x");
        ok(dentro.getParent().getFileName().toString().equals(BASE + "tdir"));
        Files.delete(dentro);
        Files.delete(base);
    }

    // ---------------------------------------------------------------------------------------
    // newByteChannel: que delegue de verdad en FileChannel y que acepte las opciones que ahi si
    // se cumplen. Va al final para no correr los indices de las comprobaciones que ya estaban.
    // ---------------------------------------------------------------------------------------
    static void canales() throws Exception {
        Path p = Path.of(BASE + "chan.bin");
        Files.deleteIfExists(p);

        // Se escribe por el canal y se lee por otro camino: si lo que quedo en el disco es lo que
        // se escribio, la delegacion esta bien hecha y no hay una copia en memoria de por medio.
        SeekableByteChannel ch = Files.newByteChannel(p, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        ok(ch.write(ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5})) == 5);
        ok(ch.position() == 5L);
        ok(ch.size() == 5L);
        ch.close();
        ok(!ch.isOpen());
        byte[] enDisco = Files.readAllBytes(p);
        ok(enDisco.length == 5 && enDisco[0] == 1 && enDisco[4] == 5);

        // Sin opciones se abre para leer, y la posicion se puede mover a mano.
        ch = Files.newByteChannel(p);
        ch.position(3L);
        ByteBuffer b = ByteBuffer.allocate(2);
        ok(ch.read(b) == 2);
        ok(b.array()[0] == 4 && b.array()[1] == 5);
        ok(ch.position() == 5L);
        // Pasado el final es -1, no cero: cero seria "no entro nada ahora", que es otra cosa.
        ok(ch.read(ByteBuffer.allocate(2)) == -1);
        ch.close();

        // truncate llega al disco, no solo a la vista del canal.
        ch = Files.newByteChannel(p, StandardOpenOption.WRITE);
        ch.truncate(2L);
        ok(ch.size() == 2L);
        ch.close();
        ok(Files.size(p) == 2L);

        // La sobrecarga con conjunto, que es la que el JDK toma como principal.
        Set<StandardOpenOption> opciones = new HashSet<>();
        opciones.add(StandardOpenOption.WRITE);
        opciones.add(StandardOpenOption.TRUNCATE_EXISTING);
        ch = Files.newByteChannel(p, opciones);
        ok(ch.size() == 0L);
        ok(ch.write(ByteBuffer.wrap(new byte[] {9})) == 1);
        ch.close();
        ok(Files.size(p) == 1L);

        // SYNC y DSYNC: las mismas que `newOutputStream` rechaza. Aca se aceptan porque el canal de
        // abajo escribe al disco en cada `write`, o sea que ya las cumplia sin que nadie las pida.
        ch = Files.newByteChannel(p, StandardOpenOption.WRITE, StandardOpenOption.SYNC,
                StandardOpenOption.DSYNC);
        ok(ch.write(ByteBuffer.wrap(new byte[] {7, 7})) == 2);
        ch.close();
        ok(Files.size(p) == 2L);

        // DELETE_ON_CLOSE: existe mientras el canal esta abierto y no despues.
        Path d = Path.of(BASE + "doc.bin");
        Files.deleteIfExists(d);
        ch = Files.newByteChannel(d, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.DELETE_ON_CLOSE);
        ok(ch.write(ByteBuffer.wrap(new byte[] {1})) == 1);
        ok(Files.exists(d));
        ch.close();
        ok(Files.notExists(d));

        // Sin CREATE, un archivo que no esta es un error y no un archivo vacio recien nacido.
        Path falta = Path.of(BASE + "nohay.bin");
        Files.deleteIfExists(falta);
        boolean tiro = false;
        try {
            Files.newByteChannel(falta);
        } catch (NoSuchFileException e) {
            tiro = true;
        }
        ok(tiro);

        tiro = false;
        try {
            Files.newByteChannel(p, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException e) {
            tiro = true;
        }
        ok(tiro);

        // Las opciones que se contradicen se detectan antes de tocar el disco.
        tiro = false;
        try {
            Files.newByteChannel(p, StandardOpenOption.READ, StandardOpenOption.APPEND);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);

        Files.delete(p);
    }

    public static int run() throws Exception {
        tiempos();
        permisos();
        acl();
        rutas();
        archivos();
        directorios();
        temporales();
        canales();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
