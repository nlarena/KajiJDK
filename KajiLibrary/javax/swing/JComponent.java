package javax.swing;

import java.awt.Container;
import java.io.Serializable;

/**
 * <strong>Un lugar reservado, no una implementación.</strong> Esta biblioteca no trae Swing.
 *
 * <p>En Swing de verdad, `JComponent` es la raíz de todo: de ella cuelgan los botones, las etiquetas,
 * las tablas, y ella aporta el doble buffer, los bordes, los atajos de teclado, las descripciones
 * emergentes y —sobre todo— el modelo de **aspecto separado**, en el que cada componente delega su
 * dibujado en un `ComponentUI` intercambiable. Nada de eso está acá, y no por descuido: sin
 * rasterizador no hay dibujado que delegar.
 *
 * <p><strong>Existe por un solo motivo</strong>: {@code java.awt.Desktop.setDefaultMenuBar} recibe un
 * {@link JMenuBar}, y un `JMenuBar` es un `JComponent`. Sin esta clase, ese método —el único de todo
 * `java.awt` cuya firma nombra un tipo de Swing— no se podría declarar, y `java.awt` quedaría
 * incompleto por una dependencia que no le pertenece.
 *
 * <p><strong>Qué falta, dicho de frente:</strong> todos sus miembros. Los doscientos y pico métodos
 * de `JComponent` no están, ni su interfaz interna `HasGetTransferHandler`. Lo único cierto acá es su
 * lugar en la jerarquía —extiende {@link Container}, que sí está entera— y eso alcanza para lo que
 * hace falta. Se prefirió una clase vacía y anunciada antes que una selección arbitraria de treinta
 * métodos que pareciera Swing sin serlo: un miembro que falta es un subconjunto legal, uno que miente
 * no lo es, y una clase a medias invita a confundir las dos cosas.
 */
public abstract class JComponent extends Container implements Serializable {

    private static final long serialVersionUID = -5876370834061273469L;

    /** Para las subclases. */
    public JComponent() {
    }
}
