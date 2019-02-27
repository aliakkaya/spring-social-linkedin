/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.linkedin.api.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.linkedin.api.CommunicationOperations;
import org.springframework.social.linkedin.api.CompanyOperations;
import org.springframework.social.linkedin.api.ConnectionOperations;
import org.springframework.social.linkedin.api.GroupOperations;
import org.springframework.social.linkedin.api.JobOperations;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.social.linkedin.api.NetworkUpdateOperations;
import org.springframework.social.linkedin.api.ProfileOperations;
import org.springframework.social.linkedin.api.impl.json.LinkedInModule;
import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;
import org.springframework.social.support.HttpRequestDecorator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * This is the central class for interacting with LinkedIn.
 * <p>
 * Greenhouse operations require OAuth authentication with the server.
 * Therefore, LinkedInTemplate must be constructed with the minimal information
 * required to sign requests with and OAuth 1 Authorization header.
 * </p>
 * @author Craig Walls
 */
public class LinkedInTemplate extends AbstractOAuth2ApiBinding implements LinkedIn {
	
	/**
	 * Creates a new LinkedInTemplate given the minimal amount of information needed to sign requests with OAuth 1 credentials.
	 * @param accessToken an access token acquired through OAuth authentication with LinkedIn
	 */
	public LinkedInTemplate(String accessToken) {
		super(accessToken);
		Assert.hasLength(accessToken, "Access token cannot be null or empty.");
		registerOAuth2Interceptor(accessToken);
		registerLinkedInJsonModule();
		registerJsonFormatInterceptor();
		initSubApis();
	}

	private void registerOAuth2Interceptor(String accessToken) {
		List<ClientHttpRequestInterceptor> interceptors = getRestTemplate().getInterceptors();
		interceptors.add(new OAuth2TokenParameterRequestInterceptor(accessToken));
		getRestTemplate().setInterceptors(interceptors);
	}
	
	public ConnectionOperations connectionOperations() {
		return connectionOperations;
	}
	
	public NetworkUpdateOperations networkUpdateOperations() {
		return networkUpdateOperations;
	}
	
	public ProfileOperations profileOperations() {
		return profileOperations;
	}
	
	public CompanyOperations companyOperations() {
		return companyOperations;
	}
	
	public CommunicationOperations communicationOperations() {
		return communicationOperations;
	}
	
	public JobOperations jobOperations() {
		return jobOperations;
	}
	
	public GroupOperations groupOperations() {
		return groupOperations;
	}
	
	public RestOperations restOperations() {
		return getRestTemplate();
	}
	
	@Override
	protected void configureRestTemplate(RestTemplate restTemplate) {
		restTemplate.setErrorHandler(new LinkedInErrorHandler());
	}
	
	// private helpers
	
	private void registerLinkedInJsonModule() {
		List<HttpMessageConverter<?>> converters = getRestTemplate().getMessageConverters();
		for (HttpMessageConverter<?> converter : converters) {
			if(converter instanceof MappingJackson2HttpMessageConverter) {
				MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
				objectMapper = new ObjectMapper();				
				objectMapper.registerModule(new LinkedInModule());
				objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
				objectMapper.configure(Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
				objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
				jsonConverter.setObjectMapper(objectMapper);
			}
		}
	}
	
	/*
	 * Have to register custom interceptor to
	 * set  "x-li-format: "json" header as
	 * otherwise we appear to get error from linkedin
	 * which suggests its expecting xml rather than json.
	 * API appears to ignore Content-Type header
	 */
	private void registerJsonFormatInterceptor() {		
		RestTemplate restTemplate = getRestTemplate();
		if (interceptorsSupported) {
			List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
			//interceptors.add(new LoggingInterceptor());
			interceptors.add(new JsonFormatInterceptor());
		} else {
			// for Spring 3.0.x where interceptors aren't supported
			ClientHttpRequestFactory originalRequestFactory = restTemplate.getRequestFactory();
			JsonFormatHeaderRequestFactory newRequestFactory = new JsonFormatHeaderRequestFactory(originalRequestFactory);
			restTemplate.setRequestFactory(newRequestFactory);
		}
	}
	
	private void initSubApis() {
		connectionOperations = new ConnectionTemplate(getRestTemplate());
		networkUpdateOperations = new NetworkUpdateTemplate(getRestTemplate());
		profileOperations = new ProfileTemplate(getRestTemplate(), objectMapper);
		companyOperations = new CompanyTemplate(getRestTemplate(), objectMapper);
		communicationOperations = new CommunicationTemplate(getRestTemplate());
		jobOperations = new JobTemplate(getRestTemplate(), objectMapper);
		groupOperations = new GroupTemplate(getRestTemplate());
	}
	
	private NetworkUpdateOperations networkUpdateOperations;
	
	private ProfileOperations profileOperations;
	
	private ConnectionOperations connectionOperations;
	
	private CompanyOperations companyOperations;
	
	private CommunicationOperations communicationOperations;
	
	private JobOperations jobOperations;
	
	private GroupOperations groupOperations;
	
	private ObjectMapper objectMapper;
	
	private static boolean interceptorsSupported = ClassUtils.isPresent("org.springframework.http.client.ClientHttpRequestInterceptor", LinkedInTemplate.class.getClassLoader());
	
	static final String BASE_URL = "https://api.linkedin.com/v2/";

	static final String BASE_ME_URL = BASE_URL + "me?projection=(id,firstName,maidenName,lastName,profilePicture(displayImage~:playableStreams))";

	static final String BASE_EMAIL_URL = BASE_URL + "emailAddress?q=members&projection=(elements*(handle~))";

	private static final class JsonFormatInterceptor implements ClientHttpRequestInterceptor {
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			HttpRequest contentTypeResourceRequest = new HttpRequestDecorator(request);
			contentTypeResourceRequest.getHeaders().add("x-li-format", "json");
			ClientHttpResponse response = execution.execute(contentTypeResourceRequest, body);
			return response;
		}
		
	}

	private static final class LoggingInterceptor implements ClientHttpRequestInterceptor {
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
											ClientHttpRequestExecution execution) throws IOException {
			HttpRequest contentTypeResourceRequest = new HttpRequestDecorator(request);
			traceRequest(request, body);
			ClientHttpResponse response = execution.execute(contentTypeResourceRequest, body);
			traceResponse(response);
			return response;
		}

	}

	private static void traceRequest(HttpRequest request, byte[] body) throws IOException {
		System.out.println("===========================request begin================================================");
		System.out.println("URI         : " + request.getURI());
		System.out.println("Method      : " + request.getMethod());
		System.out.println("Headers     : "+ request.getHeaders() );
		System.out.println("Request body: " + new String(body, "UTF-8"));
		System.out.println("==========================request end================================================");
	}

	private static void traceResponse(ClientHttpResponse response) throws IOException {
		StringBuilder inputStringBuilder = new StringBuilder();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody(), "UTF-8"));
		String line = bufferedReader.readLine();
		while (line != null) {
			inputStringBuilder.append(line);
			inputStringBuilder.append('\n');
			line = bufferedReader.readLine();
		}
		System.out.println("============================response begin==========================================");
		System.out.println("Status code  : " + response.getStatusCode());
		System.out.println("Status text  : " + response.getStatusText());
		System.out.println("Headers      : " + response.getHeaders());
		System.out.println("Response body: " + inputStringBuilder.toString());
		System.out.println("=======================response end=================================================");
	}
	
	private static final class OAuth2TokenParameterRequestInterceptor implements ClientHttpRequestInterceptor {
		private final String accessToken;
		
		public OAuth2TokenParameterRequestInterceptor(String accessToken) {
			this.accessToken = accessToken;
		}
		
		public ClientHttpResponse intercept(final HttpRequest request, final byte[] body, ClientHttpRequestExecution execution) throws IOException {
			HttpRequest protectedResourceRequest = new HttpRequestDecorator(request) {
				@Override
				public URI getURI() {
					return URI.create(super.getURI().toString() + (((super.getURI().getQuery() == null) ? "?" : "&") + "oauth2_access_token=" + accessToken));
				}
			};

			// LinkedIn doesn't accept the OAuth2 Bearer token authorization header.
			protectedResourceRequest.getHeaders().remove("Authorization"); 
			return execution.execute(protectedResourceRequest, body);
		}

	}

}
