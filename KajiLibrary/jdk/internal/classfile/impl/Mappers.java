package jdk.internal.classfile.impl;

import java.lang.classfile.AttributeMapper;
import java.lang.classfile.AttributeMapper.AttributeStability;
import java.lang.classfile.Attributes;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// El índice de nombre a mapeador. Los treinta y seis que el JVMS define salen de `Attributes`; para
// un nombre desconocido se fabrica —una sola vez por nombre— un mapeador con estabilidad `UNKNOWN`,
// que es exactamente lo que significa: se puede copiar byte a byte y no se sabe qué dice.
final class Mappers {

    private static final Map<String, AttributeMapper<RawAttribute>> KNOWN = build();
    private static final Map<String, AttributeMapper<RawAttribute>> UNKNOWN_BY_NAME =
            new HashMap<String, AttributeMapper<RawAttribute>>();

    private Mappers() {
    }

    private static Map<String, AttributeMapper<RawAttribute>> build() {
        Map<String, AttributeMapper<RawAttribute>> m =
                new HashMap<String, AttributeMapper<RawAttribute>>();
        put(m, Attributes.annotationDefault());
        put(m, Attributes.bootstrapMethods());
        put(m, Attributes.characterRangeTable());
        put(m, Attributes.code());
        put(m, Attributes.compilationId());
        put(m, Attributes.constantValue());
        put(m, Attributes.deprecated());
        put(m, Attributes.enclosingMethod());
        put(m, Attributes.exceptions());
        put(m, Attributes.innerClasses());
        put(m, Attributes.lineNumberTable());
        put(m, Attributes.localVariableTable());
        put(m, Attributes.localVariableTypeTable());
        put(m, Attributes.methodParameters());
        put(m, Attributes.module());
        put(m, Attributes.moduleHashes());
        put(m, Attributes.moduleMainClass());
        put(m, Attributes.modulePackages());
        put(m, Attributes.moduleResolution());
        put(m, Attributes.moduleTarget());
        put(m, Attributes.nestHost());
        put(m, Attributes.nestMembers());
        put(m, Attributes.permittedSubclasses());
        put(m, Attributes.record());
        put(m, Attributes.runtimeInvisibleAnnotations());
        put(m, Attributes.runtimeInvisibleParameterAnnotations());
        put(m, Attributes.runtimeInvisibleTypeAnnotations());
        put(m, Attributes.runtimeVisibleAnnotations());
        put(m, Attributes.runtimeVisibleParameterAnnotations());
        put(m, Attributes.runtimeVisibleTypeAnnotations());
        put(m, Attributes.signature());
        put(m, Attributes.sourceDebugExtension());
        put(m, Attributes.sourceFile());
        put(m, Attributes.sourceId());
        put(m, Attributes.stackMapTable());
        put(m, Attributes.synthetic());
        return m;
    }

    private static void put(Map<String, AttributeMapper<RawAttribute>> m,
            AttributeMapper<RawAttribute> mapper) {
        m.put(mapper.name(), mapper);
    }

    static AttributeMapper<RawAttribute> forName(String name) {
        AttributeMapper<RawAttribute> m = KNOWN.get(name);
        if (m != null) {
            return m;
        }
        synchronized (UNKNOWN_BY_NAME) {
            m = UNKNOWN_BY_NAME.get(name);
            if (m == null) {
                m = new AttributeMapperImpl(name, AttributeStability.UNKNOWN, true);
                UNKNOWN_BY_NAME.put(name, m);
            }
            return m;
        }
    }
}

// La función de mapeadores a medida de un lector que no tiene ninguno registrado. Registrarlos exige
// `ClassFile.Option`, que KajiLibrary no implementa; devolver siempre `null` es decir eso mismo, y no
// hay forma de que un atributo a medida se pierda en silencio: sin mapeador propio, cae en el
// genérico de `Mapeadores` y conserva su nombre y sus bytes.
final class NoCustomMappers implements Function<Utf8Entry, AttributeMapper<?>> {

    static final NoCustomMappers INSTANCE = new NoCustomMappers();

    public AttributeMapper<?> apply(Utf8Entry name) {
        return null;
    }
}
