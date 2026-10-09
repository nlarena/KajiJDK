package javax.management.openmbean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * The handler that makes a {@link CompositeData} usable through an interface of getters.
 *
 * <p>It is what solves the problem on the client side. A {@code CompositeData} is queried by
 * strings --{@code data.get("name")}-- and the compiler does not check that: a typo in the key
 * shows up at run time. With this, you declare an interface {@code Person} with {@code getName()}
 * and {@code getAge()}, wrap the data in a proxy with this handler, and from then on the compiler
 * checks the names and the types.
 *
 * <p>The method-to-item translation is the beans convention: {@code getFoo()} reads the item
 * {@code foo} and so does {@code isFoo()} --the {@code is} prefix is only accepted for
 * {@code boolean}, which is where Java allows it. The first letter is lower-cased unless the first
 * two are upper case, which is {@code Introspector}'s rule and what makes {@code getURL()} read the
 * item {@code URL} and not {@code uRL}.
 *
 * <p>Only getters with no arguments are accepted. A method with parameters cannot correspond to an
 * item, and calling it is an {@link IllegalArgumentException} instead of a made-up item.
 */
public class CompositeDataInvocationHandler implements InvocationHandler {

    private final CompositeData compositeData;

    /**
     * A handler over that data.
     *
     * @throws IllegalArgumentException if the data is null
     */
    public CompositeDataInvocationHandler(CompositeData compositeData) {
        if (compositeData == null) {
            throw new IllegalArgumentException("the composite data cannot be null");
        }
        this.compositeData = compositeData;
    }

    /** The data this handler exposes. */
    public CompositeData getCompositeData() {
        return this.compositeData;
    }

    /**
     * Answers the call by reading the matching item.
     *
     * <p>{@code equals}, {@code hashCode} and {@code toString} are served apart and do not go to
     * the items: a proxy that looked for an item called {@code toString} would be unusable with any
     * tool that prints objects.
     *
     * @throws IllegalArgumentException if the method is not a getter, or if there is no item with
     *     that name
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (args == null || args.length == 0) {
            if (name.equals("toString")) {
                return "Proxy[" + this.compositeData.toString() + "]";
            }
            if (name.equals("hashCode")) {
                return Integer.valueOf(this.compositeData.hashCode());
            }
        }
        if (name.equals("equals") && args != null && args.length == 1
                && method.getParameterTypes()[0] == Object.class) {
            return Boolean.valueOf(this.equalsProxy(proxy, args[0]));
        }

        String item = itemOf(name, method);
        if (args != null && args.length > 0) {
            throw new IllegalArgumentException(
                    "a composite data getter takes no arguments: " + name);
        }
        if (!this.compositeData.containsKey(item)) {
            throw new IllegalArgumentException(
                    "there is no item named " + item + " in this composite data");
        }
        return this.compositeData.get(item);
    }

    // Two proxies are equal if their data are. Comparing the proxies with `equals` would send them
    // back into this same method and never end.
    private boolean equalsProxy(Object proxy, Object other) {
        if (other == null) {
            return false;
        }
        if (proxy == other) {
            return true;
        }
        if (!Proxy.isProxyClass(other.getClass())) {
            return false;
        }
        InvocationHandler h = Proxy.getInvocationHandler(other);
        if (!(h instanceof CompositeDataInvocationHandler)) {
            return false;
        }
        return this.compositeData.equals(
                ((CompositeDataInvocationHandler) h).getCompositeData());
    }

    private static String itemOf(String name, Method method) {
        String rest;
        if (name.startsWith("get") && name.length() > 3) {
            rest = name.substring(3);
        } else if (name.startsWith("is") && name.length() > 2
                && method.getReturnType() == Boolean.TYPE) {
            rest = name.substring(2);
        } else {
            throw new IllegalArgumentException(name + " is not a getter");
        }
        // `Introspector`'s rule: `getURL` gives `URL`, `getName` gives `name`. Without it, an item
        // whose name starts with an acronym would never be found.
        if (rest.length() > 1 && Character.isUpperCase(rest.charAt(1))) {
            return rest;
        }
        return Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
    }
}
