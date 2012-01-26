package ca.nengo.util.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ca.nengo.model.InstantaneousOutput;
import ca.nengo.model.Node;
import ca.nengo.model.Origin;
import ca.nengo.model.Projection;
import ca.nengo.model.RealOutput;
import ca.nengo.model.SimulationException;
import ca.nengo.model.Termination;
import ca.nengo.model.Units;
import ca.nengo.model.impl.BasicOrigin;
import ca.nengo.model.impl.EnsembleTermination;
import ca.nengo.model.plasticity.impl.PlasticEnsembleTermination;
import ca.nengo.model.impl.RealOutputImpl;
import ca.nengo.model.impl.NetworkImpl;
import ca.nengo.model.impl.NetworkImpl.OriginWrapper;
import ca.nengo.model.impl.NetworkImpl.TerminationWrapper;
import ca.nengo.model.nef.NEFEnsemble;
import ca.nengo.model.nef.impl.DecodableEnsembleImpl;
import ca.nengo.model.nef.impl.DecodedOrigin;
import ca.nengo.model.nef.impl.DecodedTermination;
import ca.nengo.model.nef.impl.NEFEnsembleImpl;
import ca.nengo.model.neuron.impl.LIFSpikeGenerator;
import ca.nengo.model.neuron.impl.SpikingNeuron;
import ca.nengo.util.Memory;

public class NEFGPUInterface {
	private static boolean myUseGPU = false;
	private static boolean canUseGPU;
	private static int myNumDevices = 0;
	private static int myNumAvailableDevices;
	
	private static boolean showTiming = false;
	private boolean myShowTiming;
	private long averageTimeSpentInGPU;
	private long averageTimeSpentInCPU;
	private long totalRunTime;
	private int numSteps;
	
	
	protected NEFEnsemble[] myGPUEnsembles;
	protected Projection[] myGPUProjections;
	protected Node[] myGPUNetworkArrays;
	
	protected Node[] myNodes;
	protected Projection[] myProjections;
	
	protected float myStartTime;
	protected float myEndTime;
	
	float[][][] representedInputValues;
	float[][][] representedOutputValues;
	float[][] spikeOutput;
	boolean[][] inputOnGPU;
	
	
	// load the shared library that contains the native functions

	static{
		try {
			System.loadLibrary("NengoGPU");
			myNumAvailableDevices = nativeGetNumDevices();
			
			if(myNumAvailableDevices < 1)
			{
				System.out.println("No CUDA-enabled GPU detected.");
				canUseGPU = false;
			}
			
		} catch (java.lang.UnsatisfiedLinkError e) {
			myNumAvailableDevices = 0;
			System.out.println("Couldn't load native library NengoGPU. " +
				"Unable to use GPU for class NEFGPUInterface.");
		} catch (Exception e) {
			myNumAvailableDevices = 0;
			System.out.println(e.getStackTrace());
		}
	}

	static native int nativeGetNumDevices();

	static native void nativeSetupRun(float[][][][] terminationTransforms,
			int[][] isDecodedTermination, float[][] terminationTau,
			float[][][] encoders, float[][][][] decoders, float[][] neuronData,
			int[][] projections, int[][] networkArrayData, int[][] ensembleData, int[][] adjacencyMatrix,
			float maxTimeStep, int numDevicesRequested);

	static native void nativeStep(float[][][] representedInput,
			float[][][] representedOutput, float[][] spikes, float startTime,
			float endTime);

	static native void nativeKill();
	
	public NEFGPUInterface(){
    }

	public static int getNumAvailableDevices(){
		return myNumAvailableDevices;
	}
	
	public static void setRequestedNumDevices(int value){
		myNumDevices = value > myNumAvailableDevices ? myNumAvailableDevices : value;
	}
	
	public static int getRequestedNumDevices(){
		return myNumDevices;
	}
	
	// get whether or not to use the GPU. set whether or not to use the GPU by using setRequestedNumDevices
	public static boolean getUseGPU(){
		return myNumDevices > 0;
	}
	
	public static void showGPUTiming(){
		showTiming = true;
	}
	
	public static void hideGPUTiming(){
		showTiming = false;
	}
	
	public void initialize(){
		
		myShowTiming = showTiming;
		if(myShowTiming){
			averageTimeSpentInGPU = 0;
			averageTimeSpentInCPU = 0;
			numSteps = 0;
			totalRunTime = 0;
		}
		
		ArrayList<Node> GPUNodeList = new ArrayList<Node>();
		
		for(Node currentNode : myGPUNetworkArrays){
			// all the nodes in myGPUNetworkArrays are going to run on the GPU. 
			if(currentNode.getClass().getCanonicalName() == "org.python.proxies.nef.array$NetworkArray$6"){
				List<Node> nodeList = Arrays.asList(((NetworkImpl) currentNode).getNodes());
				GPUNodeList.addAll(nodeList);
			}
			else{
				GPUNodeList.add(currentNode);
			}
		}
		
		myGPUEnsembles = GPUNodeList.toArray(new NEFEnsemble[0]);

		if (myGPUEnsembles.length == 0)
			return;

		// Put the data in a format appropriate for passing to the GPU. 
		// Most of this function is devoted to this task.
		int i = 0, j = 0, k = 0, numEnsemblesCollectingSpikes = 0;
		NEFEnsemble workingNode;
		Termination[] terminations;
		DecodedOrigin[] origins;

		float[][][][] terminationTransforms = new float[myGPUEnsembles.length][][][];
		int[][] isDecodedTermination = new int[myGPUEnsembles.length][];
		float[][] terminationTau = new float[myGPUEnsembles.length][];
		float[][][] encoders = new float[myGPUEnsembles.length][][];
		float[][][][] decoders = new float[myGPUEnsembles.length][][][];
		float[][] neuronData = new float[myGPUEnsembles.length][];
		EnsembleData ensembleData = new EnsembleData();
		int[][] ensembleDataArray = new int[myGPUEnsembles.length][];
		boolean[] collectSpikes = new boolean[myGPUEnsembles.length];
		float maxTimeStep = ((LIFSpikeGenerator) ((SpikingNeuron) ((NEFEnsembleImpl) myGPUEnsembles[0])
				.getNodes()[0]).getGenerator()).getMaxTimeStep();

		
		int[][] adjacencyMatrix = findAdjacencyMatrix(myGPUNetworkArrays, myGPUProjections);
		
		// We put the list of projections in terms of the GPU nodes
		// For each projection we record 4 numbers: the index of the origin
		// ensemble, the index of the origin in its ensemble, the index of
		// the termination ensemble and the index of the termination in its ensemble
		int[][] adjustedProjections = new int[myGPUProjections.length][6];
		
		
		// Change this to be in terms of networkArrays
		inputOnGPU = new boolean[myGPUNetworkArrays.length][];
		
		Node workingArray;
		int networkArrayOffset = 0;

		NetworkArrayData networkArrayData = new NetworkArrayData();
		int[][] networkArrayDataArray = new int[myGPUNetworkArrays.length][];

		int totalInputSize = 0;
		
		// store networkArray data
		for(i = 0; i < myGPUNetworkArrays.length; i++){
			
			networkArrayData.reset();
			workingArray = myGPUNetworkArrays[i];
			
			networkArrayData.indexOfFirstNode = networkArrayOffset;
			
			if(workingArray instanceof NEFEnsembleImpl){
				networkArrayOffset++;
			}else{
				networkArrayOffset += ((NetworkImpl) workingArray).getNodes().length;
			}
			
			networkArrayData.endIndex = networkArrayOffset;
				
			Termination[] networkArrayTerminations = workingArray.getTerminations();
			networkArrayData.numTerminations = networkArrayTerminations.length;
			
			
			for(j = 0; j < networkArrayTerminations.length; j++){
				networkArrayData.totalInputSize += networkArrayTerminations[j].getDimensions();
			}
			
			totalInputSize += networkArrayData.totalInputSize;
			
			Origin[] networkArrayOrigins;
			if(workingArray instanceof NEFEnsembleImpl)
			{
				networkArrayOrigins = ((NEFEnsembleImpl) workingArray).getDecodedOrigins();
			}else{
				networkArrayOrigins = workingArray.getOrigins();
			}
			networkArrayData.numOrigins = networkArrayOrigins.length;
			
			for(j = 0; j < networkArrayOrigins.length; j++){
				networkArrayData.totalOutputSize += networkArrayOrigins[j].getDimensions();
			}
			
			if(workingArray instanceof NEFEnsembleImpl){
				networkArrayData.numNeurons = ((NEFEnsembleImpl) workingArray).getNeurons();
			}else{
				Node[] subNodes = ((NetworkImpl) workingArray).getNodes();
				for(j = 0; j < subNodes.length; j++){
					networkArrayData.numNeurons += ((NEFEnsembleImpl) subNodes[j]).getNeurons();
				}
			}

			networkArrayDataArray[i] = networkArrayData.getAsArray();
			
			inputOnGPU[i] = new boolean[networkArrayTerminations.length];
			
			for(j = 0; j < networkArrayTerminations.length; j++){
				Termination termination = networkArrayTerminations[j];
				boolean terminationWrapped = termination instanceof TerminationWrapper;
				if(terminationWrapped)
					termination = ((TerminationWrapper) termination).getBaseTermination();
				
				k = 0;
				boolean projectionMatches = false;
				
				while(!projectionMatches && k < myGPUProjections.length){
					Termination projectionTermination = myGPUProjections[k].getTermination();
					boolean projectionTerminationWrapped = projectionTermination instanceof TerminationWrapper;
					if(projectionTerminationWrapped)
						projectionTermination = ((TerminationWrapper) projectionTermination).getBaseTermination();
					
					projectionMatches = termination == projectionTermination;
					
					if(projectionMatches)
						break;
					
					k++;
				}
	
				if (projectionMatches) {
					adjustedProjections[k][2] = i;
					adjustedProjections[k][3] = j;
					adjustedProjections[k][4] = termination.getDimensions();
					adjustedProjections[k][5] = -1;
	
					inputOnGPU[i][j] = true;
				} else {
					inputOnGPU[i][j] = false;
				}
			}
			
			for (j = 0; j < networkArrayOrigins.length; j++) {
				Origin origin = networkArrayOrigins[j];
				boolean originWrapped = origin instanceof OriginWrapper;
				if(originWrapped)
					origin = ((OriginWrapper) origin).getWrappedOrigin();
				
				for (k = 0; k < myGPUProjections.length; k++) {
					Origin projectionOrigin = myGPUProjections[k].getOrigin();
					boolean projectionOriginWrapped = projectionOrigin instanceof OriginWrapper;
					
					if(projectionOriginWrapped)
						projectionOrigin = ((OriginWrapper) projectionOrigin).getWrappedOrigin();
					
					if (origin == projectionOrigin) {
						adjustedProjections[k][0] = i;
						adjustedProjections[k][1] = j;
					}
				}
			}
		}
		
		
		// store NEFEnsemble data
		for (i = 0; i < myGPUEnsembles.length; i++) {
			
			workingNode = myGPUEnsembles[i];
			
			ensembleData.reset();

			ensembleData.dimension = workingNode.getDimension();
			ensembleData.numNeurons = workingNode.getNodeCount();

			terminations = workingNode.getTerminations();

			int terminationDim = 0;
			ensembleData.maxTransformDimension = 0;

			terminationTransforms[i] = new float[terminations.length][][];
			terminationTau[i] = new float[terminations.length];
			isDecodedTermination[i] = new int[terminations.length];

			for (j = 0; j < terminations.length; j++) {

				if (terminations[j] instanceof DecodedTermination) {
					terminationTransforms[i][j] = ((DecodedTermination) terminations[j])
							.getTransform();
					terminationTau[i][j] = terminations[j].getTau();

					terminationDim = terminations[j].getDimensions();
					ensembleData.totalInputSize += terminationDim;

					if (terminationDim > ensembleData.maxTransformDimension) {
						ensembleData.maxTransformDimension = terminationDim;
					}

					isDecodedTermination[i][j] = 1;
					
					ensembleData.numDecodedTerminations++;
				} else if (terminations[j] instanceof PlasticEnsembleTermination) {
					terminationTransforms[i][j] = new float[1][1];
					float[][] tempTransform = ((PlasticEnsembleTermination) terminations[j])
							.getTransform();
					terminationTransforms[i][j][0][0] = tempTransform[0][0];
					terminationTau[i][j] = terminations[j].getTau();
					isDecodedTermination[i][j] = 0;

					terminationDim = 1;
					ensembleData.totalInputSize += 1;
					ensembleData.numNonDecodedTerminations++;
				}
			}

			encoders[i] = workingNode.getEncoders();
			float[] radii = workingNode.getRadii();
			for (j = 0; j < encoders[i].length; j++) {
				for (k = 0; k < encoders[i][j].length; k++)
					encoders[i][j][k] = encoders[i][j][k] / radii[k];
			}

			origins = ((NEFEnsembleImpl) workingNode).getDecodedOrigins();

			ensembleData.numOrigins = origins.length;
			ensembleData.maxDecoderDimension = 0;

			decoders[i] = new float[origins.length][][];
			int originDim;
			for (j = 0; j < origins.length; j++) {
				decoders[i][j] = origins[j].getDecoders();
				originDim = origins[j].getDimensions();

				ensembleData.totalOutputSize += originDim;

				if (originDim > ensembleData.maxDecoderDimension) {
					ensembleData.maxDecoderDimension = originDim;
				}
			}

			neuronData[i] = ((NEFEnsembleImpl) workingNode).getStaticNeuronData();

			collectSpikes[i] = workingNode.isCollectingSpikes();
			numEnsemblesCollectingSpikes++;

			ensembleDataArray[i] = ensembleData.getAsArray();
		}
		
		nativeSetupRun(terminationTransforms, isDecodedTermination,
				terminationTau, encoders, decoders, neuronData,
				adjustedProjections, networkArrayDataArray, ensembleDataArray, 
				adjacencyMatrix, maxTimeStep, myNumDevices);

		// Set up the data structures that we pass in and out of the native step call.
		// They do not change in size from step to step so we can re-use them.
		representedInputValues = new float[myGPUNetworkArrays.length][][];
		representedOutputValues = new float[myGPUNetworkArrays.length][][];
		spikeOutput = new float [myGPUEnsembles.length][];
		
		for (i = 0; i < myGPUNetworkArrays.length; i++) {
			terminations = myGPUNetworkArrays[i].getTerminations();
			representedInputValues[i] = new float[terminations.length][];
		}

		for (i = 0; i < myGPUNetworkArrays.length; i++) {
			Origin[] networkArrayOrigins;
			if(myGPUNetworkArrays[i] instanceof NEFEnsembleImpl)
			{
				networkArrayOrigins = ((NEFEnsembleImpl) myGPUNetworkArrays[i]).getDecodedOrigins();
			}else{
				networkArrayOrigins = myGPUNetworkArrays[i].getOrigins();
			}

			representedOutputValues[i] = new float[networkArrayOrigins.length][];

			for (j = 0; j < networkArrayOrigins.length; j++) {
				representedOutputValues[i][j] = new float[networkArrayOrigins[j].getDimensions()];
			}
		}

		int spikeIndex = 0;
		for (i = 0; i < myGPUEnsembles.length; i++) {
			spikeOutput[spikeIndex++] = new float[((NEFEnsembleImpl) myGPUEnsembles[i]).getNeurons()];
		}
	}
	
	public void step(float startTime, float endTime){
		
		myStartTime = startTime;
		myEndTime = endTime;
		
		long GPUinterval = 0, CPUinterval = 0;

		if(myShowTiming){
			long GPUstartTime = new Date().getTime();
			System.out.println("before GPU processing: " + GPUstartTime);
			GPUinterval = GPUstartTime;
		}
		
		if(myGPUEnsembles.length > 0){
		
			try {
				
				int count, i, j;
				float[] inputRow = new float[0];
				Termination[] terminations;
					
				// get the input data from the terminations
				for (i = 0; i < myGPUNetworkArrays.length; i++) {
					terminations = myGPUNetworkArrays[i].getTerminations();
					count = terminations.length;
					
					for (j = 0; j < count; j++) {
						Termination termination = terminations[j];
						boolean terminationWrapped = termination instanceof TerminationWrapper;
						if(terminationWrapped)
							termination = ((TerminationWrapper) termination).getBaseTermination();
						
						// we only get input for non-GPU terminations
						if (!inputOnGPU[i][j]) {
							if (termination instanceof DecodedTermination)
								inputRow = ((DecodedTermination) termination).getInput().getValues();
							else if (termination instanceof PlasticEnsembleTermination)
								inputRow = ((RealOutput) ((PlasticEnsembleTermination) termination).getInput()).getValues();
							else if(termination instanceof EnsembleTermination)
	              // this case is mostly for terminations on network arrays
								inputRow = ((RealOutput) ((EnsembleTermination) termination).getInput()).getValues();
							else
								System.out.println("warning: using an unsupported termination type");
							
							representedInputValues[i][j] = inputRow;
						}
					}
				}
	
	
				nativeStep(representedInputValues, representedOutputValues, spikeOutput, startTime, endTime);
	
				
				// Put data computed by GPU in the origins
				int spikeIndex = 0;
				Origin[] origins;
				for (i = 0; i < myGPUNetworkArrays.length; i++) {
	
					if(myGPUNetworkArrays[i] instanceof NEFEnsembleImpl)
					{
						origins = ((NEFEnsembleImpl) myGPUNetworkArrays[i]).getDecodedOrigins();
					}else{
						origins = myGPUNetworkArrays[i].getOrigins();
					}
					
					count = origins.length;
	
					for (j = 0; j < count; j++) {
						Origin origin = origins[j];
						boolean originWrapped = origin instanceof OriginWrapper;
						if(originWrapped)
							origin = ((OriginWrapper) origin).getBaseOrigin();
						
						// we have to do all this ugliness because the Origin interface does not require a setValues method
						if(origin instanceof DecodedOrigin)	{
							((DecodedOrigin) origin).setValues(new RealOutputImpl(
								representedOutputValues[i][j].clone(),
								Units.UNK, endTime));
						}else if(origin instanceof BasicOrigin) {
							((BasicOrigin) origin).setValues(new RealOutputImpl(
									representedOutputValues[i][j].clone(),
									Units.UNK, endTime));
						}else{
							System.out.println("Warning: using unsupported origin type:" + origin.getClass().getName());
						}
						
					}
					/*
					if (((NEFEnsembleImpl) myGPUNodes[i]).isCollectingSpikes()) {
						((NEFEnsembleImpl) myGPUNodes[i]).setSpikePattern(spikeOutput[spikeIndex], endTime);
					}
					*/
	
					if(myGPUNetworkArrays[i] instanceof DecodableEnsembleImpl){
						((DecodableEnsembleImpl) myGPUNetworkArrays[i]).setTime(endTime);
					}else if(myGPUNetworkArrays[i] instanceof NetworkImpl){
						((NetworkImpl) myGPUNetworkArrays[i]).setTime(endTime);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(myShowTiming){
			long GPUendTime = new Date().getTime();
			System.out.println("After GPU processing: " + GPUendTime);
			Memory.report("End of step" + numSteps);
			GPUinterval = GPUendTime - GPUinterval;
			
			averageTimeSpentInGPU = (averageTimeSpentInGPU * numSteps + GPUinterval) / (numSteps + 1);
			averageTimeSpentInCPU = (averageTimeSpentInCPU * numSteps + CPUinterval) / (numSteps + 1);
			numSteps++;
			
			totalRunTime += GPUinterval + CPUinterval;
		}
	}
	

	public void kill()
	{
		if(myShowTiming){
			System.out.println("Average time spent in gpu per step: " + averageTimeSpentInGPU + "(ms)");
			System.out.println("Average time spent in cpu per step: " + averageTimeSpentInCPU + "(ms)");
			System.out.println("Total run time: " + totalRunTime + "(ms)");
		}
		
		if (myGPUEnsembles.length == 0)
			return;
		
		nativeKill();
	}
	
	public Node[] takeGPUNodes(Node[] nodes){
		ArrayList<Node> gpuNodeList = new ArrayList<Node>();
		ArrayList<Node> nodeList = new ArrayList<Node>();
		
		for(int i = 0; i < nodes.length; i++){
			Node workingNode = nodes[i];
			boolean NEFEnsembleUseGPU = 
				workingNode instanceof NEFEnsembleImpl && ((NEFEnsembleImpl) workingNode).getUseGPU();
			
			boolean NetworkArrayUseGPU = 
				workingNode.getClass().getCanonicalName() == "org.python.proxies.nef.array$NetworkArray$6" &&
				((NetworkImpl) workingNode).getUseGPU();
		
			if(NEFEnsembleUseGPU || NetworkArrayUseGPU){
				gpuNodeList.add(workingNode);
			}
			else{
				nodeList.add(workingNode);
			}
		}
		
		myGPUNetworkArrays = gpuNodeList.toArray(new Node[0]);
		return nodeList.toArray(new Node[0]);
	}
	
	// takeGPUNodes should be called before calling this. The nodes that run on the GPU determine the
	// projections that run on the GPU
	Projection[] takeGPUProjections(Projection[] projections){
		// Sort out the GPU projections from the CPU projections
		ArrayList<Projection> gpuProjectionsList = new ArrayList<Projection>();
		ArrayList<Projection> projectionList = new ArrayList<Projection>();
		
		List<Node> GPUNetworkArrayList = Arrays.asList(myGPUNetworkArrays);
		
		for(int i = 0; i < projections.length; i++)
		{
			Node originNode = projections[i].getOrigin().getNode();
			Node terminationNode = projections[i].getTermination().getNode();

			boolean originNodeOnGPU = GPUNetworkArrayList.contains(originNode);
			boolean terminationNodeOnGPU = GPUNetworkArrayList.contains(terminationNode);
			
			if(originNodeOnGPU && terminationNodeOnGPU)
			{
				gpuProjectionsList.add(projections[i]);
			}
			else
			{
				projectionList.add(projections[i]);
			}
		}
		
		myGPUProjections = gpuProjectionsList.toArray(new Projection[0]);
		return projectionList.toArray(new Projection[0]);
		
	}
	
	// Converts a nengo network to an undirected graph stored as a lower triangular adjacency matrix.
	// If node A projects to node B with weight x and B projects to A with weight y,
	// we treat it as a single edge with weight (x + y). Self-loops (recurrent projections)
	// are deleted (this function is used for distributing ensembles to GPUs and self loops are
	// irrelevant in that task).
	public int[][] findAdjacencyMatrix(Node[] nodes, Projection[] projections) {
		
		HashMap <Node, Integer> nodeIndexes = new HashMap<Node, Integer>();
		
		int[][] adjacencyMatrix = new int[nodes.length][nodes.length];
		
		for(int i = 0; i < nodes.length; i++){
			for(int j = 0; j < nodes.length; j++){
				adjacencyMatrix[i][j] = 0;
			}
		}
		
		for(int i = 0; i < nodes.length; i++){
			nodeIndexes.put(nodes[i], i);
		}
		
		for(int i = 0; i < projections.length; i++){
			Origin origin = projections[i].getOrigin();
			Termination termination = projections[i].getTermination();
			
			boolean originWrapped = origin instanceof OriginWrapper;
			
			if(originWrapped)
				origin = ((OriginWrapper) origin).getWrappedOrigin();
			
			boolean terminationWrapped = termination instanceof TerminationWrapper;
			
			if(terminationWrapped)
				termination = ((TerminationWrapper) termination).getBaseTermination();
			
			int originNodeIndex = nodeIndexes.get(origin.getNode());
			int termNodeIndex = nodeIndexes.get(termination.getNode());
			
			if(originNodeIndex > termNodeIndex)
				adjacencyMatrix[originNodeIndex][termNodeIndex] += termination.getDimensions();
			else if(termNodeIndex > originNodeIndex)
				adjacencyMatrix[termNodeIndex][originNodeIndex] += termination.getDimensions();
		}
		
		return adjacencyMatrix;
	}
	
	private class NetworkArrayData {
		int numEntries = 7;
		
		public int indexOfFirstNode;
		public int endIndex;
		public int numTerminations;
		public int totalInputSize;
		public int numOrigins;
		public int totalOutputSize;
		public int numNeurons;
		
		public void reset(){
			indexOfFirstNode = 0;
			endIndex = 0;
			numTerminations = 0;
			totalInputSize = 0;
			numOrigins = 0;
			totalOutputSize = 0;
			numNeurons = 0;
		}
		
		public int[] getAsArray() {
			int[] array = new int[numEntries];

			int i = 0;
			array[i++] = indexOfFirstNode;
			array[i++] = endIndex;
			array[i++] = numTerminations;
			array[i++] = totalInputSize;
			array[i++] = numOrigins;
			array[i++] = totalOutputSize;
			array[i++] = numNeurons;
			
			return array;
		}
			
	}
	// Used to hold data about each ensemble to pass to native code.
	private class EnsembleData {
		int numEntries = 9;

		public int dimension;
		public int numNeurons;
		public int numOrigins;

		public int totalInputSize;
		public int totalOutputSize;

		public int maxTransformDimension;
		public int maxDecoderDimension;

		public int numDecodedTerminations;
		public int numNonDecodedTerminations;
		

		public void reset() {
			dimension = 0;
			numNeurons = 0;
			numOrigins = 0;

			totalInputSize = 0;
			totalOutputSize = 0;

			maxTransformDimension = 0;
			maxDecoderDimension = 0;
			
			numDecodedTerminations = 0;
			numNonDecodedTerminations = 0;
		}

		public int[] getAsArray() {
			int[] array = new int[numEntries];

			int i = 0;
			array[i++] = dimension;
			array[i++] = numNeurons;
			array[i++] = numOrigins;

			array[i++] = totalInputSize;
			array[i++] = totalOutputSize;

			array[i++] = maxTransformDimension;
			array[i++] = maxDecoderDimension;
			
			array[i++] = numDecodedTerminations;
			array[i++] = numNonDecodedTerminations;
			
			return array;
		}
	}
}
