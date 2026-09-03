package java.security.cert;

import java.util.Collection;
import java.util.Set;

// Un `CertPathChecker` con lo que PKIX agrega: saber que extensiones sabe procesar.
//
// La segunda cosa es la que justifica la clase. Durante la validacion se lleva la cuenta de que
// extensiones criticas quedaron sin procesar; si al final del certificado queda alguna, hay que
// rechazarlo. Por eso `check` recibe el conjunto de OIDs pendientes y el checker **saca de ahi** los
// que atendio: es como le avisa al validador que esa extension ya no es un motivo de rechazo.
// Un checker que procesa una extension critica y se olvida de sacarla hace fallar la validacion; uno
// que saca una que no proceso desarma la garantia de las extensiones criticas.
public abstract class PKIXCertPathChecker implements CertPathChecker, Cloneable {

    protected PKIXCertPathChecker() {
    }

    public abstract void init(boolean forward) throws CertPathValidatorException;

    public abstract boolean isForwardCheckingSupported();

    // Los OIDs de las extensiones que este checker sabe procesar, o null si ninguna.
    public abstract Set<String> getSupportedExtensions();

    // Comprueba el certificado y saca de `unresolvedCritExts` los OIDs que atendio.
    public abstract void check(Certificate cert, Collection<String> unresolvedCritExts)
        throws CertPathValidatorException;

    // La version sin conjunto, que viene de `CertPathChecker`. Pasa un conjunto vacio **inmutable**,
    // y esa eleccion se nota: un checker que intente sacar un OID de ahi revienta con
    // `UnsupportedOperationException` en vez de fallar en silencio. Es lo correcto, porque llamar a
    // esta version quiere decir que nadie esta llevando la cuenta de las extensiones criticas, y un
    // checker que dependa de esa cuenta tiene que enterarse.
    @Override
    public void check(Certificate cert) throws CertPathValidatorException {
        this.check(cert, java.util.Collections.<String>emptySet());
    }

    // Copia superficial. Un checker con estado mutable —casi todos lo tienen, porque acumulan a lo
    // largo del camino— tiene que sobreescribirla: si no, dos validaciones concurrentes se pisan.
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
