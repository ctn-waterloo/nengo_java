/*
 * Created on May 23, 2006
 */
package ca.neo.model;

import ca.neo.NeoException;

/**
 * A structural problem in setting up a Network. 
 *  
 * @author Bryan Tripp
 */
public class StructuralException extends NeoException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message Text explanation of the exception. 
	 */
	public StructuralException(String message) {
		super(message);
	}

	/**
	 * @param cause Another throwable that indicates a problem underlying this 
	 * 		exception.  
	 */
	public StructuralException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message Text explanation of the exception. 
	 * @param cause Another throwable that indicates a problem underlying this 
	 * 		exception.  
	 */
	public StructuralException(String message, Throwable cause) {
		super(message, cause);
	}

}
