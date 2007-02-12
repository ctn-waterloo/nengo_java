/*
 * Created on 27-Jul-2006
 */
package ca.neo.util.impl;

import ca.neo.util.VectorGenerator;

/**
 * Wraps an underlying VectorGenerator, rectifying generated vectors before 
 * they are returned. 
 *  
 * @author Bryan Tripp
 */
public class Rectifier implements VectorGenerator {

	private VectorGenerator myVG;
	
	/**
	 * @param vg A VectorGenerator to underlie this one (ie to produce non-rectified vectors)
	 */
	public Rectifier(VectorGenerator vg) {
		myVG = vg;
	}

	/**
	 * @return Rectified version of vector generated by underlying VectorGenerator
	 * 		(all components -> abs value) 
	 *  
	 * @see ca.neo.util.VectorGenerator#genVectors(int, int)
	 */
	public float[][] genVectors(int number, int dimension) {
		float[][] raw = myVG.genVectors(number, dimension);
		float[][] result = new float[raw.length][];
		
		for (int i = 0; i < raw.length; i++) {
			result[i] = new float[raw[i].length];
			for (int j = 0; j < raw[i].length; j++) {
				result[i][j] = Math.abs(raw[i][j]);
			}
		}
		
		return result;
	}

}
