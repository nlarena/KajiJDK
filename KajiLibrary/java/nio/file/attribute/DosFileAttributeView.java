package java.nio.file.attribute;

import java.io.IOException;

// La vista `"dos"`: los `BasicFileAttributes` mas los cuatro bits de DOS.
//
// **Diferencia con el JDK, y es visible en la firma.** Alla `readAttributes()` devuelve
// `DosFileAttributes` --covariante sobre el de `BasicFileAttributeView`-- y el compilador sintetiza
// el puente que devuelve `BasicFileAttributes`. Aca se declara igual; si el `javac` propio no emite
// ese puente, el que falta es un miembro **sintetico**, no uno de la API.
//
// Sin implementacion en KajiJDK: no hay nativo que lea ni escriba estos bits.
public interface DosFileAttributeView extends BasicFileAttributeView {

    /** Siempre `"dos"`. */
    String name();

    /** Los atributos, leidos de una sola vez. */
    DosFileAttributes readAttributes() throws IOException;

    /** Marca o desmarca el archivo como de solo lectura. */
    void setReadOnly(boolean value) throws IOException;

    /** Marca o desmarca el archivo como oculto. */
    void setHidden(boolean value) throws IOException;

    /** Marca o desmarca el archivo como de sistema. */
    void setSystem(boolean value) throws IOException;

    /** Marca o desmarca el bit de archivado. */
    void setArchive(boolean value) throws IOException;
}
