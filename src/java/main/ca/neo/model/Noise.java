/*
 * Created on 24-May-07
 */
package ca.neo.model;

/**
 * An model of noise that can be explicitly injected into a circuit (e.g. added to 
 * an Origin). 
 *   
 * @author Bryan Tripp
 */
public interface Noise extends Cloneable {
	
	public static final String DIMENSION_PROPERTY = "dimension";
	
	/**
	 * @param startTime Simulation time at which step starts 
	 * @param endTime Simulation time at which step ends
	 * @param input Value which is to be corrupted by noise
	 * @return The noisy values (inputs corrupted by noise) 
	 */
	public float getValue(float startTime, float endTime, float input);
	
	public Noise clone();
	
	/**
	 * An object that implements this interface is subject to Noise.  
	 * 
	 * @author Bryan Tripp
	 */
	public interface Noisy {

		/**
		 * @param noise New noise model
		 */
		public void setNoise(Noise noise);
		
		/**
		 * @return Noise with which the object is to be corrupted
		 */
		public Noise getNoise();
		
	}

}
