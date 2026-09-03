package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de elementos de las construcciones en **vista previa**. Ver
 * {@link AbstractElementVisitor6} por el mecanismo.
 *
 * <p>Es el lugar reservado para la proxima clase de declaracion que el lenguaje agregue mientras este en
 * vista previa. Hoy no agrega nada, y eso es lo normal: la clase existe **antes** que la construccion,
 * para que el dia que aparezca tenga donde ir sin tener que tocar `AbstractElementVisitor14` — que es
 * API definitiva y no se puede mover.
 *
 * <p>Es API **reflexiva** de vista previa, no una construccion del lenguaje en vista previa. La
 * diferencia es observable: se compila y se ejecuta sin `--enable-preview`, y lo unico que se gana es
 * una advertencia.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractElementVisitorPreview<R, P> extends AbstractElementVisitor14<R, P> {

    protected AbstractElementVisitorPreview() {
        super();
    }
}
