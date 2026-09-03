package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * El escaner de elementos de Java 9. Ver {@link ElementScanner6} por el mecanismo.
 *
 * <p>`visitModule` deja de tirar y baja por los elementos contenidos del modulo, que son sus paquetes.
 *
 * <p>El JDK deja una duda anotada en este mismo metodo sobre si los contenidos son lo correcto para un
 * modulo: un modulo tambien tiene **directivas** — `requires`, `exports` —, y esas no son elementos y no
 * las alcanza ningun recorrido. Vale saberlo antes de confiar en que escanear un modulo lo cubre entero:
 * no lo cubre, y para las directivas hace falta {@link ElementFilter} sobre `getDirectives()`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class ElementScanner9<R, P> extends ElementScanner8<R, P> {

    protected ElementScanner9() {
        super(null);
    }

    protected ElementScanner9(R defaultValue) {
        super(defaultValue);
    }

    public R visitModule(ModuleElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }
}
