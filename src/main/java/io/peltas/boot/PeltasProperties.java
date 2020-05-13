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

package io.peltas.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

import io.peltas.boot.PeltasProperties.Ssl.KeystoreType;
import io.peltas.core.AuthenticationType;

@ConfigurationProperties(prefix = "peltas")
@PropertySource(ignoreResourceNotFound = true, value = { "classpath:/io/peltas/peltas.properties" })
public class PeltasProperties {

	private String host = "http://localhost:8080";
	private String serviceUrl = "alfresco/service/api/audit/query";
	private String application = "alfresco-access";
	private String noMatchHandler = "donotprocess";
	private String timestamp = "database";
	private Integer chunksize = 1;

	private AuthenticationType authenticationtype = AuthenticationType.basicauth;

	private Ssl ssl;

	private Authentication auth = new Authentication(new Authentication.BasicAuth(), null, new Authentication.Header());

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getNoMatchHandler() {
		return noMatchHandler;
	}

	public void setNoMatchHandler(String noMatchHandler) {
		this.noMatchHandler = noMatchHandler;
	}

	public void setAuthenticationtype(AuthenticationType authenticationtype) {
		this.authenticationtype = authenticationtype;
	}

	public AuthenticationType getAuthenticationtype() {
		return authenticationtype;
	}

	public Ssl getSsl() {
		return ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public Authentication getAuth() {
		return auth;
	}

	public void setAuth(Authentication auth) {
		this.auth = auth;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public Integer getChunksize() {
		return chunksize;
	}

	public void setChunksize(Integer chunksize) {
		this.chunksize = chunksize;
	}

	public static class Ssl {

		private String trustStore;
		private char[] trustStorePass;
		private KeystoreType keystoreType = KeystoreType.JCEKS;
		private Boolean hostVerify = true;

		public String getTrustStore() {
			return trustStore;
		}

		public void setTrustStore(String trustStore) {
			this.trustStore = trustStore;
		}

		public char[] getTrustStorePass() {
			return trustStorePass;
		}

		public void setTrustStorePass(char[] trustStorePass) {
			this.trustStorePass = trustStorePass;
		}

		public KeystoreType getKeystoreType() {
			return keystoreType;
		}

		public void setKeystoreType(KeystoreType keystoreType) {
			this.keystoreType = keystoreType;
		}

		public Boolean getHostVerify() {
			return hostVerify;
		}

		public void setHostVerify(Boolean hostVerify) {
			this.hostVerify = hostVerify;
		}

		public static enum KeystoreType {
			JKS, JCEKS, PKCS12, PKCS12S2, JCERACFKS
		}
	}

	public static class Authentication {
		private BasicAuth basic;
		private X509 x509;
		private Header header;

		public Authentication() {
			super();
		}

		public Authentication(BasicAuth basic, X509 x509, Header header) {
			this.basic = basic;
			this.x509 = x509;
			this.header = header;
		}

		public BasicAuth getBasic() {
			return basic;
		}

		public void setBasic(BasicAuth basic) {
			this.basic = basic;
		}

		public X509 getX509() {
			return x509;
		}

		public void setX509(X509 x509) {
			this.x509 = x509;
		}

		public Header getHeader() {
			return header;
		}

		public void setHeader(Header header) {
			this.header = header;
		}

		public static class BasicAuth {
			private String username = "admin";
			private String password = "admin";

			public String getUsername() {
				return username;
			}

			public void setUsername(String username) {
				this.username = username;
			}

			public String getPassword() {
				return password;
			}

			public void setPassword(String password) {
				this.password = password;
			}
		}

		public static class Header {
			private String key = "Authorization";
			private String value = "Basic YWRtaW46YWRtaW4=";

			public String getKey() {
				return key;
			}

			public void setKey(String key) {
				this.key = key;
			}

			public String getValue() {
				return value;
			}

			public void setValue(String value) {
				this.value = value;
			}
		}

		public static class X509 {
			private String keyStore;
			private char[] keyStorePass;
			private KeystoreType keystoreType = KeystoreType.JCEKS;

			public String getKeyStore() {
				return keyStore;
			}

			public void setKeyStore(String keyStore) {
				this.keyStore = keyStore;
			}

			public char[] getKeyStorePass() {
				return keyStorePass;
			}

			public void setKeyStorePass(char[] keyStorePass) {
				this.keyStorePass = keyStorePass;
			}

			public KeystoreType getKeystoreType() {
				return keystoreType;
			}

			public void setKeystoreType(KeystoreType keystoreType) {
				this.keystoreType = keystoreType;
			}
		}

	}
}
