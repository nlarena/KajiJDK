package javax.swing.plaf.metal;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * Los colores y las tipografias de Metal.
 *
 * <h2>Ocho colores y nada mas</h2>
 *
 * <p>Un tema define ocho colores -- tres primarios, tres secundarios, blanco y negro -- y de ahi
 * sale <em>todo</em> lo demas. Los cuarenta y pico de metodos publicos de esta clase no guardan
 * nada: son nombres para combinaciones de esos ocho.
 *
 * <p>La division entre primarios y secundarios es la que hace que un tema se pueda escribir en
 * veinte lineas. Los <strong>secundarios</strong> son los grises de la chapa: el fondo de un boton,
 * su sombra, su sombra oscura. Los <strong>primarios</strong> son el color con el que ese tema
 * marca lo que esta elegido o tiene el foco -- azul en Steel, celeste en Ocean --. Dentro de cada
 * terna el 1 es el mas oscuro y el 3 el mas claro, siempre, y de eso depende que un boton dibujado
 * con {@code getControlDarkShadow} y {@code getControlHighlight} salga con relieve y no hundido.
 *
 * <p>Por eso los ocho son {@code protected} y los demas no: quien escribe un tema propio da los
 * ocho, y hereda gratis que el resto del aspecto quede coherente. Redefinir un derivado se puede y
 * es lo que hace {@link OceanTheme} en cinco casos donde la derivacion no daba lo que queria.
 *
 * <h2>Negro no es negro</h2>
 *
 * <p>{@link #getBlack} devuelve el negro en Steel y {@code (51,51,51)} en Ocean. Es el mismo lugar
 * en la estructura -- el color del texto y de los detalles -- y por eso se llama asi; que sea
 * literalmente negro es cosa del tema, no de la clase.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>{@link #addCustomEntriesToTable} no agrega nada. Un tema que solo cambia los ocho colores no
 * necesita agregar entradas; el que quiera degradados o iconos propios redefine el metodo, y es lo
 * que hace {@link OceanTheme}.
 */
public abstract class MetalTheme {

    private static final ColorUIResource BLANCO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource NEGRO = new ColorUIResource(0, 0, 0);

    public MetalTheme() {
    }

    /** Como se llama; es lo que sale por {@code MetalLookAndFeel.getCurrentTheme().getName()}. */
    public abstract String getName();

    // ---- los ocho ----

    /** El primario mas oscuro. */
    protected abstract ColorUIResource getPrimary1();

    /** El primario del medio. */
    protected abstract ColorUIResource getPrimary2();

    /** El primario mas claro. */
    protected abstract ColorUIResource getPrimary3();

    /** El secundario mas oscuro. */
    protected abstract ColorUIResource getSecondary1();

    /** El secundario del medio. */
    protected abstract ColorUIResource getSecondary2();

    /** El secundario mas claro; es el fondo de casi todo. */
    protected abstract ColorUIResource getSecondary3();

    /** El blanco; ningun tema del JDK lo cambia. */
    protected ColorUIResource getWhite() {
        return BLANCO;
    }

    /** El negro, que no siempre es negro; ver la nota de la clase. */
    protected ColorUIResource getBlack() {
        return NEGRO;
    }

    // ---- las tipografias ----

    public abstract FontUIResource getControlTextFont();

    public abstract FontUIResource getSystemTextFont();

    public abstract FontUIResource getUserTextFont();

    public abstract FontUIResource getMenuTextFont();

    public abstract FontUIResource getWindowTitleFont();

    public abstract FontUIResource getSubTextFont();

    // ---- lo derivado: los controles ----

    /** El fondo de un boton, un panel, una barra. */
    public ColorUIResource getControl() {
        return getSecondary3();
    }

    public ColorUIResource getControlShadow() {
        return getSecondary2();
    }

    public ColorUIResource getControlDarkShadow() {
        return getSecondary1();
    }

    public ColorUIResource getControlHighlight() {
        return getWhite();
    }

    /** El color de las lineas que dibuja un control: la flecha, la tilde, el punto. */
    public ColorUIResource getControlInfo() {
        return getBlack();
    }

    public ColorUIResource getControlDisabled() {
        return getSecondary2();
    }

    // ---- lo derivado: lo elegido y lo que tiene el foco ----

    public ColorUIResource getPrimaryControl() {
        return getPrimary3();
    }

    public ColorUIResource getPrimaryControlShadow() {
        return getPrimary2();
    }

    public ColorUIResource getPrimaryControlDarkShadow() {
        return getPrimary1();
    }

    public ColorUIResource getPrimaryControlHighlight() {
        return getWhite();
    }

    public ColorUIResource getPrimaryControlInfo() {
        return getBlack();
    }

    // ---- lo derivado: los textos ----

    public ColorUIResource getSystemTextColor() {
        return getBlack();
    }

    public ColorUIResource getControlTextColor() {
        return getControlInfo();
    }

    public ColorUIResource getUserTextColor() {
        return getBlack();
    }

    public ColorUIResource getInactiveSystemTextColor() {
        return getSecondary2();
    }

    public ColorUIResource getInactiveControlTextColor() {
        return getControlDisabled();
    }

    public ColorUIResource getHighlightedTextColor() {
        return getControlTextColor();
    }

    public ColorUIResource getTextHighlightColor() {
        return getPrimaryControl();
    }

    // ---- lo derivado: el fondo ----

    public ColorUIResource getWindowBackground() {
        return getWhite();
    }

    public ColorUIResource getDesktopColor() {
        return getPrimary2();
    }

    public ColorUIResource getFocusColor() {
        return getPrimary2();
    }

    // ---- lo derivado: los menues ----

    public ColorUIResource getMenuBackground() {
        return getSecondary3();
    }

    public ColorUIResource getMenuForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuSelectedBackground() {
        return getPrimary2();
    }

    public ColorUIResource getMenuSelectedForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuDisabledForeground() {
        return getSecondary2();
    }

    public ColorUIResource getAcceleratorForeground() {
        return getPrimary1();
    }

    public ColorUIResource getAcceleratorSelectedForeground() {
        return getBlack();
    }

    // ---- lo derivado: separadores y titulos ----

    /** El separador se dibuja con dos lineas, una clara y una oscura, y de ahi el relieve. */
    public ColorUIResource getSeparatorBackground() {
        return getWhite();
    }

    public ColorUIResource getSeparatorForeground() {
        return getPrimary1();
    }

    public ColorUIResource getWindowTitleBackground() {
        return getPrimary3();
    }

    public ColorUIResource getWindowTitleForeground() {
        return getBlack();
    }

    public ColorUIResource getWindowTitleInactiveBackground() {
        return getSecondary3();
    }

    public ColorUIResource getWindowTitleInactiveForeground() {
        return getBlack();
    }

    /** Nada; ver la nota de la clase. */
    public void addCustomEntriesToTable(UIDefaults table) {
    }
}
