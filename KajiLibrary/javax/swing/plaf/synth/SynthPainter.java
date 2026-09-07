package javax.swing.plaf.synth;

import java.awt.Graphics;

/**
 * Quien dibuja cada parte de cada componente.
 *
 * <h2>Ciento treinta y seis metodos que no hacen nada</h2>
 *
 * <p>Todos los metodos de esta clase estan vacios, y eso es lo que tiene que ser. Un aspecto grafico
 * redefine los que le interesan --el fondo de un boton, el borde de un campo de texto-- y hereda
 * vacios los demas, que es como se dice "esa parte no la dibujo yo".
 *
 * <p>Si fueran abstractos, escribir un aspecto que solo cambia el color de los botones obligaria a
 * escribir ciento treinta y cinco metodos vacios a mano. Si lanzaran una excepcion, cualquier
 * componente que el aspecto no previo dejaria de funcionar en vez de verse por omision.
 *
 * <h2>Fondo, borde y primer plano</h2>
 *
 * <p>Casi toda parte tiene tres: el fondo se dibuja primero, el borde encima, y el primer plano al
 * final. Estan separados porque un aspecto suele querer cambiar uno solo.
 *
 * <p>Los que llevan un cuarto parametro de orientacion --{@code paintArrowButtonForeground}, los de
 * las barras-- lo necesitan porque la misma parte se dibuja distinto segun para donde apunte.
 *
 * @since 1.5
 */
public abstract class SynthPainter {

    /** Uno; las subclases redefinen lo que dibujan. */
    public SynthPainter() {
    }

    /**
     * Dibuja el fondo de arrow button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintArrowButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de arrow button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintArrowButtonBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el primer plano de arrow button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintArrowButtonForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintButtonBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de check box menu item.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintCheckBoxMenuItemBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de check box menu item.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintCheckBoxMenuItemBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de check box.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintCheckBoxBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de check box.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintCheckBoxBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de color chooser.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintColorChooserBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de color chooser.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintColorChooserBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de combo box.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintComboBoxBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de combo box.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintComboBoxBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de desktop icon.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintDesktopIconBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de desktop icon.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintDesktopIconBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de desktop pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintDesktopPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de desktop pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintDesktopPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de editor pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintEditorPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de editor pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintEditorPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de file chooser.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintFileChooserBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de file chooser.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintFileChooserBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de formatted text field.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintFormattedTextFieldBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de formatted text field.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintFormattedTextFieldBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de internal frame title pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintInternalFrameTitlePaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de internal frame title pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintInternalFrameTitlePaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de internal frame.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintInternalFrameBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de internal frame.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintInternalFrameBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de label.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintLabelBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de label.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintLabelBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de list.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintListBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de list.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintListBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de menu bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintMenuBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de menu bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintMenuBarBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de menu item.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintMenuItemBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de menu item.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintMenuItemBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de menu.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintMenuBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de menu.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintMenuBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de option pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintOptionPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de option pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintOptionPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de panel.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintPanelBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de panel.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintPanelBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de password field.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintPasswordFieldBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de password field.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintPasswordFieldBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de popup menu.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintPopupMenuBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de popup menu.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintPopupMenuBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de progress bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintProgressBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de progress bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintProgressBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de progress bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintProgressBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de progress bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintProgressBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el primer plano de progress bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintProgressBarForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de radio button menu item.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintRadioButtonMenuItemBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de radio button menu item.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintRadioButtonMenuItemBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de radio button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintRadioButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de radio button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintRadioButtonBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de root pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintRootPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de root pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintRootPaneBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de scroll bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintScrollBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de scroll bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintScrollBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de scroll bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintScrollBarBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de scroll bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintScrollBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de scroll bar thumb.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintScrollBarThumbBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de scroll bar thumb.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintScrollBarThumbBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de scroll bar track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintScrollBarTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de scroll bar track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintScrollBarTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de scroll bar track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintScrollBarTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de scroll bar track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintScrollBarTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de scroll pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintScrollPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de scroll pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintScrollPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de separator.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSeparatorBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de separator.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSeparatorBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de separator.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSeparatorBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de separator.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSeparatorBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el primer plano de separator.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSeparatorForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de slider.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSliderBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de slider.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSliderBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de slider.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSliderBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de slider.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSliderBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de slider thumb.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSliderThumbBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de slider thumb.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSliderThumbBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de slider track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSliderTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de slider track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSliderTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de slider track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSliderTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de slider track.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSliderTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de spinner.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSpinnerBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de spinner.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSpinnerBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de split pane divider.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSplitPaneDividerBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de split pane divider.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSplitPaneDividerBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el primer plano de split pane divider.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSplitPaneDividerForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja esa parte.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintSplitPaneDragDivider(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de split pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSplitPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de split pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintSplitPaneBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tabbed pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTabbedPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tabbed pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTabbedPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tabbed pane tab area.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTabbedPaneTabAreaBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tabbed pane tab area.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintTabbedPaneTabAreaBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de tabbed pane tab area.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTabbedPaneTabAreaBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tabbed pane tab area.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintTabbedPaneTabAreaBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de tabbed pane tab.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintTabbedPaneTabBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de tabbed pane tab.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     * @param a7 el {@code int}
     */
    public void paintTabbedPaneTabBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation, int a7) {
    }

    /**
     * Dibuja el borde de tabbed pane tab.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintTabbedPaneTabBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de tabbed pane tab.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     * @param a7 el {@code int}
     */
    public void paintTabbedPaneTabBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation, int a7) {
    }

    /**
     * Dibuja el fondo de tabbed pane content.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTabbedPaneContentBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tabbed pane content.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTabbedPaneContentBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de table header.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTableHeaderBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de table header.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTableHeaderBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de table.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTableBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de table.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTableBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de text area.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTextAreaBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de text area.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTextAreaBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de text pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTextPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de text pane.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTextPaneBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de text field.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTextFieldBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de text field.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTextFieldBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de toggle button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToggleButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de toggle button.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToggleButtonBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tool bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tool bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintToolBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de tool bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolBarBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tool bar.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintToolBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de tool bar content.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolBarContentBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tool bar content.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintToolBarContentBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de tool bar content.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolBarContentBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tool bar content.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintToolBarContentBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de tool bar drag window.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolBarDragWindowBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tool bar drag window.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintToolBarDragWindowBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el borde de tool bar drag window.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolBarDragWindowBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tool bar drag window.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     * @param orientation para donde va: una de las constantes de {@code SwingConstants}
     */
    public void paintToolBarDragWindowBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * Dibuja el fondo de tool tip.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolTipBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tool tip.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintToolTipBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tree.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTreeBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tree.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTreeBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de tree cell.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTreeCellBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de tree cell.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTreeCellBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja esa parte.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintTreeCellFocus(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el fondo de viewport.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintViewportBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * Dibuja el borde de viewport.
     *
     * @param context que se esta dibujando y en que estado
     * @param g donde dibujar
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void paintViewportBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }
}
