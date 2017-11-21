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

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Curve {

	public static final int DEFAULT_STROKE_THICKNESS = 1;
	public static final int CTRL_PT_SIZE = 1;
	public static final double DEFAULT_MICRON_PIXEL_FACTOR = 0.16;

	// The number of threshold pixels we'll allow when selecting a curve, or control
	// point
	// (ie we'll allow a tolerance of this number of pixels)
	public static final int THRESHOLD_RADIUS = 5;
	public static final int CTRL_PT_TOL = 4;

	// We keep track of the minimum global and local error ever observed, as a
	// threshold value when we adjust control points.
	// Global = across the entire curve
	// Local = across the spline only.
	protected double minimumGlobalError;
	protected double minimumLocalError;

	// Drawing constants
	public static final int SELECTED_CTRL_PT_SIZE = 2;
	public static final int PT_INDICATOR_SIZE = 2;
	public static final int STRETCH_FACTOR = 1;

	// The index of the selected control point. -1 if no control point is selected
	int selectedCtrlPtIndex;
	String name;
	List<Point2D> ctrlPts;
	int noCtrlPts;
	int hoveredCtrlPt;
	int dataRadius;
	boolean selected;
	public BArrayList keyframes;
	Rectangle2D boundingBox;
	List<Point2D> bounds;
	Polygon scaledBounds;
	List<Point2D> dataFittingBounds;
	Polygon scaledDataBounds;
	List<Point2D> thresholdedPixels;
	private int t;

	// um/pixel conversion factor
	protected static double micronPixelFactor = DEFAULT_MICRON_PIXEL_FACTOR;

	public Curve(List<Point2D> ctrlPts, int t, int noCtrlPts, String name, int dataRadius) {
		this.selected = true;
		this.name = name;
		this.ctrlPts = ctrlPts;
		this.selectedCtrlPtIndex = -1;
		this.noCtrlPts = noCtrlPts;
		this.hoveredCtrlPt = -1;
		keyframes = new BArrayList();
		keyframes.add(new BControlPoints(this.ctrlPts, t));
		this.boundingBox = keyframes.getBounds(t);
		this.dataRadius = dataRadius;
		this.t = t;

		this.bounds = new ArrayList<>();
		this.scaledBounds = new Polygon();
		this.dataFittingBounds = new ArrayList<>();
		this.scaledDataBounds = new Polygon();
		this.thresholdedPixels = new ArrayList<>();
	}

	public class BArrayList extends ArrayList<BControlPoints> {

		private static final long serialVersionUID = 1L;

		@Override
		public boolean add(BControlPoints element) {
			// Adds in sorted order
			int index = Collections.binarySearch(this, element.t);
			if (index < 0) {
				this.add(-1 * (index + 1), element);
				return true;
			}

			// If an element at the same t already exists, we just exchange it with the new
			// one.
			this.set(index, element);
			return true;
		}

		public BControlPoints getInclusivePrev(int t) {
			// Finds the previous defined keyframe. If no previous defined keyframe exists,
			// it'll return the first one.
			// Returns the current keyframe if it is one.
			int index = Collections.binarySearch(this, t);
			if (index < 0) {
				index = -1 * (index + 1);
				if (index == 0) {
					return this.get(index);
				}
				return this.get(index - 1);
			} else {
				return this.get(index);
			}
		}

		public BControlPoints getInclusiveNext(int t) {
			// Finds the next defined keyframe. Returns the current keyframe if it is one.
			int index = Collections.binarySearch(this, t);
			if (index < 0) {
				index = -1 * (index + 1);
				if (index == this.size()) {
					return this.get(index - 1);
				}
				return this.get(index);
			} else {
				return this.get(index);
			}
		}

		/**
		 * Gets the Bounds for a certain time t by translating keyframe layers
		 *
		 * @param t
		 *            The time t we are interested in
		 * @return The bounds at that time t.
		 */
		public Rectangle2D.Double getBounds(int t) {
			// If the previous value is the same as the next value, then we're at a
			// keyframe.
			// Then we return these bounds, with the thresholding
			BControlPoints prev = keyframes.getInclusivePrev(t);
			BControlPoints next = keyframes.getInclusiveNext(t);
			if (prev.t == next.t) {
				return new Rectangle2D.Double(prev.minX, prev.minY, prev.maxX - prev.minX, prev.maxY - prev.minY);
			}

			// Our scale factor is t1-t0/(t2 - t0), where t1 = t, and t0 and t2 are the
			// preceding and succeeding keyframe times
			double scaleFactor = (t - prev.t) / ((next.t - prev.t) * 1.0);
			return new Rectangle2D.Double(prev.minX + (next.minX - prev.minX) * scaleFactor,
					prev.minY + (next.minY - prev.minY) * scaleFactor,
					scaleFactor * (next.maxX - next.minX) - (prev.maxX - prev.minX) * (scaleFactor - 1),
					scaleFactor * (next.maxY - next.minY) - (prev.maxY - prev.minY) * (scaleFactor - 1));
		}
	}

	// We use this class to keep track of sets of points that define keyframes
	public class BControlPoints implements Comparable<Integer> {

		public Point2D[] defPoints;
		public double minX, maxX, minY, maxY;
		public int t;

		public BControlPoints(List<Point2D> ctrlPoints, int t) {
			this.defPoints = new Point2D[noCtrlPts];
			for (int i = 0; i < noCtrlPts; i++) {
				defPoints[i] = ctrlPoints.get(i);
			}
			this.t = t;

			minX = maxX = defPoints[0].getX();
			minY = maxY = defPoints[0].getY();
			for (int i = 1; i < defPoints.length; i++) {
				if (defPoints[i].getX() < minX) {
					minX = defPoints[i].getX();
				} else if (defPoints[i].getX() > maxX) {
					maxX = defPoints[i].getX();
				}
				if (defPoints[i].getY() < minY) {
					minY = defPoints[i].getY();
				} else if (defPoints[i].getY() > maxY) {
					maxY = defPoints[i].getY();
				}
			}
		}

		@Override
		public int compareTo(Integer t) {
			return this.t - t;
		}
	}

	/**
	 * Translates the curve points with respect to the previous and subsequent
	 * keyframes
	 *
	 * @param t
	 *            The layer we are currently at
	 */
	public void translateCurve(int t) {
		// Obtains the previous and subsequent keyframes for the Bezier Curve
		BControlPoints prev = keyframes.getInclusivePrev(t);
		BControlPoints next = keyframes.getInclusiveNext(t);

		// If this is a keyframe, (or any region past or before a defined keyframe),
		// this will set the control points to
		// the keyframe control points.
		if (prev.t == next.t) {
			for (int i = 0; i < noCtrlPts; i++) {
				ctrlPts.set(i, new Point2D.Double(prev.defPoints[i].getX(), prev.defPoints[i].getY()));
			}
			fillPoints(ctrlPts, t);
			return;
		}

		// Our scale factor is t1-t0/(t2 - t0), where t1 = t, and t0 and t2 are the
		// preceding and succeeding keyframe times
		double scaleFactor = (t - prev.t) / ((next.t - prev.t) * 1.0);
		for (int i = 0; i < noCtrlPts; i++) {
			ctrlPts.set(i, new Point2D.Double(
					prev.defPoints[i].getX() + ((next.defPoints[i].getX() - prev.defPoints[i].getX()) * scaleFactor),
					prev.defPoints[i].getY() + ((next.defPoints[i].getY() - prev.defPoints[i].getY()) * scaleFactor)));
		}
		fillPoints(ctrlPts, t);
		this.boundingBox = keyframes.getBounds(t);
	}

	public void addKeyFrame(Point2D newCtrlPt, int t) {
		// Shouldn't happen upon execution, but better safe than sorry!
		if (selectedCtrlPtIndex < 0) {
			return;
		}
		ctrlPts.set(selectedCtrlPtIndex, newCtrlPt);
		keyframes.add(new BControlPoints(ctrlPts, t));
		fillPoints(ctrlPts, t);
		boundingBox = keyframes.getBounds(t);

		// Resets the minimum errors if a control point has been moved
		minimumGlobalError = Double.MAX_VALUE;
		minimumLocalError = Double.MAX_VALUE;
	}

	public void addKeyframe(List<Point2D> newCtrlPts, int t) {
		keyframes.add(new BControlPoints(ctrlPts, t));
	}

	public int[] getKeyframeLayers() {
		int[] keyframeLayers = new int[keyframes.size()];
		int i = 0;
		for (BControlPoints p : keyframes) {
			keyframeLayers[i++] = p.t;
		}
		return keyframeLayers;
	}

	public int controlPointIndex(Point2D p, int t, double scale, boolean clicked) {
		// Sees if the point is within any of the control point boxes, and sets the
		// selected control pt index if one is selected
		for (int i = 0; i < ctrlPts.size(); i++) {
			if (p.getX() >= ((ctrlPts.get(i).getX() - SELECTED_CTRL_PT_SIZE) * scale - CTRL_PT_TOL)
					&& p.getX() <= ((ctrlPts.get(i).getX() + SELECTED_CTRL_PT_SIZE) * scale + CTRL_PT_TOL)
					&& p.getY() >= ((ctrlPts.get(i).getY() - SELECTED_CTRL_PT_SIZE) * scale - CTRL_PT_TOL)
					&& p.getY() <= ((ctrlPts.get(i).getY() + SELECTED_CTRL_PT_SIZE) * scale + CTRL_PT_TOL)) {
				if (clicked) {
					selectedCtrlPtIndex = i;
				}
				return i;
			}
		}
		return -1;
	}

	// Shifts the control point indices by 1 pixel to account for the fact that the
	// image is 0 indexed in java
	public void shiftControlPoints(int t) {
		for (int i = 0; i < noCtrlPts; i++) {
			ctrlPts.set(i, new Point2D.Double(ctrlPts.get(i).getX() + 1, ctrlPts.get(i).getY() + 1));
		}
		addKeyframe(ctrlPts, t);
		fillPoints(ctrlPts, t);
	}

	// De-shifts the control point indices by 1 pixel to account for the fact that
	// the image is 0 indexed in java
	public void deshiftControlPoints(int t) {
		for (int i = 0; i < noCtrlPts; i++) {
			ctrlPts.set(i, new Point2D.Double(ctrlPts.get(i).getX() - 1, ctrlPts.get(i).getY() - 1));
		}
		addKeyframe(ctrlPts, t);
		fillPoints(ctrlPts, t);
	}

	// Scales the coordinates of the control points in the curve by a value
	public void scale(double scaleFactor, int t) {
		for (int i = 0; i < noCtrlPts; i++) {
			ctrlPts.set(i,
					new Point2D.Double(ctrlPts.get(i).getX() * scaleFactor, ctrlPts.get(i).getY() * scaleFactor));
		}
		addKeyframe(ctrlPts, t);
		fillPoints(ctrlPts, t);
	}

	public abstract void drawThresholdedPixels(Graphics2D g, double scale);

	abstract void fillPoints(List<Point2D> ctrlPts, int t);

	abstract List<Point2D> generateOffsetBounds(List<Point2D> bounds, int radius);

	abstract void draw(double scale, Graphics2D g, int currentPoint, boolean showBoundingBox, boolean scaleCurveStrokes,
			boolean showTangent, boolean showThresholdedRegion);

	public abstract boolean isPointOnCurve(Point2D p, int t, double scale);

	public abstract double getAverageCurvature();

	public abstract double getApproxCurveLength();

	public abstract double getPointCurvature(int n);

	public abstract double getCurvatureStdDev();

	public abstract List<Point2D> getIntensityDataRed();

	public abstract List<Point2D> getIntensityDataGreen();

	public abstract List<Point2D> getIntensityDataBlue();

	public abstract List<Point2D> getCurveData();

	public abstract List<Point2D> getDebugCurveData();

	public abstract List<BezierPoint> getPoints();

	public abstract List<BezierPoint> getDigitizedPoints();

	public abstract Point2D.Double getPoint(int n);

	public abstract void setSelected(boolean selected);

	public abstract boolean isSelected();

	public abstract int getNoPoints();

	public abstract void evaluateThresholdedPixels();

	public abstract void updateIntensities();

	public abstract List<Point2D> getThresholdedPixels();

	public abstract Point2D.Double getUnitTangent(int footpointIndex);

	public abstract Point2D.Double getUnitNormal(int footpointIndex);

	public abstract int getSign(int footpointIndex);

	public abstract void printValues(PrintWriter out, double[][] averaged, boolean exportAllDataPoints);

	public abstract double getMaximum(double start, double end);

	public String getName() {
		return name;
	}

	public void resetControlPointSelection() {
		selectedCtrlPtIndex = -1;
	}

	public static void setMicronPixelFactor(double newMicronPixelFactor) {
		micronPixelFactor = newMicronPixelFactor;
	}

	public static double getMicronPixelFactor() {
		return micronPixelFactor;
	}

	public Rectangle2D.Double getScaledBounds(Rectangle2D rect, double imageScale) {
		return new Rectangle2D.Double(rect.getX() * imageScale, rect.getY() * imageScale, rect.getWidth() * imageScale,
				rect.getHeight() * imageScale);
	}

	public void recalculateCurvature(int t) {
		fillPoints(ctrlPts, t);
	}

	public void setHoveredControlPoint(int index) {
		this.hoveredCtrlPt = index;
	}

	public int getHoveredControlPoint() {
		return hoveredCtrlPt;
	}

	public int getNoKeyframes() {
		return keyframes.size();
	}

	public int getNoCtrlPts() {
		return ctrlPts.size();
	}

	public void setDataRadius(int radius) {
		this.dataRadius = radius;
		this.dataFittingBounds = generateOffsetBounds(dataFittingBounds, dataRadius);
		this.evaluateThresholdedPixels();
	}

	public int getDataRadius() {
		return dataRadius;
	}

	public int getT() {
		return t;
	}

	public List<Point2D> getCtrlPts() {
		return ctrlPts;
	}

}
