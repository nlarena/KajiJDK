package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es un entero <strong>sin signo</strong>, aunque su tipo Java tenga signo.
 *
 * <p>Java no tiene enteros sin signo, asi que un valor grande guardado en un {@code long} sale
 * negativo. Esta anotacion le dice al que lee que lo interprete al reves, y es la unica forma de
 * mostrar bien un valor que vino de codigo nativo.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Unsigned Value")
@Description("Value should be interpreted as unsigned data type")
public @interface Unsigned {
}
