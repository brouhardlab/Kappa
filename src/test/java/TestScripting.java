
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

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imagej.ImageJ;
import sc.fiji.kappa.curve.BSpline;
import sc.fiji.kappa.curve.BezierGroup;
import sc.fiji.kappa.gui.KappaFrame;

public class TestScripting {

	public static void main(final String... args) throws Exception {
		// Launch ImageJ as usual.
		final ImageJ ij = new ImageJ();
		ij.launch(args);

		File kappaFile = new File("/home/hadim/test.kapp");
		BezierGroup curves = new BezierGroup();

		int thresholdRadius = 5;
		int currentLayer = 1;
		curves = loadCurveFile(kappaFile, thresholdRadius, currentLayer);

		System.out.println(curves);
	}

	public static BezierGroup loadCurveFile(File file, int thresholdRadius, int currentLayer) {
		try {
			BezierGroup curves = new BezierGroup();
			List<Point2D> points;

			BufferedReader in = new BufferedReader(new FileReader(file));
			int noCurves = Integer.parseInt(in.readLine());

			for (int n = 0; n < noCurves; n++) {
				int curveType = Integer.parseInt(in.readLine());
				int noKeyframes = Integer.parseInt(in.readLine());
				int noCtrlPts = Integer.parseInt(in.readLine());
				int bsplineType = 0;
				points = new ArrayList<Point2D>(noCtrlPts);

				// If the curve is a B-Spline, there is an extra parameter determining whether
				// it's open or closed
				if (curveType == KappaFrame.B_SPLINE) {
					bsplineType = Integer.parseInt(in.readLine());
				}

				// Initialize the curve
				int currentKeyframe = Integer.parseInt(in.readLine());
				for (int i = 0; i < noCtrlPts; i++) {
					points.add(
							new Point2D.Double(Double.parseDouble(in.readLine()), Double.parseDouble(in.readLine())));
				}

				if (curveType == KappaFrame.B_SPLINE) {
					curves.addCurve(points, currentKeyframe, noCtrlPts, KappaFrame.B_SPLINE,
							bsplineType == BSpline.OPEN, thresholdRadius);
				} else {
					KappaFrame.curves.addCurve(points, currentKeyframe, noCtrlPts, KappaFrame.BEZIER_CURVE, true,
							thresholdRadius);
				}

				// Load all the other keyframes for the curve
				for (int i = 1; i < noKeyframes; i++) {
					currentKeyframe = Integer.parseInt(in.readLine());
					points = new ArrayList<Point2D>(noCtrlPts);

					// Adds the control points for each keyframe. We add the redundant control
					// points for closed B-Spline curves.
					if (bsplineType == BSpline.OPEN) {
						for (int j = 0; j < noCtrlPts; j++) {
							points.add(new Point2D.Double(Double.parseDouble(in.readLine()),
									Double.parseDouble(in.readLine())));
						}
					} else {
						for (int j = 0; j < noCtrlPts - BSpline.B_SPLINE_DEGREE; j++) {
							points.add(new Point2D.Double(Double.parseDouble(in.readLine()),
									Double.parseDouble(in.readLine())));
						}
						for (int j = 0; j < BSpline.B_SPLINE_DEGREE; j++) {
							points.add(new Point2D.Double(points.get(i).getX(), points.get(i).getY()));
						}
					}
					curves.get(curves.size() - 1).addKeyframe(points, currentKeyframe);
				}
			}

			// Translates all the curves to their position at the current frame.
			curves.changeFrame(currentLayer);

			in.close();

			return curves;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

}
