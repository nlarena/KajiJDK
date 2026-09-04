package java.awt;

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * El contexto que mezcla de verdad: las doce reglas de Porter-Duff, píxel por píxel.
 *
 * <p>Toda la mezcla se reduce a dos números por operando. Cada regla dice qué fracción del origen y
 * qué fracción del destino sobreviven —{@code Fa} y {@code Fb}— y el resultado es siempre
 *
 * <pre>color = Fa * colorOrigen + Fb * colorDestino
 * alfa  = Fa * alfaOrigen  + Fb * alfaDestino</pre>
 *
 * <p>Eso es cierto **sólo** con el color premultiplicado por su alfa, y es la razón de que este
 * contexto premultiplique al entrar y deshaga al salir. Sin premultiplicar, cada regla necesitaría
 * su propio caso especial para no teñir el resultado con el color de un píxel invisible.
 *
 * <p>Los dos factores dependen únicamente de los alfas, y de ahí sale la tabla de doce filas que es
 * el corazón de la clase. `SRC_OVER`, la de siempre, es `Fa = 1` y `Fb = 1 - alfaOrigen`: el origen
 * entero, y del destino lo que el origen deje ver.
 *
 * <p>No es pública: es cómo está escrito {@link AlphaComposite#createContext}.
 */
class AlphaCompositeContext implements CompositeContext {

    private final int rule;
    private final float extraAlpha;

    /** Con la regla y el alfa extra que se aplica al origen. */
    AlphaCompositeContext(int rule, float extraAlpha) {
        this.rule = rule;
        this.extraAlpha = extraAlpha;
    }

    /** No hay recursos que soltar. */
    public void dispose() {
    }

    /**
     * Mezcla el origen con el destino y escribe el resultado.
     *
     * <p>Los tres rásters se recorren en su **propio** origen y por el rectángulo más chico de los
     * tres: no tienen por qué estar en el mismo lugar del plano ni medir lo mismo.
     */
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
        int w = Math.min(Math.min(src.getWidth(), dstIn.getWidth()), dstOut.getWidth());
        int h = Math.min(Math.min(src.getHeight(), dstIn.getHeight()), dstOut.getHeight());
        int sx = src.getMinX();
        int sy = src.getMinY();
        int ix = dstIn.getMinX();
        int iy = dstIn.getMinY();
        int ox = dstOut.getMinX();
        int oy = dstOut.getMinY();
        boolean srcAlfa = src.getNumBands() > 3;
        boolean inAlfa = dstIn.getNumBands() > 3;
        boolean outAlfa = dstOut.getNumBands() > 3;
        int[] s = new int[src.getNumBands()];
        int[] d = new int[dstIn.getNumBands()];
        int[] o = new int[dstOut.getNumBands()];
        float[] f = new float[2];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                s = src.getPixel(sx + i, sy + j, s);
                d = dstIn.getPixel(ix + i, iy + j, d);
                float as = (srcAlfa ? s[3] / 255.0f : 1.0f) * this.extraAlpha;
                float ad = inAlfa ? d[3] / 255.0f : 1.0f;
                this.factores(as, ad, f);
                float alfa = f[0] * as + f[1] * ad;
                for (int c = 0; c < 3; c++) {
                    // Premultiplicar, mezclar, y deshacer: el color de un pixel invisible no aporta
                    // nada, que es justamente lo que la premultiplicacion garantiza.
                    float cs = s[c] * as;
                    float cd = d[c] * ad;
                    float v = f[0] * cs + f[1] * cd;
                    if (alfa > 0.0f) {
                        v = v / alfa;
                    } else {
                        v = 0.0f;
                    }
                    o[c] = recortar(v);
                }
                if (outAlfa) {
                    o[3] = recortar(alfa * 255.0f);
                }
                dstOut.setPixel(ox + i, oy + j, o);
            }
        }
    }

    /**
     * Los dos factores de la regla, para esos alfas.
     *
     * <p>Es la tabla de Porter-Duff. `f[0]` es cuánto del origen sobrevive y `f[1]` cuánto del
     * destino.
     */
    private void factores(float as, float ad, float[] f) {
        if (this.rule == AlphaComposite.CLEAR) {
            f[0] = 0.0f;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.SRC) {
            f[0] = 1.0f;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.DST) {
            f[0] = 0.0f;
            f[1] = 1.0f;
        } else if (this.rule == AlphaComposite.SRC_OVER) {
            f[0] = 1.0f;
            f[1] = 1.0f - as;
        } else if (this.rule == AlphaComposite.DST_OVER) {
            f[0] = 1.0f - ad;
            f[1] = 1.0f;
        } else if (this.rule == AlphaComposite.SRC_IN) {
            f[0] = ad;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.DST_IN) {
            f[0] = 0.0f;
            f[1] = as;
        } else if (this.rule == AlphaComposite.SRC_OUT) {
            f[0] = 1.0f - ad;
            f[1] = 0.0f;
        } else if (this.rule == AlphaComposite.DST_OUT) {
            f[0] = 0.0f;
            f[1] = 1.0f - as;
        } else if (this.rule == AlphaComposite.SRC_ATOP) {
            f[0] = ad;
            f[1] = 1.0f - as;
        } else if (this.rule == AlphaComposite.DST_ATOP) {
            f[0] = 1.0f - ad;
            f[1] = as;
        } else {
            // XOR: cada uno sobrevive donde el otro no esta.
            f[0] = 1.0f - ad;
            f[1] = 1.0f - as;
        }
    }

    /** Un valor llevado a un byte. */
    private static int recortar(float v) {
        int i = (int) (v + 0.5f);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }
}
