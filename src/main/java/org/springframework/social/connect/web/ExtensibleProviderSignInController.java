package org.springframework.social.connect.web;

import java.util.List;

import javax.inject.Inject;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.web.ProviderSignInAttempt;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.support.URIBuilder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.servlet.view.RedirectView;

public class ExtensibleProviderSignInController extends
		ProviderSignInController {

	private final UsersConnectionRepository usersConnectionRepository;
	protected final ConnectionFactoryLocator connectionFactoryLocator;
	private final SignInAdapter signInAdapter;
	
	private String signUpUrl;
	private String signInUrl;
	private String postSignInUrl;
	
	
	@Inject
	public ExtensibleProviderSignInController(
			ConnectionFactoryLocator connectionFactoryLocator,
			UsersConnectionRepository usersConnectionRepository,
			SignInAdapter signInAdapter) {
		super(connectionFactoryLocator, usersConnectionRepository, signInAdapter);
		this.usersConnectionRepository  = usersConnectionRepository;
		this.connectionFactoryLocator = connectionFactoryLocator;
		this.signInAdapter = signInAdapter;
	}
	
	
	
	public void setSignUpUrl(String signUpUrl) {
		super.setSignUpUrl(signUpUrl);
		this.signUpUrl = signUpUrl;
		
	}



	public void setSignInUrl(String signInUrl) {
		super.setSignInUrl(signInUrl);
		this.signInUrl = signInUrl;
	}


	public void setPostSignInUrl(String postSignInUrl) {
		super.setPostSignInUrl(postSignInUrl);
		this.postSignInUrl = postSignInUrl;
	}

	protected RedirectView handleSignIn(Connection<?> connection, NativeWebRequest request) {
		List<String> userIds = usersConnectionRepository.findUserIdsWithConnection(connection);
		if (userIds.size() == 0) {
			ProviderSignInAttempt signInAttempt = new ProviderSignInAttempt(connection, connectionFactoryLocator, usersConnectionRepository);
			request.setAttribute(ProviderSignInAttempt.SESSION_ATTRIBUTE, signInAttempt, RequestAttributes.SCOPE_SESSION);
			return redirect(signUpUrl);
		} else if (userIds.size() == 1){
			usersConnectionRepository.createConnectionRepository(userIds.get(0)).updateConnection(connection);
			String originalUrl = signInAdapter.signIn(userIds.get(0), connection, request);
			return originalUrl != null ? redirect(originalUrl) : redirect(postSignInUrl);
		} else {
			return redirect(URIBuilder.fromUri(signInUrl).queryParam("error", "multiple_users").build().toString());
		}
	}

	private RedirectView redirect(String url) {
		return new RedirectView(url, true);
	}

}
