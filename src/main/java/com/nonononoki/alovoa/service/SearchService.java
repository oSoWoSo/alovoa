package com.nonononoki.alovoa.service;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SearchDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.model.UserSearchRequest;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class SearchService {

	public static final int SORT_DISTANCE = 1;
	public static final int SORT_ACTIVE_DATE = 2;
	public static final int SORT_INTEREST = 3;
	public static final int SORT_DONATION = 4;

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private PublicService publicService;

	@Value("${app.search.max}")
	private int maxResults;

	@Value("${app.search.max.distance}")
	private int maxDistance;

	@Value("${app.age.min}")
	private int ageMin;

	@Value("${app.age.max}")
	private int ageMax;

	private static final double LATITUDE = 111.1;
	private static final double LONGITUDE = 111.320;

	public SearchDto search(Double latitude, Double longitude, int distance, int sort) throws AlovoaException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {

		int ageLegal = Tools.AGE_LEGAL;
		
		if (distance > maxDistance) {
			throw new AlovoaException("max_distance_exceeded");
		}

		User user = authService.getCurrentUser();
		user.getDates().setActiveDate(new Date());
		// rounding to improve privacy
		DecimalFormat df = new DecimalFormat("#.##");
		df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		user.setLocationLatitude(Double.valueOf(df.format(latitude)));
		user.setLocationLongitude(Double.valueOf(df.format(longitude)));
		userRepo.saveAndFlush(user);

		int age = Tools.calcUserAge(user);
		boolean isLegalAge = age >= ageLegal;
		int minAge = user.getPreferedMinAge();
		int maxAge = user.getPreferedMaxAge();

		if (isLegalAge && minAge < ageLegal) {
			minAge = ageLegal;
		}
		if (!isLegalAge && maxAge >= ageLegal) {
			maxAge = ageLegal - 1;
		}

		Date minDate = Tools.ageToDate(maxAge);
		Date maxDate = Tools.ageToDate(minAge);

		double deltaLat = distance / LATITUDE;
		double deltaLong = distance / (LONGITUDE * Math.cos(latitude / 180.0 * Math.PI));
		double minLat = latitude - deltaLat;
		double maxLat = latitude + deltaLat;
		double minLong = longitude - deltaLong;
		double maxLong = longitude + deltaLong;

		UserSearchRequest request = UserSearchRequest.builder().minLat(minLat).minLong(minLong).maxLat(maxLat)
				.maxLong(maxLong).maxDate(maxDate).minDate(minDate).intentionText(user.getIntention().getText())
				.likeIds(user.getLikes().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
				.blockIds(user.getBlockedUsers().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
				.hideIds(user.getHiddenUsers().stream().map(o -> o.getUserTo().getId()).collect(Collectors.toSet()))
				.genderTexts(user.getPreferedGenders().stream().map(o -> o.getText()).collect(Collectors.toSet()))
				.build();

		// because IS IN does not work with empty list
		request.getBlockIds().add(user.getId());
		request.getLikeIds().add(0L);
		request.getHideIds().add(0L);

		List<User> users = userRepo.usersSearch(request);

		Set<Long> ignoreIds = new HashSet<>();
		ignoreIds.addAll(
				user.getBlockedByUsers().stream().map(o -> o.getUserFrom().getId()).collect(Collectors.toSet()));

		List<User> filteredUsers = filterUsers(users, ignoreIds, user, false);

		if (filteredUsers.size() < maxResults && users.size() >= UserRepository.MAX_USERS_SEARCH) {
			List<User> allUsers = userRepo.usersSearchAll(request);
			if (allUsers.size() != users.size()) {
				filteredUsers = filterUsers(allUsers, ignoreIds, user, false);
			}
		}

		if (!filteredUsers.isEmpty()) {
			return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sort, user)).build();
		}
		filteredUsers.clear();
		users = userRepo.usersSearchAllIgnoreLocation(request);
		filteredUsers = filterUsers(users, ignoreIds, user, false);

		if (!filteredUsers.isEmpty()) {
			return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sort, user))
					.message(publicService.text("search.warning.global")).global(true).build();
		}

		filteredUsers.clear();
		users = userRepo.usersSearchAllIgnoreLocationAndIntention(request);
		filteredUsers = filterUsers(users, ignoreIds, user, false);
		if (!filteredUsers.isEmpty()) {
			return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sort, user))
					.message(publicService.text("search.warning.incompatible")).global(true).build();
		}

		if (isLegalAge) {
			maxDate = Tools.ageToDate(ageLegal);
			minDate = Tools.ageToDate(ageMax);
		} else {
			maxDate = Tools.ageToDate(ageMin);
			minDate = Tools.ageToDate(ageLegal - 1);
		}
		request.setMinDate(minDate);
		request.setMaxDate(maxDate);
		
		filteredUsers.clear();
		users = userRepo.usersSearchAllIgnoreLocationAndIntention(request);
		filteredUsers = filterUsers(users, ignoreIds, user, false);
		if (!filteredUsers.isEmpty()) {
			return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sort, user))
					.message(publicService.text("search.warning.incompatible")).global(true).build();
		}
		
		filteredUsers.clear();
		users = userRepo.usersSearchAllIgnoreAll(request);
		filteredUsers = filterUsers(users, ignoreIds, user, true);
		return SearchDto.builder().users(searchResultstoUserDto(filteredUsers, sort, user))
				.message(publicService.text("search.warning.incompatible")).incompatible(true).build();

	}

	private List<User> filterUsers(List<User> users, Set<Long> ignoreIds, User user, boolean ignoreGenders) {
		List<User> filteredUsers = new ArrayList<>();
		for (User u : users) {
			if (ignoreIds.contains(u.getId())) {
				continue;
			}
			if(!ignoreGenders && !u.getPreferedGenders().contains(user.getGender())) {
				continue;
			}
			// square is fine, reduces CPU load when not calculating radius distance
			filteredUsers.add(u);

			if (filteredUsers.size() >= maxResults) {
				break;
			}
		}
		return filteredUsers;
	}

	private List<UserDto> searchResultstoUserDto(final List<User> userList, int sort, User user)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		List<UserDto> userDtos = new ArrayList<>();
		for (User u : userList) {
			UserDto dto = UserDto.userToUserDto(u, user, textEncryptor, UserDto.PROFILE_PICTURE_ONLY);
			userDtos.add(dto);
		}

		if (sort == SORT_DISTANCE) {
			userDtos = userDtos.stream().sorted(Comparator.comparing(UserDto::getDistanceToUser))
					.collect(Collectors.toList());
		} else if (sort == SORT_ACTIVE_DATE) {
			userDtos = userDtos.stream()
					.sorted(Comparator.comparing(UserDto::getActiveDate).reversed()
							.thenComparing(Comparator.comparing(UserDto::getDistanceToUser).reversed()))
					.collect(Collectors.toList());
		} else if (sort == SORT_INTEREST) {
			userDtos = userDtos.stream().filter(f -> f.getSameInterests() > 0)
					.sorted(Comparator.comparing(UserDto::getSameInterests).reversed()
							.thenComparing(Comparator.comparing(UserDto::getDistanceToUser).reversed()))
					.collect(Collectors.toList());
		} else if (sort == SORT_DONATION) {
			userDtos = userDtos.stream()
					.sorted(Comparator.comparing(UserDto::getTotalDonations).reversed()
							.thenComparing(Comparator.comparing(UserDto::getDistanceToUser).reversed()))
					.collect(Collectors.toList());
		}

		return userDtos;
	}

}
