package no.fintlabs.core.consumer.shared.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class ConsumerProps {

    @Value("${fint.consumer.org-id}")
    private String orgId;

    @Value("${fint.consumer.domain}")
    private String domainName;

    @Value("${fint.consumer.package}")
    private String packageName;

	private final ObjectMapper objectMapper;

	public ConsumerProps(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	public void init() {
		objectMapper.setDateFormat(new ISO8601DateFormat()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

}


