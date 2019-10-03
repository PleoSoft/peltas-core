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

package io.peltas.core.repository.database;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "peltas_timestamp", uniqueConstraints = { @UniqueConstraint(columnNames = { "ref", "applicationName" }) })
public class PeltasTimestamp implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String applicationName;

	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date access;

	@Column(nullable = false)
	private String ref;

	public PeltasTimestamp() {
	}

	public PeltasTimestamp(final String applicationName, final String ref, final Date access) {
		this.access = access;
		this.ref = ref != null ? ref : "-";
		this.applicationName = applicationName;
	}

	public Date getAccess() {
		return access;
	}

	public void setAccess(Date access) {
		this.access = access;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
}