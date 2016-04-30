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
