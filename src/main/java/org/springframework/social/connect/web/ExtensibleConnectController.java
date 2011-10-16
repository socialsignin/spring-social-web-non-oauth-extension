package org.springframework.social.connect.web;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.core.GenericTypeResolver;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.DuplicateConnectionException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
* Modified version of ConnectController which can be subclassed
* so as to allow use of non-OAuth1 and OAuth2 Providers (eg. Last.fm) whose 
* authentication dance is similar to OAuth but has some differences.
* 
* ConnectController itself has restriction of either OAuth1 or OAuth2 and does not
* allow required methods to be overriden to alter this behaviour
* 
* TODO - Fork ConnectController in spring-social-web
* 
* @author Michael Lavelle
*/
public class ExtensibleConnectController extends ConnectController {

	private final MultiValueMap<Class<?>, ConnectInterceptor<?>> interceptors = new LinkedMultiValueMap<Class<?>, ConnectInterceptor<?>>();
	protected final ConnectionFactoryLocator connectionFactoryLocator;
	private final ConnectionRepository connectionRepository;

	private static final String DUPLICATE_CONNECTION_ATTRIBUTE = "social.addConnection.duplicate";
	
	@Inject
	public ExtensibleConnectController(
			ConnectionFactoryLocator connectionFactoryLocator,
			ConnectionRepository connectionRepository) {
		super(connectionFactoryLocator, connectionRepository);
		this.connectionFactoryLocator = connectionFactoryLocator;
		this.connectionRepository = connectionRepository;
	} 
	

	/**
	* Adds a ConnectInterceptor to receive callbacks during the connection process.
	* Useful for programmatic configuration.
	* @param interceptor the connect interceptor to add
	*/
	@Override
	public void addInterceptor(ConnectInterceptor<?> interceptor) {
		super.addInterceptor(interceptor);
		Class<?> serviceApiType = GenericTypeResolver.resolveTypeArgument(interceptor.getClass(), ConnectInterceptor.class);
	interceptors.add(serviceApiType, interceptor);
	}
	 
	
	protected void addConnection(Connection<?> connection, ConnectionFactory<?> connectionFactory, WebRequest request) {
		try {
		connectionRepository.addConnection(connection);
		postConnect(connectionFactory, connection, request);
		} catch (DuplicateConnectionException e) {
		request.setAttribute(DUPLICATE_CONNECTION_ATTRIBUTE, e, RequestAttributes.SCOPE_SESSION);
		}
		}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void postConnect(ConnectionFactory<?> connectionFactory, Connection<?> connection, WebRequest request) {
	for (ConnectInterceptor interceptor : interceptingConnectionsTo(connectionFactory)) {
	interceptor.postConnect(connection, request);
	}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void preConnect(ConnectionFactory<?> connectionFactory, MultiValueMap<String, String> parameters, WebRequest request) {
	for (ConnectInterceptor interceptor : interceptingConnectionsTo(connectionFactory)) {
	interceptor.preConnect(connectionFactory, parameters, request);
	}
	}
	
	protected List<ConnectInterceptor<?>> interceptingConnectionsTo(ConnectionFactory<?> connectionFactory) {
		Class<?> serviceType = GenericTypeResolver.resolveTypeArgument(connectionFactory.getClass(), ConnectionFactory.class);
		List<ConnectInterceptor<?>> typedInterceptors = interceptors.get(serviceType);
		if (typedInterceptors == null) {
		typedInterceptors = Collections.emptyList();
		}
		return typedInterceptors;
		}

}
