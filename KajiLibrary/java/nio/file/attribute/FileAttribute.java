package java.nio.file.attribute;

// Un atributo suelto --nombre y valor-- que se le pasa a `Files.createFile` y compania para fijarlo
// **en el momento de crear**, atomicamente.
//
// KajiJDK acepta el tipo pero no puede honrar ningun atributo: los nativos de `jdk.internal.io.Fs`
// crean archivos y directorios sin ningun parametro de permisos. Los metodos que reciben
// `FileAttribute<?>...` documentan que un array no vacio termina en
// `UnsupportedOperationException` -- que es exactamente lo que manda la spec cuando el atributo no
// se puede fijar, asi que la mentira no llega a existir.
//
// @param <T> el tipo del valor del atributo
public interface FileAttribute<T> {

    /** El nombre del atributo, en la forma `"vista:atributo"`. */
    String name();

    /** El valor. */
    T value();
}
