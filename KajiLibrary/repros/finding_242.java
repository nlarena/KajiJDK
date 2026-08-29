// Repro de #242 - un tipo MIEMBRO de una interfaz no recibe el `public` implicito
// (JLS 9.5). Hermano de #116, que cubre solo los metodos static.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_242.java
//   javap -p -cp KajiLibrary\repros "finding_242$In"
//
//   interface finding_242$In {        <-- package-private; el JDK da `public interface`
//
// Escribir `public interface In { }` lo arregla. Sin eso, un tipo anidado de una
// interfaz queda inusable desde otro paquete: es lo que obligo a declararlos
// explicitamente en los 8 anidados de javax.lang.model.element.ModuleElement.
//
// De paso se ve #235: el SourceFile del anidado dice "In.java" en vez del archivo real.
public interface finding_242 {
    interface In { }
}
