package no.fintlabs.core.consumer.shared.config;

import no.fintlabs.core.consumer.shared.ConsumerProps;
import no.vigoiks.resourceserver.security.FintJwtCoreConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ConsumerProps consumerProps;

    public SecurityConfig(ConsumerProps consumerProps) {
        this.consumerProps = consumerProps;
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize
                        .pathMatchers("/**")
                        .access(this::hasRequiredOrgIdAndRole)
                        .anyExchange()
                        .authenticated()
                )

                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt()
                        .jwtAuthenticationConverter(new FintJwtCoreConverter())
                );
        return http.build();
    }

    private Mono<AuthorizationDecision> hasRequiredOrgIdAndRole(Mono<Authentication> authentication, AuthorizationContext context) {
        String role = String.format("ROLE_FINT_Client_%s_%s", consumerProps.getDomainName(), consumerProps.getPackageName());
        return authentication.map(auth -> {
            boolean hasOrgId = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ORGID_" + consumerProps.getOrgId()));
            boolean hasRole = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(role));
            return new AuthorizationDecision(hasRole && hasOrgId);
        });
    }
}
