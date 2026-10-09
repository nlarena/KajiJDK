// Repro de #241 - la BORRADURA de una variable de tipo acotada es Object, no su bound.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_241.java
//   javap -v -p -cp KajiLibrary\repros finding_241
//
//   descriptor: (Ljava/lang/Class;)Ljava/lang/Object;   <-- deberia ser Ljava/lang/Number;
//   Signature:  <N:Ljava/lang/Number;>(...)TN;          <-- este SI esta bien
//
// Contraste con el JDK: javax.lang.model.AnnotatedConstruct.getAnnotation emite
//   (Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
//
// Signature y descriptor son dos caminos distintos y solo uno aplica el bound. Es grave
// porque un override escrito con la borradura correcta tendria OTRO descriptor -> no
// sobreescribe -> AbstractMethodError en runtime.
// Distinto de #212, que es sobre el Signature; aca el Signature esta bien.
public interface finding_241 {
    <N extends Number> N f(Class<N> c);
}
