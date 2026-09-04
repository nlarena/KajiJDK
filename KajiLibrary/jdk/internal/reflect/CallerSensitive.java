package jdk.internal.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's jdk.internal.reflect.CallerSensitive -- marca un metodo cuyo comportamiento depende
 * de <b>quien lo llamo</b>.
 *
 * <p>{@code Class.forName(String)} carga con el cargador de su llamador. {@code MethodHandles.lookup()}
 * devuelve los permisos de su llamador. Ninguno de los dos puede contestar sin mirar la pila, y por
 * eso llaman a {@link Reflection#getCallerClass()}.
 *
 * <h2>Por que hace falta la marca</h2>
 *
 * <p>{@code getCallerClass()} devuelve el cuadro de arriba del que la llama, y eso se rompe si algo
 * se mete en el medio: invocar {@code Class.forName} por reflexion pondria los cuadros de la
 * maquinaria de reflexion entre el llamador de verdad y el metodo, y el resultado seria el cargador
 * de esa maquinaria en vez del de quien pregunto. La marca es lo que le dice al runtime "cuando
 * invoques esto reflexivamente, no cambies quien parece el llamador".
 *
 * <p>De ahi sale su condicion mas importante, y es una que no se puede verificar sola: un metodo
 * marcado <b>no debe</b> ser publico y a la vez delegar en otro marcado, porque entonces el segundo
 * veria como llamador al primero y no al de afuera. El JDK la revisa con una herramienta aparte.
 *
 * <p>Retencion en runtime porque {@link Reflection#isCallerSensitive} la lee de un
 * {@code java.lang.reflect.Method}, no del codigo fuente.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CallerSensitive {
}
