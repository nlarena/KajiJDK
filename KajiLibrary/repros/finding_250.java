// Repro de #250 - el desugar de un enum llama a Enum.valueOf(Class, String), que no existe aca.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_250.java
//   -> error: no se encuentra el metodo: valueOf   (simbolo: valueOf(Class, String))
//
// Cualquier enum sirve de repro: no hace falta usarlo ni llamar a valueOf en la fuente. El
// metodo sintetizado es el que trae la referencia.
//
// KajiLibrary NO declara Enum.valueOf(Class, String) a proposito, y su Enum.java lo dice:
// el real reflexiona sobre el $VALUES de la clase, y nuestro compilador venia sintetizando
// un valueOf(String) autocontenido dentro de cada enum, que no necesita reflexion.
public class finding_250 {

    enum Color { ROJO, VERDE }

    public static int cuantos() {
        return Color.values().length;
    }
}
