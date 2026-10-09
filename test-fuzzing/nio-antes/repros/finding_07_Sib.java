// Finding #7 (parte a) — clase KajiLibrary-only referenciable entre archivos SOLO con classpath.
// `Sib` no tiene contraparte en el JDK; sin `-cp` el finder (JDK externo + boot/) no puede
// hallarla, y como no está en la unidad de compilación de `finding_07_User.java`, ese archivo
// no la resuelve. El `import` no ayuda: no hay de dónde importar.
//
// Ver finding_07_User.java. Fix: soporte de classpath / shadowing del fuente (--patch-module).
public class Sib {
    public static int v() { return 42; }
}
