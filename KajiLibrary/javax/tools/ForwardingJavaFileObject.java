package javax.tools;

// KajiLibrary's javax.tools.ForwardingJavaFileObject<F> — the JavaFileObject rung of the
// forwarding ladder: it inherits the nine FileObject delegations from ForwardingFileObject
// and adds the four that only a JavaFileObject has.
//
// All four used to be missing, and for two different reasons. `getKind` and `isNameCompatible` name
// `JavaFileObject.Kind`, a type nested in another compilation unit, which the frozen javac could not
// name: `Outer.Kind` was a hard error and `import javax.tools.JavaFileObject.Kind` compiled but
// silently degraded the type to Object — exactly the false signature to avoid. `getNestingKind` and
// `getAccessLevel` name NestingKind and Modifier, neither of which existed in
// javax.lang.model.element. The earlier note said they were waiting for the compiler to resolve
// nested types from another unit, and that the day it did they would go in without touching anything
// else. That day came, and so it was.
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
