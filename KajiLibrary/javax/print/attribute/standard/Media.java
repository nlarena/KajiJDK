package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * La raiz de las tres maneras de decir "que papel": por {@link MediaSizeName tamano}, por
 * {@link MediaName nombre} o por {@link MediaTray bandeja}.
 *
 * <p>Lo interesante es que las tres reportan {@code Media.class} como categoria, no su propia
 * clase. Eso no es una simplificacion: es lo que hace que un conjunto de atributos no pueda llevar
 * a la vez "A4" y "bandeja manual", porque las dos son respuestas a <b>una sola</b> pregunta y en
 * un {@code AttributeSet} la categoria es la clave. Sin este truco un trabajo podria pedir dos
 * papeles contradictorios y nadie lo notaria hasta la impresora.
 *
 * <p>Por lo mismo {@code equals()} no alcanza con comparar el entero, como hace el resto de la
 * familia {@code EnumSyntax}: {@code MediaTray.TOP} y {@code MediaName.NA_LETTER_WHITE} valen los
 * dos cero, y sin mirar la clase concreta darian iguales. Ver la cabecera de familia en
 * {@link Chromaticity} para el mecanismo comun.
 */
public abstract class Media extends EnumSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -2823970704630722439L;

    protected Media(int value) {
        super(value);
    }

    /** Igual valor <b>y</b> misma clase concreta: ver la nota de la cabecera. */
    public boolean equals(Object object) {
        return object != null
            && object instanceof Media
            && object.getClass() == this.getClass()
            && ((Media) object).getValue() == this.getValue();
    }

    public final Class<? extends Attribute> getCategory() {
        return Media.class;
    }

    public final String getName() {
        return "media";
    }
}
