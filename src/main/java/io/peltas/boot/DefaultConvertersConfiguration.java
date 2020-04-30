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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.transformer.ObjectToMapTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;

import io.peltas.core.converter.PrefixStringHashMap;
import io.peltas.core.integration.PeltasConversionException;
import io.peltas.core.integration.PeltasFormatUtil;

@Configuration
@EnableConfigurationProperties(AlfrescoModelConfigurationProperties.class)
public class DefaultConvertersConfiguration {
	
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

	@Bean
	public Converter<?, ?> stringToDateConverter(PeltasFormatUtil peltasFormatUtil) {
		return new Converter<String, Date>() {
			@Override
			public Date convert(String source) {
				final String initialFormat = peltasFormatUtil.getCurrentFormat();
				String format = initialFormat;
				if (!StringUtils.hasText(format)) {
					format = "EEE MMM dd HH:mm:ss zzz yyyy"; // old audit
				}
				try {
					return new SimpleDateFormat(format, Locale.ENGLISH).parse(source);
				} catch (final ParseException e) {
					if (StringUtils.hasText(initialFormat)) {
						throw new PeltasConversionException(e);
					}
					format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; // v1
					try {
						return new SimpleDateFormat(format).parse(source);
					} catch (final ParseException e1) {
						try {
							format = "yyyy-MM-dd'T'HH:mm:ss.SSS";
							return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(source);
						} catch (final ParseException e2) {
							throw new PeltasConversionException(e2);
						}
					}
				}
			}
		};
	}

	@Bean
	public Converter<?, ?> dateToStringConverter(PeltasFormatUtil peltasFormatUtil) {
		return new Converter<Date, String>() {
			@Override
			public String convert(Date source) {
				final String format = peltasFormatUtil.getCurrentFormat();
				return new SimpleDateFormat(format).format(source);
			}
		};
	}

	@Bean
	public Converter<?, ?> mapToCollectionConverter() {
		return new Converter<HashMap<String, Object>, Collection<?>>() {
			@Override
			public Collection<?> convert(HashMap<String, Object> source) {
				final ObjectToMapTransformer transformer = new ObjectToMapTransformer();
				transformer.setShouldFlattenKeys(true);
				final Message<Map<String, Object>> message = new GenericMessage<Map<String, Object>>(source);

				@SuppressWarnings("unchecked")
				final Map<String, Object> payload = (Map<String, Object>) transformer.transform(message).getPayload();
				final Set<Map.Entry<String, Object>> entrySet = payload.entrySet();

				final ArrayList<Map<String, Object>> list = new ArrayList<>();
				for (final Map.Entry<String, Object> entry : entrySet) {
					final Object value = entry.getValue() != null ? entry.getValue() : "";
					list.add(ImmutableMap.of("key", entry.getKey(), "value", value));
				}
				return list;
			}
		};
	}

	@Bean
	public Converter<?, ?> mapToFormattedKeyMapConverter(final PeltasFormatUtil peltasFormatUtil) {
		return new Converter<Collection<Map<?, ?>>, Collection<String>>() {

			@Override
			public Collection<String> convert(Collection<Map<?, ?>> source) {
				String format = peltasFormatUtil.getCurrentFormat();
				List<String> formatKeys = peltasFormatUtil.getCurrentFormatKeys();

				final ArrayList<String> list = new ArrayList<>();
				for (Map<?, ?> map : source) {
					List<Object> dataValues = new ArrayList<>();
					if (formatKeys != null) {
						for (String key : formatKeys) {
							// no NPE check since it has to fail if the value does not exist
							dataValues.add(map.get(key));
						}
					}

					String formated = String.format(format, dataValues.toArray());
					list.add(formated);
				}

				return list;
			}

		};
	}
}
