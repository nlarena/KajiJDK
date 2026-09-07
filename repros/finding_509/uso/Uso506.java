package uso;

import base.Field;

// Repro de #509: compilando en LOTE, un import explicito pierde contra java.lang.reflect, y el
// tipo equivocado termina en el DESCRIPTOR emitido.
//
//   bin/javac.exe --emit base/Field.java uso/Uso506.java   -> descriptor ()Ljava/lang/reflect/Field;  MAL
//   bin/javac.exe --emit base/Field.java                   -> ok
//   bin/javac.exe --emit uso/Uso506.java                   -> descriptor ()Lbase/Field;              BIEN
//
// No da error: el .class sale mal y compila. Se ve con
//   javap -v -p uso/Uso506.class | grep descriptor
public interface Uso506 {

    /** El caso: nuestro javac emite java.lang.reflect.Field en el descriptor. */
    Field devuelve();

    /** Y tambien como parametro. */
    void recibe(Field field);

    /** Control: un tipo cuyo nombre simple no colisiona con nada. */
    String control();
}
