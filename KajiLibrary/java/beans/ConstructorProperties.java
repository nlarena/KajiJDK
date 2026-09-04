package java.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Dice a que propiedades corresponden los parametros de un constructor, en orden. Es lo que deja
// reconstruir un objeto INMUTABLE: sin setters, la unica forma de volver a armarlo es pasarle los
// valores al constructor, y hace falta saber cual parametro es cual propiedad.
//
// Nota: declarada pero no leida en este arbol; ver el encabezado de Introspector.
@Target({ ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConstructorProperties {

    String[] value();
}
