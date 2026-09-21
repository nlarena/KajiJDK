package java.lang.reflect;

/**
 * KajiLibrary's java.lang.reflect.ReflectPermission -- permission to skip access control by
 * reflection.
 *
 * <p>Only one name matters to it: `suppressAccessChecks`, which is what enables
 * `AccessibleObject.setAccessible(true)`. Without that permission reflection can **look at**
 * everything and **touch** only what is public, which is the difference between inspecting and
 * breaking encapsulation.
 *
 * <p>It has no actions -- the permission either is or is not-- and that is why it inherits from
 * `BasicPermission`, which already handles the wildcard (`*`, `x.*`) and prefix implication.
 */
public final class ReflectPermission extends java.security.BasicPermission {

    public ReflectPermission(String name) {
        super(name);
    }

    /** The one above; `actions` is ignored, which is what `BasicPermission` does. */
    public ReflectPermission(String name, String actions) {
        super(name, actions);
    }
}
