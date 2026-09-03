package javax.annotation.processing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marca codigo que **genero** una herramienta, para distinguirlo del que escribio una persona.
//
// La retencion es SOURCE, y esa es la decision de diseno que importa: la marca le sirve al que lee
// el fuente y a las herramientas que lo procesan, no a nadie en tiempo de ejecucion, asi que no
// tiene por que sobrevivir al compilador. (Es tambien la razon por la que el aviso de retencion que
// llevan `@SupportedOptions` y compania no aplica aca: esta anotacion nunca pretendio ser visible en
// ejecucion.)
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR,
          ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER })
public @interface Generated {

    /**
     * El o los nombres del generador. El JDK recomienda el nombre completo de la clase que genero
     * el codigo, para que sea rastreable.
     */
    String[] value();

    /** La fecha de generacion, en ISO 8601. Vacia si la herramienta no la puso. */
    String date() default "";

    /** Cualquier comentario que la herramienta quiera dejar. */
    String comments() default "";
}
