package java.beans;

// La regla de "como se rehace este objeto". La persistencia de beans no guarda los bytes de un
// objeto: guarda la secuencia de llamadas que lo vuelve a construir. Un PersistenceDelegate es
// quien sabe, para un tipo dado, cuales son esas llamadas.
//
// El reparto entre los cuatro metodos es lo unico que hay que entender, y es lo que hace que el
// formato salga chico:
//
//   - `instantiate` da la expresion que CREA el objeto —tipicamente `new Foo(...)`—.
//   - `initialize` da las llamadas que lo llevan del recien creado al que tenemos.
//   - `mutatesTo` decide cual de las dos hace falta: si el objeto que el codificador ya tiene
//     armado se puede *llevar* al que queremos con puras llamadas, se emiten solo esas; si no,
//     hay que crear uno nuevo desde cero.
//   - `writeObject` es el que orquesta lo anterior y casi nunca se redefine.
//
// Ese `mutatesTo` es la razon de que un bean con veinte propiedades y una sola distinta de la
// default salga como una linea y no como veinte: `initialize` compara contra el objeto nuevo y
// solo escribe lo que difiere.
public abstract class PersistenceDelegate {

    protected PersistenceDelegate() {
    }

    // El punto de entrada que usa el Encoder. Si el objeto que ya hay se puede mutar hasta el que
    // queremos, se emiten las diferencias; si no, se lo olvida y se emite la creacion entera.
    public void writeObject(Object oldInstance, Encoder out) {
        Object newInstance = out.get(oldInstance);
        if (!this.mutatesTo(oldInstance, newInstance)) {
            out.remove(oldInstance);
            out.writeExpression(this.instantiate(oldInstance, out));
        } else {
            this.initialize(oldInstance.getClass(), oldInstance, newInstance, out);
        }
    }

    // Por defecto, dos objetos son "el mismo molde" si son exactamente de la misma clase. Alcanza
    // porque despues initialize se encarga de las diferencias de estado.
    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        return newInstance != null && oldInstance != null
            && oldInstance.getClass() == newInstance.getClass();
    }

    protected abstract Expression instantiate(Object oldInstance, Encoder out);

    // Por defecto no hay nada que ajustar: los delegados que si tienen estado que copiar lo
    // redefinen y ademas llaman a super, que sube por la cadena de superclases.
    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            PersistenceDelegate info = out.getPersistenceDelegate(superClass);
            info.initialize(superClass, oldInstance, newInstance, out);
        }
    }
}
