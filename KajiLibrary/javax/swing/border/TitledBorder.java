package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Un borde con un titulo escrito encima de la linea.
 *
 * <h2>El unico borde de este paquete que tiene contenido</h2>
 *
 * <p>Los demas son geometria: reservan espacio y pintan lineas. Este lleva <strong>texto</strong>, y
 * eso arrastra todo lo que el texto arrastra — una fuente, un color, metricas que dependen del
 * sistema, y una decision de donde ponerlo. De ahi que tenga trece constantes de posicion donde los
 * otros no tienen ninguna.
 *
 * <h2>Las dos dimensiones de ubicacion, que son independientes</h2>
 *
 * <p>{@link #setTitlePosition} dice a que altura va —arriba del borde, encima, abajo del borde— y
 * {@link #setTitleJustification} a que costado. Son perillas separadas porque cualquier combinacion
 * es valida.
 *
 * <p>{@link #LEADING} y {@link #TRAILING} no son sinonimos de {@link #LEFT} y {@link #RIGHT}: se
 * resuelven segun la orientacion del componente, asi que en un idioma que se lee de derecha a
 * izquierda apuntan al otro lado. Es la razon de que existan las cinco y no tres.
 *
 * <h2>Envuelve a otro borde</h2>
 *
 * <p>El marco que dibuja no es propio: delega en el {@link Border} que le den, y lo unico que hace
 * es reservarle lugar al titulo y escribirlo. Un {@code TitledBorder} sin borde interno es solo el
 * texto.
 */
public class TitledBorder extends AbstractBorder {

    private static final long serialVersionUID = 8012999415147721601L;

    protected String title;
    protected Border border;
    protected int titlePosition;
    protected int titleJustification;
    protected Font titleFont;
    protected Color titleColor;

    /** La posicion que decida quien dibuje; en la practica, {@link #TOP}. */
    public static final int DEFAULT_POSITION = 0;
    /** Arriba de la linea de arriba. */
    public static final int ABOVE_TOP = 1;
    /** Encima de la linea de arriba, partiendola. */
    public static final int TOP = 2;
    /** Abajo de la linea de arriba. */
    public static final int BELOW_TOP = 3;
    /** Arriba de la linea de abajo. */
    public static final int ABOVE_BOTTOM = 4;
    /** Encima de la linea de abajo, partiendola. */
    public static final int BOTTOM = 5;
    /** Abajo de la linea de abajo. */
    public static final int BELOW_BOTTOM = 6;

    /** La justificacion que decida quien dibuje; en la practica, {@link #LEADING}. */
    public static final int DEFAULT_JUSTIFICATION = 0;
    /** Pegado a la izquierda, sin importar la orientacion. */
    public static final int LEFT = 1;
    /** Centrado. */
    public static final int CENTER = 2;
    /** Pegado a la derecha, sin importar la orientacion. */
    public static final int RIGHT = 3;
    /** Al principio segun la orientacion del componente. */
    public static final int LEADING = 4;
    /** Al final segun la orientacion del componente. */
    public static final int TRAILING = 5;

    /** Lo que se deja libre entre el titulo y el borde de la caja. */
    protected static final int EDGE_SPACING = 2;
    /** Lo que se deja libre arriba y abajo del texto. */
    protected static final int TEXT_SPACING = 2;
    /** Lo que se deja libre a los costados del texto. */
    protected static final int TEXT_INSET_H = 5;

    /** Solo el titulo, sin borde. */
    public TitledBorder(String title) {
        this(null, title, LEADING, DEFAULT_POSITION, null, null);
    }

    /** Un borde con el titulo vacio. */
    public TitledBorder(Border border) {
        this(border, "", LEADING, DEFAULT_POSITION, null, null);
    }

    /** Un borde con su titulo. */
    public TitledBorder(Border border, String title) {
        this(border, title, LEADING, DEFAULT_POSITION, null, null);
    }

    /** Igual, eligiendo donde va el titulo. */
    public TitledBorder(Border border, String title, int titleJustification, int titlePosition) {
        this(border, title, titleJustification, titlePosition, null, null);
    }

    /** Igual, con una fuente. */
    public TitledBorder(Border border, String title, int titleJustification, int titlePosition,
            Font titleFont) {
        this(border, title, titleJustification, titlePosition, titleFont, null);
    }

    /** Con todo explicito. */
    public TitledBorder(Border border, String title, int titleJustification, int titlePosition,
            Font titleFont, Color titleColor) {
        this.title = title;
        this.border = border;
        this.titleFont = titleFont;
        this.titleColor = titleColor;
        setTitleJustification(titleJustification);
        setTitlePosition(titlePosition);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Border interno = getBorder();
        String texto = getTitle();
        if (texto == null || texto.isEmpty()) {
            // Sin titulo no hay nada que reservar ni que escribir: es el borde interno tal cual.
            if (interno != null) {
                interno.paintBorder(c, g, x, y, width, height);
            }
            return;
        }

        Font fuente = getFont(c);
        FontMetrics fm = c.getFontMetrics(fuente);
        int anchoTexto = fm.stringWidth(texto);
        int altoTexto = fm.getHeight();
        int posicion = resolverPosicion();
        Insets borde = interno.getBorderInsets(c);

        // La caja del titulo: tiene el alto del renglon y se apoya donde diga la posicion.
        int cajaX = x + posicionHorizontal(c, width, anchoTexto, borde);
        int cajaY = y + arribaDeLaCaja(c, posicion, height, altoTexto);

        // El borde interno va metido `EDGE_SPACING` por lado, y del lado del titulo empieza donde
        // la caja lo deja: por su mitad si el texto va montado sobre la linea (`TOP`, `BOTTOM`), por
        // debajo o por encima de ella si el texto va aparte. Medido contra el JDK en `TOP`; el
        // resto sigue la misma regla.
        int bx = x + EDGE_SPACING;
        int by = y + EDGE_SPACING;
        int bw = width - EDGE_SPACING - EDGE_SPACING;
        int bh = height - EDGE_SPACING - EDGE_SPACING;
        if (posicion == TOP) {
            by = y + altoTexto / 2;
            bh = height - altoTexto / 2 - EDGE_SPACING;
        } else if (posicion == ABOVE_TOP) {
            by = y + altoTexto;
            bh = height - altoTexto - EDGE_SPACING;
        } else if (posicion == BOTTOM) {
            bh = height - altoTexto / 2 - EDGE_SPACING;
        } else if (posicion == BELOW_BOTTOM) {
            bh = height - altoTexto - EDGE_SPACING;
        }

        // El JDK no pinta el fondo detras del titulo: RECORTA la caja del titulo fuera del borde,
        // asi que la linea se interrumpe y lo que hay debajo sigue viendose. Se pinta el borde tres
        // veces con tres recortes que juntos son "todo menos la caja", y cada uno sobre una copia
        // del contexto para no arrastrar el recorte a quien sigue.
        int huecoX = cajaX - TEXT_SPACING;
        int huecoW = anchoTexto + TEXT_SPACING + TEXT_SPACING;
        pintarBordeRecortado(interno, c, g, bx, by, bw, bh,
                x, y, huecoX - x, height);
        pintarBordeRecortado(interno, c, g, bx, by, bw, bh,
                huecoX + huecoW, y, x + width - huecoX - huecoW, height);
        pintarBordeRecortado(interno, c, g, bx, by, bw, bh,
                huecoX, y, huecoW, cajaY - y);
        pintarBordeRecortado(interno, c, g, bx, by, bw, bh,
                huecoX, cajaY + altoTexto, huecoW, y + height - cajaY - altoTexto);

        Color colorViejo = g.getColor();
        Font fuenteVieja = g.getFont();
        g.setFont(fuente);
        // El color por omision NO es el de frente del componente; ver getTitleColor.
        g.setColor(getTitleColor());
        g.drawString(texto, cajaX, cajaY + fm.getAscent());
        g.setFont(fuenteViejaODefault(fuenteVieja, fuente));
        g.setColor(colorViejo);
    }

    /** Pinta el borde interno con el recorte dado, sobre una copia del contexto. */
    private void pintarBordeRecortado(Border interno, Component c, Graphics g,
            int bx, int by, int bw, int bh, int cx, int cy, int cw, int ch) {
        if (cw <= 0 || ch <= 0) {
            return;
        }
        Graphics copia = g.create();
        copia.clipRect(cx, cy, cw, ch);
        interno.paintBorder(c, copia, bx, by, bw, bh);
        copia.dispose();
    }

    private Font fuenteViejaODefault(Font vieja, Font usada) {
        return vieja != null ? vieja : usada;
    }

    private int resolverPosicion() {
        int p = getTitlePosition();
        return p == DEFAULT_POSITION ? TOP : p;
    }

    private int posicionHorizontal(Component c, int width, int anchoTexto, Insets borde) {
        int j = getTitleJustification();
        if (j == DEFAULT_JUSTIFICATION) {
            j = LEADING;
        }
        // `LEADING`/`TRAILING` se resuelven aca y no en el setter: la orientacion es del componente
        // y puede cambiar despues de construido el borde.
        if (j == LEADING || j == TRAILING) {
            boolean izqADer = isLeftToRight(c);
            if (j == LEADING) {
                j = izqADer ? LEFT : RIGHT;
            } else {
                j = izqADer ? RIGHT : LEFT;
            }
        }
        // Medido contra el JDK: el texto arranca pasado el borde interno, el margen y el inset
        // horizontal, y contra la derecha es lo mismo espejado.
        if (j == CENTER) {
            return (width - anchoTexto) / 2;
        }
        if (j == RIGHT) {
            return width - borde.right - EDGE_SPACING - TEXT_INSET_H - anchoTexto;
        }
        return borde.left + EDGE_SPACING + TEXT_INSET_H;
    }

    /**
     * Donde empieza la caja del titulo, medida desde el borde superior del area.
     *
     * <p>Los seis casos salen de medir {@code getBaseline} en el JDK, posicion por posicion, y no
     * de razonar la geometria: la caja del titulo tiene el alto del renglon, y lo unico que cambia
     * es contra que se apoya. Arriba se apoya en el area misma ({@link #TOP}, {@link #ABOVE_TOP}) o
     * debajo del borde y su margen ({@link #BELOW_TOP}); abajo, contra el fondo del area
     * ({@link #BOTTOM}, {@link #BELOW_BOTTOM}) o encima del borde y su margen
     * ({@link #ABOVE_BOTTOM}).
     */
    private int arribaDeLaCaja(Component c, int posicion, int height, int altoTexto) {
        Insets borde = getBorder().getBorderInsets(c);
        if (posicion == BELOW_TOP) {
            return borde.top + EDGE_SPACING;
        }
        if (posicion == ABOVE_BOTTOM) {
            return height - altoTexto - borde.bottom - EDGE_SPACING;
        }
        if (posicion == BOTTOM || posicion == BELOW_BOTTOM) {
            return height - altoTexto;
        }
        return 0;
    }

    /** La linea de base del titulo: el techo de su caja mas el ascenso. */
    private int lineaBase(Component c, int posicion, int height, int altoTexto, int ascenso) {
        return arribaDeLaCaja(c, posicion, height, altoTexto) + ascenso;
    }

    /**
     * Cuanto reserva, con la aritmetica del JDK medida en sus siete posiciones.
     *
     * <p>No es la que uno escribiria de memoria, y por eso va dicha: sin titulo reserva
     * <em>solo</em> el borde interno, sin margen alguno. Con titulo, cada lado suma
     * {@code EDGE_SPACING + TEXT_SPACING} —de ahi que un borde de un pixel de 5 por lado— y el
     * lado del titulo se lleva el alto del renglon completo, salvo en {@link #TOP} y
     * {@link #BOTTOM}, donde el texto va montado sobre la linea y el borde solo tiene que crecer
     * hasta ese alto menos el margen. Un {@code TitledBorder} adentro de otro no duplica el margen.
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        Border interno = getBorder();
        Insets i = interno.getBorderInsets(c);
        insets.top = i.top;
        insets.left = i.left;
        insets.right = i.right;
        insets.bottom = i.bottom;

        String texto = getTitle();
        if (texto == null || texto.isEmpty()) {
            return insets;
        }
        int margen = (interno instanceof TitledBorder) ? 0 : EDGE_SPACING;
        int alto = c.getFontMetrics(getFont(c)).getHeight();
        int posicion = resolverPosicion();
        if (posicion == ABOVE_TOP) {
            insets.top = insets.top + alto - margen;
        } else if (posicion == TOP) {
            if (insets.top < alto) {
                insets.top = alto - margen;
            }
        } else if (posicion == BELOW_TOP) {
            insets.top = insets.top + alto;
        } else if (posicion == ABOVE_BOTTOM) {
            insets.bottom = insets.bottom + alto;
        } else if (posicion == BOTTOM) {
            if (insets.bottom < alto) {
                insets.bottom = alto - margen;
            }
        } else if (posicion == BELOW_BOTTOM) {
            insets.bottom = insets.bottom + alto - margen;
        }
        insets.top = insets.top + margen + TEXT_SPACING;
        insets.left = insets.left + margen + TEXT_SPACING;
        insets.right = insets.right + margen + TEXT_SPACING;
        insets.bottom = insets.bottom + margen + TEXT_SPACING;
        return insets;
    }

    /**
     * Opaco solo si el borde interno lo es.
     *
     * <p>El titulo no cubre nada por su cuenta: lo unico que puede prometer opacidad es el marco que
     * este borde envuelve.
     */
    public boolean isBorderOpaque() {
        Border interno = getBorder();
        return interno != null && interno.isBorderOpaque();
    }

    /** El titulo. */
    public String getTitle() {
        return this.title;
    }

    /**
     * El borde que envuelve.
     *
     * <p>Nunca {@code null}, aunque se haya construido sin borde: el JDK sustituye ahi el borde por
     * omision del aspecto —{@code UIManager.getBorder("TitledBorder.border")}—, y medirlo confirma
     * que un {@code TitledBorder} sin borde reserva lo mismo que uno con una linea de un pixel.
     * Esta biblioteca no tiene {@code UIManager}; la sustitucion es una linea gris de un pixel,
     * compartida, que es lo que ese valor por omision es en la practica.
     */
    public Border getBorder() {
        if (this.border != null) {
            return this.border;
        }
        return BORDE_POR_OMISION;
    }

    /** Lo que hace de {@code TitledBorder.border} sin un {@code UIManager}. */
    private static final Border BORDE_POR_OMISION = new LineBorder(Color.gray, 1);

    /** A que altura va el titulo. */
    public int getTitlePosition() {
        return this.titlePosition;
    }

    /** A que costado va el titulo. */
    public int getTitleJustification() {
        return this.titleJustification;
    }

    /** La fuente del titulo, o {@code null} si sigue la del componente. */
    public Font getTitleFont() {
        return this.titleFont;
    }

    /**
     * El color del titulo.
     *
     * <p>Nunca {@code null}: sin uno fijado, el JDK devuelve
     * {@code UIManager.getColor("TitledBorder.titleColor")}, que en su aspecto por omision es un
     * gris oscuro, (51, 51, 51) — no el color de frente del componente, que es lo que uno supondria
     * y lo que esta clase hacia antes de medirlo. Sin {@code UIManager}, ese gris es la constante
     * de abajo, y es lo que hace que un titulo sin color explicito salga identico en las dos VMs.
     */
    public Color getTitleColor() {
        if (this.titleColor != null) {
            return this.titleColor;
        }
        return COLOR_POR_OMISION;
    }

    /** Lo que hace de {@code TitledBorder.titleColor} sin un {@code UIManager}: el gris de Metal. */
    private static final Color COLOR_POR_OMISION = new Color(51, 51, 51);

    /** Cambia el titulo. */
    public void setTitle(String title) {
        this.title = title;
    }

    /** Cambia el borde que envuelve. */
    public void setBorder(Border border) {
        this.border = border;
    }

    /**
     * Cambia la altura del titulo.
     *
     * @throws IllegalArgumentException si no es una de las siete constantes de posicion
     */
    public void setTitlePosition(int titlePosition) {
        if (titlePosition < DEFAULT_POSITION || titlePosition > BELOW_BOTTOM) {
            throw new IllegalArgumentException(String.valueOf(titlePosition)
                    + " no es una posicion de titulo valida");
        }
        this.titlePosition = titlePosition;
    }

    /**
     * Cambia el costado del titulo.
     *
     * @throws IllegalArgumentException si no es una de las seis constantes de justificacion
     */
    public void setTitleJustification(int titleJustification) {
        if (titleJustification < DEFAULT_JUSTIFICATION || titleJustification > TRAILING) {
            throw new IllegalArgumentException(String.valueOf(titleJustification)
                    + " no es una justificacion de titulo valida");
        }
        this.titleJustification = titleJustification;
    }

    /** Cambia la fuente del titulo; {@code null} para seguir la del componente. */
    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
    }

    /** Cambia el color del titulo; {@code null} para seguir el del componente. */
    public void setTitleColor(Color titleColor) {
        this.titleColor = titleColor;
    }

    /**
     * El tamano minimo para que el titulo entre.
     *
     * <p>Es lo que evita que un panel se encoja hasta cortar su propio titulo: un borde comun no
     * tiene nada que decir sobre el tamano del componente, este si.
     */
    public Dimension getMinimumSize(Component c) {
        Insets i = getBorderInsets(c, new Insets(0, 0, 0, 0));
        Dimension d = new Dimension(i.right + i.left, i.top + i.bottom);
        String texto = getTitle();
        if (texto == null || texto.isEmpty()) {
            return d;
        }
        // Medido en el JDK: el ancho es los insets mas el texto, en las siete posiciones — sin el
        // `TEXT_INSET_H` extra que uno esperaria, porque ese ya viaja adentro de los insets.
        FontMetrics fm = c.getFontMetrics(getFont(c));
        d.width = d.width + fm.stringWidth(texto);
        return d;
    }

    /**
     * La linea de base del titulo.
     *
     * @throws NullPointerException si {@code c} es {@code null}
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(Component c, int width, int height) {
        if (c == null) {
            throw new NullPointerException("El componente no puede ser null");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("El ancho y el alto no pueden ser negativos");
        }
        String texto = getTitle();
        if (texto == null || texto.isEmpty()) {
            return -1;
        }
        FontMetrics fm = c.getFontMetrics(getFont(c));
        return lineaBase(c, resolverPosicion(), height, fm.getHeight(), fm.getAscent());
    }

    /**
     * Como se mueve la linea de base al cambiar el tamano.
     *
     * <p>Depende de donde este el titulo: pegado arriba no se mueve, pegado abajo se mueve con el
     * borde inferior. El tipo va con el nombre binario por el finding #101 — ver
     * {@link AbstractBorder#getBaselineResizeBehavior}.
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(Component c) {
        super.getBaselineResizeBehavior(c);
        int posicion = resolverPosicion();
        if (posicion == ABOVE_TOP || posicion == TOP || posicion == BELOW_TOP) {
            return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
        }
        if (posicion == ABOVE_BOTTOM || posicion == BOTTOM || posicion == BELOW_BOTTOM) {
            return Component$BaselineResizeBehavior.CONSTANT_DESCENT;
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    /**
     * La fuente del titulo, cayendo a la del componente y despues a una por omision.
     *
     * <p>Una diferencia con el JDK que conviene saber: sin {@link #setTitleFont}, el JDK toma
     * {@code TitledBorder.font} de {@code UIManager}, que en todos sus aspectos es una
     * <strong>negrita</strong>. Esta VM dibuja toda fuente con una sola cara, Dialog 12 regular
     * —ver {@code jdk.internal.awt.FuenteBitmap}—, asi que un titulo sin fuente explicita sale
     * regular aca y negrita alla, y la diferencia es de cara, no de colocacion: con la misma fuente
     * plana en las dos puntas el borde entero coincide pixel por pixel. Es la sustitucion de fuente
     * de siempre, dicha en el lugar donde se nota.
     */
    protected Font getFont(Component c) {
        if (this.titleFont != null) {
            return this.titleFont;
        }
        if (c != null) {
            Font f = c.getFont();
            if (f != null) {
                return f;
            }
        }
        return new Font("Dialog", Font.PLAIN, 12);
    }
}
