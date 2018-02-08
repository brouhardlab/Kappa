/*
 * #%L
 * A Fiji plugin for Curvature Analysis.
 * %%
 * Copyright (C) 2016 - 2017 Gary Brouhard
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import Jama.Matrix;
import sc.fiji.kappa.gui.KappaFrame;
import sc.fiji.kappa.gui.KappaMenuBar;

public class BSpline extends Curve {
	// We define the knot intervals to be uniform size, ie [0,1], [1,2] etc.
	// Therefore the maximum polar coordinate given m curves is [m,m,m].

	private double[] knotVector;
	private boolean isOpen;
	private BezierCurve[] spline;
	private int noCurves;
	public static final int B_SPLINE_DEGREE = 3;
	public static final int OPEN = 0;
	public static final int CLOSED = 1;
	public static final int DEFAULT_BSPLINE_TYPE = OPEN;

	// Regularization term when fitting.
	public static final double SMOOTHNESS_FACTOR = 10;

	private int[] oldFootpoints;
	private Point2D[] dataPointsCopy;

	public BSpline(List<Point2D> bsplineCtrlPts, int t, int noCtrlPts, String name, boolean open, int dataRadius,
			KappaFrame frame) {
		super(bsplineCtrlPts, t, noCtrlPts, name, dataRadius, frame);
		this.isOpen = open;
		minimumGlobalError = Double.MAX_VALUE;
		minimumLocalError = Double.MAX_VALUE;

		// We generate the knot vector and the corresponding polar values for each of
		// the control points
		// See Sederberg's text on Computer Aided Graphic Design.
		int n = B_SPLINE_DEGREE;
		if (open) {
			noCurves = noCtrlPts - n;
		} else {
			noCurves = noCtrlPts;
		}
		spline = new BezierCurve[noCurves];

		computeSpline(bsplineCtrlPts, t);
		evaluateThresholdedPixels();
	}

	public void computeSpline(List<Point2D> bsplineCtrlPts, int t) {
		int n = B_SPLINE_DEGREE;
		knotVector = new double[2 * n + noCurves - 1];

		// Generates different knot vectors depending on whether the B-Spline is open or
		// closed.
		if (isOpen) {
			// An open b-spline where the ends match the first and last endpoint have n-fold
			// knots at the ends
			// Consequently, the knot vector will have 2n + m - 1 elements.
			for (int start = 0; start < n; start++) {
				knotVector[start] = 0;
			}
			for (int middle = n; middle < knotVector.length - n; middle++) {
				knotVector[middle] = middle - n + 1;
			}
			for (int end = knotVector.length - n; end < knotVector.length; end++) {
				knotVector[end] = noCurves;
			}

			// Normalizes the knot vector
			for (int i = 0; i < knotVector.length; i++) {
				knotVector[i] = knotVector[i] / knotVector[knotVector.length - 1];
			}
			fillPoints(bsplineCtrlPts, t);
		} else {
			// A closed (periodic) b-spline requires that the first n control points are
			// identical to the last n
			// And the first n intervals are also the same as the last n.
			for (int i = 0; i < knotVector.length; i++) {
				knotVector[i] = i;
			}

			// Normalizes the knot vector
			for (int i = 0; i < knotVector.length; i++) {
				knotVector[i] = knotVector[i] / knotVector[knotVector.length - 1];
			}

			for (int i = 0; i < B_SPLINE_DEGREE; i++) {
				this.ctrlPts.add(new Point2D.Double(bsplineCtrlPts.get(i).getX(), bsplineCtrlPts.get(i).getY()));
			}
			this.noCtrlPts = noCtrlPts + n;
			fillPoints(this.ctrlPts, t);
			getKeyframes().add(new BControlPoints(this.ctrlPts, t));
		}
	}

	public void convertToOpen(int t) {
		if (this.isOpen) {
			return;
		}
		int n = B_SPLINE_DEGREE;
		this.noCurves -= B_SPLINE_DEGREE;
		this.noCtrlPts = noCtrlPts - n;
		spline = new BezierCurve[noCurves];

		// Remove the last n control points, where n is the degree
		for (int i = 0; i < B_SPLINE_DEGREE; i++) {
			this.ctrlPts.remove(this.ctrlPts.size() - 1);
		}

		// An open b-spline where the ends match the first and last endpoint have n-fold
		// knots at the ends
		// Consequently, the knot vector will have 2n + m - 1 elements.
		knotVector = new double[2 * n + noCurves - 1];
		for (int start = 0; start < n; start++) {
			knotVector[start] = 0;
		}
		for (int middle = n; middle < knotVector.length - n; middle++) {
			knotVector[middle] = middle - n + 1;
		}
		for (int end = knotVector.length - n; end < knotVector.length; end++) {
			knotVector[end] = noCurves;
		}

		// Normalizes the knot vector
		for (int i = 0; i < knotVector.length; i++) {
			knotVector[i] = knotVector[i] / knotVector[knotVector.length - 1];
		}
		this.isOpen = !this.isOpen;
		fillPoints(this.ctrlPts, t);
		getKeyframes().add(new BControlPoints(this.ctrlPts, t));
	}

	public void convertToClosed(int t) {
		if (!this.isOpen) {
			return;
		}
		int n = B_SPLINE_DEGREE;
		this.noCurves += B_SPLINE_DEGREE;
		this.noCtrlPts = noCtrlPts + n;
		spline = new BezierCurve[noCurves];

		// A closed (periodic) b-spline requires that the first n control points are
		// identical to the last n
		// And the first n intervals are also the same as the last n.
		knotVector = new double[2 * n + noCurves - 1];
		for (int i = 0; i < knotVector.length; i++) {
			knotVector[i] = i;
		}

		// Normalizes the knot vector
		for (int i = 0; i < knotVector.length; i++) {
			knotVector[i] = knotVector[i] / knotVector[knotVector.length - 1];
		}

		// Generates new control points
		for (int i = 0; i < B_SPLINE_DEGREE; i++) {
			this.ctrlPts.add(new Point2D.Double(ctrlPts.get(i).getX(), ctrlPts.get(i).getY()));
		}
		this.isOpen = !this.isOpen;
		fillPoints(this.ctrlPts, t);
		getKeyframes().add(new BControlPoints(this.ctrlPts, t));
	}

	protected List<Point2D> generateOffsetBounds(List<Point2D> bounds, int radius) {
		bounds = new ArrayList<Point2D>();

		// Adds all the right offset curves. Then a cap from the last curve.
		// Then all the left offset curves in reverse order, and then the cap from the
		// first curve.
		// This produces a polygon for the BSpline offset curve.
		// If it's closed, the caps aren't needed.
		for (int i = 0; i < noCurves; i++) {
			spline[i].generateRightOffsetCurve(bounds, radius);
		}
		if (isOpen) {
			spline[noCurves - 1].generateRightCap(bounds, radius);
		}
		for (int i = noCurves - 1; i >= 0; i--) {
			spline[i].generateLeftOffsetCurve(bounds, radius);
		}
		if (isOpen) {
			spline[0].generateLeftCap(bounds, radius);
		}
		return bounds;
	}

	// Conversion from an List to a Point2D array. This is necessary because of
	// code changes to support variable control point quantities with B-Splines
	protected void fillPoints(List<Point2D> bsplineCtrlPts, int t) {
		Point2D[] arrayCtrlPts = new Point2D[bsplineCtrlPts.size()];
		for (int i = 0; i < arrayCtrlPts.length; i++) {
			arrayCtrlPts[i] = bsplineCtrlPts.get(i);
		}
		fillPoints(arrayCtrlPts, t);
	}

	private Point2D subtract(Point2D v1, Point2D v2) {
		return new Point2D.Double(v1.getX() - v2.getX(), v1.getY() - v2.getY());
	}

	private Point2D multiply(Point2D v, double d) {
		return new Point2D.Double(v.getX() * d, v.getY() * d);
	}

	private Point2D add(Point2D v1, Point2D v2) {
		return new Point2D.Double(v1.getX() + v2.getX(), v1.getY() + v2.getY());
	}

	protected void fillPoints(Point2D[] bsplineCtrlPts, int t) {
		// Extraction of the Constituent Bezier Curves using Boehm's Algorithm. Only
		// works for cubics with this implementation
		// Derivation is based on the algorithm described in Sederberg's Computer Aided
		// Geometric Design, on extracting Bezier Curves from B-Splines
		// using polar coordinates.
		for (int i = 0; i < noCurves; i++) {
			Point2D[] bezierCtrlPts = new Point2D.Double[B_SPLINE_DEGREE + 1];

			// Second Control Point derivation
			double scaleFactor = (knotVector[i + 2] - knotVector[i + 1]) / (knotVector[i + 4] - knotVector[i + 1]);
			bezierCtrlPts[1] = add(multiply(subtract(bsplineCtrlPts[i + 2], bsplineCtrlPts[i + 1]), scaleFactor),
					bsplineCtrlPts[i + 1]);

			// Third Control Point derivation
			scaleFactor = (knotVector[i + 3] - knotVector[i + 1]) / (knotVector[i + 4] - knotVector[i + 1]);
			bezierCtrlPts[2] = add(multiply(subtract(bsplineCtrlPts[i + 2], bsplineCtrlPts[i + 1]), scaleFactor),
					bsplineCtrlPts[i + 1]);

			// First Control Point derivation
			scaleFactor = (knotVector[i + 2] - knotVector[i]) / (knotVector[i + 3] - knotVector[i]);
			Point2D tempPoint = add(multiply(subtract(bsplineCtrlPts[i + 1], bsplineCtrlPts[i]), scaleFactor),
					bsplineCtrlPts[i]);
			scaleFactor = (knotVector[i + 2] - knotVector[i + 1]) / (knotVector[i + 3] - knotVector[i + 1]);
			bezierCtrlPts[0] = add(multiply(subtract(bezierCtrlPts[1], tempPoint), scaleFactor), tempPoint);

			// Fourth Control Point derivation
			scaleFactor = (knotVector[i + 3] - knotVector[i + 2]) / (knotVector[i + 5] - knotVector[i + 2]);
			tempPoint = add(multiply(subtract(bsplineCtrlPts[i + 3], bsplineCtrlPts[i + 2]), scaleFactor),
					bsplineCtrlPts[i + 2]);
			scaleFactor = (knotVector[i + 3] - knotVector[i + 2]) / (knotVector[i + 4] - knotVector[i + 2]);
			bezierCtrlPts[3] = add(multiply(subtract(tempPoint, bezierCtrlPts[2]), scaleFactor), bezierCtrlPts[2]);

			List<Point2D> bCtrlPtsArray = new ArrayList<Point2D>(bezierCtrlPts.length);
			for (Point2D p : bezierCtrlPts) {
				bCtrlPtsArray.add(p);
			}
			spline[i] = new BezierCurve(bCtrlPtsArray, t, B_SPLINE_DEGREE + 1, name, dataRadius, frame);
			spline[i].setSelected(this.isSelected());
		}
		this.bounds = generateOffsetBounds(bounds, THRESHOLD_RADIUS);
		this.dataFittingBounds = generateOffsetBounds(dataFittingBounds, dataRadius);
	}

	protected double squaredDistanceErrorTerm(List<Point2D> dataPoints, int datapointIndex, int footpointIndex) {
		BezierPoint p = this.getSpecificPoint(footpointIndex);
		Point2D x = dataPoints.get(datapointIndex);

		// (p-x), the subtraction between the curve point and the data point
		Point2D diff = new Point2D.Double((p.getX() - x.getX()), (p.getY() - x.getY()));

		// Evaluates the Squared Distance Error Term, described in Wang et al 2006:
		// Fitting B-Spline Curves to Point Clouds by
		// Curvature-Based Squared Distance Minimization.
		// The sign depends on the concavity WRT to the datapoint being compared against
		double d = this.getDistanceSign(footpointIndex, x) * Math.sqrt(squared(diff.getX()) + squared(diff.getY()));
		double k = this.getSpecificPoint(footpointIndex).k;

		// Obtains Point2Ds representing tangent and normal unit vectors, where the x
		// and y values are the x and y components of
		// the vectors.
		Point2D T = this.getUnitTangent(footpointIndex);
		Point2D N = this.getUnitNormal(footpointIndex);

		// If d < 0, the squared distance error term is d/(d-k) * [((P(t_k) - Xk)^T
		// Tk)^2 + ((P(t_k) - Xk)^T Nk)^2]
		// We represent the vectors as Point2Ds, so the operations below are equivalent
		// to multiplying [(P(t_k) - Xk)^T] by Tk, etc.
		if (d < 0) {
			return (d / (d - k)) * (squared(diff.getX() * T.getX() + diff.getY() * T.getY())
					+ squared(diff.getX() * N.getX() + diff.getY() * N.getY()));
		} // If 0 <= d < k, then the squared distance error term is [((P(t_k) - Xk)^T
			// Nk)^2]. We know that 0<=d<k if d>= 0, since the minimum distance can
			// not exceed the radius of curvature... otherwise the radius of curvature would
			// be a counterexample for a more minimal distance!
		else {
			return squared(diff.getX() * N.getX() + diff.getY() * N.getY());
		}
	}

	public Point2D.Double getUnitTangent(int footpointIndex) {
		int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * footpointIndex) / this.getNoPoints());
		return spline[n / BezierCurve.NO_CURVE_POINTS].getUnitTangent(n % BezierCurve.NO_CURVE_POINTS);
	}

	public Point2D.Double getUnitNormal(int footpointIndex) {
		int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * footpointIndex) / this.getNoPoints());
		return spline[n / BezierCurve.NO_CURVE_POINTS].getUnitNormal(n % BezierCurve.NO_CURVE_POINTS);
	}

	public int getSign(int footpointIndex) {
		int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * footpointIndex) / this.getNoPoints());
		return spline[n / BezierCurve.NO_CURVE_POINTS].getSign(n % BezierCurve.NO_CURVE_POINTS);
	}

	int getDistanceSign(int footpointIndex, Point2D x) {
		int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * footpointIndex) / this.getNoPoints());
		return spline[n / BezierCurve.NO_CURVE_POINTS].getDistanceSign(n % BezierCurve.NO_CURVE_POINTS, x);
	}

	private double squared(double x) {
		return x * x;
	}

	private int[] getFootpoints(List<Point2D> dataPoints) {
		if (dataPoints.size() == 0) {
			return new int[0];
		}
		int[] footpointIndices = new int[dataPoints.size()];
		for (int n = 0; n < dataPoints.size(); n++) {
			int minIndex = 0;
			double minDistance = Double.MAX_VALUE;
			for (int i = 0; i < this.getNoPoints(); i++) {
				double dist = Math.sqrt(squared(this.getSpecificPoint(i).getX() - dataPoints.get(n).getX())
						+ squared(this.getSpecificPoint(i).getY() - dataPoints.get(n).getY()));
				if (dist < minDistance) {
					minDistance = dist;
					minIndex = i;
				}
			}
			footpointIndices[n] = minIndex;
		}

		// Checks to see if there are any Bezier Curves that are not referenced. Will
		// result in a matrix that is not of full rank.
		boolean[] referenced = new boolean[noCurves];
		for (int footpointIndex : footpointIndices) {
			int curve = footpointIndex / BezierCurve.NO_CURVE_POINTS;
			if (curve == noCurves) {
				curve--;
			}
			referenced[curve] = true;
		}

		// If it isn't referenced, we arbitrarily assign a 'middle' curve point in the
		// Bezier Curve to its nearest data point.
		for (int i = 0; i < referenced.length; i++) {
			if (!referenced[i]) {
				int assignIndex = i * BezierCurve.NO_CURVE_POINTS + BezierCurve.NO_CURVE_POINTS / 2;
				int dataIndex = 0;
				double minDistance = Double.MAX_VALUE;
				for (int n = 0; n < dataPoints.size(); n++) {
					double dist = Math
							.sqrt(squared(this.getSpecificPoint(assignIndex).getX() - dataPoints.get(n).getX())
									+ squared(this.getSpecificPoint(assignIndex).getY() - dataPoints.get(n).getY()));
					if (dist < minDistance) {
						minDistance = dist;
						dataIndex = n;
					}
				}
				footpointIndices[dataIndex] = assignIndex;
			}
		}
		return footpointIndices;
	}

	/**
	 * Performs one iteration of the fitting algorithm on this curve (either the
	 * Point Distance Minimization Algorithm or the Squared Distance Minimization
	 * Algorithm, depending on the selected algorithm in the UI). Require that the
	 * number of data points is equal to the number of weights
	 *
	 * @param dataPoints
	 *            The collection of data points to be fit to.
	 * @param weights
	 *            The weights of each of the data points
	 * @param t
	 *            The frame in the imageStack that this fitting will be performed
	 *            on.
	 * @return The error of the fitting.
	 */
	public double fittingIteration(List<Point2D> dataPoints, List<Double> weights, int t) {
		return fittingIteration(dataPoints, getFootpoints(dataPoints), weights, t);
	}

	public double fittingIteration(List<Point2D> dataPoints, int[] footpointIndices, List<Double> weights, int t) {
		// If we have no datapoints, then there's no point of fitting the curve, hence
		// we just return.
		if (dataPoints.size() == 0) {
			return 0.0;
		}

		// If, after fitting, error across the entire spline is the smallest we've seen
		// yet, we record it.
		double oldError = evaluateGlobalError(dataPoints, weights);
		if (oldError / this.getNoPoints() < minimumGlobalError) {
			minimumGlobalError = oldError / this.getNoPoints();
		}

		// We also record the smallest observed maximum-local-error.
		// That is, the curve where the piece with maximum error is the smallest.
		double oldLocalError = evaluateMaxLocalError(dataPoints, weights);
		if (oldLocalError / BezierCurve.NO_CURVE_POINTS < minimumLocalError) {
			minimumLocalError = oldLocalError / BezierCurve.NO_CURVE_POINTS;
		}

		// Copies the old control points in case the iteration provides poor results and
		// we want to revert.
		List<Point2D> oldCtrlPts = new ArrayList<Point2D>(ctrlPts.size());
		for (Point2D p : ctrlPts) {
			oldCtrlPts.add(p);
		}

		if (KappaFrame.DEBUG_MODE) {
			oldFootpoints = new int[dataPoints.size()];
			dataPointsCopy = new Point2D[dataPoints.size()];
			for (int i = 0; i < oldFootpoints.length; i++) {
				oldFootpoints[i] = footpointIndices[i];
				dataPointsCopy[i] = dataPoints.get(i);
			}
		}

		// Generates matrices for the x and y coordinates of the data points to be
		// minimized against
		double[][] xvals = new double[dataPoints.size()][1];
		double[][] yvals = new double[dataPoints.size()][1];

		// Differing values to minimize against depending on whether we desire Point
		// Distance Minimization or Squared Distance Minimization
		double weighting;
		if (frame.getFittingAlgorithm().equals(KappaFrame.FITTING_ALGORITHMS[0])) {
			for (int i = 0; i < dataPoints.size(); i++) {
				if (weights != null) {
					weighting = Math.sqrt(weights.get(i));
				} else {
					weighting = 1;
				}
				xvals[i][0] = weighting * (dataPoints.get(i).getX());
				yvals[i][0] = weighting * (dataPoints.get(i).getY());
			}
		} // Squared Distance Minimization Values. This corresponds to directly minimizing
			// the Squared Distance Error term.
			// The canonical minimization algorithm minimizes || P(x) - X ||^2, so if we
			// choose our X to be P(x) - (sdterm/2)^(1/2),
			// the minimization will minimize the sdterm.
		else if (frame.getFittingAlgorithm().equals(KappaFrame.FITTING_ALGORITHMS[1])) {
			for (int i = 0; i < dataPoints.size(); i++) {
				if (weights != null) {
					weighting = Math.sqrt(weights.get(i));
				} else {
					weighting = 1;
				}
				double sdterm = squaredDistanceErrorTerm(dataPoints, i, footpointIndices[i]);
				if (this.getSpecificPoint(footpointIndices[i]).getX() < dataPoints.get(i).getX()) {
					xvals[i][0] = weighting
							* (this.getSpecificPoint(footpointIndices[i]).getX() + Math.sqrt(sdterm / 2));
				} else {
					xvals[i][0] = weighting
							* (this.getSpecificPoint(footpointIndices[i]).getX() - Math.sqrt(sdterm / 2));
				}
				if (this.getSpecificPoint(footpointIndices[i]).getY() < dataPoints.get(i).getY()) {
					yvals[i][0] = weighting
							* (this.getSpecificPoint(footpointIndices[i]).getY() + Math.sqrt(sdterm / 2));
				} else {
					yvals[i][0] = weighting
							* (this.getSpecificPoint(footpointIndices[i]).getY() - Math.sqrt(sdterm / 2));
				}
			}
		}
		Matrix X = new Matrix(xvals);
		Matrix Y = new Matrix(yvals);

		// Obtaining updated control points using least squares minimization. This is
		// done by solving a linear system with
		// Gaussian Elimination with No Pivoting (GENP). We use a fitting matrix, A. The
		// matrix coefficients are the
		// coefficients of the B-Spline basis functions
		int m = dataPoints.size();
		int n = noCtrlPts;
		if (!isOpen) {
			n = noCtrlPts - B_SPLINE_DEGREE;
		}
		double[][] vals = new double[m][n];
		for (int r = 0; r < m; r++) {
			double[] coefficients = evaluateBasisFunction(footpointIndices[r]);
			for (int c = 0; c < n; c++) {
				vals[r][c] = Math.sqrt(weights.get(r)) * coefficients[c];
			}
		}

		// Matrix operations to obtain the normal equations for data fitting: A^T Ac =
		// A^T y, where c is the desired result.
		// The weights are factored in by premultiplying the matrices with a weight
		// matrix.
		// See the amended normal equations for weighted least squares:
		// http://en.wikipedia.org/wiki/Least_squares#Weighted_least_squares
		Matrix A = new Matrix(vals);
		Matrix ATA = (A.transpose()).times(A);

		// Debugging Code
		if (KappaFrame.DEBUG_MODE) {
			for (int x = 0; x < vals.length; x++) {
				for (int y = 0; y < vals[0].length; y++) {
					System.out.printf("%5.7f, ", vals[x][y]);
				}
				System.out.println();
			}
			System.out.println();
			for (int x = 0; x < n; x++) {
				for (int y = 0; y < n; y++) {
					System.out.printf("%5.7f, ", ATA.get(x, y));
				}
				System.out.println();
			}
		}

		// Solves the nonlinear least squares problem with a smoothness parameter,
		// lambda.
		// This is (A'^TA' + lambdaI)c = A'^Ty, where c is the desired result, and A' =
		// W*A, or A times a weight matrix.
		// See http://en.wikipedia.org/wiki/Non-linear_least_squares and
		// http://en.wikipedia.org/wiki/Least_squares#Least_squares.2C_regression_analysis_and_statistics
		// for the relevant formulas.
		ATA = ATA.plus((Matrix.identity(n, n)).times(SMOOTHNESS_FACTOR));
		Matrix X1 = ATA.solve((A.transpose()).times(X));
		Matrix Y1 = ATA.solve((A.transpose()).times(Y));
		List<Point2D> newCtrlPts = new ArrayList<Point2D>(noCtrlPts);

		for (int i = 0; i < n; i++) {
			newCtrlPts.add(new Point2D.Double(X1.get(i, 0), Y1.get(i, 0)));
		}

		this.ctrlPts = newCtrlPts;
		fillPoints(ctrlPts, t);

		// Debugging Code
		if (KappaFrame.DEBUG_MODE) {
			for (Point2D p : newCtrlPts) {
				System.out.println(p);
			}
		}

		// Computes the new global and local errors after the curve has been fit
		double newError = evaluateGlobalError(dataPoints, weights);
		if (newError / this.getNoPoints() < minimumGlobalError) {
			minimumGlobalError = newError / this.getNoPoints();
		}
		double newLocalError = evaluateMaxLocalError(dataPoints, weights);
		if (newLocalError / BezierCurve.NO_CURVE_POINTS < minimumLocalError) {
			minimumLocalError = newLocalError / BezierCurve.NO_CURVE_POINTS;
		}

		// If the global error increased from the previous iteration, we restore the
		// previous curve
		if (newError >= oldError) {
			this.ctrlPts = oldCtrlPts;
			fillPoints(ctrlPts, t);
			getKeyframes().add(new BControlPoints(this.ctrlPts, t));
			return oldError / this.getNoPoints();
		}
		getKeyframes().add(new BControlPoints(this.ctrlPts, t));
		return newError / this.getNoPoints();
	}

	// Adjust the control point number to improve the fit
	public void adjustControlPoints(List<Point2D> dataPoints, List<Double> weights, int t) {

		// If we have no datapoints, then there's no point of adjusting the curve, hence
		// we just return.
		if (dataPoints.size() == 0) {
			return;
		}

		double globalError, localError;
		int maxRedundancyIndex;
		boolean wasReduced;
		List<Point2D> oldCtrlPts;

		// Preliminary control point removal and adjustment.
		do {
			// The regions most at risk for redundancy are where the control points are
			// densely organized.
			// Scaling by local curvature was tested too, but just using the density seemed
			// to produce the best results.
			double[] redundancyEstimates = new double[this.getNoCtrlPts()];
			for (int i = 0; i < redundancyEstimates.length; i++) {
				redundancyEstimates[i] = getLocalCtrlPtDensity(i);
			}

			// Find the control point with maximal estimated redundancy.
			double maxRedundancy = 0;
			maxRedundancyIndex = 0;
			for (int i = 0; i < redundancyEstimates.length; i++) {
				if (redundancyEstimates[i] > maxRedundancy) {
					maxRedundancy = redundancyEstimates[i];
					maxRedundancyIndex = i;
				}
			}

			// Copies the old control points
			oldCtrlPts = new ArrayList<Point2D>(ctrlPts.size());
			for (Point2D p : ctrlPts) {
				oldCtrlPts.add(p);
			}

			// Replaces this control point, and the one after it, with an average of the
			// two.
			wasReduced = reduceCurve(maxRedundancyIndex, t);
			globalError = fittingIteration(dataPoints, weights, t);
			localError = evaluateMaxLocalError(dataPoints, weights) / BezierCurve.NO_CURVE_POINTS;

			// Repeat until the error has increased by more than a certain scalar multiple
			// of the minimum error
			// observed so far, or if it cannot be reduced further.
		} while (globalError < minimumGlobalError * (1 + frame.getGlobalThreshold())
				&& localError < minimumLocalError * (1 + frame.getLocalThreshold()) && wasReduced);

		// Reverts the control points to the previous optimal result.
		if (wasReduced) {
			augmentCurve(t, oldCtrlPts);
		}

		// Now we do secondary control point removal, in a naive fashion. We try
		// removing each internal control point
		// and see which one produces the best fit. For this one, we see if it still
		// satisfies our thresholds.
		// We only consider local errors at this point.
		boolean changed;
		do {
			changed = false;

			// We attempt to remove every internal control point and see which one has
			// smallest max local error
			double minimumError = Double.MAX_VALUE;
			int minimumErrorIndex = -1;
			double reducedError;
			for (int i = 1; i < this.getNoCtrlPts() - 1; i++) {
				// Copies the old control points
				oldCtrlPts = new ArrayList<Point2D>(ctrlPts.size());
				for (Point2D p : ctrlPts) {
					oldCtrlPts.add(p);
				}

				// If the curve can't be reduced at any point, it means that the # of control
				// points is insufficient.
				if (!reduceCurve(i, t)) {
					break;
				}

				// We compute the error of the reduced curve and see if it's increased
				fittingIteration(dataPoints, weights, t);
				reducedError = evaluateMaxLocalError(dataPoints, weights) / BezierCurve.NO_CURVE_POINTS;

				if (reducedError < minimumError) {
					minimumError = reducedError;
					minimumErrorIndex = i;
				}
				// Restore the curve so we can try the next control point.
				augmentCurve(t, oldCtrlPts);
			}

			// If the minimum error from any of the removed ctrl points still satisfies our
			// thresholds, we remove it
			if (minimumErrorIndex != -1 && minimumError < minimumLocalError * (1 + frame.getLocalThreshold())) {
				reduceCurve(minimumErrorIndex, t);
				globalError = fittingIteration(dataPoints, weights, t);
				localError = evaluateMaxLocalError(dataPoints, weights) / BezierCurve.NO_CURVE_POINTS;
				changed = true;
			}
		} while (changed);

		// Set the new minimum global and local errors to that of the new reduced curve
		minimumGlobalError = globalError;
		minimumLocalError = localError;
	}

	private double getLocalCtrlPtDensity(int i) {
		// If we are looking at terminal control points, we don't want them to be
		// removed, so we bias this
		// computation by returning 'zero' density.
		if (i == 0 || i == this.getNoCtrlPts() - 1) {
			return 0.0;
		}

		Point2D pi = ctrlPts.get(i);
		Point2D pj = ctrlPts.get(i - 1);
		Point2D pk = ctrlPts.get(i + 1);

		// Estimates the local density by looking at the squared distances between the
		// current control point and its two neighbours
		double dij = Math.sqrt(squared(pi.getX() - pj.getX()) + squared(pi.getY() - pj.getY()));
		double dik = Math.sqrt(squared(pi.getX() - pk.getX()) + squared(pi.getY() - pk.getY()));

		// Returns the density as 1 over the distance to the nearest neighbour
		if (dij < dik) {
			return 1.0 / (dij);
		}
		return 1.0 / (dik);
	}

	public boolean reduceCurve(int i, int t) {
		// Removes a control point from the B-Spline piece with index i.
		// We approximate the control point polygon with the reduced size B-Spline.
		// We require at least (degree + 1) control points to have a valid B-Spline
		if (isOpen && noCurves == 1) {
			return false;
		}
		if (!isOpen && noCurves == B_SPLINE_DEGREE + 1) {
			return false;
		}
		this.noCtrlPts--;
		noCurves--;
		spline = new BezierCurve[noCurves];

		// Sets the new control point to the bisecting point of the line formed between
		// the ith and (i+1)th control point.
		// (Unless it's the last control point, in which case, we use the (i-1)th and
		// ith points.
		if (i == this.getNoCtrlPts() - 1) {
			i--;
		}
		Point2D midPoint = new Point2D.Double((ctrlPts.get(i).getX() + (ctrlPts.get(i + 1)).getX()) / 2.0,
				(ctrlPts.get(i).getY() + (ctrlPts.get(i + 1)).getY()) / 2.0);
		ctrlPts.set(i, midPoint);
		ctrlPts.remove(i + 1);

		computeSpline(ctrlPts, t);
		fillPoints(ctrlPts, t);
		getKeyframes().add(new BControlPoints(this.ctrlPts, t));
		return true;
	}

	// Reverses the reduce curve operation, to 'take back' the suboptimal final
	// reduction when
	// we optimize.
	public void augmentCurve(int t, List<Point2D> oldCtrlPts) {
		this.noCtrlPts++;
		noCurves++;
		spline = new BezierCurve[noCurves];
		this.ctrlPts = oldCtrlPts;

		computeSpline(ctrlPts, t);
		fillPoints(ctrlPts, t);
		getKeyframes().add(new BControlPoints(this.ctrlPts, t));
	}

	// TODO This and the local error evaluation do the same computation.
	// Convert the two methods into one that returns an array with both values.
	public double evaluateGlobalError(List<Point2D> dataPoints, List<Double> weights) {
		// Evaluates the total error after fitting, weighted by intensity.
		double error = 0;
		for (int i = 0; i < this.getNoPoints(); i++) {
			double minDistance = Double.MAX_VALUE;
			for (int j = 0; j < dataPoints.size(); j++) {
				double dist = (1 / (weights.get(j) * 1.0))
						* Math.sqrt(squared(this.getSpecificPoint(i).getX() - dataPoints.get(j).getX())
								+ squared(this.getSpecificPoint(i).getY() - dataPoints.get(j).getY()));
				if (dist < minDistance) {
					minDistance = dist;
				}
			}
			error += minDistance;
		}
		return error;
	}

	public double evaluateMaxLocalError(List<Point2D> dataPoints, List<Double> weights) {
		double maxLocalError = 0;
		double pieceError;

		// Goes through each curve, and finds the average piece error
		for (int n = 0; n < noCurves; n++) {
			pieceError = 0;

			// Computes the error for a single piece of the curve
			for (int i = n * BezierCurve.NO_CURVE_POINTS; i < (n + 1) * BezierCurve.NO_CURVE_POINTS; i++) {
				double minDistance = Double.MAX_VALUE;
				for (int j = 0; j < dataPoints.size(); j++) {
					double dist = (1 / (weights.get(j) * 1.0))
							* Math.sqrt(squared(this.getSpecificPoint(i).getX() - dataPoints.get(j).getX())
									+ squared(this.getSpecificPoint(i).getY() - dataPoints.get(j).getY()));
					if (dist < minDistance) {
						minDistance = dist;
					}
				}
				pieceError += minDistance;
			}

			// If this error is the largest we've seen so far, we record it.
			if (pieceError > maxLocalError) {
				maxLocalError = pieceError;
			}
		}

		return maxLocalError;
	}

	private double[] evaluateBasisFunction(int footpointIndex) {
		// We Evaluate the basis function using a recurrence relation
		// Reference:
		// http://www.cs.mtu.edu/~shene/COURSES/cs3621/NOTES/spline/B-spline/bspline-curve-coef.html
		// And: http://vision.ucsd.edu/~kbranson/research/bsplines/bsplines.pdfs
		// First we extract the corresponding t parameter for the footpointIndex
		int knotIndex = footpointIndex / BezierCurve.NO_CURVE_POINTS + B_SPLINE_DEGREE;
		double t = knotVector[knotIndex - 1] + (knotVector[knotIndex] - knotVector[knotIndex - 1])
				* (footpointIndex % BezierCurve.NO_CURVE_POINTS) / (1.0 * (BezierCurve.NO_CURVE_POINTS - 1));

		// Boundary conditions
		double[] coefficients = new double[noCtrlPts];
		if (t == knotVector[0]) {
			coefficients[0] = 1;
			return coefficients;
		} else if (t == knotVector[knotVector.length - 1]) {
			coefficients[noCtrlPts - 1] = 1;
			return coefficients;
		}

		// We now guarantee that t is in between the first and last knot parameter in
		// the B-Spline, excluding end intervals.
		// The base case is that the degree 0 coefficient is 1
		coefficients[knotIndex] = 1;
		for (int degree = 1; degree <= B_SPLINE_DEGREE; degree++) {
			coefficients[knotIndex - degree] = (knotVector[knotIndex] - t)
					/ (knotVector[knotIndex] - knotVector[knotIndex - degree]) * coefficients[knotIndex - degree + 1];
			for (int i = knotIndex - degree + 1; i < knotIndex; i++) {
				coefficients[i] = (t - knotVector[i - 1]) / (knotVector[i + degree - 1] - knotVector[i - 1])
						* coefficients[i]
						+ (knotVector[i + degree] - t) / (knotVector[i + degree] - knotVector[i]) * coefficients[i + 1];
			}
			coefficients[knotIndex] = (t - knotVector[knotIndex - 1])
					/ (knotVector[knotIndex + degree - 1] - knotVector[knotIndex - 1]) * coefficients[knotIndex];
		}
		return coefficients;
	}

	public void addKeyFrame(Point2D newCtrlPt, int t) {
		if (isOpen) {
			super.addKeyFrame(newCtrlPt, t);
		} else // Shouldn't happen upon execution, but better safe than sorry!
		{
			if (selectedCtrlPtIndex < 0) {
				return;
			} else if (selectedCtrlPtIndex >= B_SPLINE_DEGREE && selectedCtrlPtIndex < noCtrlPts - B_SPLINE_DEGREE) {
				super.addKeyFrame(newCtrlPt, t);
			} else {
				// In case it's a periodic B-Spline, where the first n control points are always
				// the same as the last n, we must update them.
				ctrlPts.set(selectedCtrlPtIndex, newCtrlPt);
				if (selectedCtrlPtIndex <= B_SPLINE_DEGREE) {
					ctrlPts.set(ctrlPts.size() - B_SPLINE_DEGREE + selectedCtrlPtIndex, newCtrlPt);
				} else {
					ctrlPts.set(selectedCtrlPtIndex - ctrlPts.size() + B_SPLINE_DEGREE, newCtrlPt);
				}
				getKeyframes().add(new BControlPoints(ctrlPts, t));
				fillPoints(ctrlPts, t);
				boundingBox = getKeyframes().getBounds(t);
			}
		}
	}

	public void printValues(PrintWriter out, double[][] averaged, boolean exportAllDataPoints) {
		// Exports in CSV format for import into Excel
		int i = 0;
		for (BezierCurve c : spline) {

			double curveLength = this.getApproxCurveLength();
			double curvature = this.getAverageCurvature();
			double curvatureStd = this.getCurvatureStdDev();

			if (!exportAllDataPoints) {

				out.print(this.name);

				out.print("," + curveLength);
				out.print("," + curvature);
				out.print("," + curvatureStd);

				c.printValues(out, averaged, exportAllDataPoints);

				out.println();
			} else {
				c.printValuesAll(out, curveLength, curvature, curvatureStd);
			}

			i++;
		}
	}

	void draw(double scale, Graphics2D g, int currentPoint, boolean showBoundingBox, boolean scaleCurveStrokes,
			boolean showTangent, boolean showThresholdedRegion) {
		if (scaleCurveStrokes) {
			g.setStroke(new BasicStroke((int) (DEFAULT_STROKE_THICKNESS * scale)));
		} else {
			g.setStroke(new BasicStroke(0));
		}

		if (selected) {
			// If the Curve is Selected, we connect all the control points
			g.setColor(Color.GRAY);
			for (int i = 0; i < noCtrlPts - 1; i++) {
				g.drawLine((int) (ctrlPts.get(i).getX() * scale), (int) (ctrlPts.get(i).getY() * scale),
						(int) (ctrlPts.get(i + 1).getX() * scale), (int) (ctrlPts.get(i + 1).getY() * scale));
			}
		}

		// Draws each of the curves in the spline
		for (BezierCurve c : spline) {
			c.draw(scale, g, scaleCurveStrokes);
		}

		// Shows bounding box if the option is chosen
		if (showBoundingBox) {
			g.setColor(Color.GRAY);
			Rectangle2D b = getScaledBounds(boundingBox, scale);
			g.drawRect((int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());
		}

		// Draws the offset curve.
		if (showThresholdedRegion) {
			scaledBounds.reset();
			for (Point2D p : dataFittingBounds) {
				scaledBounds.addPoint((int) (p.getX() * scale), (int) (p.getY() * scale));
			}
			g.setStroke(new BasicStroke(0));
			g.setColor(new Color(140, 220, 255));
			g.drawPolygon(scaledBounds);
			if (scaleCurveStrokes) {
				g.setStroke(new BasicStroke((int) (DEFAULT_STROKE_THICKNESS * scale)));
			}
		}

		// Draws in the control points if selected as well.
		if (selected) {
			g.setColor(new Color(230, 230, 230));
			for (int i = 0; i < ctrlPts.size(); i++) {
				if (i == selectedCtrlPtIndex) {
					g.setColor(Color.WHITE);
					g.fillRect((int) ((ctrlPts.get(i).getX() - 1.5 * SELECTED_CTRL_PT_SIZE) * scale),
							(int) ((ctrlPts.get(i).getY() - 1.5 * SELECTED_CTRL_PT_SIZE) * scale),
							(int) (3 * SELECTED_CTRL_PT_SIZE * scale), (int) (3 * SELECTED_CTRL_PT_SIZE * scale));
					g.setColor(new Color(230, 230, 230));
				} else if (i == hoveredCtrlPt) {
					g.fillRect((int) ((ctrlPts.get(i).getX() - SELECTED_CTRL_PT_SIZE) * scale),
							(int) ((ctrlPts.get(i).getY() - SELECTED_CTRL_PT_SIZE) * scale),
							(int) (2 * SELECTED_CTRL_PT_SIZE * scale), (int) (2 * SELECTED_CTRL_PT_SIZE * scale));
				} else {
					g.fillRect((int) ((ctrlPts.get(i).getX() - CTRL_PT_SIZE) * scale),
							(int) ((ctrlPts.get(i).getY() - CTRL_PT_SIZE) * scale), (int) (2 * CTRL_PT_SIZE * scale),
							(int) (2 * CTRL_PT_SIZE * scale));
				}
			}
			Point2D p = this.getPoint(currentPoint);
			int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * currentPoint)
					/ frame.getNumberOfPointsPerCurve());
			Point2D dp = spline[n / BezierCurve.NO_CURVE_POINTS].getHodographPoint(n % BezierCurve.NO_CURVE_POINTS);

			if (showTangent) {
				// Draws a tangent line at the point
				g.setColor(new Color(50, 100, 50));
				g.drawLine((int) ((p.getX() - dp.getX() * STRETCH_FACTOR) * scale),
						(int) ((p.getY() - dp.getY() * STRETCH_FACTOR) * scale),
						(int) ((p.getX() + dp.getX() * STRETCH_FACTOR) * scale),
						(int) ((p.getY() + dp.getY() * STRETCH_FACTOR) * scale));

				// Draws the normal line to the point.
				g.setColor(new Color(100, 50, 50));
				g.drawLine((int) ((p.getX() - dp.getY() * STRETCH_FACTOR) * scale),
						(int) ((p.getY() + dp.getX() * STRETCH_FACTOR) * scale),
						(int) ((p.getX() + dp.getY() * STRETCH_FACTOR) * scale),
						(int) ((p.getY() - dp.getX() * STRETCH_FACTOR) * scale));
			}

			// Draws a yellow indicator box at the selected point
			g.setColor(Color.YELLOW);
			g.fillRect((int) ((p.getX() - CTRL_PT_SIZE) * scale), (int) ((p.getY() - CTRL_PT_SIZE) * scale),
					(int) (2 * CTRL_PT_SIZE * scale), (int) (2 * CTRL_PT_SIZE * scale));

			// Shows a line between the data point and the footpoints.
			List<Point2D> thresholdedPixels = this.getThresholdedPixels();
			int[] footpointIndices = this.getFootpoints(thresholdedPixels);
			if (KappaFrame.DEBUG_MODE) {
				g.setColor(Color.PINK);
				g.setStroke(new BasicStroke(0));
				if (oldFootpoints == null) {
					for (int i = 0; i < thresholdedPixels.size(); i++) {
						Point2D footPoint = this.getSpecificPoint(footpointIndices[i]);
						Point2D dataPoint = thresholdedPixels.get(i);
						g.drawLine((int) (footPoint.getX() * scale), (int) (footPoint.getY() * scale),
								(int) (dataPoint.getX() * scale), (int) (dataPoint.getY() * scale));
					}
				} else {
					for (int i = 0; i < oldFootpoints.length; i++) {
						Point2D footPoint = this.getSpecificPoint(oldFootpoints[i]);
						Point2D dataPoint = dataPointsCopy[i];
						g.drawLine((int) (footPoint.getX() * scale), (int) (footPoint.getY() * scale),
								(int) (dataPoint.getX() * scale), (int) (dataPoint.getY() * scale));
					}
				}
			}
		}
	}

	@Override
	public double getPointCurvature(int percentage) {
		int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * percentage) / frame.getNumberOfPointsPerCurve());
		int curve = n / BezierCurve.NO_CURVE_POINTS;
		int point = n % BezierCurve.NO_CURVE_POINTS;
		return spline[curve].getExactPointCurvature(point);
	}

	@Override
	public boolean isPointOnCurve(Point2D p, int t, double scale) {
		for (BezierCurve c : spline) {
			if (c.isPointOnCurve(p, t, scale)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double getAverageCurvature() {
		// Returns the average curvature across the entire Bezier Spline
		// This only works because each of the sets are of the same cardinality: they're
		// the same size (same number of points)
		// In the general case, the average of the averages does not equal the global
		// average. (A weighted average must be obtained)
		double total = 0;
		for (BezierCurve c : spline) {
			total += c.getAverageCurvature();
		}
		return total / noCurves;
	}

	@Override
	public double getApproxCurveLength() {
		double length = 0;
		for (BezierCurve c : spline) {
			length += c.getApproxCurveLength();
		}
		return length;
	}

	@Override
	public double getCurvatureStdDev() {
		// This standard deviation is slightly off because it double counts the
		// curvatures values at the junction points between
		// Bezier Curves in the spline.
		double variance = 0;
		double mu = this.getAverageCurvature();
		for (BezierCurve c : spline) {
			for (Point2D point : c.getCurveData()) {
				variance += (point.getY() - mu) * (point.getY() - mu);
			}
		}
		variance /= noCurves * BezierCurve.NO_CURVE_POINTS - 1;
		return Math.sqrt(variance);
	}

	@Override
	public List<Point2D> getIntensityDataRed() {
		List<Point2D> splineData = new ArrayList<>();
		double currentPt = 0;
		for (BezierCurve c : spline) {
			List<Point2D> curveData = c.getIntensityDataRed();

			// Display y values with respect to x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				splineData.addAll(curveData);
			} // Display y values with respect to arc-length
			else if (KappaMenuBar.distributionDisplay == 1) {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += curveData.get(BezierCurve.NO_CURVE_POINTS - 1).getX();
			} // Display y values with respect to point index
			else {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += BezierCurve.NO_CURVE_POINTS - 1;
			}
		}
		return splineData;
	}

	@Override
	public List<Point2D> getIntensityDataGreen() {
		List<Point2D> splineData = new ArrayList<Point2D>();
		double currentPt = 0;
		for (BezierCurve c : spline) {
			List<Point2D> curveData = c.getIntensityDataGreen();

			// Display y values with respect to x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				splineData.addAll(curveData);
			} // Display y values with respect to arc-length
			else if (KappaMenuBar.distributionDisplay == 1) {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += curveData.get(BezierCurve.NO_CURVE_POINTS - 1).getX();
			} // Display y values with respect to point index
			else {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += BezierCurve.NO_CURVE_POINTS - 1;
			}
		}
		return splineData;
	}

	@Override
	public List<Point2D> getIntensityDataBlue() {
		List<Point2D> splineData = new ArrayList<>();
		double currentPt = 0;
		for (BezierCurve c : spline) {
			List<Point2D> curveData = c.getIntensityDataBlue();

			// Display y values with respect to x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				splineData.addAll(curveData);
			} // Display y values with respect to arc-length
			else if (KappaMenuBar.distributionDisplay == 1) {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += curveData.get(BezierCurve.NO_CURVE_POINTS - 1).getX();
			} // Display y values with respect to point index
			else {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += BezierCurve.NO_CURVE_POINTS - 1;
			}
		}
		return splineData;
	}

	@Override
	public void updateIntensities() {
		for (BezierCurve c : spline) {
			c.updateIntensities();
		}
	}

	@Override
	public List<Point2D> getCurveData() {
		ArrayList<Point2D> splineData = new ArrayList<>();
		double currentPt = 0;
		for (BezierCurve c : spline) {
			List<Point2D> curveData = c.getCurveData();

			// Display y values with respect to x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				splineData.addAll(curveData);
			} // Display y values with respect to arc-length
			else if (KappaMenuBar.distributionDisplay == 1) {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += curveData.get(BezierCurve.NO_CURVE_POINTS - 1).getX();
			} // Display y values with respect to point index
			else {
				for (Point2D p : curveData) {
					splineData.add(new Point2D.Double(p.getX() + currentPt, p.getY()));
				}
				currentPt += BezierCurve.NO_CURVE_POINTS - 1;
			}
		}
		return splineData;
	}

	@Override
	public List<Point2D> getDebugCurveData() {
		List<Point2D> splineData = new ArrayList<>();
		for (BezierCurve c : spline) {
			splineData.addAll(c.getDebugCurveData());
		}
		return splineData;
	}

	@Override
	public List<BezierPoint> getPoints() {
		List<BezierPoint> curvePoints = new ArrayList<>();
		for (BezierCurve c : spline) {
			curvePoints.addAll(c.getPoints());
		}
		return curvePoints;
	}

	@Override
	public List<BezierPoint> getDigitizedPoints() {
		List<BezierPoint> digitizedPoints = new ArrayList<>();
		for (BezierCurve c : spline) {
			digitizedPoints.addAll(c.getDigitizedPoints());
		}
		return digitizedPoints;
	}

	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
		for (BezierCurve c : spline) {
			c.setSelected(selected);
		}
	}

	@Override
	public int getNoPoints() {
		return noCurves * BezierCurve.NO_CURVE_POINTS;
	}

	// Gets a point a certain percentage along the way.
	@Override
	public Point2D.Double getPoint(int percentage) {
		int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * percentage) / frame.getNumberOfPointsPerCurve());
		return spline[n / BezierCurve.NO_CURVE_POINTS].getExactPoint(n % BezierCurve.NO_CURVE_POINTS);
	}

	// Gets a point at a certain index along the B-Spline
	public BezierPoint getSpecificPoint(int index) {
		int n = (int) (((noCurves * BezierCurve.NO_CURVE_POINTS - 1) * index) / this.getNoPoints());
		return spline[n / BezierCurve.NO_CURVE_POINTS].getExactPoint(n % BezierCurve.NO_CURVE_POINTS);
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void evaluateThresholdedPixels() {
		for (BezierCurve c : spline) {
			c.setDataRadius(dataRadius);
			c.evaluateThresholdedPixels();
		}
	}

	@Override
	public void drawThresholdedPixels(Graphics2D g, double scale) {
		for (BezierCurve c : spline) {
			c.drawThresholdedPixels(g, scale);
		}
	}

	@Override
	public List<Point2D> getThresholdedPixels() {
		// Gets the unique thresholded pixels for data fitting
		thresholdedPixels = new ArrayList<>();
		HashSet<Point2D> uniquePixels = new HashSet<>();
		for (BezierCurve c : spline) {
			for (Point2D p : c.getThresholdedPixels()) {
				uniquePixels.add(p);
			}
		}
		thresholdedPixels.addAll(uniquePixels);
		return thresholdedPixels;
	}

	@Override
	public double getMaximum(double start, double end) {
		double max = Double.MIN_VALUE;
		double pieceMax;
		for (BezierCurve c : spline) {
			pieceMax = c.getMaximum(start, end);
			if (pieceMax > max) {
				max = pieceMax;
			}
		}
		return max;
	}

}
