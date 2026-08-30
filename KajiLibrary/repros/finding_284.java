// Repro de #284 - una clase concreta que NO implementa un metodo abstracto heredado de una
// superclase del CLASSPATH compila igual, en silencio.
//
// Hace falta que la superclase este en otra unidad de compilacion. Con las dos clases en el
// mismo archivo el chequeo si dispara, que es lo que lo hacia invisible.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_284.java
//   javac: escrito ... finding_284.class            <-- lo acepta
//
// El JDK 25 sobre el mismo par de archivos:
//
//   error: AbsSub is not abstract and does not override abstract method f() in AbsBase
//
// Para verlo hace falta compilar por separado, porque el defecto es justamente ese:
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_284_base.java
//   bin\javac.exe --emit -cp KajiLibrary -cp KajiLibrary\repros KajiLibrary\repros\finding_284.java
//
// Lo que SI se detecta, y acota el hallazgo:
//   - la misma situacion con las dos clases en el MISMO archivo -> error correcto.
//   - un metodo abstracto de una INTERFAZ del classpath -> error correcto. Es el diagnostico
//     que da hoy `jdk/internal/apt/SymElement.java` ("no es abstracta y no implementa `asType`
//     de `TypeElement`"), el unico fallo de compilacion de toda la biblioteca.
//
// O sea: el chequeo existe y anda, pero no mira los metodos abstractos que llegan por
// **herencia de clase** desde el classpath. Solo los de la misma unidad y los de interfaz.
//
// Como salio: colgando `HashMap` de `AbstractMap` para que heredara `values()`. `AbstractMap`
// declara `entrySet()` abstracto, `HashMap` no lo implementa, y aun asi compilaron las dos, y
// tambien compilo un `new HashMap<>().entrySet()`. En Java real eso no llega a ejecutarse: no
// compila. Aca compila y quedaria para reventar en runtime.
public class finding_284 extends finding_284_base {
    // f() no esta. Deberia ser un error de compilacion.
}
