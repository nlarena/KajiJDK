package java.beans.beancontext;

import java.beans.BeanInfo;

/**
 * The `BeanInfo` of a provider that also describes the services it offers.
 *
 * <p>It is for a design tool: without it, all that can be shown of the provider is the provider
 * itself, and what the user cares about is the services.
 */
public interface BeanContextServiceProviderBeanInfo extends BeanInfo {

    /** One `BeanInfo` per service offered. */
    BeanInfo[] getServicesBeanInfo();
}
