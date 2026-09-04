package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.URIDereferencer -- resuelve un {@link URIReference} en datos.
 *
 * <p>Un metodo, y es el punto de control mas importante del paquete. Validar una firma implica ir a
 * buscar lo que las referencias apuntan, y esas referencias las escribio <b>quien firmo</b>.
 *
 * <p>Sin un dereferenciador propio, validar una firma de origen desconocido puede hacer que el
 * programa lea archivos locales o haga pedidos de red que nadie pidio -- el mismo problema que XXE,
 * con otro nombre. Poner uno que solo resuelva referencias internas al documento es la defensa
 * habitual.
 *
 * <p>Se instala en el {@link XMLCryptoContext}, asi que vale para toda la validacion.
 */
public interface URIDereferencer {

    /**
     * Los datos que esa referencia apunta.
     *
     * @throws URIReferenceException si no se puede resolver
     */
    Data dereference(URIReference uriReference, XMLCryptoContext context)
        throws URIReferenceException;
}
