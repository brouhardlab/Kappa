/*
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2016 - 2017 Fiji
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.kappa.curve;

import java.awt.geom.*;

public class BezierPoint extends Point2D.Double {

    private static final long serialVersionUID = 1L;

    //Curvature values for a given point along a bezier curve
    public double k;
    protected int sign;

    /**
     * Constructs a Bezier Point object
     *
     * @param x	The x-coordinate of the point
     * @param y	The y-coordinate of the point
     * @param k	The curvature of the point
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
     * Returns a String representing this Bezier Point. This contains its x-coordinate, y-coordinate
     * and curvature value
     *
     * @return	The string representing the Bezier Point
     */
    public String toString() {
        return String.format("x = %f, y = %f, k = %f", x, y, k);
    }
}
