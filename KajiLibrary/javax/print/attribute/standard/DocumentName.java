package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.TextSyntax;

/*
 * CABECERA DE FAMILIA -- los atributos {@code TextSyntax} de este paquete.
 *
 * <p>Texto libre con el idioma en el que esta escrito. El par (cadena, locale) va junto porque un
 * nombre de trabajo o un mensaje del operador se muestran a una persona, y sin saber el idioma no
 * se puede ni ordenarlos ni partirlos en lineas bien.
 *
 * <p>El mecanismo esta en {@link javax.print.attribute.TextSyntax TextSyntax}: la cadena null es
 * error, el locale null significa "el de por aca" y se resuelve al default en el constructor.
 * {@code toString()} devuelve la cadena pelada, sin el locale.
 *
 * <p>A diferencia de {@link javax.print.attribute.EnumSyntax EnumSyntax}, aca el valor no viene de
 * ninguna tabla: es lo que el usuario escribio. Lo unico que cada subclase aporta es que pregunta
 * contesta ese texto.
 */

/**
 * El nombre de un documento suelto dentro del trabajo.
 *
 * <p>Un trabajo con varios documentos le pone uno a cada uno; el nombre del trabajo entero es
 * {@link JobName}.
 */
public final class DocumentName extends TextSyntax implements DocAttribute {

    private static final long serialVersionUID = 7883105848533280430L;

    public DocumentName(String documentName, Locale locale) {
        super(documentName, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof DocumentName;
    }

    public final Class<? extends Attribute> getCategory() {
        return DocumentName.class;
    }

    public final String getName() {
        return "document-name";
    }
}
