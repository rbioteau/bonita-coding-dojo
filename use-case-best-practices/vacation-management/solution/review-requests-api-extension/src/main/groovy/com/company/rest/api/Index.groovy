package com.company.rest.api;

import java.io.Serializable;

import org.apache.http.HttpHeaders;

import groovy.json.JsonBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.engine.business.data.SimpleBusinessDataReference;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.web.extension.ResourceProvider;
import org.bonitasoft.web.extension.rest.RestAPIContext;
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder;
import org.bonitasoft.web.extension.rest.RestApiResponse;
import org.bonitasoft.web.extension.rest.RestApiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bonitasoft.engine.bpm.flownode.ActivityStates.READY_STATE
import static org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor.*
import bizdata.VacationRequestDAO;

class Index implements RestApiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(Index.class)

	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		//Retrieve p parameter
		def p = request.getParameter "p"
		if (p == null) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter p is missing"}""")
		}

		//Retrieve c parameter
		def c = request.getParameter "c"
		if (c == null) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter c is missing"}""")
		}

		def apiClient = context.apiClient
		def searchResult = apiClient.processAPI.searchMyAvailableHumanTasks(context.apiSession.userId, new SearchOptionsBuilder(p as int, c as int)
				.sort(PRIORITY, Order.DESC)
				.filter(STATE_NAME, READY_STATE)
				.filter(NAME, "Review request")
				.done())

		def dao = apiClient.getDAO(VacationRequestDAO.class);
		def result = searchResult.getResult().collect { task ->
			def taskContext = apiClient.processAPI.getUserTaskExecutionContext(task.getId());
			SimpleBusinessDataReference reference = taskContext["vacationRequest_ref"] as SimpleBusinessDataReference;
			return [
				task           : task,
				vacationRequest: dao.findByPersistenceId(reference.getStorageId())
			]
		}

		return buildPagedResponse(responseBuilder, new JsonBuilder(result).toPrettyString(),p as int, c as int, searchResult.count)
	}

	/**
	 * Build an HTTP response.
	 * 
	 * @param  responseBuilder the Rest API response builder
	 * @param  httpStatus the status of the response
	 * @param  body the response body
	 * @return a RestAPIResponse
	 */
	RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
		return responseBuilder.with {
			withResponseStatus(httpStatus)
			withResponse(body)
			build()
		}
	}

	/**
	 * Returns a paged result like Bonita BPM REST APIs.
	 * Build a response with content-range data in the HTTP header.
	 * 
	 * @param  responseBuilder the Rest API response builder
	 * @param  body the response body
	 * @param  p the page index
	 * @param  c the number of result per page
	 * @param  total the total number of results
	 * @return a RestAPIResponse
	 */
	RestApiResponse buildPagedResponse(RestApiResponseBuilder responseBuilder, Serializable body, int p, int c, long total) {
		return responseBuilder.with {
			withAdditionalHeader(HttpHeaders.CONTENT_RANGE,"$p-$c/$total");
			withResponse(body)
			build()
		}
	}

	/**
	 * Load a property file into a java.util.Properties
	 */
	Properties loadProperties(String fileName, ResourceProvider resourceProvider) {
		Properties props = new Properties()
		resourceProvider.getResourceAsStream(fileName).withStream { InputStream s ->
			props.load s
		}
		props
	}

}
