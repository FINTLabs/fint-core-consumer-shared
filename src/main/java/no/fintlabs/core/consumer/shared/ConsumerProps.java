package no.fintlabs.core.consumer.shared;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration()
public class ConsumerProps {

    @Value("${fint.consumer.org-id}")
    private String orgId;

    @Value("${fint.consumer.domain}")
    private String domainName;

    @Value("${fint.consumer.package}")
    private String packageName;

}


