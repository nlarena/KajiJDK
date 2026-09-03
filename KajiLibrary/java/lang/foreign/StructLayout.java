package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.StructLayout -- los miembros **uno detras del otro**.
 *
 * <p>El offset de cada miembro es la suma de los tamanios de los anteriores, y ahi esta la regla que
 * mas sorprende: **cada miembro tiene que caer en un offset multiplo de su propio alineamiento**, o
 * la construccion falla. Un `long` despues de un `int` no forma un struct; hay que meter
 * `MemoryLayout.paddingLayout(4)` en el medio.
 *
 * <p>Que falle en vez de acomodar es deliberado. Un struct que inserta relleno solo describe una
 * cosa distinta segun la plataforma, y este paquete existe para describir memoria exacta.
 */
public interface StructLayout extends GroupLayout {

    StructLayout withName(String name);

    StructLayout withoutName();

    StructLayout withByteAlignment(long byteAlignment);
}
