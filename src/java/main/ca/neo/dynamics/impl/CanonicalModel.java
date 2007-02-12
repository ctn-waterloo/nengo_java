/*
 * Created on 19-Jun-2006
 */
package ca.neo.dynamics.impl;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import ca.neo.model.Units;
import ca.neo.util.MU;

/**
 * Utilities related to state-space models that are in controllable-canonical form.
 * 
 * @author Bryan Tripp
 */
public class CanonicalModel {

	/**
	 * <p>Realizes a transfer function in the form:</p>
	 * <p>H(s) = d + (b1*s^(n-1) + b2*s^(n-2) + ... + bn) / (s^n + a1*s^(n-1) + ... + an).</p> 
	 *  
	 * @param numerator Coefficients of the numerator of a transfer function (b1 to bn above)
	 * @param denominator Coefficients of the denominator of a transfer function (a1 to an above)
	 * @param passthrough Passthrough value (d above). If your transfer function has numerator and denominator 
	 * 		of equal degree, divide them, give the result here, and give the remainder as the numerator 
	 * 		and denominator arguments, so that the new numerator will have degree less than denominator. 
	 * 		There is no state-space realization for TF with numerator degree > denominator degree.
	 * @return A controllable-canonical state-space realization of the specified 
	 * 		transfer function
	 */
	public static LTISystem getRealization(float[] numerator, float[] denominator, float passthrough) {
		if (numerator.length != denominator.length) {
			throw new IllegalArgumentException("Should be equal numbers of numerator and denominator coefficients. " +
					"(Use zeros instead of shorter lists.) "); 
		}
		
		float[][] A = new float[denominator.length][];
		for (int i = 0; i < A.length; i++) {
			A[i] = new float[denominator.length];
			if (i == denominator.length-1) {
				for (int j = 0; j < denominator.length; j++) {
					A[i][j] = -denominator[denominator.length - 1 - j];
				}
			} else {
				A[i][i+1] = 1f;				
			}
		}
		
		float[][] B = new float[denominator.length][];
		for (int i = 0; i < B.length; i++) {
			B[i] = new float[]{(i == B.length-1) ? 1f : 0f};
		}
		
		float[][] C = new float[1][];
		C[0] = new float[numerator.length]; 
		for (int i = 0; i < numerator.length; i++) {
			C[0][i] = numerator[numerator.length - 1 - i];
		}
		
		float[][] D = new float[][]{new float[]{passthrough}};
		
		float[] x0 = new float[numerator.length];
		
		return new LTISystem(A, B, C, D, x0, new Units[]{Units.UNK});
	}

	/**
	 * @param system Any SISO linear time-invariant system
	 * @return True if the system is in controllable-canonical form
	 */
	public static boolean isControllableCanonical(LTISystem system) {
		if (system.getInputDimension() != 1 || system.getOutputDimension() != 1) {
			throw new IllegalArgumentException("System must be single-input-single-output");
		}
		
		float[][] A = system.getA(0f);
		for (int i = 0; i < A.length-1; i++) {
			for (int j = 0; j < A.length; j++) {
				if (j == i+1) {
					if (!isClose(1f, A[i][j])) return false;
				} else {
					if (!isClose(0f, A[i][j])) return false;					
				}
			}
		}
		
		float[][] B = system.getB(0f);
		for (int i = 0; i < B.length; i++) {
			if (i == B.length-1) {
				if (!isClose(1f, B[i][0])) return false;
			} else {
				if (!isClose(0f, B[i][0])) return false;				
			}
		}
		
		return true;
	}
	
	private static boolean isClose(float a, float b) {
		float tolerance = .00000001f;

		boolean result = true;
		if (a > b+tolerance || a < b-tolerance) result = false;
		
		return result;
	}

	/**
	 * @param system An LTI system in controllable-canonical form
	 * @param tau A desired new time constant
	 * @return A copy of the system, with its slowest time constant changed to the 
	 * 		new value
	 */
	public static LTISystem changeTimeConstant(LTISystem system, float tau) {
		
		if (!isControllableCanonical(system)) {
			throw new IllegalArgumentException("System must be in controllable-canonical form");
		}
		
		EigenvalueDecomposition eig = new Matrix(MU.convert(system.getA(0f))).eig();
		double[] eigReal = eig.getRealEigenvalues(); 
		
		double slowest = eigReal[0];
		for (int i = 1; i < eig.getRealEigenvalues().length; i++) {
			if (Math.abs(eigReal[i]) < Math.abs(slowest)) slowest = eigReal[i];
		}
		
		//may be repeated / complex conjugate so change all slowest real eigenvalues
		for (int i = 0; i < eigReal.length; i++) {
			if (Math.abs(eigReal[i] - slowest) < .00001) eigReal[i] = -1d / (double) tau;
		}
	
		//coefficients of new transfer function polynomial ...  
		double[] eigImag = eig.getImagEigenvalues();
		float[][] poly = new float[][]{new float[]{1f, (float) -eigReal[0]}, new float[]{0f, (float) -eigImag[0]}};
		for (int i = 1; i < eigReal.length; i++) {
			poly = prod(poly, new float[][]{new float[]{1f, (float) -eigReal[i]}, new float[]{0f, (float) -eigImag[i]}});
		}
		
		float[][] A = system.getA(0f);
		for (int i = 0; i < A[0].length; i++) {
			A[A.length-1][i] = - poly[0][poly[0].length-1-i];
		}
		
		Units[] units = new Units[system.getOutputDimension()];
		for (int i = 0; i < units.length; i++) {
			units[i] = system.getOutputUnits(i);
		}
		
		return new LTISystem(A, system.getB(0f), system.getC(0f), system.getD(0f), system.getState(), units);
	}
	
	//product of complex polynomials
	private static float[][] prod(float[][] a, float[][] b) {
		if (a[0].length != a[1].length || b[0].length != b[1].length) {
			throw new IllegalArgumentException("Number of real and imaginary coefficients must be equal for each polynomial");
		}
		
		float[][] result = new float[2][];
		result[0] = new float[a[0].length + (b[0].length-1)]; 
		result[1] = new float[a[0].length + (b[0].length-1)]; 
		
		for (int i = 0; i < a[0].length; i++) {
			for (int j = 0; j < b[0].length; j++) {
				float[] p = prod(a[0][i], a[1][i], b[0][j], b[1][j]);
				int degree = a[0].length + b[0].length - i - j - 2;
				result[0][result[0].length - 1 - degree] += p[0];
				result[1][result[0].length - 1 - degree] += p[1];
			}
		}
		
		return result;
	}
	
	//product of complex number a0+a1*i and b0+b1*i 
	private static float[] prod(float a0, float a1, float b0, float b1) {
		return new float[]{a0*b0 - a1*b1, a0*b1 + a1*b0};
	}

}
