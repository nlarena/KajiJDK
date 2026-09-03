package java.awt;

/**
 * Con que se rellena una figura: un color plano, un degrade o una textura.
 *
 * <h2>Interfaz vacia a proposito</h2>
 *
 * <p>El JDK le declara un unico metodo,
 * {@code createContext(ColorModel, Rectangle, Rectangle2D, AffineTransform, RenderingHints)}, y
 * <b>no esta</b>: {@code java.awt.image.ColorModel} no existe en KajiLibrary y no se puede declarar
 * un metodo cuyo parametro no existe.
 *
 * <p>Inventar una firma con otro parametro seria peor que no tener el metodo: quien implemente
 * Paint contra esta interfaz compilaria y despues no encajaria con el JDK real. Un miembro que
 * falta es un subconjunto legal de la API; uno que miente no.
 *
 * <p>Lo que si sirve, y es la razon de escribirla igual, es el tipo: {@code Color} lo implementa,
 * los degrades lo implementan, y el {@code extends Transparency} --que si se puede declarar-- es la
 * parte del contrato que ya funciona. Cuando aparezca {@code java.awt.image}, el metodo es una
 * linea.
 */
public interface Paint extends Transparency {
}
