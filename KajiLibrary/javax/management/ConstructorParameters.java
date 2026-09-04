package javax.management;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Le pone nombre a los parametros de un constructor para poder reconstruir el objeto.
 *
 * <p>El problema que resuelve: para volver de un `CompositeData` al objeto Java hay que saber que
 * item corresponde a que parametro del constructor, y el bytecode no guarda los nombres de los
 * parametros salvo que se compile con `-parameters`. La anotacion los declara a mano, en el mismo
 * orden que la firma.
 *
 * <p>Nota de version: es la sucesora de `java.beans.ConstructorProperties` y existe justamente para
 * no arrastrar `java.desktop` dentro de `java.management`.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface ConstructorParameters {

    /** Los nombres, en el orden de los parametros del constructor. */
    String[] value();
}
