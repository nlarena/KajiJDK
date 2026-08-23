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
// `F extends JavaFileObject`, la cadena `extends ForwardingFileObject<F> implements
// JavaFileObject` con las nueve delegaciones heredadas, y el constructor protegido. El dia
// que el compilador resuelva anidados, los dos metodos se agregan sin tocar nada mas.
public class ForwardingJavaFileObject<F extends JavaFileObject> extends ForwardingFileObject<F> implements JavaFileObject {

    protected ForwardingJavaFileObject(F fileObject) {
        super(fileObject);
    }
}
