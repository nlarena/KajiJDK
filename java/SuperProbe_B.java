// Parte de SuperProbe (#265): vacia A PROPOSITO. Es el eslabon que hacia fallar la resolucion:
// `super.f()` emite `invokespecial SuperProbe_B.f`, y B no declara `f`.
public class SuperProbe_B extends SuperProbe_A {
}
