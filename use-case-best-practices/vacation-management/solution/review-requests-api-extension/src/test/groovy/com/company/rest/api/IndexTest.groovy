package com.company.rest.api;

import groovy.json.JsonSlurper

import java.util.logging.Logger

import javax.servlet.http.HttpServletRequest

import org.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.business.data.impl.SimpleBusinessDataReferenceImpl
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.extension.rest.RestAPIContext;
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import bizdata.VacationAvailable;
import bizdata.VacationAvailableDAO;
import bizdata.VacationRequestDAO;
import spock.lang.Specification
    
class IndexTest extends Specification {

    def request = Mock(HttpServletRequest)
    def resourceProvider = Mock(ResourceProvider)
    def context = Mock(RestAPIContext)
	def apiClient = Mock(APIClient)
	def session = Mock(APISession)
	def processAPI = Mock(ProcessAPI)
	def searchResult = Mock(SearchResult)
    def responseBuilder = new RestApiResponseBuilder()
	def humanTaskInstance = Mock(HumanTaskInstance);
	def dao = Mock(VacationAvailableDAO)
	

    def index = Spy(Index)

    def "should return a json representation as result"() {
        request.parameterNames >> (["p", "c"] as Enumeration)
        request.getParameter("p") >> "0"
        request.getParameter("c") >> "20"

		apiClient.processAPI >> processAPI
		context.apiClient >> apiClient
		context.apiSession >> session
		apiClient.getDAO(VacationRequestDAO.class) >> dao
		def vacationAvailable = new VacationAvailable()
		vacationAvailable.persistenceId = 42
		dao.findByPersistenceId(42) >> vacationAvailable
		session.userId >> 1
		processAPI.searchMyAvailableHumanTasks(_, _) >> searchResult
		humanTaskInstance.id >> 1
		processAPI.getUserTaskExecutionContext(1) >> ["vacationRequest_ref" : new SimpleBusinessDataReferenceImpl("", "", 42)]
		searchResult.result >> [humanTaskInstance]
		searchResult.count >> 100
		
        when:
        index.doHandle(request, responseBuilder, context)

        then:
        def returnedJson = new JsonSlurper().parseText(responseBuilder.build().response)
        //Assertions
        returnedJson.size == 1
		returnedJson[0].task.id == 1
		returnedJson[0].vacationRequest.persistenceId == 42
    }

    def "should return an error response if p is not set"() {
        request.parameterNames >> (["p", "c"] as Enumeration)
        request.getParameter("p") >> null
        request.getParameter("c") >> "aValue2"
        
        context.resourceProvider >> resourceProvider
        resourceProvider.getResourceAsStream("configuration.properties") >> index.class.classLoader.getResourceAsStream("configuration.properties")

        when:
        index.doHandle(request, responseBuilder, context)

        then:  
        RestApiResponse restApiResponse = responseBuilder.build()
        def returnedJson = new JsonSlurper().parseText(restApiResponse.response)
        //Assertions
        restApiResponse.httpStatus == 400
        returnedJson.error == "the parameter p is missing"
    }

    def "should return an error response if c is not set"() {
        request.parameterNames >> (["p", "c"] as Enumeration)
        request.getParameter("c") >> null
        request.getParameter("p") >> "aValue1"
        
        context.resourceProvider >> resourceProvider
        resourceProvider.getResourceAsStream("configuration.properties") >> index.class.classLoader.getResourceAsStream("configuration.properties")

        when:
        index.doHandle(request, responseBuilder, context)

        then:  
        RestApiResponse restApiResponse = responseBuilder.build()
        def returnedJson = new JsonSlurper().parseText(restApiResponse.response)
        //Assertions
        restApiResponse.httpStatus == 400
        returnedJson.error == "the parameter c is missing"
    }

}