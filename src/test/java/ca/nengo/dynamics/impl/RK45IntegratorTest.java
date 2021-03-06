package ca.nengo.dynamics.impl;

import ca.nengo.dynamics.Integrator;
import ca.nengo.model.Units;
import ca.nengo.util.InterpolatorND;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.impl.LinearInterpolatorND;
import ca.nengo.util.impl.TimeSeriesImpl;
import static org.junit.Assert.*;
import org.junit.Test;

public class RK45IntegratorTest {
	@Test
	public void testIntegrate() {
		VanderPol system = new VanderPol(new float[]{.1f, .1f});
		Integrator integrator = new RK45Integrator();
		TimeSeries input = new TimeSeriesImpl(new float[]{0, 10f}, new float[][]{new float[0], new float[0]}, new Units[]{});
		TimeSeries result = integrator.integrate(system, input);
		
		assertTrue(result.getTimes().length < 60);
		
		//check results against selected hard-coded values from matlab solution ... 
		InterpolatorND interpolator = new LinearInterpolatorND(result);
		float tolerance = 0.005f;
		float[] time2 = interpolator.interpolate(2);
		assertEquals(time2[0], 0.053f, tolerance);
		assertEquals(time2[1], -0.157f, tolerance);
		float[] time5 = interpolator.interpolate(5);
		assertEquals(time5[0], -0.128f, tolerance);
		assertEquals(time5[1], 0.223f, tolerance);
		float[] time8 = interpolator.interpolate(8);
		assertEquals(time8[0], 0.257f, tolerance);
		assertEquals(time8[1], -0.297f, tolerance);
	}
	
	public static class VanderPol extends AbstractDynamicalSystem {
		private static final long serialVersionUID = 1L;

		public VanderPol(float[] state) {
			super(state);
		}
		
		public VanderPol() {
			this(new float[2]);
		}
		
		@Override
		public float[] f(float t, float[] u) {
			float[] x = getState();
			float epsilon = 0.3f;
			return new float[]{x[1], -x[0] + epsilon*(1 - x[0]*x[0])*x[1]};
		}

		@Override
		public float[] g(float t, float[] u) {
			return getState();
		}

		@Override
		public int getInputDimension() {
			return 0;
		}

		@Override
		public int getOutputDimension() {
			return 2;
		}
	}
	
	//run as application to plot
	public static void main(String[] args) {
		RK45IntegratorTest test = new RK45IntegratorTest();
		test.testIntegrate();
	}

}
