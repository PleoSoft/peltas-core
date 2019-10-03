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

package io.peltas.core.alfresco.integration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.transformer.ObjectToMapTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;

import io.peltas.core.alfresco.PeltasEntry;
import io.peltas.core.alfresco.StringToMapUtil;
import io.peltas.core.alfresco.config.PeltasExpresionProperty;
import io.peltas.core.alfresco.config.PeltasHandlerProperties;
import io.peltas.core.alfresco.config.PeltasMapper;
import io.peltas.core.batch.PeltasDataHolder;

public class PeltasHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeltasHandler.class);

	private final DefaultFormattingConversionService conversionService;

	private final ThreadLocal<String> CURRENT_FORMAT = new ThreadLocal<>();

	public PeltasHandler() {
		this.conversionService = new DefaultFormattingConversionService();

		// TODO FIXME move to audit
		this.conversionService.addConverter(new Converter<String, HashMap<String, Object>>() {
			@Override
			public HashMap<String, Object> convert(String source) {
				LOGGER.trace("converting String -> HashMap: {}", source);

				final HashMap<String, Object> map = new HashMap<>();
				if (StringUtils.hasText(source)) {
					final Map<String, Object> stringToMap = StringToMapUtil.stringToMap(source, ',');
					map.putAll(stringToMap);
				}

				return map;
			}
		});

		this.conversionService.addConverter(new Converter<String, Date>() {
			@Override
			public Date convert(String source) {
				final String initialFormat = CURRENT_FORMAT.get();
				String format = initialFormat;
				if (!StringUtils.hasText(format)) {
					format = "EEE MMM dd HH:mm:ss zzz yyyy"; // old audit
				}
				try {
					LOGGER.trace("converting String -> Date: {} with format: {}", source, format);
					return new SimpleDateFormat(format, Locale.ENGLISH).parse(source);
				} catch (final ParseException e) {
					if (StringUtils.hasText(initialFormat)) {
						throw new PeltasConversionException(e);
					}
					format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; // v1
					try {
						LOGGER.trace("converting String -> Date: {} with format: {}", source, format);
						return new SimpleDateFormat(format).parse(source);
					} catch (final ParseException e1) {
						try {
							format = "yyyy-MM-dd'T'HH:mm:ss.SSS";
							LOGGER.trace("converting String -> Date: {} with format: {}", source, format);
							return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(source);
						} catch (final ParseException e2) {
							throw new PeltasConversionException(e2);
						}
					}
				}
			}
		});

		this.conversionService.addConverter(new Converter<Date, String>() {
			@Override
			public String convert(Date source) {
				final String format = CURRENT_FORMAT.get();
				LOGGER.trace("converting Date -> String: {} with format: {}", source, format);
				return new SimpleDateFormat(format).format(source);
			}
		});

		this.conversionService.addConverter(new Converter<HashMap<String, Object>, Collection<?>>() {
			@Override
			public Collection<?> convert(HashMap<String, Object> source) {
				LOGGER.trace("converting HashMap -> Collection : {}", source);
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
		});
	}

	@ServiceActivator(inputChannel = "auditprocess")
	public PeltasDataHolder handle(Message<PeltasEntry> message) {
		final PeltasEntry auditEntry = message.getPayload();
		LOGGER.debug("handle() processing {}", auditEntry);

		final PeltasHandlerProperties config = (PeltasHandlerProperties) message.getHeaders()
				.get("alfresco.handler.configuration");

		final PeltasMapper mapper = config.getMapper();
		final Map<String, Object> mappedProperties = new HashMap<>();
		try {

			final Map<String, PeltasExpresionProperty> configuredProperties = mapper.getProperty();
			processProperties(auditEntry, configuredProperties, mappedProperties);

			LOGGER.trace("handle() properties configured {}", configuredProperties);
			LOGGER.debug("handle() properties mapped {}", mappedProperties);
			return new PeltasDataHolder(auditEntry, configuredProperties, mappedProperties, config);
		} catch (final Throwable e) {
			throw new PeltasConversionException(e);
		}

	}

	private void processProperties(PeltasEntry auditEntry, Map<String, PeltasExpresionProperty> properties,
			Map<String, Object> builder) {
		final Set<Entry<String, PeltasExpresionProperty>> propertyEntries = properties.entrySet();

		final Map<String, PeltasExpresionProperty> postProcessProps = new HashMap<>();

		for (final Entry<String, PeltasExpresionProperty> entry : propertyEntries) {

			final String key = entry.getKey();
			final PeltasExpresionProperty expresionProperty = entry.getValue();
			final List<String> exprData = expresionProperty.getData();
			final List<Object> dataValues = new ArrayList<>();
			for (String data : exprData) {
				data = data.trim();

				Object value = null;
				if (data.startsWith("prop:")) {
					postProcessProps.put(key, expresionProperty);
					continue;
				}

				if ((data.startsWith("\"") && data.endsWith("\"")) || (data.startsWith("'") && data.endsWith("'"))) {
					value = data.substring(1, data.length() - 1).trim();
				} else {
					value = getMappedSingleValueProperty(data, auditEntry);
				}

				if (value != null) {
					dataValues.add(value);
				}
			}

			convertStringValues(builder, key, expresionProperty, dataValues);
		}

		if (!postProcessProps.isEmpty()) {
			postProcessProperties(auditEntry, postProcessProps, builder);
		}
	}

	private void convertStringValues(Map<String, Object> builder, String key, PeltasExpresionProperty expresionProperty,
			List<Object> dataValues) {
		Object value = null;
		if (dataValues != null && !dataValues.isEmpty()) {
			final String format = expresionProperty.getFormat();
			if (StringUtils.hasText(format) && String.class.isAssignableFrom(expresionProperty.getType())) {
				value = String.format(format, dataValues.toArray());
			} else {
				if (dataValues.size() == 1) {
					value = dataValues.get(0);
				} else {
					throw new IllegalArgumentException(
							"Check the format! Datavalues not supported for: " + dataValues + " and format: " + format);
				}
			}
		}
		convertValue(value, builder, key, expresionProperty);
	}

	private void convertValue(Object value, Map<String, Object> builder, String key,
			PeltasExpresionProperty expresionProperty) {
		if (value == null) {
			builder.put(key, null);
			return;
		}

		final Class<?> convertClass = expresionProperty.getType();
		if (convertClass != null) {
			final String format = expresionProperty.getFormat();
			// if(value.getClass().isAssignableFrom(convertClass) && format ==
			// null){
			// return;
			// }
			CURRENT_FORMAT.set(format);
			LOGGER.trace("convertValue() converting {} -> {} value {} using format {}", value.getClass(), convertClass,
					value, format);
			value = conversionService.convert(value, convertClass);
			LOGGER.trace("convertValue() converted {}", value);
			CURRENT_FORMAT.set(null);
		}

		if (value != null) {
			if (value instanceof Map) {
//				final Message<Map<String, ?>> message = new GenericMessage<Map<String, ?>>(ImmutableMap.of(key, value));
//				final ObjectToMapTransformer transformer = new ObjectToMapTransformer();
//				transformer.setShouldFlattenKeys(true);
//				final Map<String, ?> payload = (Map<String, ?>) transformer.transform(message).getPayload();
//
//				builder.putAll(payload);
				builder.put(key, value);
			} else {
				builder.put(key, value);
			}
		}
	}

	private void postProcessProperties(PeltasEntry auditEntry, Map<String, PeltasExpresionProperty> properties,
			Map<String, Object> builder) {

		final Set<Entry<String, PeltasExpresionProperty>> propertyEntries = properties.entrySet();
		for (final Entry<String, PeltasExpresionProperty> entry : propertyEntries) {
			final PeltasExpresionProperty expresionProperty = entry.getValue();

			final List<String> exprData = expresionProperty.getData();
			if (exprData.size() == 0) {
				throw new RuntimeException("an evaluated property (prop:) does not support empty data values");
			}

			if (exprData.size() == 1) {
				for (final String data : exprData) {
					final String replacedData = data.replaceFirst("prop:", "");
					Object value = builder.get(replacedData);

					final String[] keyDelimited = StringUtils.delimitedListToStringArray(replacedData, "@");
					if (keyDelimited.length > 1) {

						final Map<?, ?> map = (Map<?, ?>) builder.get(keyDelimited[0]);
						value = map.get(keyDelimited[1]);
					}

					if (value != null) {
						final Class<?> convertClass = expresionProperty.getType();
						if (convertClass != null) {
							convertValue(value, builder, entry.getKey(), expresionProperty);
						}
					}
				}
			} else {
				final List<Object> dataValues = new ArrayList<>();
				for (final String data : exprData) {
					final String replacedData = data.replaceFirst("prop:", "");
					final String[] keyDelimited = StringUtils.delimitedListToStringArray(replacedData, "@");
					final Map<?, ?> map = (Map<?, ?>) builder.get(keyDelimited[0]);
					dataValues.add(map.get(keyDelimited[1]));
				}

				convertStringValues(builder, entry.getKey(), expresionProperty, dataValues);
			}

		}
	}

	@SuppressWarnings("unchecked")
	public static Object getMappedSingleValueProperty(String value, PeltasEntry auditEntry) {
		LOGGER.trace("getMappedSingleValueProperty() retreiving {} from {}", value, auditEntry);

		if (value.contains("@")) {
			final String[] keyDelimited = StringUtils.delimitedListToStringArray(value, "@");
			final Object ret = auditEntry.getValues().get(keyDelimited[0]);
			if (ret instanceof Map) {
				return ((Map<String, Object>) ret).get(keyDelimited[1]);
			} else if (ret instanceof String) {
				throw new RuntimeException("should be in audit");
//				final String valuesMapString = (String) ret;
//				if (valuesMapString != null) {
//					final Map<String, String> stringToMap = AlfrescoAuditUtil.stringToMap(valuesMapString);
//					return stringToMap.get(keyDelimited[1]);
//				}
			}

			return "";
		} else {
			return auditEntry.getValues().get(value);
		}

	}

	public static List<Object> getMappedMultiValueProperty(String value, PeltasEntry auditEntry) {
		LOGGER.trace("getMappedMultiValueProperty() retreiving {} from {}", value, auditEntry);

		if (value.contains("@")) {
			final String[] keyDelimited = StringUtils.delimitedListToStringArray(value, "@");
			final Object valuesMapString = auditEntry.getValues().get(keyDelimited[0]);
			if (valuesMapString != null) {
				return StringToMapUtil.valueAsList(valuesMapString, ',');
			} else {
				return Collections.emptyList();
			}
		} else {
			final Object string = auditEntry.getValues().get(value);
			return StringToMapUtil.valueAsList(string, ',');

		}
	}

}
