package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.PaddingLayout -- lugar ocupado que no lleva nada.
 *
 * <p>Existe para escribir a mano los huecos que un compilador de C insertaria solo entre los campos
 * de un `struct`. Que haya que escribirlos es la decision de diseno del paquete: un layout que se
 * acomoda solo describiria una cosa distinta en cada plataforma, y todo esto existe para describir
 * memoria de forma exacta.
 *
 * <p>Su alineamiento es 1 y no se puede cambiar a algo mayor con sentido: una restriccion sobre
 * donde puede empezar la nada no restringe nada.
 */
public interface PaddingLayout extends MemoryLayout {

    PaddingLayout withName(String name);

    PaddingLayout withoutName();

    PaddingLayout withByteAlignment(long byteAlignment);
}
