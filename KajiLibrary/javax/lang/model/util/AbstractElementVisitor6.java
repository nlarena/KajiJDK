package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.UnknownElementException;

/**
 * KajiLibrary's javax.lang.model.util.AbstractElementVisitor6 — la base de la familia de visitantes de
 * elementos, y el lugar donde se resuelve el problema de evolucionar una interfaz de visitante.
 *
 * <h2>Por que hay una clase por version del lenguaje</h2>
 *
 * <p>{@link ElementVisitor} tiene un metodo por cada clase de declaracion que el lenguaje conoce. Pero
 * el lenguaje crece: los modulos llegaron en 9 y los componentes de registro en 16. Agregarle un metodo
 * abstracto a la interfaz habria roto **todos** los visitantes ya escritos, y darle un `default` que
 * devolviera cualquier cosa habria hecho que un visitante viejo tratara en silencio un modulo como si no
 * fuera nada.
 *
 * <p>La salida es esta familia. Cada `AbstractElementVisitorN` fija el contrato de la version `N` del
 * lenguaje: **lo que existia en `N` es abstracto y hay que implementarlo; lo que llego despues tiene un
 * cuerpo que tira**. Quien extiende `AbstractElementVisitor6` promete manejar lo que habia en Java 6, y
 * si le llega un modulo se entera con una excepcion en vez de recibir un resultado inventado. Quien
 * quiere manejar modulos extiende `AbstractElementVisitor9`, donde `visitModule` es abstracto y el
 * compilador lo obliga a escribirlo.
 *
 * <h2>Por que `visitUnknown` tirando no es un miembro que miente</h2>
 *
 * <p>Es la unica respuesta honesta. El visitante fue escrito contra un lenguaje que no tenia esa
 * construccion; no hay ningun valor de `R` que signifique "no se que es esto".
 * {@link UnknownElementException} dice exactamente eso, y el que quiera otra cosa redefine
 * `visitUnknown` — que para eso no es final.
 *
 * <p>{@link #visit(Element, Object)} si es final, y a proposito: es el punto de entrada y su cuerpo es
 * siempre el mismo despacho doble contra {@link Element#accept}. Redefinirlo solo podria romperlo.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class AbstractElementVisitor6<R, P> implements ElementVisitor<R, P> {

    protected AbstractElementVisitor6() {
    }

    /** El despacho: la que sabe que clase de elemento es, es la implementacion de `accept`. */
    public final R visit(Element e, P p) {
        return e.accept(this, p);
    }

    /** Igual, con parametro nulo, para los visitantes a los que `P` no les importa. */
    public final R visit(Element e) {
        return e.accept(this, null);
    }

    public R visitUnknown(Element e, P p) {
        throw new UnknownElementException(e, p);
    }

    // Los modulos son de 9 y los componentes de registro de 16: los dos son desconocidos para un
    // visitante de 6. El cuerpo repite el del `default` de `ElementVisitor` en vez de delegarle con
    // `ElementVisitor.super`, porque nuestro javac todavia no acepta esa sintaxis (COMPILER_FINDINGS
    // #400). Es la misma llamada: las dos formas terminan en el `visitUnknown` que la subclase tenga.

    public R visitModule(ModuleElement e, P p) {
        return this.visitUnknown(e, p);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.visitUnknown(e, p);
    }
}
