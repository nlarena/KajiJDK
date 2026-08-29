// Repro de #208 - un tipo que el generador no resuelve se EMITIA igual, y con dos mentiras
// distintas en el mismo class file.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_208.java
//   javap -p -v finding_208.class
//
// Lo que salia antes (compilaba, exit 0):
//
//   void f(NoExiste, java.lang.Class<?>)
//     descriptor: (Ljava/lang/Object;Ljava/lang/Class;)V     <- degradado a Object
//     Signature:  (LNoExiste;Ljava/lang/Class<*>;)V          <- nombre crudo, no es ninguna clase
//
// Los dos artefactos describen el mismo parametro y NO coinciden, porque los calculan dos caminos
// distintos: el descriptor degrada a `Object` cuando el nombre no resuelve, y la firma escribe el
// nombre tal como se lo escribio, con los puntos vueltos barras. Ninguno de los dos es el tipo.
//
// El disparador que decia el reporte original -"otro parametro de la misma firma lleva argumentos
// de tipo"- era el del SINTOMA: el `Signature` solo se emite si algo en la firma usa genericos, asi
// que sin el `Class<?>` el nombre roto no se llegaba a ver. La causa no depende de eso.
//
// Por que llega sin resolver hasta el generador: un `import` de tipo unico se da por bueno en la
// fase semantica -sin classpath no hay forma de descartarlo, la misma indulgencia que con
// `import *`-. Esa indulgencia esta bien; lo que estaba mal era que el generador, al no resolverlo,
// inventara un artefacto plausible en vez de fallar.
//
// Esperado ahora: `error: el generador de bytecode no puede resolver el tipo `NoExiste``.
import p.NoExiste;

public class finding_208 {

    void f(NoExiste x, Class<?> t) { }

    NoExiste campo;
}
