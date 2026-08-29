package javax.tools;

// KajiLibrary's javax.tools.JavaFileObject — a FileObject that a Java tool understands,
// i.e. one that knows whether it holds source, bytecode, documentation or something else.
// Everything a compiler reads or writes travels through this interface.
//
// OMITIDOS (salida (a), omitir el miembro), por tipos que no existen en KajiLibrary:
//   - `javax.lang.model.element.NestingKind getNestingKind()` — no hay NestingKind
//     (javax/lang/model/element solo tiene Element, ElementKind, Name, TypeElement).
//   - `javax.lang.model.element.Modifier getAccessLevel()` — no hay Modifier ahi tampoco.
// Ambos volverian con el tipo cambiado a Object; preferimos la ausencia.
//
// --- javax.tools.SimpleJavaFileObject: OMITIDA ENTERA (salida (b)) ---------------------------
//
// SimpleJavaFileObject es la implementacion canonica de esta interfaz, y no esta en el paquete
// a proposito. No es un descuido ni falta de tiempo: no queda nada de ella que se pueda
// declarar sin mentir.
//
//   - Su estado son dos campos, `protected final URI uri` y `protected final Kind kind`.
//     `java.net.URI` no existe en KajiLibrary (java.net esta en cero clases) y `Kind` es un
//     tipo anidado de OTRA unidad de compilacion, que el javac congelado no puede nombrar.
//   - Su UNICO constructor es `protected SimpleJavaFileObject(URI, Kind)`. Al caerse, javac
//     sintetizaria un `public SimpleJavaFileObject()` — un miembro publico que la API real NO
//     tiene. Eso es precisamente una declaracion falsa, no una ausencia.
//   - `implements JavaFileObject` tampoco sobrevive: sin poder declarar `getKind` ni
//     `isNameCompatible`, el javac rechaza la clase concreta por no implementar la interfaz.
//
// Lo que quedaria es una clase sin relacion con JavaFileObject, sin su estado, sin su
// constructor y con uno inventado. El nombre seria lo unico correcto. Se omite.
public interface JavaFileObject extends FileObject {

    // El "que clase de archivo es esto", con la extension canonica que le corresponde.
    public enum Kind {
        SOURCE(".java"),
        CLASS(".class"),
        HTML(".html"),
        OTHER("");

        public final String extension;

        private Kind(String extension) {
            this.extension = extension;
        }
    }

    Kind getKind();

    boolean isNameCompatible(String simpleName, Kind kind);
}
