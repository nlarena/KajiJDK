package javax.xml.validation;

import org.w3c.dom.TypeInfo;

/**
 * KajiLibrary's javax.xml.validation.TypeInfoProvider -- que tipo tenia lo que acaba de pasar.
 *
 * <p>Es lo que hace que validar con {@link ValidatorHandler} sirva para algo mas que decir si el
 * documento esta bien: mientras la validacion avanza, esto dice de que <b>tipo de esquema</b> es
 * cada elemento y cada atributo. Con eso, quien escucha puede convertir el texto al tipo que
 * corresponde en vez de adivinar por la forma.
 *
 * <h2>Solo vale adentro de la llamada</h2>
 *
 * <p>Es la regla que hay que tener presente y la que rompe a quien lo usa mal: los metodos de
 * elemento solo se pueden llamar desde {@code startElement} o {@code endElement}, y los de atributo
 * solo desde {@code startElement}. Guardarse el {@code TypeInfoProvider} y preguntarle despues no da
 * un valor viejo: da un {@code IllegalStateException}.
 *
 * <p>Tiene sentido -- no hay nada guardado. El proveedor es una ventana al estado del validador en
 * ese instante, y por eso no cuesta nada tenerlo: si tuviera que guardar los tipos de todo el
 * documento para poder contestar mas tarde, seria justamente lo que la via SAX quiere evitar.
 *
 * <p>Los atributos se piden <b>por indice</b>, el mismo del {@code Attributes} que llego a
 * {@code startElement}.
 */
public abstract class TypeInfoProvider {

    /** Para las subclases. */
    protected TypeInfoProvider() {
    }

    /**
     * El tipo del elemento actual.
     *
     * @throws IllegalStateException fuera de {@code startElement} o {@code endElement}
     */
    public abstract TypeInfo getElementTypeInfo();

    /**
     * El tipo de ese atributo del elemento actual.
     *
     * @param index el indice en el {@code Attributes} de {@code startElement}
     * @throws IllegalStateException fuera de {@code startElement}
     * @throws IndexOutOfBoundsException si el indice no existe
     */
    public abstract TypeInfo getAttributeTypeInfo(int index);

    /**
     * Si ese atributo es de tipo identificador.
     *
     * <p>Es la pregunta que decide si el valor sirve para {@code getElementById}, y no se puede
     * contestar sin el esquema: en un documento sin DTD ni esquema, ningun atributo es identificador
     * por mas que se llame {@code id}.
     */
    public abstract boolean isIdAttribute(int index);

    /**
     * Si el atributo estaba <b>escrito</b> en el documento.
     *
     * <p>False significa que lo puso el esquema como valor por omision. La diferencia importa cuando
     * hay que reescribir el documento: emitir los valores por omision lo cambia, y emitirlos con otro
     * esquema puede cambiarle el significado.
     */
    public abstract boolean isSpecified(int index);
}
