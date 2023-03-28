package no.fintlabs.core.consumer.shared.resource;

import no.fint.model.resource.FintLinks;
import no.fintlabs.core.consumer.shared.ConsumerProps;

import java.io.Serializable;

public abstract class ConsumerConfig<T extends FintLinks & Serializable> {

    private final ConsumerProps consumerProps;

    public ConsumerConfig(ConsumerProps consumerProps) {
        this.consumerProps = consumerProps;
    }

    protected abstract String resourceName();

    public String getDomainName() {
        return consumerProps.getDomainName().toLowerCase();
    }

    public String getPackageName() {
        return consumerProps.getPackageName().toLowerCase();
    }

    public String getResourceName() {
        return resourceName().toLowerCase();
    }

    public String getOrgId() {
        return consumerProps.getOrgId();
    }
}
