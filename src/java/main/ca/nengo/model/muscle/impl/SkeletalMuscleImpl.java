/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "SkeletalMuscleImpl.java". Description:
"Basic SkeletalMuscle implementation with unspecified activation-force dynamics.

  TODO: origins (need spindle and GTO implementations)

  @author Bryan Tripp"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU
Public License license (the GPL License), in which case the provisions of GPL
License are applicable  instead of those above. If you wish to allow use of your
version of this file only under the terms of the GPL License and not to allow
others to use your version of this file under the MPL, indicate your decision
by deleting the provisions above and replace  them with the notice and other
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on 3-Apr-07
 */
package ca.nengo.model.muscle.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ca.nengo.dynamics.DynamicalSystem;
import ca.nengo.dynamics.Integrator;
import ca.nengo.dynamics.impl.EulerIntegrator;
import ca.nengo.dynamics.impl.RK45Integrator;
import ca.nengo.dynamics.impl.SimpleLTISystem;
import ca.nengo.model.Node;
import ca.nengo.model.Origin;
import ca.nengo.model.SimulationException;
import ca.nengo.model.SimulationMode;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.Units;
import ca.nengo.model.impl.BasicTermination;
import ca.nengo.model.muscle.SkeletalMuscle;
import ca.nengo.util.MU;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.VisiblyMutableUtils;
import ca.nengo.util.impl.ScriptGenerator;
import ca.nengo.util.impl.TimeSeries1DImpl;
import ca.nengo.util.impl.TimeSeriesImpl;

/**
 * Basic SkeletalMuscle implementation with unspecified activation-force dynamics.
 *
 * TODO: origins (need spindle and GTO implementations)
 *
 * @author Bryan Tripp
 */
public class SkeletalMuscleImpl implements SkeletalMuscle {

	private static final long serialVersionUID = 1L;

	private String myName;
	private BasicTermination myTermination;
	private DynamicalSystem myEADynamics; //excitation-activation dynamics
	private DynamicalSystem myAFDynamics; //activation-force dynamics
	private Integrator myIntegrator;
	private float myLength;
	private String myDocumentation;
	private transient List<Listener> myListeners;

	private TimeSeries myActivationHistory; //saved for single timestep to support Probeable
	private TimeSeries myForceHistory;
	private TimeSeries myLengthHistory;

	/**
	 * @param name Muscle name
	 * @param dynamics Dynamics for the muscle
	 * @throws StructuralException if dimensionality isn't 2 in, 1 out
	 */
	public SkeletalMuscleImpl(String name, DynamicalSystem dynamics) throws StructuralException {
		myName = name;
		myTermination = makeTermination();

		if (dynamics.getInputDimension() != 2) {
            throw new StructuralException("Input dimension of dynamics must be 2 (activation; length)");
        }
		if (dynamics.getOutputDimension() != 1) {
            throw new StructuralException("Output dimension of dynamics must be 1 (force)");
        }
		myAFDynamics = dynamics;

		myIntegrator = new RK45Integrator();
	}

	private BasicTermination makeTermination() {
		Units[] units = Units.uniform(Units.UNK, 1);
		DynamicalSystem myEADynamics = new SimpleLTISystem(new float[]{-1f/.005f}, MU.I(1), MU.I(1), new float[1], units) {
			private static final long serialVersionUID = 1L;

			//override to rectify excitation (can't have negative excitation to muscles)
			public float[] f(float t, float[] u) {
				u[0] = Math.abs(u[0]);
				return super.f(t, u);
			}
		};
		return new BasicTermination(this, myEADynamics, new EulerIntegrator(.001f), SkeletalMuscle.EXCITATION_TERMINATION);
	}

	/**
	 * @see ca.nengo.model.Node#getMode()
	 */
	public SimulationMode getMode() {
		return SimulationMode.DEFAULT;
	}

	/**
	 * @see ca.nengo.model.Node#setMode(ca.nengo.model.SimulationMode)
	 */
	public void setMode(SimulationMode mode) {
		//only default is supported
	}

	/**
	 * @see ca.nengo.model.Node#getName()
	 */
	public String getName() {
		return myName;
	}

	/**
	 * @param name The new name (must be unique within any networks of which this Node
	 * 		will be a part)
	 */
	public void setName(String name) throws StructuralException {
		VisiblyMutableUtils.nameChanged(this, getName(), name, myListeners);
		myName = name;
	}

	/**
	 * @see ca.nengo.model.Node#getOrigin(java.lang.String)
	 */
	public Origin getOrigin(String name) throws StructuralException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see ca.nengo.model.Node#getOrigins()
	 */
	public Origin[] getOrigins() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see ca.nengo.model.Node#getTermination(java.lang.String)
	 */
	public Termination getTermination(String name) throws StructuralException {
		if (name.equals(SkeletalMuscle.EXCITATION_TERMINATION)) {
			return myTermination;
		} else {
			throw new StructuralException("Termination " + name + " does not exist");
		}
	}

	/**
	 * @see ca.nengo.model.Node#getTerminations()
	 */
	public Termination[] getTerminations() {
		return new Termination[]{myTermination};
	}

	/**
	 * @see ca.nengo.model.Node#run(float, float)
	 */
	public void run(float startTime, float endTime) throws SimulationException {
		myTermination.run(startTime, endTime);
		myActivationHistory = myTermination.getOutput();
		myLengthHistory = new TimeSeries1DImpl(new float[]{startTime}, new float[]{myLength}, Units.M);

		float[][] activation = myActivationHistory.getValues();
		float[][] input = new float[activation.length][];
//		for (float[] element : input) {
//
//		}
		myForceHistory = myIntegrator.integrate(myAFDynamics, new TimeSeriesImpl(myActivationHistory.getTimes(), input, new Units[]{Units.UNK, Units.M}));
	}

	/**
	 * @see ca.nengo.model.muscle.SkeletalMuscle#getForce()
	 */
	public float getForce() {
		float[][] force = myForceHistory.getValues();
		return force[force.length][0];
	}

	/**
	 * @see ca.nengo.model.muscle.SkeletalMuscle#setLength(float)
	 */
	public void setLength(float length) {
		myLength = length;
	}

	/**
	 * @see ca.nengo.model.Resettable#reset(boolean)
	 */
	public void reset(boolean randomize) {
		myEADynamics.setState(new float[]{myEADynamics.getState().length});
		myAFDynamics.setState(new float[]{myAFDynamics.getState().length});
		myTermination.reset(randomize);
	}

	/**
	 * @see ca.nengo.model.Probeable#getHistory(java.lang.String)
	 */
	public TimeSeries getHistory(String stateName) throws SimulationException {
		TimeSeries result = null;

		if (stateName.equals(SkeletalMuscle.ACTIVATION)) {
			result = myActivationHistory;
		} else if (stateName.equals(SkeletalMuscle.FORCE)) {
			result = myForceHistory;
		} else if (stateName.equals(SkeletalMuscle.LENGTH)) {
			result = myLengthHistory;
		}

		return result;
	}

	/**
	 * @see ca.nengo.model.Probeable#listStates()
	 */
	public Properties listStates() {
		Properties result = new Properties();

		result.setProperty(SkeletalMuscle.ACTIVATION, "Muscle activation level (0 to 1)");
		result.setProperty(SkeletalMuscle.FORCE, "Tension in muscle (N)");
		result.setProperty(SkeletalMuscle.LENGTH, "Muscle length (m)");

		return result;
	}
	
	/**
	 * @see ca.nengo.model.Node#getDocumentation()
	 */
	public String getDocumentation() {
		return myDocumentation;
	}

	/**
	 * @see ca.nengo.model.Node#setDocumentation(java.lang.String)
	 */
	public void setDocumentation(String text) {
		myDocumentation = text;
	}

	/**
	 * @see ca.nengo.util.VisiblyMutable#addChangeListener(ca.nengo.util.VisiblyMutable.Listener)
	 */
	public void addChangeListener(Listener listener) {
		if (myListeners == null) {
			myListeners = new ArrayList<Listener>(2);
		}
		myListeners.add(listener);
	}

	/**
	 * @see ca.nengo.util.VisiblyMutable#removeChangeListener(ca.nengo.util.VisiblyMutable.Listener)
	 */
	public void removeChangeListener(Listener listener) {
		myListeners.remove(listener);
	}

	@Override
	public SkeletalMuscle clone() throws CloneNotSupportedException {
		try {
			SkeletalMuscleImpl result = new SkeletalMuscleImpl(myName, myAFDynamics.clone());
			result.setLength(myLength);
			result.setDocumentation(myDocumentation);
			result.myActivationHistory = myActivationHistory.clone();
			result.myForceHistory = myForceHistory.clone();
			result.myLengthHistory = myLengthHistory.clone();
			return result;
		} catch (StructuralException e) {
			throw new CloneNotSupportedException("Problem trying to clone: " + e.getMessage());
		}
	}

	@Override
	public Node[] getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String generatePythonCode(ScriptGenerator script) {
		// TODO Auto-generated method stub
		return null;
	}

}
