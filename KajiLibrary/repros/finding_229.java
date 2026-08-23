// Repro de #229 - VM: las constantes String no-ASCII se leen como bytes UTF-8 crudos,
// un char por byte.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_229.java
//   bin\run-headless.exe KajiLibrary\repros\finding_229.class run
//
// Devuelve 2; deberia devolver 1. El .class esta BIEN (el pool lleva 01 00 02 CC 81,
// que es U+0301 en UTF-8 modificado): es la LECTURA de la VM la que no decodifica.
//
// Los literales de char si funcionan (el mismo escape en un char da 769), asi que el
// defecto es especifico de la constante String.
public class finding_229 {
    public static int run() { return "\u0301".length(); }
}
