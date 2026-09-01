package com.techshard.download;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * Suppresses the {@code WWW-Authenticate} challenge for calls made by the
 * upload page.
 *
 * <p>
 * The page collects the username and password in its own fields and sends them
 * as a Basic header. If the server answered a rejected login with the standard
 * challenge, the browser would pop its native credential dialog on top of that
 * form. Requests carrying {@code X-Requested-With: XMLHttpRequest} therefore get
 * a bare 401 that the page reports itself, while curl, Postman and every other
 * client still receive the normal challenge.
 * </p>
 */
public class XhrAwareBasicAuthEntryPoint extends BasicAuthenticationEntryPoint {

	private static final String REQUESTED_WITH_HEADER = "X-Requested-With";

	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

	@Override
	public void commence(final HttpServletRequest request, final HttpServletResponse response,
			final AuthenticationException authException) throws IOException {

		if (XML_HTTP_REQUEST.equals(request.getHeader(REQUESTED_WITH_HEADER))) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bad credentials");
			return;
		}
		super.commence(request, response, authException);
	}

}
