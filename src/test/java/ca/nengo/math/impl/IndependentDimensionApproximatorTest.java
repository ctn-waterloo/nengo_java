package ca.nengo.math.impl;

import static org.junit.Assert.*;
import ca.nengo.math.Function;
import ca.nengo.math.LinearApproximator;
import ca.nengo.math.impl.IndependentDimensionApproximator.EncoderFactory;
import ca.nengo.util.MU;
import org.junit.Test;

public class IndependentDimensionApproximatorTest {
	@Test
	public void testFindCoefficients() {
		float[][] polyCoeffs = new float[][]{{5,5},{1,-2},{-2,3}};

		float[] evalPoints = new float[100];
		for (int i = 0; i < evalPoints.length; i++) {
			evalPoints[i] = -10 + 20 * (float)i / evalPoints.length;
		}

		Function target = new IdentityFunction(1,0);

		float[][] values = new float[polyCoeffs.length][];
		for (int i = 0; i < values.length; i++) {
			Function component = new Polynomial(polyCoeffs[i]);
			values[i] = new float[evalPoints.length];
			for (int j = 0; j < evalPoints.length; j++) {
				values[i][j] = component.map(new float[]{evalPoints[j]});
			}
		}

		LinearApproximator approximator = new IndependentDimensionApproximator(evalPoints, values, new int[]{0,1,0}, 2, new ConstantFunction(1,1f), 0f);
		float[] coefficients = approximator.findCoefficients(target);

		float approx;
		for (int j = 0; j < evalPoints.length; j++) {
			approx = 0f;
			for (int i = 0; i < polyCoeffs.length; i++) {
				approx += coefficients[i] * values[i][j];
			}
			assertEquals(approx, target.map(new float[]{evalPoints[j]}), 0.0001f);
		}
	}

    public static void main(String[] args) {
        EncoderFactory ef = new EncoderFactory(1);
        float[][] foo = ef.genVectors(10, 3);
        System.out.println(MU.toString(foo, 10));
    }
}
