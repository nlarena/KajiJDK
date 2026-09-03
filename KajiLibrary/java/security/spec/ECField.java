package java.security.spec;

// El cuerpo finito sobre el que vive una curva eliptica.
//
// La interfaz tiene un solo metodo y aun asi es la que decide todo: los dos cuerpos que la
// implementan —`ECFieldFp` (primo) y `ECFieldF2m` (binario)— no comparten nada mas que el tamano,
// porque la aritmetica de uno no se parece a la del otro. Lo que este tipo permite es que
// `EllipticCurve` pueda nombrar a cualquiera de los dos sin saber cual es.
public interface ECField {

    // La cantidad de bits que hace falta para escribir un elemento del cuerpo.
    int getFieldSize();
}
