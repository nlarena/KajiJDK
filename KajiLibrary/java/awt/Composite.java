package java.awt;

/**
 * Como se mezcla lo que se dibuja con lo que ya estaba.
 *
 * <h2>Interfaz vacia a proposito</h2>
 *
 * <p>Su unico metodo, {@code createContext(ColorModel, ColorModel, RenderingHints)}, menciona dos
 * veces {@code java.awt.image.ColorModel}, que no existe en KajiLibrary. Queda afuera por lo mismo
 * que el de {@code Paint}: declararlo con otra firma seria una mentira que compila.
 *
 * <p>El tipo sirve igual --{@code AlphaComposite} lo implementa-- y con eso se puede escribir y
 * probar toda la parte de la API que elige la regla de mezcla, que es la que un programa toca.
 */
public interface Composite {
}
