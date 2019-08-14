package io.peltas.core.alfresco.integration;

import org.springframework.core.convert.ConversionException;

public class PeltasConversionException extends ConversionException {

	/**
	 * Construct a new conversion exception.
	 * 
	 * @param cause   the cause
	 */
	public PeltasConversionException(Throwable cause) {
		super(cause.getMessage(), cause);
	}
}
