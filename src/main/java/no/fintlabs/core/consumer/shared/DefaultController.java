package no.fintlabs.core.consumer.shared;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DefaultController {

    private final Map<String, Map<String, String>> endpoints;

    public DefaultController(@Qualifier("defaultEndpoints") Map<String, Map<String, String>> endpoints) {
        this.endpoints = endpoints;
    }

    @GetMapping
    public Map<String, Map<String, String>> getEndpoints() {
        return endpoints;
    }

}
