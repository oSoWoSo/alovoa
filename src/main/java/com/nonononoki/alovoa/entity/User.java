package com.nonononoki.alovoa.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.Message;
import com.nonononoki.alovoa.entity.user.UserAudio;
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.entity.user.UserDates;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserInterest;
import com.nonononoki.alovoa.entity.user.UserLike;
import com.nonononoki.alovoa.entity.user.UserNotification;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.entity.user.UserRegisterToken;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.entity.user.UserWebPush;

import lombok.Data;

@SuppressWarnings("serial")
@Component
@Data
@Entity
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false, unique = true)
	@Convert(converter = TextEncryptorConverter.class)
	private String email;

	private String password;

	@Convert(converter = TextEncryptorConverter.class)
	private String firstName;

	private String description;

	// used for emails
	private String language;

	private String accentColor;

	private String uiDesign;

	private int preferedMinAge;

	private int preferedMaxAge;

	private Double locationLatitude;
	private Double locationLongitude;

	private double totalDonations;

	private boolean admin;

	private boolean confirmed;

	private boolean disabled;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn
	private UserRegisterToken registerToken;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn
	private UserPasswordToken passwordToken;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn
	private UserDeleteToken deleteToken;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn
	private UserDates dates;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn
	private UserAudio audio;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn
	private UserProfilePicture profilePicture;

	@ManyToOne
	private Gender gender;

	@ManyToMany
	@JoinTable(name = "user2genders")
	private Set<Gender> preferedGenders;

	@ManyToOne
	private UserIntention intention;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserInterest> interests;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserImage> images;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserDonation> donations;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
	private List<UserWebPush> webPush;

	// Tables with multiple users

	@OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true, mappedBy = "userFrom")
	private List<Message> messageSent;

	@OneToMany(orphanRemoval = true, mappedBy = "userTo")
	private List<Message> messageReceived;

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, mappedBy = "users")
	private List<Conversation> conversations;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserLike> likes;

	@OneToMany(orphanRemoval = true, mappedBy = "userTo")
	private List<UserLike> likedBy;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userTo")
	private List<UserNotification> notifications;

	@OneToMany(orphanRemoval = true, mappedBy = "userFrom")
	private List<UserNotification> notificationsFrom;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserHide> hiddenUsers;

	@OneToMany(orphanRemoval = true, mappedBy = "userTo")
	private List<UserHide> hiddenByUsers;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserBlock> blockedUsers;

	@OneToMany(orphanRemoval = true, mappedBy = "userTo")
	private List<UserBlock> blockedByUsers;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "userFrom")
	private List<UserReport> reported;

	@OneToMany(orphanRemoval = true, mappedBy = "userTo")
	private List<UserReport> reportedByUsers;

	// some more data
	long numberProfileViews;

	long numberSearches;

	@Transient
	public static final String ACCENT_COLOR_PINK = "pink";
	@Transient
	public static final String ACCENT_COLOR_BLUE = "blue";
	@Transient
	public static final String ACCENT_COLOR_ORANGE = "orange";
	@Transient
	public static final String ACCENT_COLOR_PURPLE = "purple";

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authorities = new ArrayList<>();
		String role;
		if (admin) {
			role = SecurityConfig.getRoleAdmin();
		} else {
			role = SecurityConfig.getRoleUser();
		}
		authorities.add(new SimpleGrantedAuthority(role));

		return authorities;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return !disabled;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !disabled;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return !disabled;
	}

	@Override
	public boolean isEnabled() {
		return !disabled;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		throw new IOException();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		throw new IOException();
	}

	public static boolean isCompatible(User user1, User user2) {
		if (user2.getPreferedGenders() == null || user1.getPreferedGenders() == null || user1.getDates() == null
				|| user2.getDates() == null) {
			return false;
		}
		return Tools.calcUserAge(user2) < Tools.AGE_LEGAL == Tools.calcUserAge(user1) < Tools.AGE_LEGAL
				&& user2.getPreferedGenders().contains(user1.getGender())
				&& user1.getPreferedGenders().contains(user2.getGender())
				&& user2.getIntention().getText().equals(user1.getIntention().getText());
	}

}
