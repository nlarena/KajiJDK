package com.sun.jdi;

import java.util.List;
import java.util.Map;

/**
 * A class, interface or array type loaded in the debugged machine.
 *
 * <p>It is JDI's largest type because it is where everything static is asked for: the fields,
 * the methods, the source, the line table.
 *
 * <p>Two pairs of methods that are best not confused. {@code fields} gives the
 * <strong>declared</strong> fields and {@code allFields} adds the inherited ones;
 * {@code methods} and {@code allMethods}, the same. The first of each pair is the one that is
 * almost always wanted and the second the one that is almost always used by mistake.
 *
 * <p>{@code isPrepared} tells a class the VM has already linked from one it has only loaded.
 * Before preparing it its static fields cannot be read, and half the
 * {@link ClassNotPreparedException} come from there.
 *
 * @since 1.3
 */
public interface ReferenceType extends Type, Comparable<ReferenceType>, Accessible {

    /**
     * The name.
     *
     * @return the result
     */
    String name();

    /**
     * The generic signature.
     *
     * @return the result
     */
    String genericSignature();

    /**
     * The class loader.
     *
     * @return the result
     */
    ClassLoaderReference classLoader();

    /**
     * The module.
     *
     * @return the result
     */
    ModuleReference module();

    /**
     * The source name.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    String sourceName()
            throws AbsentInformationException;

    /**
     * The source names.
     *
     * @param name the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<String> sourceNames(String name)
            throws AbsentInformationException;

    /**
     * The source paths.
     *
     * @param name the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<String> sourcePaths(String name)
            throws AbsentInformationException;

    /**
     * The source debug extension.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    String sourceDebugExtension()
            throws AbsentInformationException;

    /**
     * Whether static.
     *
     * @return the result
     */
    boolean isStatic();

    /**
     * Whether abstract.
     *
     * @return the result
     */
    boolean isAbstract();

    /**
     * Whether final.
     *
     * @return the result
     */
    boolean isFinal();

    /**
     * Whether prepared.
     *
     * @return the result
     */
    boolean isPrepared();

    /**
     * Whether verified.
     *
     * @return the result
     */
    boolean isVerified();

    /**
     * Whether initialized.
     *
     * @return the result
     */
    boolean isInitialized();

    /**
     * The failed to initialize.
     *
     * @return the result
     */
    boolean failedToInitialize();

    /**
     * The fields.
     *
     * @return the result
     */
    List<Field> fields();

    /**
     * The visible fields.
     *
     * @return the result
     */
    List<Field> visibleFields();

    /**
     * Every field, the inherited ones included.
     *
     * @return the result
     */
    List<Field> allFields();

    /**
     * The field by name.
     *
     * @param name the String
     * @return the result
     */
    Field fieldByName(String name);

    /**
     * The methods.
     *
     * @return the result
     */
    List<Method> methods();

    /**
     * The visible methods.
     *
     * @return the result
     */
    List<Method> visibleMethods();

    /**
     * Every method, the inherited ones included.
     *
     * @return the result
     */
    List<Method> allMethods();

    /**
     * The methods by name.
     *
     * @param name the String
     * @return the result
     */
    List<Method> methodsByName(String name);

    /**
     * The methods by name.
     *
     * @param name the String
     * @param name2 the String
     * @return the result
     */
    List<Method> methodsByName(String name, String name2);

    /**
     * The nested types.
     *
     * @return the result
     */
    List<ReferenceType> nestedTypes();

    /**
     * The value.
     *
     * @param field the Field
     * @return the result
     */
    Value getValue(Field field);

    /**
     * The values.
     *
     * @param values the List<? extends Field>
     * @return the result
     */
    Map<Field, Value> getValues(List<? extends Field> values);

    /**
     * The class object.
     *
     * @return the result
     */
    ClassObjectReference classObject();

    /**
     * Every line location, the inherited ones included.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> allLineLocations()
            throws AbsentInformationException;

    /**
     * Every line location, the inherited ones included.
     *
     * @param name the String
     * @param name2 the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> allLineLocations(String name, String name2)
            throws AbsentInformationException;

    /**
     * The locations of line.
     *
     * @param index the int
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> locationsOfLine(int index)
            throws AbsentInformationException;

    /**
     * The locations of line.
     *
     * @param name the String
     * @param name2 the String
     * @param index the int
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> locationsOfLine(String name, String name2, int index)
            throws AbsentInformationException;

    /**
     * The available strata.
     *
     * @return the result
     */
    List<String> availableStrata();

    /**
     * The default stratum.
     *
     * @return the result
     */
    String defaultStratum();

    /**
     * The instances.
     *
     * @param index the long
     * @return the result
     */
    List<ObjectReference> instances(long index);

    /**
     * Two mirrors are equal if they name the same thing in the same VM.
     *
     * @param obj the Object
     * @return the result
     */
    boolean equals(Object obj);

    /**
     * Consistent with {@link #equals}.
     *
     * @return the result
     */
    int hashCode();

    /**
     * The major version.
     *
     * @return the result
     */
    int majorVersion();

    /**
     * The minor version.
     *
     * @return the result
     */
    int minorVersion();

    /**
     * The constant pool count.
     *
     * @return the result
     */
    int constantPoolCount();

    /**
     * The constant pool.
     *
     * @return the result
     */
    byte[] constantPool();
}
