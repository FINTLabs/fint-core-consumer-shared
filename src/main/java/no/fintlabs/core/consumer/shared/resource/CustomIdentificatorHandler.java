package no.fintlabs.core.consumer.shared.resource;

import lombok.Getter;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import no.fintlabs.cache.Cache;
import no.fintlabs.core.consumer.shared.resource.exception.EntityNotFoundException;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

public class CustomIdentificatorHandler<T extends FintLinks & Serializable> {

    @Getter
    private final String name;

    private final Function<T, Identifikator> identificatorFunction;

    public CustomIdentificatorHandler(String name, Function<T, Identifikator> identificatorFunction) {
        this.name = name;
        this.identificatorFunction = identificatorFunction;
    }

    public T findResourceByIdentifier(Cache<T> cache, String idVerdi) {
        return cache.getLastUpdatedByFilter(idVerdi.hashCode(),
                resource1 -> Optional
                        .ofNullable(resource1)
                        .map(identificatorFunction::apply)
                        .map(Identifikator::getIdentifikatorverdi)
                        .map(idVerdi::equals)
                        .orElse(false)
        ).orElseThrow(() -> new EntityNotFoundException(idVerdi));
    }
}
