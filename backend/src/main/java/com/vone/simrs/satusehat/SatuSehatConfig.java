package com.vone.simrs.satusehat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Bean khusus untuk komunikasi ke SATUSEHAT, dengan timeout yang dapat dikonfigurasi.
 */
@Configuration
@EnableScheduling
public class SatuSehatConfig {

    @Bean
    public RestTemplate satuSehatRestTemplate(SatuSehatProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }
}
