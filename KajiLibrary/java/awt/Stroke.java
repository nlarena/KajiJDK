package java.awt;

/**
 * Como se convierte el contorno de una figura en la figura rellena que de verdad se pinta.
 *
 * <p>Esta entera --un solo metodo-- porque {@code Shape} ya existe: es la unica de las cinco
 * interfaces de pintado de {@code java.awt} que no menciona nada de {@code java.awt.image}.
 */
public interface Stroke {

    /**
     * Devuelve la figura que hay que rellenar para que se vea el trazo de {@code p}. No dibuja: el
     * grosor, las puntas y las uniones se resuelven aca, en geometria, y el rasterizador despues
     * rellena y ya.
     */
    Shape createStrokedShape(Shape p);
}
