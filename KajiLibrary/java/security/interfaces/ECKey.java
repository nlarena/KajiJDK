package java.security.interfaces;

import java.security.spec.ECParameterSpec;

// Lo que toda clave EC tiene: sus parametros de dominio.
//
// Sin ellos la clave no se interpreta —el mismo punto es valido en infinitas curvas— asi que es el
// unico dato realmente comun entre la publica y la privada.
public interface ECKey {

    ECParameterSpec getParams();
}
