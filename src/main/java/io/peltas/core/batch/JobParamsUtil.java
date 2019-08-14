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

package io.peltas.core.batch;

import java.text.MessageFormat;
import java.util.Date;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.util.StringUtils;

public class JobParamsUtil {

	public static Long getLong(StepExecution stepExecution, String paramKey) {
		return getParameter(stepExecution, paramKey, Long.class);
	}

	public static String getString(StepExecution stepExecution, String paramKey) {
		return getParameter(stepExecution, paramKey, String.class);
	}

	public static Double getDouble(StepExecution stepExecution, String paramKey) {
		return getParameter(stepExecution, paramKey, Double.class);
	}

	public static Date getDate(StepExecution stepExecution, String paramKey) {
		return getParameter(stepExecution, paramKey, Date.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getParameter(StepExecution stepExecution, String paramKey, Class<T> paramType) {

		JobExecution jobExecution = stepExecution.getJobExecution();
		JobParameters jobParameters = jobExecution.getJobParameters();

		if (String.class.equals(paramType)) {
			String paramValue = jobParameters.getString(paramKey);
			if (!StringUtils.hasText(paramValue)) {
				throw new BatchConfigurationException(
						new RuntimeException(MessageFormat.format("Missing {} parameter to start", paramKey)));
			}
			return (T) paramValue;
		}

		if (Long.class.equals(paramType)) {
			Long paramValue = jobParameters.getLong(paramKey);
			if (paramValue == null || paramValue == 0) {
				throw new BatchConfigurationException(
						new RuntimeException(MessageFormat.format("Missing {} parameter to start", paramKey)));
			}
			return (T) paramValue;
		}

		if (Double.class.equals(paramType)) {
			Double paramValue = jobParameters.getDouble(paramKey);
			if (paramValue == null || paramValue == 0.0) {
				throw new BatchConfigurationException(
						new RuntimeException(MessageFormat.format("Missing {} parameter to start", paramKey)));
			}
			return (T) paramValue;
		}

		if (Date.class.equals(paramType)) {
			Date paramValue = jobParameters.getDate(paramKey);
			if (paramValue == null) {
				throw new BatchConfigurationException(
						new RuntimeException(MessageFormat.format("Missing {} parameter to start", paramKey)));
			}
			return (T) paramValue;
		}

		return null;
	}
}
