/*
 * Copyright 2011 the original author or authors.
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

/**
* Modified version of ProviderSignInController which can be subclassed
* so as to allow use of non-OAuth1 and OAuth2 Providers (eg. Last.fm) whose 
* authentication dance is similar to OAuth but has some differences.
* ProviderSignInController itself has restriction of either OAuth1 or OAuth2
* 
* TODO - Fork ProviderSignInController in spring-social-web
* 
* @author Michael Lavelle
*/
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
