/*
 * Created on 30-Mar-07
 */
package ca.neo.model.neuron.impl;

import ca.neo.dynamics.impl.AbstractDynamicalSystem;
import ca.neo.dynamics.impl.RK45Integrator;

/**
 * A SpikeGenerator based on the Hodgkin-Huxley model.
 * 
 * TODO: find firing rate curve empirically
 * TODO: factory
 * TODO: unit test
 * 
 * @author Bryan Tripp
 */
public class HodgkinHuxleySpikeGenerator extends DynamicalSystemSpikeGenerator {

	private static final long serialVersionUID = 1L;
	
	public HodgkinHuxleySpikeGenerator() {
		super(new HodgkinHuxleySystem(new float[4]), new RK45Integrator(), 0, 30f, .002f);
	}
	
	/**
	 * Hodgkin-Huxley spiking dynamics. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class HodgkinHuxleySystem extends AbstractDynamicalSystem {
		
		private static final long serialVersionUID = 1L;
	    private static float G_Na = 120f;
	    private static float E_Na = 115f; //this potential and others are relative to -60 mV
	    private static float G_K = 36f;
	    private static float E_K = -12f;
	    private static float G_m = 0.3f;
	    private static float V_rest = 10.613f;
	    private static float C_m = 1f;

		public HodgkinHuxleySystem(float[] state) {
			super(state);
		}

		public float[] f(float t, float[] u) {

			float I_inj = u[0];
			
			float[] state = getState();
			float V = state[0];
			float m = state[1];
			float h = state[2];
			float n = state[3];
			
		    float alpha_m = (25f-V) / (10f * ((float) Math.exp((25d-V)/10d) - 1f));
		    float beta_m = 4 * (float) Math.exp(-V/18d);
		    float alpha_h = 0.07f * (float) Math.exp(-V/20d);
		    float beta_h = 1f / ((float) Math.exp((30d-V)/10d) + 1f);
		    float alpha_n = (10f-V) / (100f * ((float) Math.exp((10d-V)/10d) - 1f));
		    float beta_n = 0.125f * (float) Math.exp(-V/80d);

		    return new float[] { // dV, dm, dh, dn
				    1000 * ((G_Na * (m*m*m) * h * (E_Na - V) + G_K * (n*n*n*n) * (E_K - V) + G_m * (V_rest - V) + I_inj) / C_m), 
				    1000 * (alpha_m * (1-m) - beta_m * m), 
				    1000 * (alpha_h * (1-h) - beta_h * h), 
				    1000 * (alpha_n * (1-n) - beta_n * n)
		    };
		}

		/**
		 * @see ca.neo.dynamics.impl.AbstractDynamicalSystem#g(float, float[])
		 */
		public float[] g(float t, float[] u) {
			return getState();
		}

		/**
		 * @see ca.neo.dynamics.impl.AbstractDynamicalSystem#getInputDimension()
		 */
		public int getInputDimension() {
			return 1;
		}

		/**
		 * @see ca.neo.dynamics.impl.AbstractDynamicalSystem#getOutputDimension()
		 */
		public int getOutputDimension() {
			return 4;
		}
		
	}
	
	//functional test
//	public static void main(String[] args) {
//		float[] x0 = new float[4];
////		x0[0] = 5;
//		DynamicalSystem hh = new HodgkinHuxleySystem(x0);
//		Integrator integrator = new RK45Integrator(1e-6f);
//		TimeSeries input = new TimeSeries1DImpl(new float[]{0, .1f}, new float[]{10, 10}, Units.uAcm2);
//		long start = System.currentTimeMillis();
//		TimeSeries result = integrator.integrate(hh, input);
//		System.out.println("Elapsed time: " + (System.currentTimeMillis() - start));
//		Plotter.plot(result, "Hodgkin-Huxley");
//		
//		try {
//			Network network = new NetworkImpl();
//			
//			LinearSynapticIntegrator si = new LinearSynapticIntegrator(.001f, Units.ACU);
//			
//			SpikeGenerator sg = new HodgkinHuxleySpikeGenerator();
//			PlasticExpandableSpikingNeuron neuron = new PlasticExpandableSpikingNeuron(si, sg, 10, 0, "neuron");
//			Ensemble ensemble = new EnsembleImpl("ensemble", new Node[]{neuron});
//			ensemble.collectSpikes(true);
//			network.addNode(ensemble);
//			
//			FunctionInput fi = new FunctionInput("input", new Function[]{new ConstantFunction(1, 1)}, Units.ACU);
//			network.addNode(fi);
//			
//			neuron.addTermination("input", new float[][]{new float[]{1}}, .005f, false);
//			network.addProjection(fi.getOrigin(FunctionInput.ORIGIN_NAME), neuron.getTermination("input"));
//			
//			network.run(0, .5f);
//			
//			Plotter.plot(ensemble.getSpikePattern());
//		} catch (StructuralException e) {
//			e.printStackTrace();
//		} catch (SimulationException e) {
//			e.printStackTrace();
//		}
//		
//	}

}
