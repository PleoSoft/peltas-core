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

package io.peltas.core.alfresco.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import io.peltas.core.alfresco.converter.PrefixStringHashMap;

@Configuration
public class AlfrescoDefaultConvertersConfiguration {

	@Bean
	public Converter<?, ?> mapToPrefixedMapConverter(AlfrescoModelConfigurationProperties alfescoModelConfiguration) {
		return new Converter<Map<String, Object>, PrefixStringHashMap<String, Object>>() {
			@Override
			public PrefixStringHashMap<String, Object> convert(Map<String, Object> source) {
				PrefixStringHashMap<String, Object> prefixStringHashMap = new PrefixStringHashMap<>();

				Pattern pattern = Pattern.compile("(\\{.*\\})(.*)");

				Set<Entry<String, Object>> entrySet = source.entrySet();
				for (Entry<String, Object> entry : entrySet) {
					String key = entry.getKey();
					Matcher matcher = pattern.matcher(key);
					if (matcher.find() && matcher.groupCount() == 2) {
						String group = matcher.group(1);
						String namespace = group.substring(1, group.length() - 1);
						String localName = matcher.group(2);

						String prefix = alfescoModelConfiguration.getPrefix(namespace);

						String formated = String.format("%s:%s", prefix, localName);
						prefixStringHashMap.put(formated, entry.getValue());
					}
				}

				return prefixStringHashMap;
			}
		};
	}
}
