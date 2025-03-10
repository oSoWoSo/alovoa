package com.nonononoki.alovoa.rest;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.html.LoginResource;
import com.nonononoki.alovoa.html.ProfileResource;
import com.nonononoki.alovoa.html.RegisterResource;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;

@RestController
@RequestMapping("/")
public class Oauth2Controller {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private OAuth2AuthorizedClientService clientService;

	@Autowired
	private RegisterResource registerResource;

	@Autowired
	private LoginResource loginResource;

	@SuppressWarnings("rawtypes")
	@GetMapping("/login/oauth2/success")
	public ModelAndView oauth2Success()
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		try {
			OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

			String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();

			OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(clientRegistrationId,
					oauthToken.getName());
			String endpoint = client.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();

			if (!endpoint.isEmpty()) {
				RestTemplate template = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken().getTokenValue());
				HttpEntity<String> entity = new HttpEntity<>("", headers);

				// get user data via URL from the oauth2 provider
				ResponseEntity<Map> response = template.exchange(endpoint, HttpMethod.GET, entity, Map.class);
				Map attributes = response.getBody();

				if (attributes == null) {
					throw new AlovoaException("oauth_attributes_not_found");
				}

				String email = (String) attributes.get("email");
				email = email.toLowerCase();

				User user = userRepo.findByEmail(email);
				if (user == null) {
					user = new User();
					user.setEmail(email);
				}

				// administrator cannot use oauth for security reason e.g. password breach on
				// oath provider
				if (user.isAdmin()) {
					throw new AlovoaException("not_supported_for_admin");
				}

				if (!user.isConfirmed()) {
					return registerResource.registerOauth();
				} else {
					return new ModelAndView("redirect:" + ProfileResource.getUrl());
				}
			}

			return loginResource.login();

		} catch (Exception e) {
			return loginResource.login();
		}
	}
}
