import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.IntBinaryOperator;

public class Lambdas {
  int base = 5;
  int add(int a, int b) { return a + b + base; }
  static int mul(int a, int b) { return a * b; }

  IntBinaryOperator plainLambda() { return (a, b) -> a + b; }
  IntBinaryOperator captureLambda() { int k = 3; return (a, b) -> a + b + k; }
  IntBinaryOperator staticRef() { return Lambdas::mul; }
  IntBinaryOperator boundRef() { return this::add; }
  Function<String, Integer> unboundRef() { return String::length; }
  Supplier<Object> ctorRef() { return Object::new; }
}
