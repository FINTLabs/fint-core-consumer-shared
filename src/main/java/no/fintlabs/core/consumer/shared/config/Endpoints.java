package no.fintlabs.core.consumer.shared.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
public class Endpoints {

    @Value("${fint.relations.default-base-url:}")
    private String baseUrl;

    @Bean(name = "defaultEndpoints")
    @ConditionalOnBean
    public Map<String, Map<String, String>> getEndpoints(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entrySet -> entrySet.getKey().getName() != null)
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getName(),
                        Collectors.toMap(
                                entry -> entry.getValue().getMethod().getName(),
                                entry -> setUrl(entry.getKey().getPatternsCondition())
                        )
                ));
    }

    private String setUrl(PatternsRequestCondition patternsRequestCondition) {
        return baseUrl + getFirstPattern(patternsRequestCondition);
    }

    private String getFirstPattern(PatternsRequestCondition patternsRequestCondition) {
        Optional<PathPattern> first = patternsRequestCondition.getPatterns().stream().findFirst();
        if (first.isPresent()) {
            return first.get().getPatternString();
        }
        throw new RuntimeException("Pattern not found");
    }

}
