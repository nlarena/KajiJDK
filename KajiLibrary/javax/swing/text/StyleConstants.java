package javax.swing.text;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;

/**
 * Las claves de los atributos que Swing entiende, y los accesores con tipo para leerlas y
 * ponerlas.
 *
 * <h2>Claves que son objetos, no cadenas</h2>
 *
 * <p>Cada clave es una <em>instancia</em> de esta clase, no un {@code String}. Dos ventajas: dos
 * atributos distintos no pueden chocar por llamarse igual, y la clave lleva encima de que familia
 * es —de caracter, de parrafo, de color, de fuente— por las interfaces que implementa
 * ({@link AttributeSet.CharacterAttribute} y compania). Eso ultimo es lo que permite a un editor
 * preguntar "que atributos de parrafo tiene esto" sin una lista escrita a mano.
 *
 * <p>Los accesores estaticos son la unica forma comoda de leer un atributo: el conjunto guarda
 * {@code Object}, y aca esta el casteo y el valor por omision de cada uno. Un atributo que no
 * esta no es un error: se contesta lo que corresponda —{@code false}, cero, el color de frente—.
 *
 * <p>{@link #Family} y {@link #FontFamily} son la misma clave, igual que {@link #Size} y
 * {@link #FontSize}: son dos nombres historicos del mismo objeto.
 */
public class StyleConstants {

    /** El nombre de elemento de un componente incrustado. */
    public static final String ComponentElementName = "component";

    /** El nombre de elemento de un icono incrustado. */
    public static final String IconElementName = "icon";

    /** El nombre del atributo que guarda el nombre de un elemento o estilo. */
    public static final Object NameAttribute = new StyleConstants("name");

    /** El padre de resolucion; ver {@link MutableAttributeSet}. */
    public static final Object ResolveAttribute = new StyleConstants("resolver");

    /** El modelo de un componente incrustado. */
    public static final Object ModelAttribute = new StyleConstants("model");

    /** El nivel bidireccional del texto, para mezclar escrituras de distinto sentido. */
    public static final Object BidiLevel = new CharacterConstants("bidiLevel");

    public static final Object FontFamily = new FontConstants("family");

    /** El otro nombre de {@link #FontFamily}; es el mismo objeto. */
    public static final Object Family = FontFamily;

    public static final Object FontSize = new FontConstants("size");

    /** El otro nombre de {@link #FontSize}; es el mismo objeto. */
    public static final Object Size = FontSize;

    public static final Object Bold = new FontConstants("bold");

    public static final Object Italic = new FontConstants("italic");

    public static final Object Underline = new CharacterConstants("underline");

    public static final Object StrikeThrough = new CharacterConstants("strikethrough");

    public static final Object Superscript = new CharacterConstants("superscript");

    public static final Object Subscript = new CharacterConstants("subscript");

    public static final Object Foreground = new ColorConstants("foreground");

    public static final Object Background = new ColorConstants("background");

    /** El componente incrustado en el texto. */
    public static final Object ComponentAttribute = new CharacterConstants("component");

    /** El icono incrustado en el texto. */
    public static final Object IconAttribute = new CharacterConstants("icon");

    /** El texto en composicion de un metodo de entrada. */
    public static final Object ComposedTextAttribute = new StyleConstants("composed text");

    public static final Object FirstLineIndent = new ParagraphConstants("FirstLineIndent");

    public static final Object LeftIndent = new ParagraphConstants("LeftIndent");

    public static final Object RightIndent = new ParagraphConstants("RightIndent");

    public static final Object LineSpacing = new ParagraphConstants("LineSpacing");

    public static final Object SpaceAbove = new ParagraphConstants("SpaceAbove");

    public static final Object SpaceBelow = new ParagraphConstants("SpaceBelow");

    public static final Object Alignment = new ParagraphConstants("Alignment");

    public static final Object TabSet = new ParagraphConstants("TabSet");

    /** El sentido de la escritura del parrafo. */
    public static final Object Orientation = new ParagraphConstants("Orientation");

    public static final int ALIGN_LEFT = 0;

    public static final int ALIGN_CENTER = 1;

    public static final int ALIGN_RIGHT = 2;

    /** El texto se estira para llegar a los dos margenes. */
    public static final int ALIGN_JUSTIFIED = 3;

    /**
     * Todas las claves, para recorrerlas.
     *
     * <p>Es un arreglo publico y modificable, lo que hoy seria un error de diseno; esta asi en el
     * JDK desde 1.2 y cambiarlo romperia programas.
     */
    public static Object[] keys = {
        NameAttribute, ResolveAttribute, BidiLevel,
        FontFamily, FontSize, Bold, Italic, Underline,
        StrikeThrough, Superscript, Subscript,
        Foreground, Background, ComponentAttribute,
        FirstLineIndent, LeftIndent, RightIndent, LineSpacing,
        SpaceAbove, SpaceBelow, Alignment, TabSet, Orientation,
        ModelAttribute, ComponentElementName, IconElementName,
        IconAttribute, ComposedTextAttribute };

    private String representation;

    /** Una clave nueva con ese nombre para mostrar; solo la usan las anidadas. */
    StyleConstants(String representation) {
        this.representation = representation;
    }

    /** El nombre de la clave; es lo que se ve al imprimir un conjunto de atributos. */
    public String toString() {
        return representation;
    }

    // -- caracter --------------------------------------------------------------------------------

    /** El nivel bidireccional; cero si no esta. */
    public static int getBidiLevel(AttributeSet a) {
        Integer o = (Integer) a.getAttribute(BidiLevel);
        if (o != null) {
            return o.intValue();
        }
        return 0;
    }

    public static void setBidiLevel(MutableAttributeSet a, int o) {
        a.addAttribute(BidiLevel, Integer.valueOf(o));
    }

    /** El componente incrustado, o {@code null}. */
    public static Component getComponent(AttributeSet a) {
        return (Component) a.getAttribute(ComponentAttribute);
    }

    /** Incrusta un componente; tambien pone el nombre de elemento que le corresponde. */
    public static void setComponent(MutableAttributeSet a, Component c) {
        a.addAttribute(AbstractDocument.ElementNameAttribute, ComponentElementName);
        a.addAttribute(ComponentAttribute, c);
    }

    /** El icono incrustado, o {@code null}. */
    public static Icon getIcon(AttributeSet a) {
        return (Icon) a.getAttribute(IconAttribute);
    }

    public static void setIcon(MutableAttributeSet a, Icon c) {
        a.addAttribute(AbstractDocument.ElementNameAttribute, IconElementName);
        a.addAttribute(IconAttribute, c);
    }

    /** La familia tipografica; "Monospaced" si no esta. */
    public static String getFontFamily(AttributeSet a) {
        String family = (String) a.getAttribute(FontFamily);
        if (family == null) {
            family = "Monospaced";
        }
        return family;
    }

    public static void setFontFamily(MutableAttributeSet a, String fam) {
        a.addAttribute(FontFamily, fam);
    }

    /** El cuerpo de la fuente; 12 si no esta. */
    public static int getFontSize(AttributeSet a) {
        Integer size = (Integer) a.getAttribute(FontSize);
        if (size != null) {
            return size.intValue();
        }
        return 12;
    }

    public static void setFontSize(MutableAttributeSet a, int s) {
        a.addAttribute(FontSize, Integer.valueOf(s));
    }

    public static boolean isBold(AttributeSet a) {
        Boolean bold = (Boolean) a.getAttribute(Bold);
        if (bold != null) {
            return bold.booleanValue();
        }
        return false;
    }

    public static void setBold(MutableAttributeSet a, boolean b) {
        a.addAttribute(Bold, Boolean.valueOf(b));
    }

    public static boolean isItalic(AttributeSet a) {
        Boolean italic = (Boolean) a.getAttribute(Italic);
        if (italic != null) {
            return italic.booleanValue();
        }
        return false;
    }

    public static void setItalic(MutableAttributeSet a, boolean b) {
        a.addAttribute(Italic, Boolean.valueOf(b));
    }

    public static boolean isUnderline(AttributeSet a) {
        Boolean underline = (Boolean) a.getAttribute(Underline);
        if (underline != null) {
            return underline.booleanValue();
        }
        return false;
    }

    public static boolean isStrikeThrough(AttributeSet a) {
        Boolean strike = (Boolean) a.getAttribute(StrikeThrough);
        if (strike != null) {
            return strike.booleanValue();
        }
        return false;
    }

    public static boolean isSuperscript(AttributeSet a) {
        Boolean superscript = (Boolean) a.getAttribute(Superscript);
        if (superscript != null) {
            return superscript.booleanValue();
        }
        return false;
    }

    public static boolean isSubscript(AttributeSet a) {
        Boolean subscript = (Boolean) a.getAttribute(Subscript);
        if (subscript != null) {
            return subscript.booleanValue();
        }
        return false;
    }

    public static void setUnderline(MutableAttributeSet a, boolean b) {
        a.addAttribute(Underline, Boolean.valueOf(b));
    }

    public static void setStrikeThrough(MutableAttributeSet a, boolean b) {
        a.addAttribute(StrikeThrough, Boolean.valueOf(b));
    }

    public static void setSuperscript(MutableAttributeSet a, boolean b) {
        a.addAttribute(Superscript, Boolean.valueOf(b));
    }

    public static void setSubscript(MutableAttributeSet a, boolean b) {
        a.addAttribute(Subscript, Boolean.valueOf(b));
    }

    /** El color del texto; negro si no esta. */
    public static Color getForeground(AttributeSet a) {
        Color fg = (Color) a.getAttribute(Foreground);
        if (fg == null) {
            fg = Color.black;
        }
        return fg;
    }

    public static void setForeground(MutableAttributeSet a, Color fg) {
        a.addAttribute(Foreground, fg);
    }

    /** El color del fondo; blanco si no esta. */
    public static Color getBackground(AttributeSet a) {
        Color fg = (Color) a.getAttribute(Background);
        if (fg == null) {
            fg = Color.black;
        }
        return fg;
    }

    public static void setBackground(MutableAttributeSet a, Color fg) {
        a.addAttribute(Background, fg);
    }

    // -- parrafo ---------------------------------------------------------------------------------

    /** La sangria de la primera linea; cero si no esta. */
    public static float getFirstLineIndent(AttributeSet a) {
        Float indent = (Float) a.getAttribute(FirstLineIndent);
        if (indent != null) {
            return indent.floatValue();
        }
        return 0;
    }

    public static void setFirstLineIndent(MutableAttributeSet a, float i) {
        a.addAttribute(FirstLineIndent, Float.valueOf(i));
    }

    public static float getRightIndent(AttributeSet a) {
        Float indent = (Float) a.getAttribute(RightIndent);
        if (indent != null) {
            return indent.floatValue();
        }
        return 0;
    }

    public static void setRightIndent(MutableAttributeSet a, float i) {
        a.addAttribute(RightIndent, Float.valueOf(i));
    }

    public static float getLeftIndent(AttributeSet a) {
        Float indent = (Float) a.getAttribute(LeftIndent);
        if (indent != null) {
            return indent.floatValue();
        }
        return 0;
    }

    public static void setLeftIndent(MutableAttributeSet a, float i) {
        a.addAttribute(LeftIndent, Float.valueOf(i));
    }

    /** El interlineado, como fraccion del alto de la linea; cero si no esta. */
    public static float getLineSpacing(AttributeSet a) {
        Float space = (Float) a.getAttribute(LineSpacing);
        if (space != null) {
            return space.floatValue();
        }
        return 0;
    }

    public static void setLineSpacing(MutableAttributeSet a, float i) {
        a.addAttribute(LineSpacing, Float.valueOf(i));
    }

    public static float getSpaceAbove(AttributeSet a) {
        Float space = (Float) a.getAttribute(SpaceAbove);
        if (space != null) {
            return space.floatValue();
        }
        return 0;
    }

    public static void setSpaceAbove(MutableAttributeSet a, float i) {
        a.addAttribute(SpaceAbove, Float.valueOf(i));
    }

    public static float getSpaceBelow(AttributeSet a) {
        Float space = (Float) a.getAttribute(SpaceBelow);
        if (space != null) {
            return space.floatValue();
        }
        return 0;
    }

    public static void setSpaceBelow(MutableAttributeSet a, float i) {
        a.addAttribute(SpaceBelow, Float.valueOf(i));
    }

    /** La alineacion; a la izquierda si no esta. */
    public static int getAlignment(AttributeSet a) {
        Integer align = (Integer) a.getAttribute(Alignment);
        if (align != null) {
            return align.intValue();
        }
        return ALIGN_LEFT;
    }

    public static void setAlignment(MutableAttributeSet a, int align) {
        a.addAttribute(Alignment, Integer.valueOf(align));
    }

    /** Las paradas de tabulacion del parrafo, o {@code null}. */
    public static TabSet getTabSet(AttributeSet a) {
        return (TabSet) a.getAttribute(StyleConstants.TabSet);
    }

    public static void setTabSet(MutableAttributeSet a, TabSet tabs) {
        a.addAttribute(StyleConstants.TabSet, tabs);
    }

    /** Una clave de atributo de caracter. */
    public static final class CharacterConstants extends StyleConstants
            implements AttributeSet$CharacterAttribute {

        private CharacterConstants(String representation) {
            super(representation);
        }
    }

    /** Una clave de color; tambien es de caracter. */
    public static final class ColorConstants extends StyleConstants
            implements AttributeSet$ColorAttribute, AttributeSet$CharacterAttribute {

        private ColorConstants(String representation) {
            super(representation);
        }
    }

    /** Una clave de fuente; tambien es de caracter. */
    public static final class FontConstants extends StyleConstants
            implements AttributeSet$FontAttribute, AttributeSet$CharacterAttribute {

        private FontConstants(String representation) {
            super(representation);
        }
    }

    /** Una clave de atributo de parrafo. */
    public static final class ParagraphConstants extends StyleConstants
            implements AttributeSet$ParagraphAttribute {

        private ParagraphConstants(String representation) {
            super(representation);
        }
    }
}
