package javax.xml.validation;

/**
 * KajiLibrary's javax.xml.validation.SchemaFactoryLoader -- una fabrica de fabricas.
 *
 * <p>Un solo metodo, que dado el URI de un lenguaje de esquema devuelve la {@link SchemaFactory} que
 * lo entiende. Es un nivel mas de indireccion sobre {@link SchemaFactory#newInstance}, y existe para
 * quien quiera decidir por su cuenta que implementacion usa cada lenguaje en vez de aceptar la
 * busqueda por propiedades y servicios.
 *
 * <p>La plataforma no la usa por su cuenta: no hay ningun {@code newInstance} que la busque. Es una
 * clase para que la instancie y la llame quien la necesita, y por eso no le hace falta ninguna via de
 * registro.
 */
public abstract class SchemaFactoryLoader {

    /** Para las subclases. */
    protected SchemaFactoryLoader() {
    }

    /**
     * La fabrica para ese lenguaje.
     *
     * @param schemaLanguage el URI del lenguaje, por ejemplo
     *     {@code javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI}
     * @return null si no conoce ese lenguaje
     */
    public abstract SchemaFactory newFactory(String schemaLanguage);
}
