package javax.tools;

// KajiLibrary's javax.tools.ForwardingJavaFileObject<F> — the JavaFileObject rung of the
// forwarding ladder: it inherits the nine FileObject delegations from ForwardingFileObject
// and adds the two that only a JavaFileObject has.
//
// OMITIDOS — salida (a), y por el mismo defecto en los dos casos: el javac congelado no puede
// nombrar un tipo anidado de otra unidad de compilacion, y `JavaFileObject.Kind` vive en
// JavaFileObject.java. `Outer.Kind` da error duro; `import javax.tools.JavaFileObject.Kind`
// compila pero degrada el tipo a Object en silencio, que es exactamente la firma falsa que no
// queremos. Caen entonces los DOS unicos metodos propios de la clase:
//   - `JavaFileObject.Kind getKind()`
//   - `boolean isNameCompatible(String, JavaFileObject.Kind)`
// y tambien los dos que ya faltaban por tipos ausentes (`getNestingKind` -> NestingKind,
// `getAccessLevel` -> Modifier, ninguno de los dos en javax.lang.model.element).
//
// Queda la clase igual, y no vacia: aporta su identidad de tipo, su parametro acotado
// La nota anterior decia que `getNestingKind`/`getAccessLevel` esperaban a que el compilador
// resolviera tipos anidados de otra unidad, y que ese dia se agregaban sin tocar nada mas. Llego, y
// fue asi.
public class ForwardingJavaFileObject<F extends JavaFileObject> extends ForwardingFileObject<F> implements JavaFileObject {

    protected ForwardingJavaFileObject(F fileObject) {
        super(fileObject);
    }

    public JavaFileObject.Kind getKind() {
        return this.fileObject.getKind();
    }

    public boolean isNameCompatible(String simpleName, JavaFileObject.Kind kind) {
        return this.fileObject.isNameCompatible(simpleName, kind);
    }

    public javax.lang.model.element.NestingKind getNestingKind() {
        return this.fileObject.getNestingKind();
    }

    public javax.lang.model.element.Modifier getAccessLevel() {
        return this.fileObject.getAccessLevel();
    }
}
