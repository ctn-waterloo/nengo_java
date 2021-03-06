package ca.nengo.math.impl;

import java.io.Serializable;
import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Test;

public class PostfixFunctionTest {
	@Test
	public void testGetDimension() {
		int dim = 10;
		PostfixFunction f = new PostfixFunction(new ArrayList<Serializable>(), "", dim);
		assertEquals(dim, f.getDimension());
	}

	@Test
	public void testMap() {
		//some basic tests follow, and a more exhaustive list is included in DefaultFunctionInterpreterTest 
		PostfixFunction f = null; 
		
		ArrayList<Serializable> l = new ArrayList<Serializable>();
		l.add(new Float(5.5f));
		f = new PostfixFunction(l, "", 0);
		assertEquals(5.5f, f.map(new float[0]), .0001f);

		l.clear();
		l.add(Integer.valueOf(0));
		f = new PostfixFunction(l, "", 1);
		assertEquals(1f, f.map(new float[]{1f}), .0001f);

		l.clear();
		l.add(Integer.valueOf(0));
		l.add(new SineFunction(1));
		f = new PostfixFunction(l, "", 1);
		assertEquals(0f, f.map(new float[]{(float) Math.PI}), .0001f);
	}

	@Test
	public void testMultiMap() {
		ArrayList<Serializable> l = new ArrayList<Serializable>();
		l.add(Integer.valueOf(0));
		PostfixFunction f = new PostfixFunction(l, "", 1);
		float[] values = f.multiMap(new float[][]{new float[]{1f}, new float[]{2f}});
		assertEquals(1f, values[0], .0001f);
		assertEquals(2f, values[1], .0001f);
	}
	
	@Test
	public void testClone() throws CloneNotSupportedException {
		PostfixFunction f1 = new PostfixFunction("x0 + x1^2", 2);
		PostfixFunction f2 = (PostfixFunction) f1.clone();
		f2.setExpression("x1"); 
		assertEquals("x0 + x1^2", f1.getExpression());
		assertTrue(f1.map(new float[]{0, 2}) - f2.map(new float[]{0, 2}) > 1);
	}
}
