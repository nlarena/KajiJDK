package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.ContinuationScope -- the name a group of continuations shares.
 *
 * <p>A minimal stub: KajiJDK has no continuations (Project Loom), and this type exists only so the
 * signatures that mention it ({@code StackWalker}/{@code LiveStackFrame}) can name it.
 */
public class ContinuationScope {

    public final String name;

    public ContinuationScope(String name) {
        this.name = name;
    }

    /**
     * Para una subclase que sea su **propio** alcance.
     *
     * <p>El JDK lo usa asi: `ContinuationScope` se extiende y la subclase **es** el nombre, con lo
     * cual el nombre sale de `getClass().getName()` en vez de pasarse. Es `protected` porque solo
     * tiene sentido desde adentro de una subclase -- un alcance sin nombre creado desde afuera no
     * se podria distinguir de otro.
     */
    protected ContinuationScope() {
        this.name = null;
    }

    /** El nombre; para una subclase que uso el constructor sin argumentos, el de su clase. */
    public String getName() {
        if (this.name == null) {
            return this.getClass().getName();
        }
        return this.name;
    }

    public String toString() {
        return this.getName();
    }
}
