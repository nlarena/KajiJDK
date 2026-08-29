// Repro de #210 - un nombre calificado de java.lang degrada EN SILENCIO a Object.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_210.java
//   javap -p -cp KajiLibrary\repros finding_210
//
// Emitido:
//   public java.lang.Object simple(java.lang.Object);     <-- MAL, degradado
//   public java.lang.String control(java.lang.String);    <-- bien
//
// El javap REAL confirma que el .class esta mal, asi que no es cosa del desensamblador.
// Fuera de java.lang el mismo codigo da error duro ("no se encuentra el simbolo:
// java.util.List"), que al menos avisa. Lo peligroso es esta mitad: un override escrito
// con nombre calificado deja de sobreescribir y nadie se entera.
public class finding_210 {
    public java.lang.String simple(java.lang.String s) { return s; }
    public String           control(String s)          { return s; }
}
