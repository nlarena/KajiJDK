// Repro de #235 - el SourceFile decia el nombre de CADA clase, no el de la unidad.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_235.java
//   javap -v finding_235.class finding_235$Anidada.class finding_235_secundaria.class | findstr SourceFile
//
// Antes:
//   finding_235.class             SourceFile: "finding_235.java"          <- por casualidad bien
//   finding_235$Anidada.class     SourceFile: "Anidada.java"              <- no existe
//   finding_235_secundaria.class  SourceFile: "finding_235_secundaria.java"  <- tampoco
//
// El atributo (JVMS 4.7.10) es de la UNIDAD DE COMPILACION, no de la clase: las secundarias y
// las anidadas comparten archivo con la principal. Un depurador que quiera abrir la fuente por
// ese nombre no la encuentra, y es justo para eso que existe el atributo.
//
// El compilador no recibe la ruta del archivo, asi que el nombre se deduce del tipo que le da
// nombre a la unidad: el publico de nivel superior si lo hay -que segun JLS 7.6 obliga a que el
// archivo se llame como el-, y si no el primero declarado.
//
// Esperado ahora: las tres dicen "finding_235.java".
public class finding_235 {
    static class Anidada { int f() { return 1; } }
}

class finding_235_secundaria { int g() { return 2; } }
