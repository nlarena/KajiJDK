package jakarta.validation.impl;

import java.lang.reflect.Field;

// KajiLibrary-internal reflection support for the reference Validator: reads a field's constraint
// annotations (presence + element values) straight from the runtime's parsed metadata — no
// java.lang.reflect.Proxy needed. Not part of the Bean Validation API.
public final class Reflect {

    private Reflect() {
    }

    public static native boolean hasConstraint(Field field, String annotationType);

    public static native long constraintLong(Field field, String annotationType, String element);

    public static native String constraintString(Field field, String annotationType, String element);
}
