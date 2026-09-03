package java.awt.color;

/**
 * Un perfil ICC con datos inválidos.
 *
 * <p>Como {@link CMMException}, está por contrato y no se tira acá: sin `ICC_Profile` no hay
 * perfiles que puedan estar mal.
 */
public class ProfileDataException extends RuntimeException {

    private static final long serialVersionUID = 7286140888240322498L;

    /** Con ese mensaje. */
    public ProfileDataException(String s) {
        super(s);
    }
}
