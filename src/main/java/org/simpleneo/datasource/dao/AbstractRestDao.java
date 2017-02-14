package org.simpleneo.datasource.dao;

import javax.annotation.Resource;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractRestDao {

    private String neo4JBaseUrl;
	private String neo4JUsername;
    private String neo4JPassword;

    @Resource
    private RestTemplate restTemplate;

    @Value("${neo4j.api.url}")
    public void setNeo4JBaseUrl(String neo4jBaseUrl) {
		neo4JBaseUrl = neo4jBaseUrl;
	}

    @Value("${neo4j.api.username}")
    public void setNeo4JUsername(String neo4jUsername) {
		neo4JUsername = neo4jUsername;
	}

    @Value("${neo4j.api.password}")
    public void setNeo4JPassword(String neo4jPassword) {
		neo4JPassword = neo4jPassword;
	}

    protected <T> ResponseEntity<T> callNeo4JAPI(final String path, final HttpMethod httpMethod, final Class<T> responseClazz) {
    	return callNeo4JAPI(path, null, httpMethod, responseClazz);
    }

    protected <T> ResponseEntity<T> callNeo4JAPI(final String path, Object object, final HttpMethod httpMethod, final Class<T> responseClazz) {
    	final HttpEntity<?> httpEntity = getHttpEntity(object, neo4JUsername, neo4JPassword);
        String url = neo4JBaseUrl;
        if(path!=null) {
            url = neo4JBaseUrl+path;
        }
        return restTemplate.exchange(url, httpMethod, httpEntity, responseClazz);
    }


    protected HttpEntity<?> getHttpEntity() {
        return getHttpEntity(null, null, null);
    }

    protected HttpEntity<?> getHttpEntity(final String username, final String password) {
        return getHttpEntity(null, username, password);
    }

    protected HttpEntity<?> getHttpEntity(final Object object, final String username, final String password) {
        final HttpHeaders headers = new HttpHeaders();
        if(username!=null && password!=null) {
            final String plainCreds = username+":"+password;
            final byte[] plainCredsBytes = plainCreds.getBytes();
            final String base64Creds = Base64.getEncoder().encodeToString(plainCredsBytes);
            headers.add("Accept", "application/json");
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Basic " + base64Creds);
        }

        final HttpEntity<?> httpEntity = new HttpEntity<Object>(object, headers);
        return httpEntity;
    }

    protected HttpEntity<String> getHttpEntity(final HttpHeaders headers) {
        return getHttpEntity(null, headers);
    }

    protected HttpEntity<String> getHttpEntity(final Object object, final HttpHeaders headers) {
        final HttpEntity<String> httpEntity = new HttpEntity(object, headers);
        return httpEntity;
    }

}
