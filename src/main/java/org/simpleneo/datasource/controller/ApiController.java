package org.simpleneo.datasource.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generic api endpoints. Paginated, searchable and return fields customisable.
 *
 *  Format:- /api/<node-name>.<format>?fields=accountId
 * i.e.
 * /api/users.json?fields=accountId&page=2&size=4
 * /api/users.xml
 */
@RestController
public class ApiController extends AbstractController {

	/**
	 * The main generic endpoint (called by node names - in plural)
	 * @param request
	 * @param response
	 * @param labelsPath
	 * @param contentFormat
	 * @return
	 */
	@RequestMapping(value="/api/{labelsPath:.+}.{contentFormat}")
    public Object api(final HttpServletRequest request,
    				  final HttpServletResponse response,
    				  @PathVariable("labelsPath") String labelsPath,
    				  @PathVariable("contentFormat") ContentFormat contentFormat) {

		setContentType(contentFormat, response);
        return getResult(buildCypherQuery(labelsPath, request), labelsPath, contentFormat, request);

	}

	/**
	 * Raw result (as it comes out of Neo4j REST API)
	 * @param request
	 * @param response
	 * @param labelsPath
	 * @param contentFormat
	 * @return
	 */
	@RequestMapping(value="/raw/{labelsPath:.+}.{contentFormat}")
    public Object rawApi(final HttpServletRequest request,
    				  final HttpServletResponse response,
    				  @PathVariable("labelsPath") String labelsPath,
    				  @PathVariable("contentFormat") ContentFormat contentFormat) {

		setContentType(contentFormat, response);
        return getRawResult(buildCypherQuery(labelsPath, request), labelsPath, contentFormat, request);

	}

}