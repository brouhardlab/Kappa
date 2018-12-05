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
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import sc.fiji.kappa.gui.ImageUtils;
import sc.fiji.kappa.gui.KappaFrame;
import sc.fiji.kappa.gui.KappaMenuBar;

public class BezierCurve extends Curve {

	private ArrayList<BezierPoint> curvePoints;
	private ArrayList<BezierPoint> hodographPoints;
	private ArrayList<int[]> RGBvals;
	private Point2D[] hodographCtrlPts;

	// The range of values sampled around each Bezier Point to determine an average
	// intensity per Bezier Region
	public static final int RECURSE_DEPTH = 7;
	public static final Color DEFAULT_CURVE_COLOR = Color.RED;

	// The number of points is 2^n + 1, where n is the recurse depth.
	// Can be seen by solving the recurrence T(n) = 2T(n-1) - 1, T0 = 2
	public static final int NO_CURVE_POINTS = ((int) Math.pow(2, RECURSE_DEPTH) + 1);
	public static final int NO_CURVE_POINTS_DIGITS = 4;

	// The step size constants used when going along the curve to generate vertices
	// for the bounds.
	public static final double STEP_SIZE_ANGLE = 0.2;
	public static final int STEP_SIZE_CURVE = Math.max(RECURSE_DEPTH - 3, 1);

	public BezierCurve(List<Point2D> ctrlPts, int t, int noCtrlPts, String name, int dataRadius, KappaFrame frame) {
		super(ctrlPts, t, noCtrlPts, name, dataRadius, frame);
		fillPoints(ctrlPts, t);
		evaluateThresholdedPixels();
	}

	@Override
	public void addKeyFrame(Point2D newCtrlPt, int t) {
		super.addKeyFrame(newCtrlPt, t);
		evaluateThresholdedPixels();
	}

	@Override
	public void translateCurve(int t) {
		super.translateCurve(t);
		evaluateThresholdedPixels();
	}

	@Override
	public void printValues(PrintWriter out, double[][] averaged, boolean exportAllDataPoints) {

		// We export all the unique pixel data points if this is the case.
		if (exportAllDataPoints) {
			// We bin all points on the curve that evaluate to the same pixel coordinate
			// into one averaged point

			for (int i = 0; i < curvePoints.size(); i++) {
				BezierPoint p = curvePoints.get(i);
				int n = 1;
				double totalX = p.getX();
				double totalY = p.getY();
				double totalK = p.k;
				double totalRed = RGBvals.get(i)[0];
				double totalGreen = RGBvals.get(i)[1];
				double totalBlue = RGBvals.get(i)[2];

				// Keeps adding to the total while successive points in our curve round to the
				// same pixel coordinate.
				// while (i + 1 < curvePoints.size() && (int) curvePoints.get(i + 1).getX() ==
				// (int) p.getX()
				// && (int) curvePoints.get(i + 1).getY() == (int) p.getY()) {
				// n++;
				// p = curvePoints.get(++i);
				// totalX += p.getX();
				// totalY += p.getY();
				// totalK += p.sign * p.k;
				// totalRed += RGBvals.get(i)[0];
				// totalGreen += RGBvals.get(i)[1];
				// totalBlue += RGBvals.get(i)[2];
				// }

				// Now we print out the averaged point.
				out.print("," + (totalX / n) * micronPixelFactor);
				out.print("," + (totalY / n) * micronPixelFactor);
				out.print("," + totalK / n);

				out.print("," + totalRed / n);
				out.print("," + totalGreen / n);
				out.print("," + totalBlue / n);

			}
		} // Otherwise, we only export averages
		else {
			double totalX = 0, totalY = 0, totalK = 0, totalAveraged = 0, totalRed = 0, totalGreen = 0, totalBlue = 0;

			for (int i = 0; i < curvePoints.size(); i++) {
				BezierPoint p = curvePoints.get(i);
				totalX += p.getX() + 1;
				totalY += p.getY() + 1;
				totalK += p.sign * p.k;
				totalRed += RGBvals.get(i)[0];
				totalGreen += RGBvals.get(i)[1];
				totalBlue += RGBvals.get(i)[2];
			}

			out.print("," + (totalX / curvePoints.size()) * micronPixelFactor);
			out.print("," + (totalY / curvePoints.size()) * micronPixelFactor);
			out.print("," + totalK / curvePoints.size());

			out.print("," + totalRed / curvePoints.size());
			out.print("," + totalGreen / curvePoints.size());
			out.print("," + totalBlue / curvePoints.size());
		}
	}

	public void printValuesAll(PrintWriter out, double curveLength, double curvature, double curvatureStd) {
		for (int i = 0; i < curvePoints.size(); i++) {
			BezierPoint p = curvePoints.get(i);
			int n = 1;
			double totalX = p.getX();
			double totalY = p.getY();
			double totalK = p.k;
			double totalRed = RGBvals.get(i)[0];
			double totalGreen = RGBvals.get(i)[1];
			double totalBlue = RGBvals.get(i)[2];

			// // Keeps adding to the total while successive points in our curve round to
			// the
			// // same pixel coordinate.
			// while (i + 1 < curvePoints.size() && (int) curvePoints.get(i + 1).getX() ==
			// (int) p.getX()
			// && (int) curvePoints.get(i + 1).getY() == (int) p.getY()) {
			// n++;
			// p = curvePoints.get(++i);
			// totalX += p.getX();
			// totalY += p.getY();
			// totalK += p.sign * p.k;
			// totalRed += RGBvals.get(i)[0];
			// totalGreen += RGBvals.get(i)[1];
			// totalBlue += RGBvals.get(i)[2];
			// }

			out.print(this.name);

			out.print("," + curveLength);
			out.print("," + curvature);
			out.print("," + curvatureStd);

			// Now we print out the averaged point.
			out.print("," + (totalX / n) * micronPixelFactor);
			out.print("," + (totalY / n) * micronPixelFactor);
			out.print("," + totalK / n);

			out.print("," + totalRed / n);
			out.print("," + totalGreen / n);
			out.print("," + totalBlue / n);

			out.println();
		}
	}

	private Point2D[] reverse(Point2D[] orig) {
		Point2D[] rev = new Point2D[orig.length];
		for (int i = 0; i < orig.length; i++) {
			rev[i] = orig[orig.length - i - 1];
		}
		return rev;
	}

	@Override
	public void evaluateThresholdedPixels() {
		int pixelThreshold = frame.getInfoPanel().getDataThresholdSlider().getValue();
		boolean isBrighter = frame.getInfoPanel().getDataRangeComboBox().getSelectedIndex() == 0;

		// Finds all pixels nearby the curve that are higher than a threshold intensity.
		thresholdedPixels = new ArrayList<>();
		scaledDataBounds.reset();
		for (Point2D p : dataFittingBounds) {
			scaledDataBounds.addPoint((int) (p.getX()), (int) (p.getY()));
		}

		ImageUtils imgUtils = new ImageUtils<>();

		for (int x = (int) boundingBox.getX() - dataRadius; x <= (int) boundingBox.getX() + (int) boundingBox.getWidth()
				+ dataRadius; x++) {
			for (int y = (int) boundingBox.getY() - dataRadius; y <= (int) boundingBox.getY()
					+ (int) boundingBox.getHeight() + dataRadius; y++) {
				if (x >= 0 && x < frame.getCurrImage().getWidth() && y >= 0 && y < frame.getCurrImage().getHeight()) {
					int[] rgb = imgUtils.getPixels(frame.getImageStack(), x, y);

					// Checks the correct channel depending on the UI preference
					int channel = frame.getInfoPanel().getFittingChannelsComboBox().getSelectedIndex();
					long intensity;
					if (channel == 0) {
						intensity = rgb[0];
					} else if (channel == 1) {
						intensity = rgb[1];
					} else if (channel == 2) {
						intensity = rgb[2];
					} else {
						intensity = (rgb[0] + rgb[1] + rgb[2]) / 3;
					}

					if ((isBrighter && intensity >= pixelThreshold) || (!isBrighter && intensity <= pixelThreshold)) {
						if (scaledDataBounds.contains(new Point2D.Double(x, y))) {
							thresholdedPixels.add(new Point2D.Double(x, y));
						}
					}
				}
			}
		}
	}

	@Override
	public void drawThresholdedPixels(Graphics2D g, double scale) {
		g.setColor(Color.MAGENTA);
		for (Point2D p : thresholdedPixels) {
			g.fillRect((int) Math.round(p.getX() * scale), (int) Math.round(p.getY() * scale), (int) Math.round(scale),
					(int) Math.round(scale));
		}
	}

	@Override
	protected void fillPoints(List<Point2D> ctrlPtsList, int t) {
		curvePoints = new ArrayList<>(NO_CURVE_POINTS);
		RGBvals = new ArrayList<>(NO_CURVE_POINTS);
		hodographPoints = new ArrayList<>(NO_CURVE_POINTS);

		Point2D[] ctrlPts = new Point2D[noCtrlPts];
		for (int i = 0; i < ctrlPtsList.size(); i++) {
			ctrlPts[i] = ctrlPtsList.get(i);
		}

		// Generates a set of points along the Bezier Curve using the De Casteljau
		// algorithm for subdivision
		// Simultaneously calculates the curvature at these points.
		// We fill in the first two endpoint values manually.

		ImageUtils imgUtils = new ImageUtils<>();

		curvePoints.add(new BezierPoint(ctrlPts[0].getX(), ctrlPts[0].getY(), getCurvature(ctrlPts)));
		RGBvals.add(imgUtils.getPixels(frame.getImageStack(), (int) ctrlPts[0].getX(), (int) ctrlPts[0].getY()));
		fillBezierPoints(curvePoints, ctrlPts, noCtrlPts, RECURSE_DEPTH, 0, 1, true);
		curvePoints.add(new BezierPoint(ctrlPts[noCtrlPts - 1].getX(), ctrlPts[noCtrlPts - 1].getY(),
				getCurvature(reverse(ctrlPts))));
		RGBvals.add(imgUtils.getPixels(frame.getImageStack(), (int) ctrlPts[noCtrlPts - 1].getX(),
				(int) ctrlPts[noCtrlPts - 1].getY()));

		// Generates the hodograph, or first derivative curve for the Bezier Curve. (see
		// section 2.7 of Sederberg's CAGD text)
		hodographCtrlPts = new Point2D[ctrlPts.length - 1];
		for (int i = 0; i < hodographCtrlPts.length; i++) {
			hodographCtrlPts[i] = new Point2D.Double(ctrlPts.length * (ctrlPts[i + 1].getX() - ctrlPts[i].getX()),
					ctrlPts.length * (ctrlPts[i + 1].getY() - ctrlPts[i].getY()));
		}
		hodographPoints.add(new BezierPoint(hodographCtrlPts[0].getX(), hodographCtrlPts[0].getY(),
				getCurvature(hodographCtrlPts)));
		fillBezierPoints(hodographPoints, hodographCtrlPts, noCtrlPts - 1, RECURSE_DEPTH, 0, 1, false);
		hodographPoints.add(new BezierPoint(hodographCtrlPts[hodographCtrlPts.length - 1].getX(),
				hodographCtrlPts[hodographCtrlPts.length - 1].getY(), getCurvature(reverse(hodographCtrlPts))));
		this.bounds = generateOffsetBounds(bounds, THRESHOLD_RADIUS);
		this.dataFittingBounds = generateOffsetBounds(dataFittingBounds, dataRadius);
	}

	protected void generateRightOffsetCurve(List<Point2D> bounds, int radius) {
		// Generates the bounds for the offset polygon around the curve.
		// We use the formula for the Offset Curve O(R, P(t)) = P(t) + R*(y'(t),
		// -x'(t))/(sqrt(x'(t)^2 + y'(t)^2))
		// Offset Curve along the right.
		Point2D p, dp;
		double normalizationFactor;
		for (int i = 0; i < curvePoints.size(); i += STEP_SIZE_CURVE) {
			p = curvePoints.get(i);
			dp = hodographPoints.get(i);
			normalizationFactor = Math.sqrt(dp.getX() * dp.getX() + dp.getY() * dp.getY());
			bounds.add(new Point2D.Double(p.getX() + radius * (dp.getY() / normalizationFactor),
					p.getY() + radius * (-dp.getX() / normalizationFactor)));
		}
	}

	protected void generateRightCap(List<Point2D> bounds, int radius) {
		// We generate a rounded end for one tip of the polygon with some trigonometry
		Point2D p = curvePoints.get(curvePoints.size() - 1);
		Point2D dp = hodographPoints.get(curvePoints.size() - 1);
		double normalizationFactor = Math.sqrt(dp.getX() * dp.getX() + dp.getY() * dp.getY());
		Point2D offsetPt = new Point2D.Double(p.getX() + radius * (dp.getY() / normalizationFactor),
				p.getY() + radius * (-dp.getX() / normalizationFactor));
		Point2D translatedPt = new Point2D.Double(offsetPt.getX() - p.getX(), offsetPt.getY() - p.getY());

		// Multiplies the translated point by a rotation matrix and then adds the
		// translation factor to move it back to the original point.
		for (double theta = 0; theta <= Math.PI; theta += STEP_SIZE_ANGLE) {
			bounds.add(new Point2D.Double(
					translatedPt.getX() * Math.cos(theta) - translatedPt.getY() * Math.sin(theta) + p.getX(),
					translatedPt.getX() * Math.sin(theta) + translatedPt.getY() * Math.cos(theta) + p.getY()));
		}
	}

	protected void generateLeftOffsetCurve(List<Point2D> bounds, int radius) {
		// Offset Curve along the left
		Point2D p, dp;
		double normalizationFactor;
		for (int i = curvePoints.size() - 1; i >= 0; i -= STEP_SIZE_CURVE) {
			p = curvePoints.get(i);
			dp = hodographPoints.get(i);
			normalizationFactor = Math.sqrt(dp.getX() * dp.getX() + dp.getY() * dp.getY());
			bounds.add(new Point2D.Double(p.getX() - radius * (dp.getY() / normalizationFactor),
					p.getY() - radius * (-dp.getX() / normalizationFactor)));
		}
	}

	protected void generateLeftCap(List<Point2D> bounds, int radius) {
		// We generate a rounded end for one tip of the polygon with some trigonometry
		Point2D p = curvePoints.get(0);
		Point2D dp = hodographPoints.get(0);
		double normalizationFactor = Math.sqrt(dp.getX() * dp.getX() + dp.getY() * dp.getY());
		Point2D offsetPt = new Point2D.Double(p.getX() - radius * (dp.getY() / normalizationFactor),
				p.getY() - radius * (-dp.getX() / normalizationFactor));
		Point2D translatedPt = new Point2D.Double(offsetPt.getX() - p.getX(), offsetPt.getY() - p.getY());

		// Multiplies the translated point by a rotation matrix and then adds the
		// translation factor to move it back to the original point.
		for (double theta = 0; theta <= Math.PI; theta += STEP_SIZE_ANGLE) {
			bounds.add(new Point2D.Double(
					translatedPt.getX() * Math.cos(theta) - translatedPt.getY() * Math.sin(theta) + p.getX(),
					translatedPt.getX() * Math.sin(theta) + translatedPt.getY() * Math.cos(theta) + p.getY()));
		}
	}

	@Override
	protected List<Point2D> generateOffsetBounds(List<Point2D> bounds, int radius) {
		bounds = new ArrayList<>();
		generateRightOffsetCurve(bounds, radius);
		generateRightCap(bounds, radius);
		generateLeftOffsetCurve(bounds, radius);
		generateLeftCap(bounds, radius);
		return bounds;
	}

	/**
	 * Obtains the curvature at the starting point of the B�zier Curve defined by
	 * the given list of control points
	 *
	 * @param p
	 *            A list of the control points that define the B�zier Curve
	 * @return The curvature at the starting point of this B�zier Curve.
	 */
	private double getCurvature(Point2D[] p) {
		// If the Bezier Curve is less than degree 2 (3 control points), than you have
		// either a point or a line. Hence we just return 0.
		if (p.length < 3) {
			return 0;
		}

		// The curvature of a Bezier Curve at an endpoint is [(n-1)/n][h/a^2], where a
		// is the distance between P0 and P1,
		// and h is the perpendicular distance between the line crossing through P0 and
		// P1, and P2.
		// This is described in pg. 31 of the Jan. 2011 version of Computer Aided
		// Graphic Design, by Thomas Sederberg.
		double a = p[0].distance(p[1]);

		// This uses the formula for the signed distance between a line passing through
		// points (x0, y0) and (x1, y1), and point (x2, y2):
		// d = ((x1-x0)(y0-y2) - (y1-y0)(x0-x2))/sqrt((x1-x0)^2 + (y1-y0)^2)
		double dx = p[1].getX() - p[0].getX();
		double dy = p[1].getY() - p[0].getY();
		double h = (dx * (p[0].getY() - p[2].getY()) - dy * (p[0].getX() - p[2].getX())) / Math.sqrt(dx * dx + dy * dy);

		int n = noCtrlPts - 1;
		return ((n - 1) / (n * 1.0)) * (h / (a * a)) * (1 / micronPixelFactor);
	}

	private void fillBezierPoints(ArrayList<BezierPoint> curvePoints, Point2D[] controlPoints, int noCtrlPts, int depth,
			double t0, double t2, boolean addRGB) {
		if (depth == 0) {
			return;
		}

		// Splits the Bezier Curve into 2 curves over the interval [t0, t1] and [t1, t2]
		double t1 = (t0 + t2) / 2;
		double tau = (t1 - t0) / (t2 - t0);

		// Calculates the intermediate points needed to generate these two Bezier Curves
		// This is a dynamic programming approach with our base cases being our original
		// control points
		Point2D[][] points = new Point2D[noCtrlPts][noCtrlPts];
		for (int i = 0; i < noCtrlPts; i++) {
			points[i][0] = controlPoints[i];
		}

		// DP for new control points
		for (int j = 1; j < noCtrlPts; j++) {
			for (int i = 0; i < noCtrlPts - j; i++) {
				points[i][j] = new Point2D.Double(
						(points[i][j - 1]).getX() * (1 - tau) + tau * (points[i + 1][j - 1]).getX(),
						(points[i][j - 1]).getY() * (1 - tau) + tau * (points[i + 1][j - 1]).getY());
			}
		}

		// Looks through our matrix to find correct points for our new Bezier Curves to
		// repeat this process
		Point2D[] firstHalfPoints = new Point2D[noCtrlPts];
		Point2D[] secondHalfPoints = new Point2D[noCtrlPts];
		for (int j = 0; j < noCtrlPts; j++) {
			firstHalfPoints[j] = points[0][j];
			secondHalfPoints[j] = points[j][noCtrlPts - j - 1];
		}

		// We use something akin to inorder traversal here so that the points we
		// generate will be added to our
		// list of curve points in order. points [0][noCtrlPts-1] is now the point at
		// t=t1. We find the curvature at this point
		fillBezierPoints(curvePoints, firstHalfPoints, noCtrlPts, depth - 1, t0, t1, addRGB);
		curvePoints.add(new BezierPoint(points[0][noCtrlPts - 1].getX(), points[0][noCtrlPts - 1].getY(),
				getCurvature(secondHalfPoints)));
		if (addRGB) {
			ImageUtils imgUtils = new ImageUtils<>();
			RGBvals.add(imgUtils.getPixels(frame.getImageStack(), (int) points[0][noCtrlPts - 1].getX(),
					(int) points[0][noCtrlPts - 1].getY()));
		}
		fillBezierPoints(curvePoints, secondHalfPoints, noCtrlPts, depth - 1, t1, t2, addRGB);
	}

	/**
	 * Returns a String representing this Bezier Curve. This prints out all the
	 * points defined along the curve that we have calculated, and their curvature
	 *
	 * @return The string representing the Bezier Curve
	 */
	@Override
	public String toString() {
		StringBuffer curveString = new StringBuffer();
		curveString.append(name + "\n");
		for (int i = 0; i < curvePoints.size(); i++) {
			curveString.append(curvePoints.get(i).toString() + "\n");
		}
		return curveString.toString();
	}

	/**
	 * Draws the (Scaled) Bezier Curve in the Graphics context.
	 *
	 * @param g
	 *            Graphics context to draw the Bezier Curve in
	 */
	@Override
	public void draw(double scale, Graphics2D g, int currentPoint, boolean showBoundingBox, boolean scaleCurveStrokes,
			boolean showTangent, boolean showThresholdRegion) {
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		if (scaleCurveStrokes) {
			g.setStroke(new BasicStroke((int) frame.getStrokeThickness(scale)));
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
			g.drawLine((int) (ctrlPts.get(noCtrlPts - 1).getX() * scale),
					(int) (ctrlPts.get(noCtrlPts - 1).getY() * scale), (int) (ctrlPts.get(0).getX() * scale),
					(int) (ctrlPts.get(0).getY() * scale));
		}

		// Draws the curve by drawing lines between each point on our Bezier Curve. This
		// piecewise
		// construction will be sufficient because of how close the points are to one
		// another.
		if (this.selected) {
			g.setColor(DEFAULT_CURVE_COLOR);
		} else {
			g.setColor(Color.GRAY);
		}

		for (int i = 0; i < curvePoints.size() - 1; i++) {
			g.drawLine((int) (curvePoints.get(i).x * scale), (int) (curvePoints.get(i).y * scale),
					(int) (curvePoints.get(i + 1).x * scale), (int) (curvePoints.get(i + 1).y * scale));
		}

		// Shows bounding box if the option is chosen
		if (showBoundingBox) {
			g.setColor(Color.GRAY);
			Rectangle2D b = getScaledBounds(boundingBox, scale);
			g.drawRect((int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());
		}

		// Draws the offset curve.
		if (showThresholdRegion) {
			scaledDataBounds.reset();
			for (Point2D p : dataFittingBounds) {
				scaledDataBounds.addPoint((int) (p.getX() * scale), (int) (p.getY() * scale));
			}
			g.setStroke(new BasicStroke(0));
			g.setColor(new Color(140, 220, 255));
			g.drawPolygon(scaledDataBounds);
			if (scaleCurveStrokes) {
				g.setStroke(new BasicStroke((int) frame.getStrokeThickness(scale)));
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
			Point2D dp = hodographPoints
					.get((int) ((BezierCurve.NO_CURVE_POINTS - 1) * currentPoint / frame.getNumberOfPointsPerCurve()));

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

			g.setColor(Color.YELLOW);
			g.fillRect((int) ((p.getX() - CTRL_PT_SIZE) * scale), (int) ((p.getY() - CTRL_PT_SIZE) * scale),
					(int) (2 * CTRL_PT_SIZE * scale), (int) (2 * CTRL_PT_SIZE * scale));
		}
	}

	public void draw(double scale, Graphics2D g, boolean scaleCurveStrokes) {
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		if (scaleCurveStrokes) {
			g.setStroke(new BasicStroke((int) frame.getStrokeThickness(scale)));
		} else {
			g.setStroke(new BasicStroke(0));
		}

		// Draws the curve by drawing lines between each point on our Bezier Curve. This
		// piecewise
		// construction will be sufficient because of how close the points are to one
		// another.
		if (this.selected) {
			g.setColor(DEFAULT_CURVE_COLOR);
		} else {
			g.setColor(Color.GRAY);
		}

		for (int i = 0; i < curvePoints.size() - 1; i++) {
			g.drawLine((int) (curvePoints.get(i).x * scale), (int) (curvePoints.get(i).y * scale),
					(int) (curvePoints.get(i + 1).x * scale), (int) (curvePoints.get(i + 1).y * scale));
		}

		// Put debugging code here if needed
		if (KappaFrame.DEBUG_MODE) {
			g.setColor(Color.YELLOW);
			g.fillRect((int) ((curvePoints.get(0).getX() - PT_INDICATOR_SIZE) * scale),
					(int) ((curvePoints.get(0).getY() - PT_INDICATOR_SIZE) * scale),
					(int) (2 * PT_INDICATOR_SIZE * scale), (int) (2 * PT_INDICATOR_SIZE * scale));
		}
	}

	@Override
	public double getPointCurvature(int percentage) {
		int n = (int) ((BezierCurve.NO_CURVE_POINTS - 1) * percentage / frame.getNumberOfPointsPerCurve());
		return getExactPointCurvature(n);
	}

	/**
	 * Returns the point curvature at the nth point
	 *
	 * @param n
	 *            The value of n
	 * @return The curvature at the nth point along the Bezier Curve
	 */
	public double getExactPointCurvature(int n) {
		return curvePoints.get(n).k * curvePoints.get(n).sign;
	}

	@Override
	public boolean isPointOnCurve(Point2D p, int t, double scale) {
		// To Speed this up, if the point is outside the bounding box of the curve, we
		// automatically return false
		// We ignore the exception where the height or width of the curve is actually
		// less than the threshold size for data selection
		Rectangle2D curveBounds = getScaledBounds(boundingBox, scale);
		if (!curveBounds.contains(p) && curveBounds.getHeight() >= THRESHOLD_RADIUS
				&& curveBounds.getWidth() >= THRESHOLD_RADIUS) {
			return false;
		}
		scaledBounds.reset();
		for (Point2D v : bounds) {
			scaledBounds.addPoint((int) (v.getX() * scale), (int) (v.getY() * scale));
		}
		return scaledBounds.contains(p);
	}

	/**
	 * Gets the Average Curvature along the Bezier Curve
	 *
	 * @return The Average Curvature along the Bezier Curve
	 */
	@Override
	public double getAverageCurvature() {
		double total = 0;
		for (BezierPoint point : curvePoints) {
			total += point.k;
		}
		return total / curvePoints.size();
	}

	/**
	 * Gets the approximate curve length of the Bezier Curve, approximated by the
	 * sum of the line segments lengths between all the points in the curve
	 *
	 * @param segment
	 *            The point you want to stop at. At most curves.size() - 1
	 * @return The approximate curve length of the Bezier Curve
	 */
	private double getApproxCurveLength(int segment) {
		double length = 0;
		for (int i = 0; i < segment; i++) {
			length += curvePoints.get(i).distance(curvePoints.get(i + 1));
		}
		return length * micronPixelFactor;
	}

	@Override
	public double getApproxCurveLength() {
		return getApproxCurveLength(curvePoints.size() - 1);
	}

	/**
	 * Returns the Standard Deviation for all the obtained curvature values
	 *
	 * @return The Standard Deviation of all curvature values along the Bezier Curve
	 */
	@Override
	public double getCurvatureStdDev() {
		double variance = 0;
		double mu = getAverageCurvature();
		for (BezierPoint point : curvePoints) {
			variance += (point.k - mu) * (point.k - mu);
		}
		variance /= curvePoints.size() - 1;
		return Math.sqrt(variance);
	}

	@Override
	public List<BezierPoint> getPoints() {
		return curvePoints;
	}

	@Override
	public List<BezierPoint> getDigitizedPoints() {
		// Averages subpixel values into a single pixel coordinate
		// We bin all points on the curve that evaluate to the same pixel coordinate
		// into one averaged point
		ArrayList<BezierPoint> digitizedPoints = new ArrayList<>();
		int i = 0;

		// When we export the data, we augment both the x and y coordinate by 1. This is
		// because the first row pixel and the first
		// column pixel both have index 0. To convert this to a 1-based index as in the
		// picture, we add 1 to both.
		while (i < curvePoints.size()) {
			BezierPoint p = curvePoints.get(i);
			int n = 1;
			double totalX = p.getX() + 1, totalY = p.getY() + 1, totalK = p.k;

			// Keeps adding to the total while successive points in our curve round to the
			// same pixel coordinate.
			while (i + 1 < curvePoints.size() && (int) curvePoints.get(i + 1).getX() == (int) p.getX()
					&& (int) curvePoints.get(i + 1).getY() == (int) p.getY()) {
				n++;
				p = curvePoints.get(++i);
				totalX += p.getX();
				totalY += p.getY();
				totalK += p.k;
			}

			// Now we add the digitized point
			digitizedPoints.add(new BezierPoint(totalX / n, totalY / n, totalK / n));
			i++;
		}
		return digitizedPoints;
	}

	@Override
	public List<Point2D> getIntensityDataRed() {
		ArrayList<Point2D> intensityData = new ArrayList<>(NO_CURVE_POINTS);
		for (int i = 0; i < curvePoints.size(); i++) {
			// Displays curvature with respect to the x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				intensityData.add(new Point2D.Double(curvePoints.get(i).x, RGBvals.get(i)[0]));
			} // Displays curvature with respect to the arc length
			else if (KappaMenuBar.distributionDisplay == 1) {
				intensityData.add(new Point2D.Double(getApproxCurveLength(i), RGBvals.get(i)[0]));
			} // Displays curvature with respect to the point index
			else {
				intensityData.add(new Point2D.Double(i, RGBvals.get(i)[0]));
			}
		}
		return intensityData;
	}

	@Override
	public List<Point2D> getIntensityDataGreen() {
		ArrayList<Point2D> intensityData = new ArrayList<>(NO_CURVE_POINTS);
		for (int i = 0; i < curvePoints.size(); i++) {
			// Displays curvature with respect to the x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				intensityData.add(new Point2D.Double(curvePoints.get(i).x, RGBvals.get(i)[1]));
			} // Displays curvature with respect to the arc length
			else if (KappaMenuBar.distributionDisplay == 1) {
				intensityData.add(new Point2D.Double(getApproxCurveLength(i), RGBvals.get(i)[1]));
			} // Displays curvature with respect to the point index
			else {
				intensityData.add(new Point2D.Double(i, RGBvals.get(i)[1]));
			}
		}
		return intensityData;
	}

	@Override
	public List<Point2D> getIntensityDataBlue() {
		ArrayList<Point2D> intensityData = new ArrayList<>(NO_CURVE_POINTS);
		for (int i = 0; i < curvePoints.size(); i++) {
			// Displays curvature with respect to the x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				intensityData.add(new Point2D.Double(curvePoints.get(i).x, RGBvals.get(i)[2]));
			} // Displays curvature with respect to the arc length
			else if (KappaMenuBar.distributionDisplay == 1) {
				intensityData.add(new Point2D.Double(getApproxCurveLength(i), RGBvals.get(i)[2]));
			} // Displays curvature with respect to the point index
			else {
				intensityData.add(new Point2D.Double(i, RGBvals.get(i)[2]));
			}
		}
		return intensityData;
	}

	@Override
	public List<Point2D> getCurveData() {
		ArrayList<Point2D> curveData = new ArrayList<>(NO_CURVE_POINTS);
		for (int i = 0; i < curvePoints.size(); i++) {
			// Displays curvature with respect to the x-coordinate
			if (KappaMenuBar.distributionDisplay == 0) {
				curveData.add(new Point2D.Double(curvePoints.get(i).x, curvePoints.get(i).k));
			} // Displays curvature with respect to the arc length
			else if (KappaMenuBar.distributionDisplay == 1) {
				curveData.add(new Point2D.Double(getApproxCurveLength(i), curvePoints.get(i).k));
			} // Displays curvature with respect to the point index
			else {
				curveData.add(new Point2D.Double(i, curvePoints.get(i).k));
			}
		}
		return curveData;
	}

	@Override
	public List<Point2D> getDebugCurveData() {
		ArrayList<Point2D> debugCurveData = new ArrayList<>(NO_CURVE_POINTS);
		for (Point2D p : curvePoints) {
			debugCurveData.add(new Point2D.Double(p.getX(), frame.computeCurvature(p.getX(),
					6000 / (Curve.getMicronPixelFactor() * 1000.0), (2 * Math.PI) / frame.getCurrImage().getWidth())));
		}
		return debugCurveData;
	}

	@Override
	public void updateIntensities() {
		RGBvals = new ArrayList<>();
		int[] pixels;
		ImageUtils imgUtils = new ImageUtils<>();
		for (Point2D p : curvePoints) {
			pixels = imgUtils.getPixels(frame.getImageStack(), (int) p.getX(), (int) p.getY());
			RGBvals.add(pixels);
		}
	}

	@Override
	public int getNoPoints() {
		return NO_CURVE_POINTS;
	}

	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	@Override
	public BezierPoint getPoint(int percentage) {
		int n = (int) ((BezierCurve.NO_CURVE_POINTS - 1) * percentage / frame.getNumberOfPointsPerCurve());
		return getExactPoint(n);
	}

	public BezierPoint getExactPoint(int n) {
		return curvePoints.get(n);
	}

	@Override
	public Point2D.Double getUnitTangent(int n) {
		double dx = hodographPoints.get(n).getX();
		double dy = hodographPoints.get(n).getY();
		double normalizationFactor = Math.sqrt(dx * dx + dy * dy);
		return new Point2D.Double(dx / normalizationFactor, dy / normalizationFactor);
	}

	@Override
	public Point2D.Double getUnitNormal(int n) {
		double dx = hodographPoints.get(n).getX();
		double dy = hodographPoints.get(n).getY();
		double normalizationFactor = Math.sqrt(dx * dx + dy * dy);

		// Negative reciprocal of the unit tangent vector
		return new Point2D.Double(-dy / normalizationFactor, dx / normalizationFactor);
	}

	@Override
	public int getSign(int n) {
		return curvePoints.get(n).sign;
	}

	// Faster than Math.pow
	private double squared(double x) {
		return x * x;
	}

	int getDistanceSign(int n, Point2D x) {
		// Let p be a point on a curve, x the corresponding data point, and C the
		// curvature centre at
		// that point. If x lies on the opposite side of the curve from the curvature
		// centre, then:
		// |(p-x) + (C-p)| < 1/k, where 1/k is the radius of curvature. The sign is then
		// negative.
		// Otherwise, if x lies on the same side of the curve from the curvature centre,
		// then:
		// |(p-x) + (C-p)| > 1/k. The sign is then positive.
		BezierPoint p = curvePoints.get(n);
		Point2D N = this.getUnitNormal(n);

		// We multiply N by the sign of the curvature at the point. This
		// allows for N to always point towards the curvature centre.
		Point2D C = new Point2D.Double(p.getX() + this.getSign(n) * N.getX() * (1 / p.k),
				p.getY() + this.getSign(n) * N.getY() * (1 / p.k));
		if (Math.sqrt(squared(p.getX() - x.getX() + (C.getX() - p.getX()))
				+ squared(p.getY() - x.getY() + (C.getY() - p.getY()))) > (1 / p.k)) {
			return 1;
		}
		return -1;
	}

	public Point2D.Double getHodographPoint(int n) {
		return hodographPoints.get(n);
	}

	@Override
	public List<Point2D> getThresholdedPixels() {
		return thresholdedPixels;
	}

	@Override
	public double getMaximum(double start, double end) {
		double max = Double.MIN_VALUE;
		for (BezierPoint p : curvePoints) {
			if (p.getX() >= start && p.getX() <= end && p.k > max) {
				max = p.k;
			}
		}
		return max;
	}

}
