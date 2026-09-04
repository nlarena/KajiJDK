package javax.print;

/**
 * KajiLibrary's javax.print.ServiceUIFactory -- la interfaz grafica propia de una impresora.
 *
 * <p>Un fabricante puede tener opciones que el modelo de atributos estandar no cubre, y esta fabrica es
 * como las expone: devuelve componentes ya armados por el driver.
 *
 * <p>Se pide por <b>rol</b> --para que sirve la pantalla-- y por <b>tipo</b> --de que clase se quiere
 * el componente--, y ese cruce es el punto: el mismo panel de administracion se puede pedir como
 * {@link #JCOMPONENT_UI} para meterlo en una ventana propia, o como {@link #DIALOG_UI} para mostrarlo
 * suelto.
 *
 * <p>{@link #getUI} devuelve null si esa combinacion no existe, que es lo normal. {@link
 * #getUIClassNamesForRole} sirve para preguntar antes.
 *
 * <p>Los tipos son cadenas y no clases para que pedir uno de Swing no obligue a cargar Swing.
 */
public abstract class ServiceUIFactory {

    /** Un {@code javax.swing.JComponent}. */
    public static final String JCOMPONENT_UI = "javax.swing.JComponent";

    /** Un {@code java.awt.Panel}. */
    public static final String PANEL_UI = "java.awt.Panel";

    /** Un {@code java.awt.Dialog}. */
    public static final String DIALOG_UI = "java.awt.Dialog";

    /** Un {@code javax.swing.JDialog}. */
    public static final String JDIALOG_UI = "javax.swing.JDialog";

    /** Pantalla de "acerca de". */
    public static final int ABOUT_UIROLE = 1;

    /** Pantalla de administracion. */
    public static final int ADMIN_UIROLE = 2;

    /** La pantalla principal. */
    public static final int MAIN_UIROLE = 3;

    /** El primer rol libre para roles propios; los menores estan reservados. */
    public static final int RESERVED_UIROLE = 99;

    /** Para las subclases. */
    protected ServiceUIFactory() {
    }

    /**
     * El componente de ese rol y ese tipo, o null si no hay.
     *
     * @param role uno de los roles, o uno propio mayor que {@link #RESERVED_UIROLE}
     * @param ui uno de los cuatro tipos
     */
    public abstract Object getUI(int role, String ui);

    /** Que tipos hay para ese rol, o null si no hay ninguno. */
    public abstract String[] getUIClassNamesForRole(int role);
}
