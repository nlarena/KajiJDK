package javax.swing.plaf.synth;

import java.awt.Graphics;

/**
 * Who draws each part of each component.
 *
 * <h2>A hundred and thirty-six methods that do nothing</h2>
 *
 * <p>Every method of this class is empty, and that is what it has to be. A look and feel
 * redefines the ones that interest it --a button's background, a text field's border-- and
 * inherits the rest empty, which is how "I do not draw that part" is said.
 *
 * <p>If they were abstract, writing a look and feel that only changes the colour of the buttons
 * would force writing a hundred and thirty-five empty methods by hand. If they threw an
 * exception, any component the look and feel did not foresee would stop working instead of
 * looking like the default.
 *
 * <h2>Background, border and foreground</h2>
 *
 * <p>Almost every part has three: the background is drawn first, the border on top, and the
 * foreground at the end. They are separate because a look and feel usually wants to change only
 * one.
 *
 * <p>Those that carry a fourth orientation parameter --{@code paintArrowButtonForeground}, the
 * bars' ones-- need it because the same part is drawn differently according to which way it
 * points.
 *
 * @since 1.5
 */
public abstract class SynthPainter {

    /** One; the subclasses redefine what they draw. */
    public SynthPainter() {
    }

    /**
     * It draws arrow button's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintArrowButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws arrow button's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintArrowButtonBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws arrow button's foreground.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintArrowButtonForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws button's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws button's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintButtonBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws check box menu item's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintCheckBoxMenuItemBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws check box menu item's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintCheckBoxMenuItemBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws check box's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintCheckBoxBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws check box's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintCheckBoxBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws color chooser's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintColorChooserBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws color chooser's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintColorChooserBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws combo box's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintComboBoxBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws combo box's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintComboBoxBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws desktop icon's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintDesktopIconBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws desktop icon's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintDesktopIconBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws desktop pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintDesktopPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws desktop pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintDesktopPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws editor pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintEditorPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws editor pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintEditorPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws file chooser's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintFileChooserBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws file chooser's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintFileChooserBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws formatted text field's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintFormattedTextFieldBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws formatted text field's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintFormattedTextFieldBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws internal frame title pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintInternalFrameTitlePaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws internal frame title pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintInternalFrameTitlePaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws internal frame's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintInternalFrameBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws internal frame's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintInternalFrameBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws label's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintLabelBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws label's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintLabelBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws list's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintListBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws list's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintListBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws menu bar's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintMenuBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws menu bar's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintMenuBarBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws menu item's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintMenuItemBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws menu item's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintMenuItemBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws menu's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintMenuBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws menu's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintMenuBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws option pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintOptionPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws option pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintOptionPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws panel's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintPanelBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws panel's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintPanelBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws password field's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintPasswordFieldBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws password field's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintPasswordFieldBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws popup menu's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintPopupMenuBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws popup menu's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintPopupMenuBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws progress bar's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintProgressBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws progress bar's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintProgressBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws progress bar's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintProgressBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws progress bar's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintProgressBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws progress bar's foreground.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintProgressBarForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws radio button menu item's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintRadioButtonMenuItemBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws radio button menu item's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintRadioButtonMenuItemBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws radio button's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintRadioButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws radio button's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintRadioButtonBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws root pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintRootPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws root pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintRootPaneBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws scroll bar's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintScrollBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws scroll bar's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintScrollBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws scroll bar's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintScrollBarBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws scroll bar's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintScrollBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws scroll bar thumb's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintScrollBarThumbBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws scroll bar thumb's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintScrollBarThumbBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws scroll bar track's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintScrollBarTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws scroll bar track's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintScrollBarTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws scroll bar track's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintScrollBarTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws scroll bar track's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintScrollBarTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws scroll pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintScrollPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws scroll pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintScrollPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws separator's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSeparatorBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws separator's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSeparatorBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws separator's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSeparatorBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws separator's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSeparatorBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws separator's foreground.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSeparatorForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws slider's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSliderBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws slider's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSliderBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws slider's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSliderBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws slider's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSliderBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws slider thumb's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSliderThumbBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws slider thumb's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSliderThumbBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws slider track's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSliderTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws slider track's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSliderTrackBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws slider track's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSliderTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws slider track's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSliderTrackBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws spinner's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSpinnerBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws spinner's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSpinnerBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws split pane divider's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSplitPaneDividerBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws split pane divider's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSplitPaneDividerBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws split pane divider's foreground.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSplitPaneDividerForeground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws that part.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintSplitPaneDragDivider(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws split pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSplitPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws split pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintSplitPaneBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tabbed pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTabbedPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tabbed pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTabbedPaneBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tabbed pane tab area's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTabbedPaneTabAreaBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tabbed pane tab area's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintTabbedPaneTabAreaBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tabbed pane tab area's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTabbedPaneTabAreaBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tabbed pane tab area's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintTabbedPaneTabAreaBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tabbed pane tab's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintTabbedPaneTabBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tabbed pane tab's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     * @param a7 the {@code int}
     */
    public void paintTabbedPaneTabBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation, int a7) {
    }

    /**
     * It draws tabbed pane tab's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintTabbedPaneTabBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tabbed pane tab's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     * @param a7 the {@code int}
     */
    public void paintTabbedPaneTabBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation, int a7) {
    }

    /**
     * It draws tabbed pane content's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTabbedPaneContentBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tabbed pane content's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTabbedPaneContentBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws table header's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTableHeaderBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws table header's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTableHeaderBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws table's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTableBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws table's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTableBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws text area's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTextAreaBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws text area's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTextAreaBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws text pane's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTextPaneBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws text pane's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTextPaneBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws text field's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTextFieldBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws text field's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTextFieldBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws toggle button's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToggleButtonBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws toggle button's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToggleButtonBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool bar's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool bar's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintToolBarBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tool bar's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolBarBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool bar's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintToolBarBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tool bar content's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolBarContentBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool bar content's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintToolBarContentBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tool bar content's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolBarContentBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool bar content's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintToolBarContentBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tool bar drag window's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolBarDragWindowBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool bar drag window's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintToolBarDragWindowBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tool bar drag window's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolBarDragWindowBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool bar drag window's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     * @param orientation which way it goes: one of {@code SwingConstants}' constants
     */
    public void paintToolBarDragWindowBorder(
            SynthContext context, Graphics g, int x, int y, int w, int h, int orientation) {
    }

    /**
     * It draws tool tip's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolTipBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tool tip's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintToolTipBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tree's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTreeBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tree's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTreeBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tree cell's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTreeCellBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws tree cell's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTreeCellBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws that part.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintTreeCellFocus(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws viewport's background.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintViewportBackground(
            SynthContext context, Graphics g, int x, int y, int w, int h) {
    }

    /**
     * It draws viewport's border.
     *
     * @param context what is being drawn and in what state
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void paintViewportBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
    }
}
