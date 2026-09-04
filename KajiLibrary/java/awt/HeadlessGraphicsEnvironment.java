package java.awt;

import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * El entorno gráfico de una máquina sin pantalla.
 *
 * <p>Es al {@link GraphicsEnvironment} lo que {@link HeadlessToolkit} al {@link Toolkit}, y sigue la
 * misma regla: contesta con la verdad lo que no necesita pantalla, tira {@link HeadlessException} en
 * lo que sí, y no inventa nada en el medio.
 *
 * <p>Las listas de tipografías salen vacías. No es una pantalla lo que falta ahí sino un motor de
 * tipografías: no hay cómo leer las instaladas ni cómo crear una desde un archivo, así que no hay
 * ninguna que nombrar. El motivo está en {@link GraphicsEnvironment#registerFont}.
 */
final class HeadlessGraphicsEnvironment extends GraphicsEnvironment {

    /** Lo arma {@link GraphicsEnvironment#getLocalGraphicsEnvironment}. */
    HeadlessGraphicsEnvironment() {
    }

    /**
     * Las pantallas.
     *
     * @throws HeadlessException siempre: no hay ninguna
     */
    public GraphicsDevice[] getScreenDevices() throws HeadlessException {
        throw new HeadlessException(GraphicsEnvironment.getHeadlessMessage());
    }

    /**
     * La pantalla principal.
     *
     * @throws HeadlessException siempre, por lo mismo
     */
    public GraphicsDevice getDefaultScreenDevice() throws HeadlessException {
        throw new HeadlessException(GraphicsEnvironment.getHeadlessMessage());
    }

    /**
     * Un contexto de dibujo sobre esa imagen.
     *
     * <p>No hay pantalla que lo impida —dibujar sobre una {@link BufferedImage} es todo en memoria—
     * pero sí falta el rasterizador: nadie sabe convertir una línea o una letra en píxeles.
     *
     * @throws UnsupportedOperationException siempre, con el motivo dicho. Devolver un `Graphics2D`
     *     que acepte todo y no dibuje nada sería peor: el llamador creería que la imagen tiene algo.
     * @throws NullPointerException si la imagen es `null`
     */
    public Graphics2D createGraphics(BufferedImage img) {
        if (img == null) {
            throw new NullPointerException("BufferedImage cannot be null");
        }
        throw new UnsupportedOperationException(
                "esta implementación no tiene rasterizador: no hay con qué dibujar sobre la imagen");
    }

    /**
     * Todas las tipografías.
     *
     * @return un arreglo vacío; nunca `null`
     */
    public Font[] getAllFonts() {
        return new Font[0];
    }

    /**
     * Los nombres de familia.
     *
     * @return un arreglo vacío
     */
    public String[] getAvailableFontFamilyNames() {
        return new String[0];
    }

    /**
     * Lo mismo, con los nombres en ese idioma.
     *
     * @param l el idioma; se acepta `null` porque no hay nada que traducir
     * @return un arreglo vacío
     */
    public String[] getAvailableFontFamilyNames(Locale l) {
        return new String[0];
    }
}
