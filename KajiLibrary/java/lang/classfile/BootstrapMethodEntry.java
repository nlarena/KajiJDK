package java.lang.classfile;

import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.util.List;

// Una fila de la tabla del atributo `BootstrapMethods` (JVMS §4.7.23): el method handle de arranque
// y sus argumentos estáticos. No es una `PoolEntry` —no vive en el pool de constantes— pero se
// indexa igual que él desde `CONSTANT_Dynamic` y `CONSTANT_InvokeDynamic`, y por eso la API la trata
// como parte del pool.
public interface BootstrapMethodEntry {

    /** El pool al que pertenece. */
    ConstantPool constantPool();

    /** El índice de esta fila dentro de la tabla. */
    int bsmIndex();

    /** El method handle de arranque. */
    MethodHandleEntry bootstrapMethod();

    /** Los argumentos estáticos, en orden. */
    List<LoadableConstantEntry> arguments();
}
