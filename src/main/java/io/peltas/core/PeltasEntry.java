/**
 * Copyright 2019 Pleo Soft d.o.o. (pleosoft.com)

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.peltas.core;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class PeltasEntry implements Serializable {

	private static final long serialVersionUID = -3279850839274915825L;

	@JsonAlias({ "id" })
	private String id;

	@JsonAlias({ "auditApplicationId", "application" })
	private String application;

	@JsonAlias({ "time", "createdAt" })
	private Timestamp time;

	@JsonAlias({ "user" })
	private String user;

	@JsonAlias({ "values" })
	private Map<String, Object> values;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public Timestamp getTime() {
		return time;
	}

	public void setTime(Timestamp time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "PeltasEntry [id=" + id + ", application=" + application + ", time=" + time + ", user=" + user
				+ ", values=" + values + "]";
	}

}
