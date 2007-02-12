/*
 * Created on May 18, 2006
 */
package ca.neo.model.neuron;

import ca.neo.model.SimulationException;
import ca.neo.model.StructuralException;
import ca.neo.model.Termination;

/**
 * A SynapticIntegrator to which Terminations can be added after construction, 
 * in a standard way. This facilitates circuit building. However, this may  
 * not be possible with a sophisticated dendritic model, with which more 
 * involved setup is probably needed (e.g. constructing individual synapse models; 
 * specifying spatial confuguration of synapses). In this case, the synpases 
 * should be defined first, before assembling the circuit, and the SynapticIntegrator 
 * might not be expandable in the standard manner defined here. 
 *   
 * @author Bryan Tripp
 */
public interface ExpandableSynapticIntegrator extends SynapticIntegrator {

	/**
	 * @param name Name of Termination
	 * @param weights Synaptic weights associated with this Termination
	 * @param tauPSC Time constant of post-synaptic current decay (all Terminations have  
	 * 		this property but it may have slightly different interpretations depending on 
	 * 		the SynapticIntegrator or other properties of the Termination).
	 * @return resulting Termination
	 * @throws SimulationException if there is already a Termination of the same name on this 
	 * 		SynapticIntegrator  
	 */
	public Termination addTermination(String name, float[] weights, float tauPSC) throws StructuralException;
	
	/**
	 * @param name Name of Termination to remove. 
	 * @throws SimulationException if there is no Termination of the given name on this 
	 * 		SynapticIntegrator  
	 */
	public void removeTermination(String name) throws StructuralException;
	
}
