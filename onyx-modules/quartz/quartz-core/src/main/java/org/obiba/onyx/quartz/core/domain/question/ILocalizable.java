package org.obiba.onyx.quartz.core.domain.question;

public interface ILocalizable {
	
	/**
	 * Get the localiation key for the given property.
	 * @param property
	 * @return
	 */
	public String getPropertyKey(String property);
}
