package java.io;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's java.io.Serial — marca un campo o un metodo como parte del mecanismo de
 * serializacion, para que el compilador lo revise.
 *
 * <p>Existe porque los miembros de la serializacion se declaran <b>por convencion de nombre y
 * firma</b>, no por implementar nada: {@code writeObject} es privado, no sobreescribe ni implementa
 * un metodo, y si se lo escribe con la firma equivocada nadie se queja — simplemente no se llama
 * nunca, y el objeto se serializa distinto de como su autor creia. Un {@code serialVersionUID} que
 * no sea {@code private static final long} tiene el mismo problema: se ignora en silencio.
 *
 * <p>Esta anotacion convierte ese error silencioso en un error de compilacion. Es el mismo trato
 * que {@code @Override} le hace a los metodos heredados, y por la misma razon: donde el contrato es
 * una convencion y no un tipo, hace falta algo que lo diga en voz alta.
 *
 * <p>Se aplica a {@code serialVersionUID}, {@code serialPersistentFields}, {@code writeObject},
 * {@code readObject}, {@code readObjectNoData}, {@code writeReplace} y {@code readResolve}.
 *
 * <p>No tiene efecto en tiempo de ejecucion: es {@code SOURCE}, la revisa el compilador y no llega
 * al {@code .class}.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Serial {
}
