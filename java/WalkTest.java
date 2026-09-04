import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/** Enumerar un directorio: newDirectoryStream, list, walk, find, walkFileTree. */
public class WalkTest {

    static Path raiz;

    // a/
    //   f1.txt  f2.java
    //   b/  f3.txt
    //   c/  (vacio)
    private static void armar() throws IOException {
        raiz = Files.createTempDirectory("kajiwalk");
        Files.write(raiz.resolve("f1.txt"), new byte[] {1});
        Files.write(raiz.resolve("f2.java"), new byte[] {2});
        Path b = Files.createDirectory(raiz.resolve("b"));
        Files.write(b.resolve("f3.txt"), new byte[] {3});
        Files.createDirectory(raiz.resolve("c"));
    }

    private static List<String> nombres(java.util.stream.Stream<Path> s) {
        List<String> out = new ArrayList<String>();
        java.util.Iterator<Path> it = s.iterator();
        while (it.hasNext()) {
            Path p = it.next();
            Path rel = raiz.relativize(p);
            out.add(rel.toString().replace(java.io.File.separatorChar, '/'));
        }
        java.util.Collections.sort(out);
        return out;
    }

    public static int run() throws IOException {
        int i = 0;
        armar();

        // -- newDirectoryStream: las cuatro entradas directas
        List<String> directas = new ArrayList<String>();
        DirectoryStream<Path> ds = Files.newDirectoryStream(raiz);
        java.util.Iterator<Path> it = ds.iterator();
        while (it.hasNext()) { directas.add(it.next().getFileName().toString()); }
        ds.close();
        java.util.Collections.sort(directas);
        if (directas.size() != 4) { return i; } i++;
        if (!directas.toString().equals("[b, c, f1.txt, f2.java]")) { return i; } i++;

        // -- el iterador es uno solo
        DirectoryStream<Path> ds2 = Files.newDirectoryStream(raiz);
        ds2.iterator();
        boolean dosVeces = false;
        try { ds2.iterator(); } catch (IllegalStateException e) { dosVeces = true; }
        ds2.close();
        if (!dosVeces) { return i; } i++;

        // -- glob
        List<String> txt = new ArrayList<String>();
        DirectoryStream<Path> g = Files.newDirectoryStream(raiz, "*.txt");
        java.util.Iterator<Path> gi = g.iterator();
        while (gi.hasNext()) { txt.add(gi.next().getFileName().toString()); }
        g.close();
        if (txt.size() != 1 || !txt.get(0).equals("f1.txt")) { return i; } i++;

        // -- filtro propio: solo directorios
        List<String> dirs = new ArrayList<String>();
        DirectoryStream<Path> f = Files.newDirectoryStream(raiz, new DirectoryStream.Filter<Path>() {
            public boolean accept(Path entry) throws IOException { return Files.isDirectory(entry); }
        });
        java.util.Iterator<Path> fi = f.iterator();
        while (fi.hasNext()) { dirs.add(fi.next().getFileName().toString()); }
        f.close();
        java.util.Collections.sort(dirs);
        if (!dirs.toString().equals("[b, c]")) { return i; } i++;

        // -- list
        if (nombres(Files.list(raiz)).size() != 4) { return i; } i++;

        // -- walk: la raiz mas las cuatro directas mas f3
        List<String> todo = nombres(Files.walk(raiz));
        if (todo.size() != 6) { return i; } i++;
        if (!todo.contains("b/f3.txt")) { return i; } i++;
        if (!todo.contains("")) { return i; } i++;   // la raiz relativizada contra si misma

        // -- walk con profundidad 1: la raiz y las cuatro directas, sin bajar a b
        List<String> d1 = nombres(Files.walk(raiz, 1));
        if (d1.size() != 5) { return i; } i++;
        if (d1.contains("b/f3.txt")) { return i; } i++;

        // -- walk con profundidad 0: solo la raiz
        if (nombres(Files.walk(raiz, 0)).size() != 1) { return i; } i++;

        // -- find: los .txt
        List<String> hallados = nombres(Files.find(raiz, Integer.MAX_VALUE,
                new java.util.function.BiPredicate<Path, BasicFileAttributes>() {
                    public boolean test(Path p, BasicFileAttributes a) {
                        return a.isRegularFile() && p.toString().endsWith(".txt");
                    }
                }));
        if (hallados.size() != 2) { return i; } i++;
        if (!hallados.contains("f1.txt") || !hallados.contains("b/f3.txt")) { return i; } i++;

        // -- walkFileTree: el orden y los conteos
        final int[] cuenta = new int[3];
        Files.walkFileTree(raiz, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                cuenta[0] = cuenta[0] + 1;
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult visitFile(Path p, BasicFileAttributes a) {
                cuenta[1] = cuenta[1] + 1;
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult postVisitDirectory(Path d, IOException e) {
                cuenta[2] = cuenta[2] + 1;
                return FileVisitResult.CONTINUE;
            }
        });
        if (cuenta[0] != 3) { return i; } i++;   // raiz, b, c
        if (cuenta[1] != 3) { return i; } i++;   // f1, f2, f3
        if (cuenta[2] != 3) { return i; } i++;

        // -- SKIP_SUBTREE: no baja a b
        final int[] vistos = new int[1];
        Files.walkFileTree(raiz, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                if (d.getFileName() != null && d.getFileName().toString().equals("b")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult visitFile(Path p, BasicFileAttributes a) {
                vistos[0] = vistos[0] + 1;
                return FileVisitResult.CONTINUE;
            }
        });
        if (vistos[0] != 2) { return i; } i++;

        // -- TERMINATE corta todo
        final int[] antes = new int[1];
        Files.walkFileTree(raiz, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path p, BasicFileAttributes a) {
                antes[0] = antes[0] + 1;
                return FileVisitResult.TERMINATE;
            }
        });
        if (antes[0] != 1) { return i; } i++;

        // -- un directorio que no existe
        boolean noHay = false;
        try { Files.newDirectoryStream(raiz.resolve("nada")); }
        catch (java.nio.file.NoSuchFileException e) { noHay = true; }
        if (!noHay) { return i; } i++;

        // -- un archivo no es un directorio
        boolean noDir = false;
        try { Files.newDirectoryStream(raiz.resolve("f1.txt")); }
        catch (java.nio.file.NotDirectoryException e) { noDir = true; }
        if (!noDir) { return i; } i++;

        // -- isSameFile: la identidad no es la cadena
        Path f1 = raiz.resolve("f1.txt");
        if (!Files.isSameFile(f1, f1)) { return i; } i++;
        if (!Files.isSameFile(f1, raiz.resolve("b").resolve("..").resolve("f1.txt"))) { return i; } i++;
        if (Files.isSameFile(f1, raiz.resolve("f2.java"))) { return i; } i++;
        boolean faltante = false;
        try { Files.isSameFile(raiz.resolve("nada"), f1); }
        catch (java.nio.file.NoSuchFileException e) { faltante = true; }
        if (!faltante) { return i; } i++;
        // Dos caminos IGUALES son el mismo archivo aunque no existan: no se mira el disco.
        Path fantasma = raiz.resolve("no-existe");
        if (!Files.isSameFile(fantasma, fantasma)) { return i; } i++;

        // -- setLastModifiedTime
        java.nio.file.attribute.FileTime cuando =
                java.nio.file.attribute.FileTime.fromMillis(1000000000000L);
        Path vuelto = Files.setLastModifiedTime(f1, cuando);
        if (vuelto != f1) { return i; } i++;
        if (Files.getLastModifiedTime(f1).toMillis() != 1000000000000L) { return i; } i++;
        boolean noHayArchivo = false;
        try { Files.setLastModifiedTime(raiz.resolve("nada"), cuando); }
        catch (java.nio.file.NoSuchFileException e) { noHayArchivo = true; }
        if (!noHayArchivo) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) throws IOException { System.out.println(run()); }
}
