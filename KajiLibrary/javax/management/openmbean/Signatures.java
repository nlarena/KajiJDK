package javax.management.openmbean;

import javax.management.MBeanParameterInfo;

// Lo que las tres clases de soporte de firmas --constructor, operacion e informacion-- necesitan y
// no pueden heredar: cada una extiende su `MBeanXxxInfo` de `javax.management`.
//
// Es el mismo arreglo que `Constraints` y por el mismo motivo. De paquete: no es contrato.
final class Signatures {

    private Signatures() {
    }

    static String requireName(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("el nombre no puede estar en blanco");
        }
        return name;
    }

    static String requireDescription(String description) {
        if (description == null || description.trim().length() == 0) {
            throw new IllegalArgumentException("la descripción no puede estar en blanco");
        }
        return description;
    }

    // Un arreglo de `OpenMBeanParameterInfo` visto como uno de `MBeanParameterInfo`. Los objetos
    // son los mismos; lo que cambia es el tipo del arreglo, que Java no convierte solo.
    static MBeanParameterInfo[] asParameters(OpenMBeanParameterInfo[] signature) {
        if (signature == null || signature.length == 0) {
            return new MBeanParameterInfo[0];
        }
        MBeanParameterInfo[] out = new MBeanParameterInfo[signature.length];
        for (int i = 0; i < signature.length; i++) {
            if (signature[i] == null) {
                throw new IllegalArgumentException("el parámetro " + i + " es nulo");
            }
            if (!(signature[i] instanceof MBeanParameterInfo)) {
                // No es un capricho: la firma que se hereda de `javax.management` es de
                // `MBeanParameterInfo`, asi que una implementacion de `OpenMBeanParameterInfo` que
                // no lo sea no puede entrar ahi. Decirlo es mejor que un `ClassCastException`.
                throw new IllegalArgumentException("el parámetro " + i
                        + " no extiende MBeanParameterInfo y no se puede usar en una firma");
            }
            out[i] = (MBeanParameterInfo) signature[i];
        }
        return out;
    }
}
