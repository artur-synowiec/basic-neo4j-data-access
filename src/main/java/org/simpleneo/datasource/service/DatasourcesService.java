package org.simpleneo.datasource.service;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.simpleneo.datasource.dao.Neo4JRestDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatasourcesService {

	@Autowired
	private Neo4JRestDao neo4JRestDao;

    public <T> T queryForObject(final String cypherQuery, final Map<String, Object> parameters, final Class<T> type) {
		return neo4JRestDao.queryForObject(cypherQuery, parameters, type);
    }

    public <T> T queryForJson(final String cypherQuery, final Map<String, Object> parameters, final Class<T> type) {
    	return queryForJson(cypherQuery, null, parameters, type, false);
    }

    public <T> T queryForJson(final String cypherQuery, final String element, final Map<String, Object> parameters, final Class<T> type) {
    	return queryForJson(cypherQuery, element, parameters, type, false);
    }

    public <T> T queryForJson(final String cypherQuery, final Map<String, Object> parameters, final Class<T> type, boolean raw) {
    	return queryForJson(cypherQuery, null, parameters, type, raw);
    }

    public <T> T queryForJson(final String cypherQuery, final String element, final Map<String, Object> parameters, final Class<T> type, boolean raw) {

    	if(raw) {
    		return neo4JRestDao.queryForJson(cypherQuery, parameters, type, raw);
    	}

    	if(type.isAssignableFrom(JSONObject.class)) {
    		JSONObject result = new JSONObject();
    		result.put(element, neo4JRestDao.queryForJson(cypherQuery, parameters, JSONArray.class));
    		return (T) result;
    	} else if(type.isAssignableFrom(JSONArray.class)) {
        	return neo4JRestDao.queryForJson(cypherQuery, parameters, type);
    	}

    	return null;
    }

}