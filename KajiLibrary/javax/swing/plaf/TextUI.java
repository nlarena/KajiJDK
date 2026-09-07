package javax.swing.plaf;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * El aspecto de un componente de texto: el puente entre el componente y su arbol de vistas.
 *
 * <p>Es el UI de Swing que mas metodos agrega, y por una razon: el componente no sabe nada de como
 * se ve su texto. Donde cae una posicion, que posicion hay en un punto, que hay que repintar
 * cuando cambia un tramo, todo eso lo sabe el arbol de vistas, y el UI es quien lo tiene.
 *
 * <p>Los pares {@code modelToView}/{@code modelToView2D} y
 * {@code viewToModel}/{@code viewToModel2D} son lo mismo con coordenadas enteras o fraccionarias;
 * las fraccionarias llegaron despues, para pantallas donde un pixel logico no es uno fisico.
 */
public abstract class TextUI extends ComponentUI {

    protected TextUI() {
    }

    /** @deprecated es {@link #modelToView2D}. */
    @Deprecated
    public abstract Rectangle modelToView(JTextComponent t, int pos) throws BadLocationException;

    /** @deprecated es {@link #modelToView2D}. */
    @Deprecated
    public abstract Rectangle modelToView(JTextComponent t, int pos, Position.Bias bias)
            throws BadLocationException;

    /** Donde cae esa posicion del documento, en coordenadas del componente. */
    public Rectangle2D modelToView2D(JTextComponent t, int pos, Position.Bias bias)
            throws BadLocationException {
        return modelToView(t, pos, bias);
    }

    /** @deprecated es {@link #viewToModel2D}. */
    @Deprecated
    public abstract int viewToModel(JTextComponent t, Point pt);

    /** @deprecated es {@link #viewToModel2D}. */
    @Deprecated
    public abstract int viewToModel(JTextComponent t, Point pt, Position.Bias[] biasReturn);

    /** Que posicion del documento hay en ese punto. */
    public int viewToModel2D(JTextComponent t, Point2D pt, Position.Bias[] biasReturn) {
        return viewToModel(t, new Point((int) pt.getX(), (int) pt.getY()), biasReturn);
    }

    /** A donde va el cursor desde esa posicion en esa direccion. */
    public abstract int getNextVisualPositionFrom(JTextComponent t, int pos, Position.Bias b,
            int direction, Position.Bias[] biasRet) throws BadLocationException;

    /** Marca ese tramo como que hay que repintarlo. */
    public abstract void damageRange(JTextComponent t, int p0, int p1);

    public abstract void damageRange(JTextComponent t, int p0, int p1, Position.Bias firstBias,
            Position.Bias secondBias);

    /** El juego de herramientas de edicion que el componente usa. */
    public abstract EditorKit getEditorKit(JTextComponent t);

    /** La raiz del arbol de vistas. */
    public abstract View getRootView(JTextComponent t);

    /** @deprecated es {@link #getToolTipText2D}. */
    @Deprecated
    public String getToolTipText(JTextComponent t, Point pt) {
        return null;
    }

    /** El texto de ayuda en ese punto, si el contenido tiene alguno. */
    public String getToolTipText2D(JTextComponent t, Point2D pt) {
        return getToolTipText(t, new Point((int) pt.getX(), (int) pt.getY()));
    }
}
