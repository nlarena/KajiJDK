package javax.management.openmbean;

import javax.management.MBeanParameterInfo;

// What the three signature support classes --constructor, operation and info-- need and cannot
// inherit: each extends its own `MBeanXxxInfo` from `javax.management`.
//
// It is the same fix as `Constraints` and for the same reason. Package-private: it is not
// contract.
final class Signatures {

    private Signatures() {
    }

    static String requireName(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("the name cannot be blank");
        }
        return name;
    }

    static String requireDescription(String description) {
        if (description == null || description.trim().length() == 0) {
            throw new IllegalArgumentException("the description cannot be blank");
        }
        return description;
    }

    // An array of `OpenMBeanParameterInfo` seen as one of `MBeanParameterInfo`. The objects are the
    // same; what changes is the array's type, which Java does not convert on its own.
    static MBeanParameterInfo[] asParameters(OpenMBeanParameterInfo[] signature) {
        if (signature == null || signature.length == 0) {
            return new MBeanParameterInfo[0];
        }
        MBeanParameterInfo[] out = new MBeanParameterInfo[signature.length];
        for (int i = 0; i < signature.length; i++) {
            if (signature[i] == null) {
                throw new IllegalArgumentException("parameter " + i + " is null");
            }
            if (!(signature[i] instanceof MBeanParameterInfo)) {
                // It is not a whim: the signature inherited from `javax.management` is of
                // `MBeanParameterInfo`, so an implementation of `OpenMBeanParameterInfo` that is
                // not one cannot go in there. Saying so is better than a `ClassCastException`.
                throw new IllegalArgumentException("parameter " + i
                        + " does not extend MBeanParameterInfo and cannot be used in a signature");
            }
            out[i] = (MBeanParameterInfo) signature[i];
        }
        return out;
    }
}
