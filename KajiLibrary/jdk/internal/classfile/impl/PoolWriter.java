package jdk.internal.classfile.impl;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.DynamicConstantPoolEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.MethodTypeEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.List;

/**
 * Serializa el pool de constantes (JVMS 4.4) y el atributo `BootstrapMethods` que lo acompana.
 *
 * <p>Va aparte del escritor de la clase por una razon de orden y no de tamano: el pool se escribe
 * **al final** aunque vaya al principio del archivo. Las entradas se van creando mientras se
 * escriben los metodos --cada `invokevirtual` puede agregar una-- asi que no se sabe cuantas hay
 * hasta que no se escribio todo lo demas. El escritor de la clase arma el cuerpo primero, pregunta
 * el pool despues, y recien ahi los pega en el orden del formato.
 *
 * <h2>El UTF-8 **modificado**</h2>
 *
 * <p>La codificacion de un `CONSTANT_Utf8` no es UTF-8. Se aparta en dos puntos, y los dos importan:
 * el caracter nulo va en **dos** bytes en vez de uno --para que ningun byte del contenido sea cero-- y
 * los caracteres de fuera del plano basico van como **dos pares subrogados codificados por separado**,
 * seis bytes, en vez de los cuatro del UTF-8 de verdad. Usar el UTF-8 del sistema produce un archivo
 * que la JVM rechaza en cuanto aparece un emoji en una constante de texto.
 */
final class PoolWriter {

    private PoolWriter() {
    }

    /** El pool entero: la cantidad y despues cada entrada. */
    static void writePool(BufWriterImpl buf, ConstantPoolBuilder pool) {
        int n = pool.size();
        buf.writeU2(n);
        // Desde 1: el indice 0 no existe en el formato. Un `long` o un `double` ocupan dos indices y
        // el segundo queda vacio -- por eso se pregunta por la entrada y se saltea la que no hay.
        for (int i = 1; i < n; i++) {
            PoolEntry e = pool.entryByIndex(i);
            if (e == null) {
                continue;
            }
            PoolWriter.writeEntry(buf, e);
        }
    }

    private static void writeEntry(BufWriterImpl buf, PoolEntry e) {
        int tag = e.tag();
        buf.writeU1(tag);
        if (tag == PoolEntry.TAG_UTF8) {
            PoolWriter.writeModifiedUtf8(buf, ((Utf8Entry) e).stringValue());
            return;
        }
        if (tag == PoolEntry.TAG_INTEGER) {
            buf.writeInt(((IntegerEntry) e).intValue());
            return;
        }
        if (tag == PoolEntry.TAG_FLOAT) {
            buf.writeFloat(((FloatEntry) e).floatValue());
            return;
        }
        if (tag == PoolEntry.TAG_LONG) {
            buf.writeLong(((LongEntry) e).longValue());
            return;
        }
        if (tag == PoolEntry.TAG_DOUBLE) {
            buf.writeDouble(((DoubleEntry) e).doubleValue());
            return;
        }
        if (tag == PoolEntry.TAG_CLASS) {
            buf.writeIndex(((ClassEntry) e).name());
            return;
        }
        if (tag == PoolEntry.TAG_STRING) {
            buf.writeIndex(((StringEntry) e).utf8());
            return;
        }
        if (tag == PoolEntry.TAG_FIELDREF || tag == PoolEntry.TAG_METHODREF
                || tag == PoolEntry.TAG_INTERFACE_METHODREF) {
            MemberRefEntry m = (MemberRefEntry) e;
            buf.writeIndex(m.owner());
            buf.writeIndex(m.nameAndType());
            return;
        }
        if (tag == PoolEntry.TAG_NAME_AND_TYPE) {
            NameAndTypeEntry nt = (NameAndTypeEntry) e;
            buf.writeIndex(nt.name());
            buf.writeIndex(nt.type());
            return;
        }
        if (tag == PoolEntry.TAG_METHOD_HANDLE) {
            MethodHandleEntry mh = (MethodHandleEntry) e;
            buf.writeU1(mh.kind());
            buf.writeIndex(mh.reference());
            return;
        }
        if (tag == PoolEntry.TAG_METHOD_TYPE) {
            buf.writeIndex(((MethodTypeEntry) e).descriptor());
            return;
        }
        if (tag == PoolEntry.TAG_DYNAMIC || tag == PoolEntry.TAG_INVOKE_DYNAMIC) {
            DynamicConstantPoolEntry d = (DynamicConstantPoolEntry) e;
            buf.writeU2(d.bootstrapMethodIndex());
            buf.writeIndex(d.nameAndType());
            return;
        }
        if (tag == PoolEntry.TAG_MODULE) {
            buf.writeIndex(((ModuleEntry) e).name());
            return;
        }
        if (tag == PoolEntry.TAG_PACKAGE) {
            buf.writeIndex(((PackageEntry) e).name());
            return;
        }
        throw new IllegalArgumentException("etiqueta de pool desconocida: " + tag);
    }

    /** El atributo `BootstrapMethods`, o nada si el pool no tiene ninguno. */
    static void writeBootstrapMethods(BufWriterImpl buf, ConstantPoolBuilder pool) {
        int n = pool.bootstrapMethodCount();
        buf.writeIndex(pool.utf8Entry("BootstrapMethods"));
        int lenPos = buf.size();
        buf.writeInt(0);
        buf.writeU2(n);
        for (int i = 0; i < n; i++) {
            BootstrapMethodEntry b = pool.bootstrapMethodEntry(i);
            buf.writeIndex(b.bootstrapMethod());
            List<LoadableConstantEntry> args = b.arguments();
            buf.writeU2(args.size());
            for (int j = 0; j < args.size(); j++) {
                buf.writeIndex(args.get(j));
            }
        }
        buf.patchInt(lenPos, 4, buf.size() - lenPos - 4);
    }

    /** Si hace falta escribir `BootstrapMethods`. */
    static boolean hasBootstrapMethods(ConstantPoolBuilder pool) {
        return pool.bootstrapMethodCount() > 0;
    }

    // El UTF-8 modificado del JVMS 4.4.7. Ver la nota de la clase sobre en que se aparta del de
    // verdad.
    //
    // El largo va adelante en dos bytes y **es el de los bytes, no el de los caracteres**: hay que
    // codificar primero para saberlo, o dejar el hueco y taparlo. Se hace lo segundo.
    private static void writeModifiedUtf8(BufWriterImpl buf, String s) {
        int lenPos = buf.size();
        buf.writeU2(0);
        int desde = buf.size();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                buf.writeU1(c);
            } else if (c <= 0x07FF) {
                // El nulo cae acá y no arriba: por eso el rango de un byte empieza en 1.
                buf.writeU1(0xC0 | ((c >> 6) & 0x1F));
                buf.writeU1(0x80 | (c & 0x3F));
            } else {
                buf.writeU1(0xE0 | ((c >> 12) & 0x0F));
                buf.writeU1(0x80 | ((c >> 6) & 0x3F));
                buf.writeU1(0x80 | (c & 0x3F));
            }
        }
        int largo = buf.size() - desde;
        if (largo > 65535) {
            throw new IllegalArgumentException(
                    "una constante de texto no puede pasar de 65535 bytes; esta mide " + largo);
        }
        buf.patchInt(lenPos, 2, largo);
    }
}
