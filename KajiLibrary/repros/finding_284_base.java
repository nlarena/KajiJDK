// La superclase de #284: se compila APARTE, para que llegue por classpath y no por la misma
// unidad de compilacion. Ahi esta la diferencia que el chequeo no cubre.
public abstract class finding_284_base {
    public abstract int f();
}
