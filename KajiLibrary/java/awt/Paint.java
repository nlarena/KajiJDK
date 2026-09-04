package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * Con qué se rellena una figura: un color plano, un degradé o una textura.
 *
 * <p>Es la generalización de "el color con el que se dibuja". Un {@link Color} es un `Paint` que
 * contesta lo mismo en todos los puntos; un degradé contesta distinto según dónde esté el punto. Al
 * dibujado le da igual: le pide al `Paint` un {@link PaintContext} y le pide píxeles.
 *
 * <p>Extiende {@link Transparency} porque quien dibuja necesita saber, **antes** de empezar, si lo
 * que va a pintar puede dejar ver lo de abajo: eso decide si puede escribir directo o tiene que
 * componer.
 */
public interface Paint extends Transparency {

    /**
     * Arma la máquina que va a generar los píxeles.
     *
     * @param cm el formato en el que el destino preferiría recibirlos, o `null` si le da igual;
     *     es una sugerencia y el contexto puede devolver otro
     * @param deviceBounds el rectángulo del dispositivo que se va a pintar
     * @param userBounds el mismo rectángulo en coordenadas de usuario
     * @param xform de coordenadas de usuario a coordenadas de dispositivo
     * @param hints las pistas de calidad
     */
    PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
            AffineTransform xform, RenderingHints hints);
}
