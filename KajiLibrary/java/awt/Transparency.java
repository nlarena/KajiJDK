package java.awt;

/**
 * Cuanto deja pasar por detras lo que se pinta: opaco, recortado o mezclado.
 *
 * <p>Es una de las pocas interfaces de {@code java.awt} que no menciona ningun tipo del sistema de
 * ventanas: son tres enteros y un metodo. Esta primero porque {@code Color},
 * {@code MultipleGradientPaint} y {@code TexturePaint} la implementan y sin ella no se declaran.
 */
public interface Transparency {

    /** Todo lo que se pinta tapa el fondo por completo. */
    int OPAQUE = 1;

    /**
     * Cada pixel tapa del todo o no tapa nada: no hay puntos medios. Es lo que hace un GIF con su
     * color transparente, y por eso vale la pena distinguirlo de TRANSLUCENT: quien compone puede
     * saltearse la mezcla y limitarse a copiar o no copiar.
     */
    int BITMASK = 2;

    /** Cada pixel tiene su propio alfa y hay que mezclarlo con el fondo. */
    int TRANSLUCENT = 3;

    int getTransparency();
}
