// Repro de #216 - VM: el atributo ConstantValue no se aplica en la preparacion de la
// clase (JVMS 5.4.2), asi que todo static final primitivo lee 0.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_216.java
//   bin\jvm.exe -v KajiLibrary\repros\finding_216.class      (muestra ConstantValue: int 7)
//   bin\run-headless.exe KajiLibrary\repros\finding_216.class run
//
// Devuelve Some(Int(0)); se esperaba 7. El class file esta BIEN - el defecto es del
// runtime, no del compilador.
//
// Por que se paso por alto: docs/roadmap.md lo tenia como "no testeable con javac",
// porque el javac real inlinea las constantes y nunca emite un getstatic que lo observe.
// Nuestro javac no las pliega (#112), asi que si lo emite y el hueco queda a la vista.
// Dos defectos tolerados por separado que se componen en una respuesta incorrecta
// silenciosa.
//
// Impacto medido en codigo ya publicado de KajiLibrary: Modifier.isPublic(1) da false.
public class finding_216 {
    public static final int K = 7;
    public static int run() { return K; }
}
