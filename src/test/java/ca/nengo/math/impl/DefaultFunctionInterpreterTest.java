package ca.nengo.math.impl;

import java.io.Serializable;
import java.util.List;

import ca.nengo.math.Function;

import static org.junit.Assert.*;
import org.junit.Test;

public class DefaultFunctionInterpreterTest {
	@Test
	public void testRegisterFunction() {
		Function c = new ConstantFunction(2, 1f);
		DefaultFunctionInterpreter interpreter = new DefaultFunctionInterpreter();
		interpreter.registerFunction("const", c);
		PostfixFunction f = (PostfixFunction) interpreter.parse("const(x0, x1)", 2);
		List<Serializable> l = f.getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(c, l.get(2));
		
		try {
			interpreter.registerFunction("const", new ConstantFunction(1, 1f));
			fail("Should have thrown exception due to duplicate name");			
		} catch (Exception e) {} //exception is expected
	}

	@Test
	public void testParse() {
		float tolerance = .0001f;
		DefaultFunctionInterpreter interpreter = new DefaultFunctionInterpreter();
		Function f = null;
		List<Serializable> l = null;
		
		// basic binary cases ...
		f = interpreter.parse("x0 + x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("+", l.get(2).toString());
		assertEquals(2f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 - x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("-", l.get(2).toString());
		assertEquals(0f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 * x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("*", l.get(2).toString());
		assertEquals(1f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 / x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("/", l.get(2).toString());
		assertEquals(.5f, f.map(new float[]{1f, 2f}), tolerance);
		
		f = interpreter.parse("x0 ^ x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("^", l.get(2).toString());
		assertEquals(4f, f.map(new float[]{2f, 2f}), tolerance);
		
		f = interpreter.parse("x0 < x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("<", l.get(2).toString());
		assertEquals(1f, f.map(new float[]{0f, 1f}), tolerance);
		assertEquals(0f, f.map(new float[]{1f, 0f}), tolerance);
		
		f = interpreter.parse("x0 > x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(">", l.get(2).toString());
		assertEquals(0f, f.map(new float[]{0f, 1f}), tolerance);
		assertEquals(1f, f.map(new float[]{1f, 0f}), tolerance);
		
		f = interpreter.parse("x0 & x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("&", l.get(2).toString());
		assertEquals(1f, f.map(new float[]{1f, 1f}), tolerance);
		assertEquals(0f, f.map(new float[]{0f, 1f}), tolerance);
		
		f = interpreter.parse("x0 | x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("|", l.get(2).toString());
		assertEquals(0f, f.map(new float[]{0f, 0f}), tolerance);
		assertEquals(1f, f.map(new float[]{0f, 1f}), tolerance);
		
		f = interpreter.parse("x0|x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("|", l.get(2).toString());
		
		f = interpreter.parse("x0| x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("|", l.get(2).toString());
		
		f = interpreter.parse("x0 |x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("|", l.get(2).toString());
		
		f = interpreter.parse("x0 % x1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("%", l.get(2).toString());
		assertEquals(0f, f.map(new float[]{10f, 2f}), tolerance);
		assertEquals(1f, f.map(new float[]{10f, 3f}), tolerance);
		
		// basic unary cases ...  
		f = interpreter.parse("x0", 1);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(1f, f.map(new float[]{1f}), tolerance);
		
		f = interpreter.parse("-x0", 1);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals("~", l.get(1).toString());
		assertEquals(-1f, f.map(new float[]{1f}), tolerance);
		
		f = interpreter.parse("!x0", 1);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals("!", l.get(1).toString());
		assertEquals(0f, f.map(new float[]{1f}), tolerance);
		assertEquals(1f, f.map(new float[]{0f}), tolerance);
		
		f = interpreter.parse("! x0", 1);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals("!", l.get(1).toString());
		
		//precedence ... 
		f = interpreter.parse("x0 + x1 + x2", 3);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("+", l.get(2).toString());
		assertEquals(Integer.valueOf(2), l.get(3));
		assertEquals("+", l.get(4).toString());
		assertEquals(6f, f.map(new float[]{1f, 2f, 3f}), tolerance);
		
		f = interpreter.parse("x0 * x1 + x2", 3);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("*", l.get(2).toString());
		assertEquals(Integer.valueOf(2), l.get(3));
		assertEquals("+", l.get(4).toString());
		assertEquals(5f, f.map(new float[]{1f, 2f, 3f}), tolerance);
		
		f = interpreter.parse("x0 / x1 - x2", 3);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("/", l.get(2).toString());
		assertEquals(Integer.valueOf(2), l.get(3));
		assertEquals("-", l.get(4).toString());
		assertEquals(-2.5f, f.map(new float[]{1f, 2f, 3f}), tolerance);
		
		f = interpreter.parse("x0 + x1 * x2", 3);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(Integer.valueOf(2), l.get(2));
		assertEquals("*", l.get(3).toString());
		assertEquals("+", l.get(4).toString());
		assertEquals(7f, f.map(new float[]{1f, 2f, 3f}), tolerance);
		
		f = interpreter.parse("x0 + x1 * -x2", 3);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(Integer.valueOf(2), l.get(2));
		assertEquals("~", l.get(3).toString());
		assertEquals("*", l.get(4).toString());
		assertEquals("+", l.get(5).toString());
		assertEquals(-5f, f.map(new float[]{1f, 2f, 3f}), tolerance);
		
		f = interpreter.parse("x0 * x1 ^ x2", 3);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(Integer.valueOf(2), l.get(2));
		assertEquals("^", l.get(3).toString());
		assertEquals("*", l.get(4).toString());
		assertEquals(8f, f.map(new float[]{1f, 2f, 3f}), tolerance);
		
		f = interpreter.parse("x0 * x1 ^ x2 & x3", 4);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(Integer.valueOf(2), l.get(2));
		assertEquals("^", l.get(3).toString());
		assertEquals("*", l.get(4).toString());
		assertEquals(Integer.valueOf(3), l.get(5));
		assertEquals("&", l.get(6).toString());
		assertEquals(1f, f.map(new float[]{1f, 2f, 3f, 1f}), tolerance);
		
		//brackets ... 
		f = interpreter.parse("x0 * (x1 + x2)", 3);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(Integer.valueOf(2), l.get(2));
		assertEquals("+", l.get(3).toString());
		assertEquals("*", l.get(4).toString());
		assertEquals(5f, f.map(new float[]{1f, 2f, 3f}), tolerance);
		
		f = interpreter.parse("(x0 * x1) ^ (x2 & x3)", 4);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals("*", l.get(2).toString());
		assertEquals(Integer.valueOf(2), l.get(3));
		assertEquals(Integer.valueOf(3), l.get(4));
		assertEquals("&", l.get(5).toString());
		assertEquals("^", l.get(6).toString());
		assertEquals(2f, f.map(new float[]{1f, 2f, 3f, 4f}), tolerance);
		
		f = interpreter.parse("x0 ^ (x1 * (x2 + x3))", 4);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(Integer.valueOf(2), l.get(2));
		assertEquals(Integer.valueOf(3), l.get(3));
		assertEquals("+", l.get(4).toString());
		assertEquals("*", l.get(5).toString());
		assertEquals("^", l.get(6).toString());
		assertEquals(16384f, f.map(new float[]{2f, 2f, 3f, 4f}), tolerance);
		
		//functions ... 
		f = interpreter.parse("x0 + sin(x1)", 4);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertTrue(l.get(2) instanceof SimpleFunctions.Sin);
		assertEquals("+", l.get(3).toString());
		assertEquals(2f, f.map(new float[]{2f, (float) Math.PI, 1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 + sin(x1 + x2)", 4);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertEquals(Integer.valueOf(1), l.get(1));
		assertEquals(Integer.valueOf(2), l.get(2));
		assertEquals("+", l.get(3).toString());
		assertTrue(l.get(4) instanceof SimpleFunctions.Sin);
		assertEquals("+", l.get(5).toString());
		assertEquals(2f, f.map(new float[]{2f, (float) Math.PI / 2f, (float) Math.PI / 2f, 1f}), tolerance);
		
		//constants ... 
		f = interpreter.parse("1.1", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertTrue(l.get(0) instanceof Float);
		assertEquals(1.1f, ((Float) l.get(0)).floatValue(), .0001f);
		assertEquals(1.1f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 + 5", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertTrue(l.get(1) instanceof Float);
		assertEquals(5f, ((Float) l.get(1)).floatValue(), .0001f);
		assertEquals("+", l.get(2).toString());
		assertEquals(6f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 + 1.5", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertTrue(l.get(1) instanceof Float);
		assertEquals(1.5f, ((Float) l.get(1)).floatValue(), .0001f);
		assertEquals("+", l.get(2).toString());
		assertEquals(2.5f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 + 0.5", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertTrue(l.get(1) instanceof Float);
		assertEquals(0.5f, ((Float) l.get(1)).floatValue(), .0001f);
		assertEquals("+", l.get(2).toString());
		assertEquals(1.5f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 + .5", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertTrue(l.get(1) instanceof Float);
		assertEquals(0.5f, ((Float) l.get(1)).floatValue(), .0001f);
		assertEquals("+", l.get(2).toString());
		assertEquals(1.5f, f.map(new float[]{1f, 1f}), tolerance);
		
		f = interpreter.parse("x0 + pi", 2);
		l = ((PostfixFunction) f).getExpressionList();
		assertEquals(Integer.valueOf(0), l.get(0));
		assertTrue(l.get(1) instanceof Float);
		assertEquals((float) Math.PI, ((Float) l.get(1)).floatValue(), .0001f);
		assertEquals("+", l.get(2).toString());
		assertEquals(1f + (float) Math.PI, f.map(new float[]{1f, 1f}), tolerance);

		//dimension ... 
		f = interpreter.parse("x0", 5);
		assertEquals(5, f.getDimension());
		
		//everything ... 
		f = interpreter.parse("2*pi + .5*x0 / sin((x1 + x2)^x3)", 4);
		l = ((PostfixFunction) f).getExpressionList();
		assertTrue(l.get(0) instanceof Float);
		assertEquals(2f, ((Float) l.get(0)).floatValue(), .0001f);
		assertTrue(l.get(1) instanceof Float);
		assertEquals((float) Math.PI, ((Float) l.get(1)).floatValue(), .0001f);
		assertEquals("*", l.get(2).toString());
		assertTrue(l.get(3) instanceof Float);
		assertEquals(.5f, ((Float) l.get(3)).floatValue(), .0001f);
		assertEquals(Integer.valueOf(0), l.get(4));
		assertEquals("*", l.get(5).toString());
		assertEquals(Integer.valueOf(1), l.get(6));
		assertEquals(Integer.valueOf(2), l.get(7));
		assertEquals("+", l.get(8).toString());
		assertEquals(Integer.valueOf(3), l.get(9));
		assertEquals("^", l.get(10).toString());
		assertTrue(l.get(11) instanceof SimpleFunctions.Sin);		
		assertEquals("/", l.get(12).toString());
		assertEquals("+", l.get(13).toString());
		assertEquals(2f * (float) Math.PI - 1 / .13235175f, f.map(new float[]{2f, 2f, 3f, 2f}), tolerance);
	}
}
