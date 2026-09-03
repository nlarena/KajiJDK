package java.awt.image;

/**
 * Escala una imagen **promediando** los píxeles del origen que caen en cada píxel del destino.
 *
 * <p>Es el escalado bueno, y la diferencia con {@link ReplicateScaleFilter} se nota sobre todo al
 * achicar: repetir y saltear puede hacer desaparecer una línea fina entera, promediar la deja como
 * un gris tenue. Cuesta una multiplicación y una suma por cada solapamiento.
 *
 * <p>El reparto se hace en aritmética entera y sin errores de redondeo acumulados, trabajando en
 * unidades donde la imagen entera mide `srcWidth * destWidth` de ancho: ahí un píxel de origen ocupa
 * exactamente `destWidth` unidades y uno de destino, `srcWidth`. Cada solapamiento es el mínimo de
 * los dos restos, y no hay divisiones hasta el final.
 *
 * <p>Los colores se promedian **premultiplicados por el alfa**, y por eso hay que deshacerlo al
 * cerrar cada fila. Sin premultiplicar, un píxel transparente aportaría su color al promedio: al
 * achicar un logo sobre fondo transparente aparecería una orla del color del fondo invisible.
 *
 * <p>Necesita recibir las filas en orden y completas. Si el productor avisa que no las va a mandar
 * así, el filtro se resigna y se comporta como su clase base, que puede trabajar en cualquier orden.
 */
public class AreaAveragingScaleFilter extends ReplicateScaleFilter {

    private static final ColorModel rgbmodel = ColorModel.getRGBdefault();
    private static final int neededHints = ImageConsumer.TOPDOWNLEFTRIGHT
            | ImageConsumer.COMPLETESCANLINES;

    private boolean passthrough;
    private float[] reds;
    private float[] greens;
    private float[] blues;
    private float[] alphas;

    /** La fila de destino que se está armando. */
    private int savedy;

    /** Cuántas unidades le faltan a esa fila para estar completa. */
    private int savedyrem;

    /**
     * Con el tamaño de destino.
     *
     * @throws IllegalArgumentException si alguna de las dos medidas es cero
     */
    public AreaAveragingScaleFilter(int width, int height) {
        super(width, height);
    }

    /**
     * Anota si se van a poder promediar los píxeles.
     *
     * <p>Sin filas completas y en orden no se puede: el promedio de una fila de destino necesita
     * todas las de origen que la tocan, y sin garantía de orden habría que guardar la imagen entera.
     */
    public void setHints(int hints) {
        this.passthrough = (hints & neededHints) != neededHints;
        super.setHints(hints);
    }

    /** Reserva los acumuladores de una fila de destino. */
    private void crearAcumuladores() {
        this.reds = new float[this.destWidth];
        this.greens = new float[this.destWidth];
        this.blues = new float[this.destWidth];
        this.alphas = new float[this.destWidth];
    }

    /** Pone los acumuladores en cero para empezar otra fila. */
    private void limpiarAcumuladores() {
        for (int i = 0; i < this.destWidth; i++) {
            this.alphas[i] = 0.0f;
            this.reds[i] = 0.0f;
            this.greens[i] = 0.0f;
            this.blues[i] = 0.0f;
        }
    }

    /**
     * Cierra la fila de destino: divide por el área y deshace la premultiplicación.
     *
     * <p>Con alfa cero no hay color que recuperar y el píxel sale transparente y negro; con alfa
     * lleno no hace falta dividir dos veces y basta el promedio.
     */
    private int[] cerrarFila() {
        float areaTotal = ((float) this.srcWidth) * this.srcHeight;
        if (this.outpixbuf == null || !(this.outpixbuf instanceof int[])) {
            this.outpixbuf = new int[this.destWidth];
        }
        int[] outpix = (int[]) this.outpixbuf;
        for (int x = 0; x < this.destWidth; x++) {
            float mult = areaTotal;
            int a = Math.round(this.alphas[x] / mult);
            if (a <= 0) {
                a = 0;
            } else if (a >= 255) {
                a = 255;
            } else {
                // Dividir por este otro factor hace la division por el area y la de deshacer la
                // premultiplicacion en un solo paso.
                mult = this.alphas[x] / 255;
            }
            int r = Math.round(this.reds[x] / mult);
            int g = Math.round(this.greens[x] / mult);
            int b = Math.round(this.blues[x] / mult);
            if (r < 0) {
                r = 0;
            } else if (r > 255) {
                r = 255;
            }
            if (g < 0) {
                g = 0;
            } else if (g > 255) {
                g = 255;
            }
            if (b < 0) {
                b = 0;
            } else if (b > 255) {
                b = 255;
            }
            outpix[x] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return outpix;
    }

    /**
     * Reparte una tanda de píxeles entre las filas de destino que toca.
     *
     * <p>`pixels` es un `byte[]` o un `int[]`; el modelo de color dice cómo interpretarlo.
     */
    private void acumular(int x, int y, int w, int h, ColorModel model, Object pixels, int off,
            int scansize) {
        if (this.reds == null) {
            this.crearAcumuladores();
        }
        int sy = y;
        int syrem = this.destHeight;
        int dy;
        int dyrem;
        if (sy == 0) {
            dy = 0;
            dyrem = 0;
        } else {
            dy = this.savedy;
            dyrem = this.savedyrem;
        }
        int fila = off;
        while (sy < y + h) {
            if (dyrem == 0) {
                this.limpiarAcumuladores();
                dyrem = this.srcHeight;
            }
            int amty = syrem < dyrem ? syrem : dyrem;
            this.acumularFila(model, pixels, fila, w, amty);
            syrem = syrem - amty;
            dyrem = dyrem - amty;
            if (dyrem == 0 && dy < this.destHeight) {
                int[] outpix = this.cerrarFila();
                this.consumer.setPixels(0, dy, this.destWidth, 1, rgbmodel, outpix, 0,
                        this.destWidth);
                dy = dy + 1;
            }
            if (syrem == 0) {
                sy = sy + 1;
                syrem = this.destHeight;
                fila = fila + scansize;
            }
        }
        this.savedy = dy;
        this.savedyrem = dyrem;
    }

    /** Suma una fila de origen a los acumuladores, con el peso vertical dado. */
    private void acumularFila(ColorModel model, Object pixels, int off, int w, int amty) {
        int dx = 0;
        int dxrem = this.srcWidth;
        for (int sx = 0; sx < w; sx++) {
            int crudo;
            if (pixels instanceof byte[]) {
                crudo = ((byte[]) pixels)[off + sx] & 0xFF;
            } else {
                crudo = ((int[]) pixels)[off + sx];
            }
            int rgb = model.getRGB(crudo);
            float a = rgb >>> 24;
            float r = (rgb >> 16) & 0xFF;
            float g = (rgb >> 8) & 0xFF;
            float b = rgb & 0xFF;
            if (a != 255.0f) {
                float escala = a / 255.0f;
                r = r * escala;
                g = g * escala;
                b = b * escala;
            }
            int sxrem = this.destWidth;
            while (sxrem > 0 && dx < this.destWidth) {
                int amtx = sxrem < dxrem ? sxrem : dxrem;
                float mult = ((float) amtx) * amty;
                this.alphas[dx] = this.alphas[dx] + mult * a;
                this.reds[dx] = this.reds[dx] + mult * r;
                this.greens[dx] = this.greens[dx] + mult * g;
                this.blues[dx] = this.blues[dx] + mult * b;
                sxrem = sxrem - amtx;
                dxrem = dxrem - amtx;
                if (dxrem == 0) {
                    dx = dx + 1;
                    dxrem = this.srcWidth;
                }
            }
        }
    }

    /** Promedia, o se resigna a repetir si el productor no garantiza el orden. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (this.passthrough) {
            super.setPixels(x, y, w, h, model, pixels, off, scansize);
        } else {
            this.acumular(x, y, w, h, model, pixels, off, scansize);
        }
    }

    /** Lo mismo para píxeles de un `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (this.passthrough) {
            super.setPixels(x, y, w, h, model, pixels, off, scansize);
        } else {
            this.acumular(x, y, w, h, model, pixels, off, scansize);
        }
    }
}
