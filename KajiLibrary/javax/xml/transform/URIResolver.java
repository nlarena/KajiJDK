package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.URIResolver -- quien decide que hay del otro lado de un `href`.
 *
 * <p>Una hoja de estilo trae referencias a otros documentos: `&lt;xsl:import href="base.xsl"/&gt;`,
 * `&lt;xsl:include&gt;`, y la funcion `document()` de XPath. El procesador no las resuelve solo:
 * se las pasa a este objeto, y usa lo que devuelva. Ese es todo el punto de la interfaz --
 * **interponerse entre una URI y su contenido**.
 *
 * <p>Que se gana con esa indireccion, que es lo que justifica que exista: se puede servir la hoja
 * de estilo desde un catalogo en memoria o desde el jar de la aplicacion en vez de la red; se puede
 * cachear; y sobre todo se puede **negar** el acceso, que es la defensa habitual contra que un
 * documento hostil se traiga archivos del disco o abra conexiones salientes.
 *
 * <p>El contrato de {@link #resolve} tiene un detalle que se pasa por alto: devolver {@code null}
 * **no es un error**. Significa "resolvelo vos como sabes", y el procesador vuelve a su mecanismo
 * por omision. Para prohibir de verdad hay que lanzar {@link TransformerException}.
 */
public interface URIResolver {

    /**
     * El documento que corresponde a {@code href} resuelto contra {@code base}.
     *
     * @param href la URI tal como aparece en el documento, posiblemente relativa
     * @param base la URI base contra la cual resolverla
     * @return la fuente a usar, o null para dejar que el procesador resuelva por su cuenta
     * @throws TransformerException si la referencia no se puede o no se debe seguir
     */
    Source resolve(String href, String base) throws TransformerException;
}
