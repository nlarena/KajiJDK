package javax.naming;

/**
 * El que sabe convertir una cadena en un `Name` **con la sintaxis de un espacio de nombres**.
 *
 * <p>Existe porque la sintaxis no es del que llama sino del proveedor: para parsear un nombre de
 * LDAP hay que saber que se separa con coma, que se cita con comillas y que el orden es de derecha
 * a izquierda. En vez de exponer esas propiedades, `Context.getNameParser()` devuelve un objeto
 * que ya las tiene adentro, y el que llama solo le pasa la cadena.
 *
 * <p>Dos contextos distintos pueden devolver el **mismo** parser, y ahi el contrato dice algo
 * util: si `p1.equals(p2)`, los dos nombres viven en el mismo espacio de nombres y se pueden
 * comparar entre si. Es la unica manera de saberlo sin preguntarle al servidor.
 */
public interface NameParser {

    Name parse(String name) throws NamingException;
}
