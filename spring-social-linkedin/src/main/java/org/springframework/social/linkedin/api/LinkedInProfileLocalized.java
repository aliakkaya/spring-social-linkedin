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

import java.io.Serializable;

/**
 * Model class containing a user's LinkedIn profile information.
 * 
 * @author Craig Walls
 */
public class LinkedInProfileLocalized extends LinkedInObject implements Serializable {

	public LinkedInProfileLocalized(String en_US, String fr_FR, String tr_TR) {
		this.fr_FR = fr_FR;
		this.tr_TR = tr_TR;
		this.en_US = en_US;
	}

	private String en_US = null;
	private String fr_FR = null;
	private String tr_TR = null;

	public String getEn_US() {
			return en_US;
		}
	public String getFr_FR() {
		return fr_FR;
	}
	public String getTr_TR() {
		return tr_TR;
	}


}
