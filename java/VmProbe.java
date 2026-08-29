// Probes de los hallazgos "VM / interprete" del indice de COMPILER_FINDINGS.
// Se corren con `run-headless`, cuyo bootclasspath es KajiLibrary + boot — que es
// exactamente la configuracion de la que hablan los hallazgos.
// Cada metodo devuelve el valor correcto segun Java; cualquier otra cosa (o un panic)
// es el hallazgo vivo.
public class VmProbe {
    // #216 — ConstantValue no aplicado en la preparacion (JVMS §5.4.2). Correcto: 7.
    public static final int K = 7;
    public static int p216() { return K; }

    // #225 — invokeinterface con receptor String. Correcto: 3.
    public static int p225() {
        CharSequence cs = "abc";
        return cs.length();
    }

    // #226 — falta el nativo String.valueOf([CII): la concatenacion no produce String.
    // Correcto: "a1".length() == 2.
    public static int p226() {
        int x = 1;
        String s = "a" + x;
        return s.length();
    }

    // #229 — constantes String no-ASCII leidas como bytes UTF-8 crudos, un char por byte.
    // "ñ" es UN char en Java. Correcto: 1. Si lee bytes crudos: 2.
    public static int p229() {
        return "ñ".length();
    }

    // #227 — Throwable de KajiLibrary sin `backtrace`: toda excepcion no capturada panica.
    // Correcto: la VM imprime el reporte estilo JDK y termina; no panica.
    public static int p227() {
        throw new RuntimeException("boom");
    }

    // Control: si esto falla, el problema es el arnes, no el hallazgo. Correcto: 42.
    public static int control() { return 42; }
}
