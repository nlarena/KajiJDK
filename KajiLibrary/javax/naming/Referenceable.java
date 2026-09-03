package javax.naming;

/**
 * Lo implementa el objeto que sabe decir como reconstruirse.
 *
 * <p>Atar un objeto vivo en un servicio de nombres no se puede: el servicio guarda datos, no
 * memoria de otro proceso. Lo que se guarda es una `Reference` --el nombre de una clase fabrica
 * mas un par de direcciones-- y el que despues hace `lookup` la recibe y **reconstruye** el
 * objeto. Un `DataSource`, por ejemplo, se ata como "esta fabrica, con esta URL y este usuario".
 *
 * <p>La diferencia con `Serializable` es que la reconstruccion la maneja el objeto y no el
 * mecanismo: `getReference()` decide que datos hacen falta, y suelen ser muchos menos que el
 * estado completo.
 */
public interface Referenceable {

    Reference getReference() throws NamingException;
}
