/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "SimulatorEvent.java". Description: 
""

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

package ca.nengo.sim;

public class SimulatorEvent {

	public enum Type {
		STARTED, FINISHED, STEP_TAKEN
	};

	private float myProgress;
	private Type myType;
	
	private boolean interrupt=false;

	public SimulatorEvent(float progress, Type type) {
		super();
		myProgress = progress;
		myType = type;
	}

	public float getProgress() {
		return myProgress;
	}

	public Type getType() {
		return myType;
	}
	
	public boolean getInterrupt() {return interrupt;}
	public void setInterrupt(boolean state) {interrupt=state;}

}
