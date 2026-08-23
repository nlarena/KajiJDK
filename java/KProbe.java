// Repro de COMPILER_FINDINGS #216: `ConstantValue` no aplicado en la preparacion (JVMS §5.4.2).
// Se compila con NUESTRO javac a proposito: el javac real pliega la constante en el uso
// (`bipush 7`) y nunca emite el `getstatic` que expone el defecto.
// Cubre los cuatro anchos + String, para que un error de ancho no pase inadvertido.
public class KProbe {
    public static final int K = 7;
    public static final long L = 5000000000L;
    public static final double D = 2.5;
    public static final String S = "abc";
    public static final boolean B = true;

    public static int run() { return K; }
    public static int runLong() { return L == 5000000000L ? 1 : 0; }
    public static int runDouble() { return D == 2.5 ? 1 : 0; }
    public static int runString() { return S.length(); }
    public static int runBool() { return B ? 1 : 0; }
}
