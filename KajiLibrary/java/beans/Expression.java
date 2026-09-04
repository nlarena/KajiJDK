package java.beans;

// Un Statement que ademas se queda con lo que la llamada devolvio. La diferencia con Statement es
// justo esa: un Statement se ejecuta por su efecto, una Expression por su valor.
//
// El valor se calcula una sola vez, perezosamente. La marca de "todavia no se calculo" no puede ser
// null —null es un resultado legitimo— asi que se usa un centinela propio. Sin el, una expresion
// que devuelve null se reevaluaria en cada getValue().
public class Expression extends Statement {

    // Centinela de "sin calcular". Un objeto privado y unico: ningun metodo puede devolverlo.
    private static final Object SIN_CALCULAR = new Object();

    private Object value = SIN_CALCULAR;

    public Expression(Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
    }

    // Con el valor ya sabido: no se va a ejecutar nada.
    public Expression(Object value, Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
        this.value = value;
    }

    // Ejecuta y guarda el resultado, incluso si es null.
    public void execute() throws Exception {
        this.value = this.invocar();
    }

    // El valor, ejecutando la llamada la primera vez que se lo pide.
    public Object getValue() throws Exception {
        if (this.value == SIN_CALCULAR) {
            this.value = this.invocar();
        }
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String toString() {
        String v = this.value == SIN_CALCULAR ? "<unbound>" : String.valueOf(this.value);
        return v + "=" + super.toString();
    }
}
