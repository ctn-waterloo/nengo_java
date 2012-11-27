/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "NetworkImpl.java". Description:
"Default implementation of Network"

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
 * Created on 23-May-2006
 */
package ca.nengo.model.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.nengo.model.Ensemble;
import ca.nengo.model.InstantaneousOutput;
import ca.nengo.model.Network;
import ca.nengo.model.Node;
import ca.nengo.model.Origin;
import ca.nengo.model.Probeable;
import ca.nengo.model.Projection;
import ca.nengo.model.RealOutput;
import ca.nengo.model.SimulationException;
import ca.nengo.model.SimulationMode;
import ca.nengo.model.SpikeOutput;
import ca.nengo.model.StepListener;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.Units;
import ca.nengo.model.nef.impl.DecodableEnsembleImpl;
import ca.nengo.model.nef.impl.DecodedOrigin;
import ca.nengo.model.nef.impl.NEFEnsembleImpl;
import ca.nengo.model.neuron.Neuron;
import ca.nengo.sim.Simulator;
import ca.nengo.sim.impl.LocalSimulator;
import ca.nengo.util.MU;
import ca.nengo.util.Probe;
import ca.nengo.util.ScriptGenException;
import ca.nengo.util.TaskSpawner;
import ca.nengo.util.ThreadTask;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.VisiblyMutable;
import ca.nengo.util.VisiblyMutableUtils;
import ca.nengo.util.impl.ProbeTask;
import ca.nengo.util.impl.ScriptGenerator;

/**
 * Default implementation of Network.
 *
 * @author Bryan Tripp
 */
public class NetworkArrayImpl extends NetworkImpl {

	/**
	 * Default name for a Network
	 */
	private static final long serialVersionUID = 1L;
	// ?? private static Logger ourLogger = Logger.getLogger(NetworkImpl.class);
	
	private final int myDimension;
	
	private final NEFEnsembleImpl[] myNodes;
	private Map<String, Origin> myOrigins;
	private int myNeurons;

	/**
	 * Sets up a network's data structures
	 */
	public NetworkArrayImpl(String name, NEFEnsembleImpl[] nodes) throws StructuralException {
		super();
		
		this.setName(name);
		myDimension = nodes.length * nodes[0].getDimension();
		
		myNodes = nodes.clone();
		myNeurons = 0;
		
		myOrigins = new HashMap<String, Origin>(10);
		
		for(int i = 0; i < nodes.length; i++) {
			this.addNode(nodes[i]);
			myNeurons += nodes[i].getNeurons();
		}
		
		this.setUseGPU(true);
	}

	private void createEnsembleOrigin(String name) {
		// Create array origin too.
		this.exposeOrigin(this.myOrigins.get(name), name);
	}
	
	public int getNeurons() {
		return myNeurons;
	}
	
	public class ArrayOrigin extends BasicOrigin {

		private static final long serialVersionUID = 1L;
		
		private String myName;
		private NetworkArrayImpl myParent;
		private DecodedOrigin[] myOrigins;
		private int myDimensions;

		public ArrayOrigin(NetworkArrayImpl parent, String name, DecodedOrigin[] origins) {
			myParent = parent;
			myName = name;
			myOrigins = origins;
			myDimensions = 0;
			for(int i=0; i < myOrigins.length; i++)
				myDimensions += myOrigins[i].getDimensions();
		}
		
		public String getName() {
			return myName;
		}
		
		public int getDimensions() {
			return myDimensions;
		}
		
		public void setValues(RealOutput values) {
			float time = values.getTime();
			Units units = values.getUnits();
			float[] vals = ((RealOutput)values).getValues();
			
			int d=0;
			for(int i=0; i < myOrigins.length; i++) {
				float[] ovals = new float[myOrigins[i].getDimensions()];
				for(int j=0; j < ovals.length; j++)
					ovals[j] = vals[d+j];
				d += myOrigins[i].getDimensions();
				
				myOrigins[i].setValues(new RealOutputImpl(ovals, units, time));
			}
			
		}

		public InstantaneousOutput getValues() throws SimulationException {
			InstantaneousOutput v0 = myOrigins[0].getValues();
			
			Units unit = v0.getUnits();
			float time = v0.getTime();
			
			if(v0 instanceof PreciseSpikeOutputImpl) {
				float[] vals = new float[myDimensions];
				int d=0;
				for(int i=0; i < myOrigins.length; i++) {
					float[] ovals = ((PreciseSpikeOutputImpl)myOrigins[i].getValues()).getSpikeTimes();
					for(int j=0; j < ovals.length; j++)
						vals[d++] = ovals[j];
				}
				
				return new PreciseSpikeOutputImpl(vals, unit, time);
			} else if(v0 instanceof RealOutputImpl) {
				float[] vals = new float[myDimensions];
				int d=0;
				for(int i=0; i < myOrigins.length; i++) {
					float[] ovals = ((RealOutputImpl)myOrigins[i].getValues()).getValues();
					for(int j=0; j < ovals.length; j++)
						vals[d++] = ovals[j];
				}
				
				return new RealOutputImpl(vals, unit, time);
			} else if(v0 instanceof SpikeOutputImpl) {
				boolean[] vals = new boolean[myDimensions];
				int d=0;
				for(int i=0; i < myOrigins.length; i++) {
					boolean[] ovals = ((SpikeOutputImpl)myOrigins[i].getValues()).getValues();
					for(int j=0; j < ovals.length; j++)
						vals[d++] = ovals[j];
				}
				
				return new SpikeOutputImpl(vals, unit, time);
			} else {
				System.err.println("Unknown type in ArrayOrigin.getValues()");
				return null;
			}
		}
		
		public Node getNode() {
			return myParent;
		}
		
		public boolean getRequiredOnCPU() {
			for(int i=0; i < myOrigins.length; i++)
				if(myOrigins[i].getRequiredOnCPU())
					return true;
			return false;
		}
		
		public void setRequiredOnCPU(boolean req) {
			for(int i=0; i < myOrigins.length; i++)
				myOrigins[i].setRequiredOnCPU(req);
		}
		
		public Origin clone() {
			//this is how it was implemented in networkarray, but I don't think it will work (myOrigins needs to be updated to the cloned origins)
			return new ArrayOrigin(myParent, myName, myOrigins);
		}

		public float[][] getDecoders() {
			int neurons = myParent.getNeurons();
			float[][] decoders = new float[neurons*myOrigins.length][myDimensions];
			for(int i=0; i < myOrigins.length; i++) {
				MU.copyInto(myOrigins[i].getDecoders(), decoders, i*neurons, i*myOrigins[i].getDimensions(), neurons);
			}
			return decoders;
		}

	}
	
}