/*
 * #%L
 * A Fiji plugin for Curvature Analysis.
 * %%
 * Copyright (C) 2016 - 2017 Fiji developers.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package sc.fiji.kappa.curve;

import java.awt.geom.Point2D;

public class BezierPoint extends Point2D.Double {

	private static final long serialVersionUID = 1L;

	// Curvature values for a given point along a bezier curve
	public double k;
	protected int sign;

	/**
	 * Constructs a Bezier Point object
	 *
	 * @param x
	 *            The x-coordinate of the point
	 * @param y
	 *            The y-coordinate of the point
	 * @param k
	 *            The curvature of the point
	 */
	public BezierPoint(double x, double y, double k) {
		super(x, y);
		if (k >= 0) {
			this.sign = 1;
		} else {
			this.sign = -1;
		}
		this.k = Math.abs(k);
	}

	/**
	 * Returns a String representing this Bezier Point. This contains its
	 * x-coordinate, y-coordinate and curvature value
	 *
	 * @return The string representing the Bezier Point
	 */
	public String toString() {
		return String.format("x = %f, y = %f, k = %f", x, y, k);
	}
}
