package org.simpleneo.datasource.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.simpleneo.datasource.dao.Neo4JRestDao;
import org.simpleneo.datasource.service.DatasourcesService;
import org.simpleneo.datasource.utils.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.CaseFormat;
import com.google.common.base.Objects;
import com.sun.jersey.core.impl.provider.entity.Inflector;

public abstract class AbstractController {

	private static final String FIELDS_PARAM_NAME = "fields";
	private static final String PAGE_SIZE_PARAM_NAME = "size";
	private static final String OFFSET_PARAM_NAME = "offset";
	private static final String PAGE_PARAM_NAME = "page";
	private static final String SORT_PARAM_NAME = "sort";
	private static final String FROM_DATE_PARAM_NAME = "from";
	private static final String TO_DATE_PARAM_NAME = "to";
    static final int PAGE_DEFAULT_VALUE = 1;
    static final int SIZE_DEFAULT_VALUE = 20;

	public static final String DEFAULT_SKIP = "0";
	public static final String DEFAULT_LIMIT = "30";

    static final SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static final SimpleDateFormat REQUEST_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    static final String FROM_DATE_HOUR = "000000";
    static final String TO_DATE_HOUR = "235959";
    static final String NEO4J_DATE_FIELD = "createdAt";

    static final String PARAM_PREFIX = "param:";

    static final List<String> NON_WILDCARD_PROPERTIES = Collections.unmodifiableList(Arrays.asList("from", "to", "gender"));

	private static final String SEARCH_PARAM_SEPARATOR = ",";

	private static final List<String> RESERVED_PARAMS = Collections.unmodifiableList(Arrays.asList("attributes", FIELDS_PARAM_NAME, PAGE_PARAM_NAME, PAGE_SIZE_PARAM_NAME, OFFSET_PARAM_NAME, SORT_PARAM_NAME));

	private static final List<String> PAGINATION_AS_ATTRIBUTES = Collections.unmodifiableList(Arrays.asList("pagination"));

    protected static final Inflector INFLECTOR = Inflector.getInstance();

    private final Logger log = LoggerFactory.getLogger(AbstractController.class);
    static final String LOGGED_IN_SESSION_ATTRIBUTE = "LOGGED_IN";


	@Autowired
	protected Neo4JRestDao neo4JRestDao;

    @Autowired
    DatasourcesService datasourcesService;

	protected String createLabel(final String labelsPath) {
		String label = INFLECTOR.singularize(labelsPath);
		return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, label.substring(0, 1).toUpperCase() + label.substring(1));
	}

	protected String createMainNode(final String labelsPath) {
		return INFLECTOR.singularize(labelsPath).replaceAll("-", "");
	}

	protected void setContentType(final ContentFormat contentFormat, final HttpServletResponse response) {
		response.setContentType(contentFormat.getContenType());
	}

	protected String buildCypherQuery(final String labelsPath, final HttpServletRequest request) {
		String mainNode = createMainNode(labelsPath);
		String label = createLabel(labelsPath);
		return "MATCH (" + mainNode + ":" + label + ") " + queryWhere(request, mainNode, label) + buildReturnStatement(mainNode, request) + queryPagination(request);
	}

	protected Object getRawResult(final String cypherQuery, final String labelsPath, final ContentFormat contentFormat, final HttpServletRequest request) {
		return datasourcesService.queryForObject(cypherQuery, getQueryParameters(request), JSONObject.class);
	}

	protected Object getResult(String cypherQuery, final String labelsPath, final ContentFormat contentFormat, final HttpServletRequest request) {

		String mainNode = createMainNode(labelsPath);

		Map<String, Object> parameters = null;
    	Map<String, Object> result = new LinkedHashMap<>(); // Keep the order of Map

    	buildPaginationElement(result, cypherQuery, mainNode, null, request);

    	String elementName = ContentFormat.xml.equals(contentFormat) ? mainNode : labelsPath;

    	result.put(elementName, datasourcesService.queryForJson(cypherQuery, parameters, JSONArray.class) );

		switch (contentFormat) {
			case xml:
				return XmlUtils.JsonToXml(result, labelsPath, PAGINATION_AS_ATTRIBUTES);
			case json:
				return result;
			default:
				break;
		}

		return null;
	}

	protected String queryWhere(final HttpServletRequest request, String mainNode, String label) {
		return queryWhere(request, mainNode, label, null);
	}

	protected String queryWhere(final HttpServletRequest request, String mainNode, String label, String originalQuery) {

		StringBuilder result = new StringBuilder();
		StringBuilder queryWithSearch = new StringBuilder();
        final Map<String, String[]> parameters = request.getParameterMap();

        boolean camelCaseProperties = false;

        int i = 0;
        boolean hasSearchParams = false;
        for (final Entry<String, String[]> entry : parameters.entrySet()) {

            final String parameter = entry.getKey();

            boolean queryContains = originalQuery != null &&
            		(originalQuery.indexOf("{"+parameter+"}") > -1 || originalQuery.indexOf("{" + PARAM_PREFIX + parameter) > -1);

            if(RESERVED_PARAMS.contains(parameter) || (queryContains) ) {
                continue;
            }

            hasSearchParams = true;
            if(i>0) {
            	queryWithSearch.append(" AND ");
            }

            final String[] values = getParameterValues(parameter, parameters);

            if(values.length>1) {
            	queryWithSearch.append(" (");
            }

            for (int j = 0; j<values.length; j++) {
                if(j>0) {
                	queryWithSearch.append(" OR ");
                }
                final String value = values[j];

                final String names[] = parameter.split(SEARCH_PARAM_SEPARATOR);
                buildOrForMultipleAttributes(queryWithSearch, names, value, mainNode, label, camelCaseProperties);
            }
            if(values.length>1) {
            	queryWithSearch.append(") ");
            }
            i++;
        }

        if(hasSearchParams) {
        	result.append(" WHERE ").append(queryWithSearch);
        }

		return result.toString();
	}

	protected enum ContentFormat {
		json("application/json"),
		xml("text/xml"),
		csv("text/csv"),
		txt("text/plain");

		private final String contentType;

		private ContentFormat(String contentType) {
			this.contentType = contentType;
		}

		public String getContenType() {
			return contentType;
		}
	}

    private String[] getParameterValues(final String parameter, final Map<String, String[]> parameters) {
        final Set<String> setResult = new HashSet<String>(Arrays.asList(parameters.get(parameter)));
        for (final Entry<String, String[]> entry : parameters.entrySet()) {
            final String name = entry.getKey();
            final String names[] = name.split(SEARCH_PARAM_SEPARATOR);
            final String[] values = entry.getValue();
            for (final String n : names) {
                if(n.equals(parameter)) {
                    setResult.addAll(Arrays.asList(values));
                }
            }
        }
        return setResult.toArray(new String[setResult.size()]);
    }

    protected String queryPagination(final HttpServletRequest request) {

		String skipParam = request.getParameter("offset");
		String limitParam = request.getParameter("length");

		int skip = Integer.valueOf(DEFAULT_SKIP);
		int limit = Integer.valueOf(DEFAULT_LIMIT);

		if(skipParam != null) skip = Integer.valueOf(skipParam);
		if(limitParam != null) limit = Integer.valueOf(limitParam);

		String pageParam = request.getParameter("page");
		String sizeParam = request.getParameter("size");

		if(sizeParam != null) {
			limit = Integer.valueOf(sizeParam);
		}

		if(pageParam != null) {
			int page = Integer.valueOf(pageParam);
			skip = limit * (page-1);
		}

		return " SKIP " +skip + " LIMIT " + limit;

	}

    private void buildOrForMultipleAttributes(final StringBuilder queryWithSearch, final String[] names, String value, final String mainNode, final String label, final boolean camelCaseProperties) {

        if(names.length>1) {
            queryWithSearch.append(" (");
        }

        for (int j = 0;j<names.length;j++) {
            String name = names[j];
            if(camelCaseProperties) {
                name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
            }

            if(j>0) {
                queryWithSearch.append(" OR ");
            }

            final String operator = getOperator(name, value);

            if(FROM_DATE_PARAM_NAME.equals(name) || TO_DATE_PARAM_NAME.equals(name)) {
                try {
                    if(FROM_DATE_PARAM_NAME.equals(name)) value += FROM_DATE_HOUR;
                    if(TO_DATE_PARAM_NAME.equals(name)) value += TO_DATE_HOUR;
                    value = SQL_DATE_FORMAT.format(REQUEST_DATE_FORMAT.parse(value));
                    name = NEO4J_DATE_FIELD;

                } catch (final ParseException e) {
                    e.printStackTrace();
                }
            }

            // if name already has a dot - leave it
            name = name.indexOf(".")>-1 ? name : mainNode+"."+name;

            int indOfSecondTilda = value.indexOf("~", 1);
            if(indOfSecondTilda > -1) {
                value = value.substring(indOfSecondTilda + 1);
            }

            queryWithSearch.append(name + operator + getStartQuote(name, value) + value + getEndQuote(name, value));

        }

        if(names.length>1) {
            queryWithSearch.append(") ");
        }

    }

	private String buildReturnStatement(String mainNode, HttpServletRequest request) {
		List<String> fieldList = new ArrayList<>();
		String[] fields = request.getParameterValues(FIELDS_PARAM_NAME);
		if(fields != null) {
			if(fields.length == 1 && fields[0].indexOf(",") > -1) {
				fields = fields[0].split(",");
			}

			for (String field : fields) {
				fieldList.add(field);
	        }
		}

		StringBuilder fieldsQuery = new StringBuilder();

		for (int i = 0; i < fieldList.size(); i++) {
			String comma = (i < fieldList.size()-1)?", ":"";
			String fieldItem = fieldList.get(i);
			fieldsQuery.append(mainNode + "."+fieldItem + " AS "+fieldItem+" " + comma);
        }

		if(fieldsQuery.length() > 0) {
			return " RETURN " + fieldsQuery;
		}
		return " RETURN " + mainNode+ " ";
	}

    protected void buildPaginationElement(JSONObject result, String cypherQuery, String mainNode, HttpServletRequest request) {
    	buildPaginationElement(result, cypherQuery, mainNode, null, request);
    }

    protected void buildPaginationElement(Map<String, Object> result, String cypherQuery, String mainNode, String countQuery, HttpServletRequest request) {

        final int page = getNumericalParam(request, PAGE_PARAM_NAME, PAGE_DEFAULT_VALUE);
        final int size = getNumericalParam(request, PAGE_SIZE_PARAM_NAME, SIZE_DEFAULT_VALUE);

        if(countQuery == null) {
        	countQuery = countQuery(cypherQuery, mainNode);
        }

        Integer total = -1;
        try {
        	//TODO: Simplify by not calling raw
    		JSONObject paginationResult = datasourcesService.queryForJson(countQuery, "total", getQueryParameters(request), JSONObject.class, true);
        	List<Map<String, Object>> results = (List<Map<String, Object>>) paginationResult.get("results");
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) results.get(0).get("data");
            List<Integer> totals = (List<Integer>) dataList.get(0).get("row");
            total = totals.get(0);
        } catch (Exception e) {
        	log.error("Pagination query exception: " + e);
        	e.printStackTrace();
        	return;
        }

        final int totalPages = (int) Math.ceil((double) total / size);

        JSONObject paginationElement = new JSONObject();
        paginationElement.put("total", total);
        paginationElement.put("totalPages", totalPages);
        paginationElement.put("page", page);
        paginationElement.put("size", size);

        result.put("pagination", paginationElement);
    }

    //TODO: CREATE DIFFERENT UTIL CLASSES
	private String countQuery(String query) {
		return countQuery(query, null);
	}

	protected String countQuery(String query, String mainNode) {
		if(mainNode == null) {
			mainNode = "n";
		}

		int indOf = query.indexOf("RETURN");
        String countQuery = query.substring(0, indOf) + " RETURN DISTINCT COUNT("+mainNode+") AS total";
		return countQuery;
	}

    protected int getNumericalParam(final HttpServletRequest request, final String name, final int defaultValue) {
        if(request.getParameter(name)==null) {
            return defaultValue;
        } else {
            return Integer.valueOf(request.getParameter(name));
        }
    }

    protected Map<String, Object> getQueryParameters(HttpServletRequest request) {
        Map<String, Object> parameters = new HashMap<String, Object>();
		for(Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();) {
			String name = (String) e.nextElement();
			String value = request.getParameter(name);
			parameters.put(name, value);
		}
		return parameters;
    }

    protected String getOperator(final String name, String value) {

    	if(FROM_DATE_PARAM_NAME.equals(name)) {
            return " > ";
        }

        if(TO_DATE_PARAM_NAME.equals(name)) {
            return " < ";
        }

        if(value.startsWith("~")) {
            int indOfSecondTilda = value.indexOf("~", 1);
            if(indOfSecondTilda > -1) {
                String op = value.substring(0, indOfSecondTilda + 1);
                QueryOperator queryOperator = QueryOperator.toQueryOperator(op);
                return queryOperator.getOperator();
            }
        }

        return "true".equals(value) || "false".equals(value) || NumberUtils.isNumber(value) ? " = " : " CONTAINS ";
    }

    protected String getStartQuote(final String name, final String value) {
        if(NON_WILDCARD_PROPERTIES.contains(name)) {
            return "'";
        }
        return "true".equals(value) || "false".equals(value) ? "" : "'";
    }

    protected String getEndQuote(final String name, final String value) {
        if(NON_WILDCARD_PROPERTIES.contains(name)) {
            return "'";
        }
        return "true".equals(value) || "false".equals(value) ? "" : "'";
    }


	/**
	Based on IBM:
	http://www.ibm.com/support/knowledgecenter/SSZRHJ/com.ibm.mif.doc/gp_intfrmwk/rest_api/c_resource_attribute_param.html

	TODO: ~num~ (numerical values)
	Equals  	        ~eq~	             status=~eq~APPR Use the equals operator to perform an exact match.
	Not equals	        ~neq~	             status=~neq~APPR
	Greater than	    ~gt~	             quantity=~gt~2.5
	Greater than equals	~gteq~	             quantity=~gteq~2.5
	Less than	        ~lt~	             quantity=~lt~2.5
	Less than equals	~lteq~	             quantity=~lteq~2.5
	Ends with	        ~ew~	             description=~ew~APPR
	Starts with	        ~sw~	             description=~sw~APPR
	Like	            No notation required description=APPR
	 */

	private enum QueryOperator {
		LIKE("", "  CONTAINS "),
		EQUALS("~eq~", " = "),
		NOT_EQUALS("~neq~", " != "),
		GREATER_THAN("~gt~", " > "),
		GREATER_THAN_OR_EQUALS("~gteq~", " >= "),
		LESS_THAN("~lt~", " < "),
		LESS_THAN_OR_EQUALS("~lteq~", " <= "),
		STARTS_WITH("~sw~", " =~ "),
		ENDS_WITH("~ew~", " =~ ");

		private final String prefix;
		private final String operator;

		QueryOperator(String prefix, String operator) {
			this.prefix = prefix;
			this.operator = operator;
		}

		public static QueryOperator toQueryOperator(String prefix) {
			for (QueryOperator qo : QueryOperator.values()) {
				if(Objects.equal(qo.getPrefix(), prefix)) {
					return qo;
				}
			}
			return null;
		}

		public String getOperator() {
			return operator;
		}

		public String getPrefix() {
			return prefix;
		}
	}


}