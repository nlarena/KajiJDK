package javax.swing;

import java.awt.Image;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

/**
 * Convierte una imagen a grises y le sube el brillo: es como se ve un icono apagado.
 *
 * <h2>Dos pasos, no uno</h2>
 *
 * <p>Primero pasa a gris, y despues aclara u oscurece. El gris solo no alcanzaria: un icono en
 * grises con el mismo contraste que el original se sigue leyendo como activo. Lo que dice "esto no
 * se puede tocar" es que ademas este <em>lavado</em>, y de eso se encarga el porcentaje.
 *
 * <h2>La conversion a gris no es el promedio</h2>
 *
 * <p>Es {@code 0.30 R + 0.59 G + 0.11 B}. Los tres numeros no son arbitrarios: el ojo es mucho mas
 * sensible al verde que al azul, asi que promediar los tres por igual daria un gris que se ve mal
 * -- los verdes salen demasiado oscuros y los azules demasiado claros --.
 *
 * <p>La transparencia se conserva tal cual: se filtran los tres canales de color y el alfa pasa
 * intacto. Un icono con bordes suavizados sigue teniendolos.
 */
public class GrayFilter extends RGBImageFilter {

    private boolean brighter;
    private int percent;

    /**
     * La version apagada de esa imagen.
     *
     * <p>Aclarando al 50%, que es lo que usa Swing para los iconos de los botones apagados.
     */
    public static Image createDisabledImage(Image i) {
        GrayFilter filter = new GrayFilter(true, 50);
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        return java.awt.Toolkit.getDefaultToolkit().createImage(prod);
    }

    /**
     * Un filtro que aclara u oscurece ese porcentaje.
     *
     * @param b cierto para aclarar, falso para oscurecer
     * @param p cuanto, de 0 a 100
     */
    public GrayFilter(boolean b, int p) {
        brighter = b;
        percent = p;
        // El filtro no mira a los vecinos de un pixel, asi que se puede aplicar sobre la tabla de
        // colores en vez de sobre cada pixel: en una imagen indexada eso es una pasada de 256 en
        // lugar de una de un millon.
        canFilterIndexColorModel = true;
    }

    /**
     * El color de ese pixel, ya en gris y aclarado.
     *
     * <p>Ver la nota de la clase: los pesos no son iguales, y el alfa pasa sin tocar.
     */
    public int filterRGB(int x, int y, int rgb) {
        int gray = (int) ((0.30 * ((rgb >> 16) & 0xff)
                + 0.59 * ((rgb >> 8) & 0xff)
                + 0.11 * (rgb & 0xff)) / 3);
        if (brighter) {
            gray = (255 - ((255 - gray) * (100 - percent) / 100));
        } else {
            gray = (gray * (100 - percent) / 100);
        }
        if (gray < 0) {
            gray = 0;
        }
        if (gray > 255) {
            gray = 255;
        }
        return (rgb & 0xff000000) | (gray << 16) | (gray << 8) | (gray << 0);
    }
}
