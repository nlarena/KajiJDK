package java.awt.image;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Una imagen de varias resoluciones dada como una lista fija de versiones.
 *
 * <p>Es la implementación directa: se le pasan las versiones ya hechas y elige entre ellas. La
 * elección es la más chica que **alcance** para el tamaño pedido, y sólo si ninguna alcanza se usa
 * la más grande. Agrandar una imagen se nota mucho más que achicarla, así que ante la duda conviene
 * la que sobra.
 *
 * <p>Las versiones tienen que venir ordenadas de menor a mayor; no se comprueba, porque comprobarlo
 * obligaría a pedirles el ancho a todas y eso puede disparar la carga de imágenes que nunca se van a
 * usar.
 */
public class BaseMultiResolutionImage extends AbstractMultiResolutionImage {

    private final int baseImageIndex;
    private final Image[] resolutionVariants;

    /**
     * Con la primera versión como base.
     *
     * @throws IllegalArgumentException si no se pasa ninguna versión o alguna es `null`
     */
    public BaseMultiResolutionImage(Image... resolutionVariants) {
        this(0, resolutionVariants);
    }

    /**
     * Con la versión base indicada por su posición.
     *
     * @throws IllegalArgumentException si no se pasa ninguna versión, si alguna es `null`, o si el
     *     índice de la base no existe
     */
    public BaseMultiResolutionImage(int baseImageIndex, Image... resolutionVariants) {
        if (resolutionVariants == null || resolutionVariants.length == 0) {
            throw new IllegalArgumentException("Null or empty resolution variants array");
        }
        for (int i = 0; i < resolutionVariants.length; i++) {
            if (resolutionVariants[i] == null) {
                throw new IllegalArgumentException("Null resolution variant");
            }
        }
        if (baseImageIndex < 0 || baseImageIndex >= resolutionVariants.length) {
            throw new IllegalArgumentException("Base image index is out of range");
        }
        this.baseImageIndex = baseImageIndex;
        this.resolutionVariants = Arrays.copyOf(resolutionVariants, resolutionVariants.length);
    }

    /** La versión que define el tamaño lógico. */
    protected Image getBaseImage() {
        return this.resolutionVariants[this.baseImageIndex];
    }

    /**
     * La versión más chica que alcance para ese tamaño.
     *
     * <p>Si ninguna alcanza se devuelve la más grande, que es lo mejor que hay.
     *
     * @throws IllegalArgumentException si alguna de las dos medidas no es positiva
     */
    public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
        if (destImageWidth <= 0 || destImageHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be > 0");
        }
        for (int i = 0; i < this.resolutionVariants.length; i++) {
            Image v = this.resolutionVariants[i];
            if (v.getWidth(null) >= destImageWidth && v.getHeight(null) >= destImageHeight) {
                return v;
            }
        }
        return this.resolutionVariants[this.resolutionVariants.length - 1];
    }

    /** Todas las versiones, de menor a mayor. */
    public List<Image> getResolutionVariants() {
        List<Image> out = new ArrayList<Image>();
        for (int i = 0; i < this.resolutionVariants.length; i++) {
            out.add(this.resolutionVariants[i]);
        }
        return java.util.Collections.unmodifiableList(out);
    }
}
