package com.nonononoki.alovoa.rest;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.DonationBmac;
import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.model.DonationKofi;
import com.nonononoki.alovoa.service.DonateService;

@Controller
@RequestMapping("/donate")
public class DonateController {

	@Autowired
	private DonateService donateService;

	@Autowired
	private ObjectMapper objectMapper;

	private static final Logger logger = LoggerFactory.getLogger(DonateController.class);

	@GetMapping("/search/{filter}")
	public String filterRecent(Model model, @PathVariable int filter)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		List<DonationDto> donations = donateService.filter(filter);
		model.addAttribute("donations", donations);
		model.addAttribute("filter", filter);
		return "fragments :: donate-filter";
	}

	@PostMapping(value = "/received/kofi", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> receivedKofi(String data)
			throws UnknownHostException, MalformedURLException, JsonProcessingException {
		logger.info(data);
		donateService.donationReceivedKofi(objectMapper.readValue(data, DonationKofi.class));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/received/bmac")
	public ResponseEntity<String> receivedBmac(@RequestBody DonationBmac data)
			throws JsonProcessingException, UnknownHostException, MalformedURLException {
		String logInfo = objectMapper.writeValueAsString(data);
		logger.info(logInfo);
		donateService.donationReceivedBmac(data);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
