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
package org.springframework.social.linkedin.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Model class containing a user's LinkedIn profile information.
 * 
 * @author Craig Walls
 */
public class LinkedInProfile extends LinkedInObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String id;

	private final LinkedInProfileLocalizedField firstName;

	private final LinkedInProfileLocalizedField maidenName;

	private final LinkedInProfileLocalizedField lastName;

	private final LinkedInProfileProfilePicture profilePicture;

	private ConnectionAuthorization connectionAuthorization;
	
	public LinkedInProfile(String id, LinkedInProfileLocalizedField firstName, LinkedInProfileLocalizedField lastName, LinkedInProfileLocalizedField maidenName, LinkedInProfileProfilePicture profilePicture) {
		this.id = id;
		this.firstName = firstName;
		this.maidenName = maidenName;
		this.lastName = lastName;
		this.profilePicture = profilePicture;
	}

	/**
	 * The user's LinkedIn profile ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * The user's first name
	 */
	public LinkedInProfileLocalizedField getFirstName() {
		return firstName;
	}

	/**
	 * The user's first name
	 */
	public LinkedInProfileLocalizedField getMaidenName() {
		return maidenName;
	}
	/**
	 * The user's last name
	 */
	public LinkedInProfileLocalizedField getLastName() {
		return lastName;
	}


	/**
	 * A URL to the user's profile picture.
	 */
	public LinkedInProfileProfilePicture getProfilePicture() {
		return profilePicture;
	}
	

	/**
	 * @return Authorization information required for connecting to this user.
	 */
	public ConnectionAuthorization getConnectionAuthorization() {
		return connectionAuthorization;
	}



}
