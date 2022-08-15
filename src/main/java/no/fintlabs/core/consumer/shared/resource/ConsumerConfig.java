package no.fintlabs.core.consumer.shared.resource;

import no.fint.model.resource.FintLinks;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.core.consumer.shared.ConsumerProps;

import java.io.Serializable;

public abstract class ConsumerConfig<T extends FintLinks & Serializable> {

    private final ConsumerProps consumerProps;

    public ConsumerConfig(ConsumerProps consumerProps) {
        this.consumerProps = consumerProps;
    }

    protected abstract String domainName();

    protected abstract String packageName();

    protected abstract String resourceName();

    public String getDomainName() {
        return domainName().toLowerCase();
    }

    public String getPackageName() {
        return packageName().toLowerCase();
    }

    public String getResourceName() {
        return resourceName().toLowerCase();
    }

    public String getOrgId() {
        return consumerProps.getOrgId();
    }
}
