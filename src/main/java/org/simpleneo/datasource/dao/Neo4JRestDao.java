package org.simpleneo.datasource.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

@Repository
public class Neo4JRestDao extends AbstractRestDao {

    private static final String DEFAULT_DATA_NODE = "data";

    private final Logger log = LoggerFactory.getLogger(Neo4JRestDao.class);

    public <T> T queryForObject(final String cypherQuery, final Map<String, Object> parameters, final Class<T> type) {


    	String params = buildParametersString(parameters);
    	String path = "transaction/commit";
    	String body = "{\"statements\" : [ {\"statement\" : \""+cypherQuery+"\""
    			+ params
    			+ "} ]}";

    	return (T) callNeo4JAPI(path, body, HttpMethod.POST, type);
    }



    public <T> T queryForJson(final String cypherQuery, final Map<String, Object> parameters, final Class<T> type) {
    	return queryForJson(cypherQuery, parameters, type, false);
    }

    public <T> T queryForJson(String cypherQuery, Map<String, Object> parameters, Class<T> type, boolean raw) {

    	log.debug("Running cypher cypher: " + cypherQuery);

    	String params = buildParametersString(parameters);
    	String path = "transaction/commit";
    	String body = "{\"statements\" : [ {\"statement\" : \""+cypherQuery+"\""
    			+ params
    			+ "} ]}";

    	ResponseEntity<JSONObject> result = callNeo4JAPI(path, body, HttpMethod.POST, JSONObject.class);

    	if(raw) {
    		return (T) result.getBody();
    	}

    	if(type.isAssignableFrom(JSONObject.class)) {
        	return (T) toJson(result.getBody(), JSONObject.class);
    	} else if(type.isAssignableFrom(JSONArray.class)) {
        	return (T) toJson(result.getBody(), JSONArray.class);
    	}
    	return null;
    }


    private String buildParametersString(Map<String, Object> parameters) {
    	StringBuilder params = new StringBuilder();
    	if(parameters!=null) {
        	params.append(", \"parameters\" : ");
        	params.append(" { ");
        	int i = 0;
            for (final Entry<String, Object> entry : parameters.entrySet()) {
                final String name = entry.getKey();
                Object value = entry.getValue();
                String val = NumberUtils.isNumber((String)value)?""+value:"\""+value+"\"";
            	if(i>0) {
                	params.append(", ");
            	}
                params.append(" \""+name+"\" : "+val+"  ");
            	i++;
            }
        	params.append(" } ");
    	}
    	return params.toString();
    }

    private <T> T toJson(JSONObject neo4jJson, Class<T> type) {

    	List<Map<String, Object>> results = (List<Map<String, Object>>) neo4jJson.get("results");

    	JSONArray simpleJsonArray = new JSONArray();

        List<String> columns = (List<String>) results.get(0).get("columns");
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) results.get(0).get("data");

        for (Map<String, Object> dataListItem : dataList) {
        	List<String> row = (List<String>) dataListItem.get("row");

        	Map<Object, Object> dataItem = new LinkedHashMap<>(); // Keep the order of Map

	        for(int i=0; i<columns.size(); i++) {
	        	String column = columns.get(i);
	        	Object r = row.get(i);
	        	if(r instanceof Map<?, ?>) {
			        Map<String, Object> m = (Map<String, Object>) r;
			        Set<?> keys = m.keySet();
			        for (Object key : keys) {
			        	Object value = m.get(key);
			        	dataItem.put(key, value);
			        }
	        	} else if(r instanceof String) {
	        		dataItem.put(column, r);
	        	} else {
	        		dataItem.put(column, r);
	        	}

		        //dataItem.put(column, row.get(i));
	        }
	        simpleJsonArray.add(dataItem);
        }

    	if(type.isAssignableFrom(JSONObject.class)) {
        	JSONObject result = new JSONObject();
            result.put(DEFAULT_DATA_NODE, simpleJsonArray);
        	return (T) result;
    	} else if(type.isAssignableFrom(JSONArray.class)) {
        	return (T) simpleJsonArray;
    	}

    	return null;

    }

}