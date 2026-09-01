package com.techshard.download;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * Only {@code /api/**} is guarded — by HTTP Basic, using the credentials
 * configured in {@code application.yml}. The upload page itself is served
 * anonymously; it collects the credentials in its own fields and sends them on
 * each API call, so the browser never has to challenge for them (see
 * {@link XhrAwareBasicAuthEntryPoint}).
 *
 * <p>
 * CSRF is switched off for the API because it is driven by non-browser clients
 * (curl, scripts, other services) that carry no token, and the default
 * protection would reject every upload with a 403.
 * </p>
 */
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	private static final String API_PATTERN = "/api/**";

	/** The static upload page and the assets a browser fetches alongside it. */
	private static final String[] PUBLIC_PATTERNS = { "/", "/index.html", "/favicon.ico" };

	private static final String REALM_NAME = "streaming-file-service";

	@Bean
	public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
		final XhrAwareBasicAuthEntryPoint entryPoint = new XhrAwareBasicAuthEntryPoint();
		entryPoint.setRealmName(REALM_NAME);
		return entryPoint;
	}

	@Override
	protected void configure(final HttpSecurity http) throws Exception {
		http.csrf().ignoringAntMatchers(API_PATTERN)
			.and()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
			.authorizeRequests()
				.antMatchers(PUBLIC_PATTERNS).permitAll()
				.antMatchers(API_PATTERN).authenticated()
				.anyRequest().authenticated()
			.and()
			.httpBasic().authenticationEntryPoint(basicAuthenticationEntryPoint());
	}

}
