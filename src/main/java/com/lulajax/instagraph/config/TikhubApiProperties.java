package com.lulajax.instagraph.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "tikhub.api")
@Data
public class TikhubApiProperties {
    private String xRapidapiHost;
    private String xRapidapiKey;
    private Map<String, String> url;
}
