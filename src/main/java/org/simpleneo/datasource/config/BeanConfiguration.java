package org.simpleneo.datasource.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@PropertySource(value="classpath:properties/application.${spring.profiles.active}.properties")
public class BeanConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        final List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        final ObjectMapper objectMapper = new ObjectMapper();
        final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);
        converters.add(mappingJackson2HttpMessageConverter);
        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }

}