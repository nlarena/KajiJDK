package java.security.spec;

// El nombre de una curva estandar, para pedirla al generar un par de claves EC.
//
// Desde el JDK 11 no es mas que un `NamedParameterSpec`: quedo como subclase por compatibilidad
// —hay codigo que hace `instanceof ECGenParameterSpec` para distinguir "me pidieron EC por nombre"
// de "me pidieron otra cosa por nombre"— y porque el tipo distinto sigue documentando la intencion.
public class ECGenParameterSpec extends NamedParameterSpec {

    public ECGenParameterSpec(String stdName) {
        super(stdName);
    }
}
