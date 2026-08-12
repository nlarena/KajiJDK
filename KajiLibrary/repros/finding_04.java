// Finding #4 — el finder no auto-carga un tipo del classpath por su nombre SIMPLE cuando el
// tipo vive en el mismo paquete (no está en la unidad de compilación ni en la lista core de
// java.lang). `List` está en el classpath de referencia (java/util/List.class) pero no se
// resuelve por nombre simple desde `package java.util`.
//
// Esperado (javac real): resuelve `List` (mismo paquete → visible sin import).
// Sìntoma del bug:       "no se encuentra el símbolo: List".
// Workaround conocido:   agregar `import java.util.List;`.
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_04.java   (desde la raíz del repo)
package java.util;

public class P implements List<Object> {}
