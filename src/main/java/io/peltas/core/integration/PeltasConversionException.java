package io.peltas.core.integration;

import org.springframework.core.convert.ConversionException;

public class PeltasConversionException extends ConversionException {

	private static final long serialVersionUID = 3345703352907658355L;

	/**
	 * Construct a new conversion exception.
	 * 
	 * @param cause the cause
	 */
	public PeltasConversionException(Throwable cause) {
		super(cause.getMessage(), cause);
	}
}
