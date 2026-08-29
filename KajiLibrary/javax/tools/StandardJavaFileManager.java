package javax.tools;

// KajiLibrary's javax.tools.StandardJavaFileManager — the file manager that actually reads
// and writes the host file system, and the one ToolProvider's compiler hands you by default.
// Everything it adds over JavaFileManager is a convenience for turning host file names into
// JavaFileObjects, plus per-location search-path configuration.
//
// AVISO: esta es, por lejos, la clase mas amputada del paquete, y no por el compilador sino
// por la biblioteca. Su razon de ser es traducir entre el sistema de archivos del host y los
// JavaFileObject — y KajiLibrary no tiene `java.io.File` (java.io existe pero sin File) ni
// `java.nio.file.Path` (java.nio existe, java.nio.file no). Se evaluo la salida (b), omitir la
// clase entera; se eligio (a) porque los tres miembros que quedan son reales, exactos, y
// StandardJavaFileManager sigue siendo un tipo nombrable de la API.
//
// OMITIDOS (salida (a), omitir el miembro):
//
//   por `java.io.File` ausente:
//     - `Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File>)`
//     - `Iterable<? extends JavaFileObject> getJavaFileObjects(File...)`
//     - `void setLocation(Location, Iterable<? extends File>)`
//     - `Iterable<? extends File> getLocation(Location)`
//
//   por `java.nio.file.Path` ausente:
//     - `getJavaFileObjectsFromPaths(Collection<? extends Path>)` y su sobrecarga con Iterable
//     - `getJavaFileObjects(Path...)`
//     - `setLocationFromPaths(Location, Collection<? extends Path>)`
//     - `setLocationForModule(Location, String, Collection<? extends Path>)`
//     - `Iterable<? extends Path> getLocationAsPaths(Location)`
//     - `Path asPath(FileObject)`
//     - `void setPathFactory(StandardJavaFileManager.PathFactory)` y el propio anidado
//       `PathFactory` (salida (b) para ese: una fabrica de Path sin Path no significa nada).
//
//   Notar ademas que las cuatro de setLocation/getLocation mencionan `JavaFileManager.Location`,
//   que el javac congelado tampoco puede nombrar desde otra unidad de compilacion: caerian
//   igual aunque File existiera.
public interface StandardJavaFileManager extends JavaFileManager {

    boolean isSameFile(FileObject a, FileObject b);

    Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names);

    Iterable<? extends JavaFileObject> getJavaFileObjects(String... names);
}
