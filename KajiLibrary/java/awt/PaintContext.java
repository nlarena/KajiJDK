package java.awt;

/**
 * El estado de un {@code Paint} mientras dura una operacion de dibujo: quien genera los pixeles.
 *
 * <h2>Superficie parcial</h2>
 *
 * <p>De los tres metodos que declara el JDK solo esta {@code dispose()}. Los otros dos --
 * {@code getColorModel()} y {@code getRaster(int, int, int, int)}-- devuelven
 * {@code java.awt.image.ColorModel} y {@code java.awt.image.Raster}, que no existen en
 * KajiLibrary; un metodo cuyo tipo de retorno no existe no se puede declarar.
 *
 * <p>Se escribe igual porque {@code dispose()} es la mitad del contrato que importa a quien
 * implementa: un contexto de pintado tiene recursos vivos y hay que soltarlos.
 */
public interface PaintContext {

    /**
     * Suelta los recursos del contexto. Se llama siempre, tambien cuando el dibujo fallo, asi que
     * tiene que poder llamarse sobre un contexto que nunca genero un pixel.
     */
    void dispose();
}
