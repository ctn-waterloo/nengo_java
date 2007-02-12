/*
 * Created on 2-Jun-2006
 */
package ca.neo.util;

import java.io.Serializable;

/**
 * A tool for interpolating within a SCALAR time series (see also 
 * InterpolatorND for vector time series').  
 * 
 * @author Bryan Tripp
 */
public interface Interpolator extends Serializable {

	public void setTimeSeries(TimeSeries1D series);
	
	public float interpolate(float time);
	
}
