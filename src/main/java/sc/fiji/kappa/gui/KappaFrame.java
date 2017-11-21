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
package sc.fiji.kappa.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import sc.fiji.kappa.curve.BSpline;
import sc.fiji.kappa.curve.BezierCurve;
import sc.fiji.kappa.curve.BezierGroup;
import sc.fiji.kappa.curve.BezierPoint;
import sc.fiji.kappa.curve.Curve;

public class KappaFrame extends JFrame {
	// Debugging Constants

	private static final long serialVersionUID = 1L;
	public static final boolean DEBUG_MODE = false;

	// Whether we allow for control point adjustment or not
	public static final boolean DEFAULT_CTRL_PT_ADJUSTMENT = false;
	public static boolean enableCtrlPtAdjustment = DEFAULT_CTRL_PT_ADJUSTMENT;

	// Application Constants
	public static final String APPLICATION_NAME = "Kappa";
	public static final int UNIT_SCALE = 1000;
	public static final int APP_DEFAULT_WIDTH = 1250;
	public static final int APP_DEFAULT_HEIGHT = 950;
	public static final int APP_MIN_WIDTH = 700;
	public static final int APP_MIN_HEIGHT = 550;
	public static final int APP_DEFAULT_X = 0;
	public static final int APP_DEFAULT_Y = 0;
	public static final Color PANEL_COLOR = new Color(240, 240, 240);
	public static final Color DRAWING_PANEL_COLOR = new Color(180, 180, 180);
	public static final int COMBO_BOX_HEIGHT_OSX = 22;
	public static final int SCROLL_BAR_THICKNESS = 19;
	public static final double PERCENT_END_CONDITIONS = 0.1;
	public static final int PANEL_WIDTH = 225;
	public static final int DEFAULT_NO_CTRL_PTS = 4;
	public static final boolean ANTIALIASING_DEFAULT = false;
	public static final int DEFAULT_BG_THRESHOLD = 36;
	public static final int BG_AVERAGING_RANGE = 1;

	// Curve Variables and Constants
	public static final String[] FITTING_ALGORITHMS = { "Point Distance Minimization",
			"Squared Distance Minimization" };
	public static final String[] CURVE_TYPES = { "Bézier Curve", "B-Spline" };
	public static final int BEZIER_CURVE = 0;
	public static final int B_SPLINE = 1;
	public static final int DEFAULT_INPUT_CURVE = B_SPLINE;
	public static final String[] BSPLINE_TYPES = { "Open", "Closed" };

	public static int bsplineType;
	public static int inputType;

	// 0 = Point Distance Minimization
	// 1 = Squared Distance Minimization
	public static final int DEFAULT_FITTING_ALGORITHM = 0;
	public static String fittingAlgorithm;

	public static Overlay overlay;

	// Bezier Curve information
	public static BezierGroup curves = new BezierGroup();
	public static List<Point2D> points = new ArrayList<Point2D>(DEFAULT_NO_CTRL_PTS);
	public Curve currEditedCurve;
	public static int currCtrlPt = 0;
	public boolean controlPointSelected;
	public boolean shiftPressed;
	public boolean dragged;
	public static boolean fittingRunning;
	public int prevIndex;

	public static final int INIT_LAYER = 1;
	public static int maxLayer;
	public static int maxLayerDigits;

	// Image variables
	public static ImagePlus displayedImageStack;
	public static ImagePlus imageStack;
	public static ImageStack[] imageStackLayers;
	public static BufferedImage currImage;
	public static BufferedImage scaled;
	public static JLabel currImageLabel;
	protected static boolean[][] thresholded;
	public static ScrollDrawingPane scrollPane;

	// Panels
	public static InfoPanel infoPanel;
	public static ExportPanel exportPanel;
	public static ControlPanel controlPanel;
	public static ToolPanel toolPanel;

	@Parameter
	private Context context;

	public static KappaFrame frame;

	public KappaFrame(Context context) {

		// Set up the original frame
		super(APPLICATION_NAME);
		context.inject(this);

		setSize(APP_DEFAULT_WIDTH, APP_DEFAULT_HEIGHT);
		setLocation(APP_DEFAULT_X, APP_DEFAULT_Y);

		setLayout(new BorderLayout());
		infoPanel = new InfoPanel();
		exportPanel = new ExportPanel();
		controlPanel = new ControlPanel();
		toolPanel = new ToolPanel();
		add(infoPanel, BorderLayout.EAST);
		add(controlPanel, BorderLayout.SOUTH);
		add(toolPanel, BorderLayout.NORTH);

		// Sets the glass pane up for notifications
		overlay = new Overlay();
		this.setGlassPane(overlay);
		overlay.setOpaque(false);

		// Default Curve input
		inputType = DEFAULT_INPUT_CURVE;
		bsplineType = BSpline.DEFAULT_BSPLINE_TYPE;
		fittingAlgorithm = FITTING_ALGORITHMS[DEFAULT_FITTING_ALGORITHM];

		// We define the currentImage as a label so the centering and scaling can be
		// done by the layout manager
		currImageLabel = new JLabel();
		currImageLabel.setHorizontalAlignment(JLabel.CENTER);

		// We add the JScrollPane containing the desired Image
		scrollPane = new ScrollDrawingPane(currImageLabel);
		scrollPane.setVisible(true);
		add(scrollPane);

		// Key Bindings for the Hand Tool
		scrollPane.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "space pressed");
		scrollPane.getInputMap().put(KeyStroke.getKeyStroke("released SPACE"), "space released");
		scrollPane.getActionMap().put("space pressed", (new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (!toolPanel.isEnabled(1) || toolPanel.isSelected(1)) {
					return;
				}
				prevIndex = 0;
				while (!toolPanel.isSelected(prevIndex)) {
					prevIndex++;
				}
				toolPanel.setSelected(1, true);
				scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
		}));
		scrollPane.getActionMap().put("space released", (new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (!toolPanel.isEnabled(1)) {
					return;
				}
				toolPanel.setSelected(prevIndex, true);
				scrollPane.setCursor(ToolPanel.TOOL_CURSORS[prevIndex]);
			}
		}));

		// Key Bindings for the SHIFT key
		scrollPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK),
				"shift pressed");
		scrollPane.getInputMap().put(KeyStroke.getKeyStroke("released SHIFT"), "shift released");
		scrollPane.getActionMap().put("shift pressed", (new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				shiftPressed = true;
			}
		}));
		scrollPane.getActionMap().put("shift released", (new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				shiftPressed = false;
			}
		}));
		imageStack = null;

		// Adds the menubar
		this.setJMenuBar(new MenuBar(context));

		// Moves the export button position when the window is resized.
		this.getRootPane().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (System.getProperty("os.name").equals("Mac OS X")) {
					exportPanel.exportButton.setBounds(new Rectangle(20, getHeight() - 150, PANEL_WIDTH - 40, 25));
				} else {
					exportPanel.exportButton.setBounds(new Rectangle(20, getHeight() - 200, PANEL_WIDTH - 40, 25));
				}
			}
		});

		this.setFocusable(true);
		this.requestFocusInWindow();
	}

	public static void fitCurves() {
		// If no curves are selected, no fitting is done
		if (curves.getNoSelected() == 0) {
			return;
		}

		// Shows that the fitting algorithm is running
		fittingRunning = true;
		overlay.setVisible(true);

		// We draw an overlay without a built in delay because we turn it off ourselves.
		// Hence the
		// delay interval is -1 by convention.
		overlay.drawNotification("Fitting in Progress...", scrollPane.getVisibleRect(), -1);

		// We fit every selected B-Spline.
		for (Curve c : curves.getSelected()) {
			if (c instanceof BSpline) {
				// Performs curve fitting with the current B-Spline
				double error = Double.MAX_VALUE;
				double oldError;
				List<Point2D> dataPoints;
				List<Double> weights;

				// Sets the x and y coordinate to (x-1, y-1), because the image is zero-indexed
				// in java,
				// we want to 'de-shift' it when we fit the curve
				c.deshiftControlPoints(ControlPanel.currentLayerSlider.getValue());

				// If the b-spline is closed, we convert it to an open curve for fitting.
				// Converts it to an open B-Spline for fitting if it was originally a closed
				// spline
				boolean wasOpen = ((BSpline) c).isOpen();
				if (!((BSpline) c).isOpen()) {
					((BSpline) c).convertToOpen(ControlPanel.currentLayerSlider.getValue());
				}
				do {
					oldError = error;

					// Checks to make sure that some data points are there
					dataPoints = c.getThresholdedPixels();

					weights = getWeights(dataPoints);
					error = ((BSpline) c).fittingIteration(dataPoints, weights,
							ControlPanel.currentLayerSlider.getValue());
				} while (oldError > error);
				error = oldError;

				// Once the fitting has been done, we remove unnecessary control points.
				if (enableCtrlPtAdjustment) {
					((BSpline) c).adjustControlPoints(dataPoints, weights, ControlPanel.currentLayerSlider.getValue());
				}
				if (!wasOpen) {
					((BSpline) c).convertToClosed(ControlPanel.currentLayerSlider.getValue());
				}

				// Sets the x and y coordinate to (x+1, y+1), because the image is zero-indexed
				// in java, so there's a 1 pixel offset
				c.shiftControlPoints(ControlPanel.currentLayerSlider.getValue());

				// Updates curve display
				infoPanel.repaint();
				drawImageOverlay();
				InfoPanel.updateHistograms();
			}
		}

		// Shows that the execution has stopped
		fittingRunning = false;
		overlay.setVisible(false);
	}

	/**
	 * Resets the set of curves and the corresponding list
	 */
	protected static void resetCurves() {
		InfoPanel.listData = new Vector<>();
		InfoPanel.list.setListData(InfoPanel.listData);
		curves = new BezierGroup();
	}

	/**
	 * Modifies a composite image that is both scaled and has all Bezier Curves
	 * drawn onto it
	 *
	 * @param scale
	 *            The scale factor to scale the image by
	 */
	protected static void setScaledImage(double scale) {
		if (currImage == null) {
			return;
		}

		// Optimizes the image type for drawing onto the screen
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = env.getDefaultScreenDevice();
		GraphicsConfiguration config = device.getDefaultConfiguration();

		int w = (int) (scale * currImage.getWidth());
		int h = (int) (scale * currImage.getHeight());
		scaled = config.createCompatibleImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = scaled.createGraphics();
		if (MenuBar.antialiasingMenu.getState()) {
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		}
		g2.drawImage(currImage, 0, 0, w, h, null);

		// Draws the thresholded pixels on top
		if (InfoPanel.bgCheckBox.isSelected()) {
			g2.setColor(Color.RED);
			for (int i = 0; i < currImage.getWidth(); i++) {
				for (int j = 0; j < currImage.getHeight(); j++) {
					if (thresholded[i][j]) {
						g2.fillRect((int) Math.round(i * scale), (int) Math.round(j * scale), (int) Math.round(scale),
								(int) Math.round(scale));
					}
				}
			}
		}
		g2.dispose();
	}

	/**
	 * Draws everything on top of the scaled image
	 */
	protected static void drawImageOverlay() {
		if (currImage == null) {
			return;
		}
		double scale = ControlPanel.scaleSlider.getValue() / 100.0;
		BufferedImage combined = new BufferedImage(scaled.getWidth(), scaled.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = (Graphics2D) combined.getGraphics();
		g2.drawImage(scaled, 0, 0, null);

		// Draws the data threshold pixels on top
		if (InfoPanel.showDatapointsCheckBox.isSelected()) {
			for (Curve c : curves) {
				c.drawThresholdedPixels(g2, scale);
			}
		}

		// Draws all the Bezier Curves
		g2.setColor(Color.GRAY);
		g2.setStroke(new BasicStroke((int) (Curve.DEFAULT_STROKE_THICKNESS * scale)));
		curves.draw(g2, scale, InfoPanel.pointSlider.getValue(), MenuBar.boundingBoxMenu.getState(),
				MenuBar.scaleCurvesMenu.getState(), MenuBar.tangentMenu.getState(),
				InfoPanel.showRadiusCheckBox.isSelected());

		// Draws the points we've built so far if a complete Bezier Curve has not been
		// formed
		if (currCtrlPt != 0) {
			g2.setColor(Color.GRAY);
			for (int i = 0; i < currCtrlPt - 1; i++) {
				g2.drawLine((int) (points.get(i).getX() * scale), (int) (points.get(i).getY() * scale),
						(int) (points.get(i + 1).getX() * scale), (int) (points.get(i + 1).getY() * scale));
			}

			// If it's a closed B-Spline, then we close the polygon.
			if (inputType == B_SPLINE && bsplineType == BSpline.CLOSED) {
				g2.drawLine((int) (points.get(0).getX() * scale), (int) (points.get(0).getY() * scale),
						(int) (points.get(points.size() - 1).getX() * scale),
						(int) (points.get(points.size() - 1).getY() * scale));
			}

			g2.setColor(Color.WHITE);
			for (int i = 0; i < currCtrlPt; i++) {
				g2.fillRect((int) ((points.get(i).getX() - Curve.CTRL_PT_SIZE) * scale),
						(int) ((points.get(i).getY() - Curve.CTRL_PT_SIZE) * scale),
						(int) (2 * Curve.CTRL_PT_SIZE * scale), (int) (2 * Curve.CTRL_PT_SIZE * scale));
			}
		}
		currImageLabel.setIcon(new ImageIcon(combined));
	}

	protected static void setLayer(int layer, double scale) {
		// If there is an open image stack, it will draw it in the drawing panel
		// Also changes the frame for our bezier curves, for keyframing.
		displayedImageStack.setZ(layer);
		currImage = KappaFrame.displayedImageStack.getBufferedImage();
		setScaledImage(scale);
		curves.changeFrame(layer);

		// Updates histograms and background thresholds
		updateDisplayed();
	}

	protected static void setDisplayedChannels(int displayRange) {
		RGBStackMerge merge = new RGBStackMerge();
		// The currImage variable must be set before the histogram visibility is
		// changed,
		// otherwise the histograms may not update the image intensities in a newly
		// displayed channel
		switch (displayRange) {
		case 0:
			displayedImageStack = new ImagePlus(imageStack.getTitle(), merge.mergeStacks(imageStack.getWidth(),
					imageStack.getHeight(), imageStack.getNSlices(), null, null, null, true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(false, false, false);
			break;
		case 1:
			displayedImageStack = new ImagePlus(imageStack.getTitle(), merge.mergeStacks(imageStack.getWidth(),
					imageStack.getHeight(), imageStack.getNSlices(), null, null, imageStackLayers[2], true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(false, false, true);
			break;
		case 2:
			displayedImageStack = new ImagePlus(imageStack.getTitle(), merge.mergeStacks(imageStack.getWidth(),
					imageStack.getHeight(), imageStack.getNSlices(), null, imageStackLayers[1], null, true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(false, true, false);
			break;
		case 3:
			displayedImageStack = new ImagePlus(imageStack.getTitle(),
					merge.mergeStacks(imageStack.getWidth(), imageStack.getHeight(), imageStack.getNSlices(), null,
							imageStackLayers[1], imageStackLayers[2], true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(false, true, true);
			break;
		case 4:
			displayedImageStack = new ImagePlus(imageStack.getTitle(), merge.mergeStacks(imageStack.getWidth(),
					imageStack.getHeight(), imageStack.getNSlices(), imageStackLayers[0], null, null, true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(true, false, false);
			break;
		case 5:
			displayedImageStack = new ImagePlus(imageStack.getTitle(),
					merge.mergeStacks(imageStack.getWidth(), imageStack.getHeight(), imageStack.getNSlices(),
							imageStackLayers[0], null, imageStackLayers[2], true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(true, false, true);
			break;
		case 6:
			displayedImageStack = new ImagePlus(imageStack.getTitle(),
					merge.mergeStacks(imageStack.getWidth(), imageStack.getHeight(), imageStack.getNSlices(),
							imageStackLayers[0], imageStackLayers[1], null, true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(true, true, false);
			break;
		case 7:
			displayedImageStack = new ImagePlus(imageStack.getTitle(),
					merge.mergeStacks(imageStack.getWidth(), imageStack.getHeight(), imageStack.getNSlices(),
							imageStackLayers[0], imageStackLayers[1], imageStackLayers[2], true));
			displayedImageStack.setZ(ControlPanel.currentLayerSlider.getValue());
			currImage = displayedImageStack.getBufferedImage();
			InfoPanel.setHistogramVisibility(true, true, true);
		}
		setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
		infoPanel.repaint();
		drawImageOverlay();
	}

	protected static void updateThresholded() {
		// Update thresholded level
		int thresholdLevel = InfoPanel.thresholdSlider.getValue();
		int[] rgb;
		int channel, intensity;
		channel = InfoPanel.thresholdChannelsComboBox.getSelectedIndex();
		for (int i = 0; i < currImage.getWidth(); i++) {
			for (int j = 0; j < currImage.getHeight(); j++) {

				// Checks the intensity level and compares it to the threshold level
				rgb = BezierCurve.getRGB(i, j);
				switch (channel) {
				case 0:
					intensity = rgb[0];
					break;
				case 1:
					intensity = rgb[1];
					break;
				case 2:
					intensity = rgb[2];
					break;
				default:
					intensity = (rgb[0] + rgb[1] + rgb[2]) / 3;
					break;
				}

				thresholded[i][j] = (intensity < thresholdLevel);
			}
		}
		setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
		drawImageOverlay();
	}

	/**
	 * Takes an (x, y) coordinate and translates it into the equivalent (x, y)
	 * coordinate for the *scaled* image with respect to the position of the image
	 * (ie. the top left corner of the image will correspond to (0,0)
	 *
	 * @param p
	 *            The original (x,y) point
	 * @return The translated (x,y) point
	 */
	private Point mapPoint(Point p) {
		Point ref = scrollPane.getViewport().getViewPosition();

		// If both scrollbars are visible, then the we want to translate p by the
		// coordinates of the viewpoint origin
		// 3 px is for the border for the scrollPane
		if (scrollPane.getHorizontalScrollBar().isVisible() && scrollPane.getVerticalScrollBar().isVisible()) {
			return new Point(p.x + ref.x - 3, p.y + ref.y - 3);
		} // If only one of the scrollbars are visible, then only one of the coordinates
			// will directly correspond.
			// If none are visible, none will directly correspond, and we have to obtain
			// these coordinates directly,
			// using the the width and height of the bounding box and the image.
		else if (scrollPane.getVerticalScrollBar().isVisible()) {
			return new Point(
					p.x - (currImageLabel.getWidth()
							- (int) (ControlPanel.scaleSlider.getValue() / 100.0 * currImage.getWidth())) / 2 - 3,
					p.y + ref.y - 3);
		} else if (scrollPane.getHorizontalScrollBar().isVisible()) {
			return new Point(
					p.x + ref.x - 3, p.y
							- (currImageLabel.getHeight()
									- (int) (ControlPanel.scaleSlider.getValue() / 100.0 * currImage.getHeight())) / 2
							- 3);
		}
		return new Point(
				p.x - (currImageLabel.getWidth()
						- (int) (ControlPanel.scaleSlider.getValue() / 100.0 * currImage.getWidth())) / 2 - 3,
				p.y - (currImageLabel.getHeight()
						- (int) (ControlPanel.scaleSlider.getValue() / 100.0 * currImage.getHeight())) / 2 - 3);
	}

	/**
	 * Takes an input number and returns the number with the given number of digits
	 * (or more). In other words, this will prepend '0's until the number is the
	 * required number of digits If it is already has more digits, this does
	 * nothing.
	 *
	 * @param number
	 *            The input number
	 * @param noDigits
	 *            The desired number of digits
	 * @return The number modified so that it has at least the desired number of
	 *         digits.
	 */
	public static String formatNumber(int number, int noDigits) {
		if (number == 0) {
			return "0000";
		}
		StringBuilder numberAsString = new StringBuilder();
		int start = 1;
		for (int i = 1; i < noDigits; i++) {
			start *= 10;
		}

		for (int i = start; i > number; i /= 10) {
			numberAsString.append("0");
		}
		numberAsString.append(Integer.toString(number));
		return numberAsString.toString();
	}

	/**
	 * Gets the weight of the data points depending on their intensity and whether
	 * we are looking for dark spots or bright spots.
	 *
	 * @param dataPoints
	 *            The data points, in an ArrayList with n elements
	 * @return An ArrayList with n elements with corresponding weight values.
	 */
	private static List<Double> getWeights(List<Point2D> dataPoints) {
		List<Double> weights = new ArrayList<>(dataPoints.size());
		for (Point2D p : dataPoints) {
			int[] rgb = BezierCurve.getRGB((int) p.getX(), (int) p.getY());

			int channel = InfoPanel.fittingChannelsComboBox.getSelectedIndex();
			double intensity = 0;
			switch (channel) {
			case 0:
				intensity = rgb[0];
			case 1:
				intensity = rgb[1];
			case 2:
				intensity = rgb[2];
			case 3:
				intensity = (rgb[0] + rgb[1] + rgb[2]) / 3;
			}

			// We want the higher weights to be for the darker pixels (with lower
			// intensities) when
			// we're looking for darker pixels in the image. Hence we adjust the weights
			// here
			if (InfoPanel.dataRangeComboBox.getSelectedIndex() == 1) {
				if (displayedImageStack.getBitDepth() == 24) // RGB Colour
				{
					intensity = 256 - intensity;
				} else // Grayscale, then 2^bitdepth is the max intensity.
				{
					intensity = (int) (Math.pow(2, displayedImageStack.getBitDepth()) - intensity);
				}
			}
			weights.add(intensity);
		}
		return weights;
	}

	protected static void updateDisplayed() {
		// Updates the background thresholding display
		if (InfoPanel.bgCheckBox.isSelected()) {
			updateThresholded();
		}

		// Updates the image
		drawImageOverlay();
		InfoPanel.updateHistograms();
	}

	protected static void enterCurve() {
		// Enters a new Bezier Curve or B-Spline when the user presses ENTER
		if (inputType == B_SPLINE) {
			curves.addCurve(points, ControlPanel.currentLayerSlider.getValue(), currCtrlPt, B_SPLINE,
					(bsplineType == BSpline.OPEN), (Integer) (InfoPanel.thresholdRadiusSpinner.getValue()));
		} else {
			curves.addCurve(points, ControlPanel.currentLayerSlider.getValue(), currCtrlPt, BEZIER_CURVE, true,
					(Integer) (InfoPanel.thresholdRadiusSpinner.getValue()));
		}
		InfoPanel.updateHistograms();

		// Updates our list after adding the curve
		InfoPanel.listData.addElement("  CURVE " + curves.getCount());
		InfoPanel.list.setListData(InfoPanel.listData);
		InfoPanel.list.setSelectedIndex(curves.size() - 1);
		InfoPanel.curvesList.revalidate();
		InfoPanel.pointSlider.setEnabled(true);
		InfoPanel.pointSlider.setValue(0);
		currCtrlPt = 0;
		MenuBar.enter.setEnabled(false);
		drawImageOverlay();
	}

	protected static void deleteCurve() {
		// Deletes a curve when the user presses DELETE
		// Deletes any control points not formed into a curve
		if (currCtrlPt != 0) {
			currCtrlPt = 0;
		}

		// We go down the list of indices so that the array indices don't get changed
		// when we remove elements
		if (InfoPanel.list.isSelectionEmpty()) {
			drawImageOverlay();
			return;
		}
		int[] selectedIndices = InfoPanel.list.getSelectedIndices();
		for (int i = selectedIndices.length - 1; i >= 0; i--) {
			InfoPanel.listData.removeElementAt(selectedIndices[i]);
			curves.remove(selectedIndices[i]);
		}
		InfoPanel.list.setListData(InfoPanel.listData);
		scrollPane.revalidate();
		drawImageOverlay();
		infoPanel.repaint();
		controlPanel.repaint();
	}

	// Inner class for the drawing pane. We make it scrollable but also make it so
	// that it can detect
	// mouse events
	protected class ScrollDrawingPane extends JScrollPane {

		private static final long serialVersionUID = 1L;
		Point startPoint;
		Point startOrigin;

		public ScrollDrawingPane(JLabel imageLabel) {
			super(imageLabel);
			this.getViewport().setBackground(DRAWING_PANEL_COLOR);
			startPoint = new Point(0, 0);

			// Add mouse listeners to the drawing panel
			this.addMouseListener(new MouseHandler());
			this.addMouseMotionListener(new MouseMotionHandler());
			this.addMouseWheelListener(new MouseWheelHandler());
			this.setFocusable(true);
		}

		// Inner class to handle mouse events
		private class MouseHandler extends MouseAdapter {

			@Override
			public void mousePressed(MouseEvent event) {
				// The point slider is only enabled if displaying curves is enabled
				InfoPanel.pointSlider.setEnabled(curves.isCurveSelected());

				// Clicking when the selection tool is enabled will select anything in the
				// region.
				if (toolPanel.isSelected(0)) {
					Curve c;

					// Selecting a control point
					boolean anythingClicked = false;
					for (Curve curve : curves.getSelected()) {
						if (curve.controlPointIndex(mapPoint(event.getPoint()),
								ControlPanel.currentLayerSlider.getValue(), ControlPanel.scaleSlider.getValue() / 100.0,
								true) != -1) {
							anythingClicked = true;
							currEditedCurve = curve;
							controlPointSelected = true;
						}
					}
					if (anythingClicked) {
						return;
					}

					// Selecting a curve
					for (int i = 0; i < curves.size(); i++) {
						if ((c = curves.get(i)).isPointOnCurve(mapPoint(event.getPoint()),
								ControlPanel.currentLayerSlider.getValue(),
								ControlPanel.scaleSlider.getValue() / 100.0)) {
							anythingClicked = true;
							if (!shiftPressed) {
								curves.setAllUnselected();
							}

							// If the curve is still selected, this means that shift must have been pressed.
							// Consequently, clicking the curve once it's already selected while shift is
							// pressed implies de-selection
							if (c.isSelected()) {
								curves.setUnselected(c);
								if (!shiftPressed) {
									InfoPanel.list.clearSelection();
								} else {
									InfoPanel.list.removeSelectionInterval(i, i);
								}
							} // Otherwise, we set the curve to selected
							else {
								curves.setSelected(c);
								InfoPanel.updateHistograms();
								if (!shiftPressed) {
									InfoPanel.list.setSelectedIndex(i);
								} else {
									InfoPanel.list.addSelectionInterval(i, i);
								}
							}
							InfoPanel.curvesList.revalidate();
						}
					}
					if (anythingClicked) {
						return;
					}

					// If a control point wasn't clicked, or a curve wasn't clicked, then we
					// deselect everything (if SHIFT isn't being pressed)
					if (!shiftPressed) {
						curves.setAllUnselected();
						InfoPanel.list.clearSelection();
						InfoPanel.updateHistograms();
					}
				}

				// If the hand mode is enabled, then clicking defines the start point for
				// dragging
				if (toolPanel.isSelected(1)) {
					startPoint = event.getPoint();
					startOrigin = scrollPane.getViewport().getViewPosition();
				}

				// If the control point tool is selected, clicking defines a new control point
				if (toolPanel.isSelected(2)) {
					// Once we start a new curve, any previous curves are not selected anymore
					if (currCtrlPt == 0) {
						curves.setAllUnselected();
						InfoPanel.list.clearSelection();
						InfoPanel.updateHistograms();
						points = new ArrayList<>(DEFAULT_NO_CTRL_PTS);
						MenuBar.delete.setEnabled(true);
					}

					Point2D mappedPoint = mapPoint(event.getPoint());
					double scale = ControlPanel.scaleSlider.getValue() / 100.0;
					points.add(new Point2D.Double(mappedPoint.getX() / scale, mappedPoint.getY() / scale));
					currCtrlPt++;

					// If the input type is a BSpline, then pressing enter will be enabled after the
					// base case.
					if (currCtrlPt == BSpline.B_SPLINE_DEGREE + 1 && inputType == B_SPLINE) {
						MenuBar.enter.setEnabled(true);
					} // The minimum size Bezier Curve we allow is a quadradic bezier curve
					else if (currCtrlPt >= 3 && inputType == BEZIER_CURVE) {
						MenuBar.enter.setEnabled(true);
					}
					infoPanel.repaint();
					drawImageOverlay();
				}
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				if (toolPanel.isSelected(2) && currCtrlPt == 0) {
					controlPanel.repaint();
				}

				// Releasing the mouse deselects the control point
				if (toolPanel.isSelected(0)) {
					if (controlPointSelected) {
						currEditedCurve.resetControlPointSelection();
					}
					controlPointSelected = false;
					drawImageOverlay();
					controlPanel.repaint();
				}
			}
		}

		// Inner Class to handle mouse movements
		private class MouseMotionHandler implements MouseMotionListener {

			@Override
			public void mouseMoved(MouseEvent event) {
				requestFocusInWindow();
				int index;
				if (toolPanel.isSelected(0) && curves.getNoSelected() != 0) {
					for (Curve c : curves.getSelected()) {
						if ((index = c.controlPointIndex(mapPoint(event.getPoint()),
								ControlPanel.currentLayerSlider.getValue(), ControlPanel.scaleSlider.getValue() / 100.0,
								false)) != -1) {
							c.setHoveredControlPoint(index);
							drawImageOverlay();
							return;
						}
					}

					// If none of the control points were hovered over before, there's no need to
					// update the screen.
					// Saves computation time.
					boolean noneHovered = true;
					for (Curve c : curves.getSelected()) {
						if (c.getHoveredControlPoint() != -1) {
							noneHovered = false;
						}
					}
					if (noneHovered) {
						return;
					}

					// If we haven't returned yet, none of the control points are hovered over.
					for (Curve c : curves.getSelected()) {
						c.setHoveredControlPoint(-1);
					}
					infoPanel.repaint();
					drawImageOverlay();
				}
			}

			@Override
			public void mouseDragged(MouseEvent event) {
				if (toolPanel.isSelected(0)) {
					// If the selection tool is enabled, and a control point is selected, dragging
					// moves the control point
					if (controlPointSelected) {
						Point2D newPt = mapPoint(event.getPoint());
						double scale = ControlPanel.scaleSlider.getValue() / 100.0;
						currEditedCurve.addKeyFrame(new Point2D.Double(newPt.getX() / scale, newPt.getY() / scale),
								ControlPanel.currentLayerSlider.getValue());
						InfoPanel.updateHistograms();
						infoPanel.repaint();
						drawImageOverlay();
					}
				}

				// If the hand mode is enabled, dragging pans the viewport.
				if (toolPanel.isSelected(1)) {
					Point currentPoint = event.getPoint();
					JViewport currentPort = scrollPane.getViewport();
					int dx = (int) (currentPoint.x - startPoint.x);
					int dy = (int) (currentPoint.y - startPoint.y);
					int nx = startOrigin.x - dx;
					int ny = startOrigin.y - dy;

					// Bounds the panning to the edges of the image.
					// The maximum viewport origin point is off by the thickness of the scrollbar,
					// so we modify the bounds depending on if the scrollbar is visible or not.
					int mx = 4, my = 4;
					if (scrollPane.getHorizontalScrollBar().isVisible()) {
						mx = SCROLL_BAR_THICKNESS;
					}
					if (scrollPane.getVerticalScrollBar().isVisible()) {
						my = SCROLL_BAR_THICKNESS;
					}
					if (nx > currentPort.getViewSize().width - getWidth() + mx) {
						nx = currentPort.getViewSize().width - getWidth() + mx;
					}
					if (nx < 0) {
						nx = 0;
					}
					if (ny > currentPort.getViewSize().height - getHeight() + my) {
						ny = currentPort.getViewSize().height - getHeight() + my;
					}
					if (ny < 0) {
						ny = 0;
					}

					// Only pans the viewport if the new one is bounded by the image
					scrollPane.getViewport().setViewPosition(new Point(nx, ny));
				}
			}
		}

		private class MouseWheelHandler implements MouseWheelListener {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				double scale = ControlPanel.scaleSlider.getValue() / 100.0;
				if (notches < 0) {
					// If we are at the min scaling increment or lower, we can't zoom out
					if (scale <= ControlPanel.SCALE_INCREMENTS[0]) {
						return;
					}

					// Finds the next smallest scaling increment and sets the scale to that.
					int i = 1;
					while (i < ControlPanel.SCALE_INCREMENTS.length && ControlPanel.SCALE_INCREMENTS[i] < scale) {
						i++;
					}
					ControlPanel.scaleSlider.setValue((int) Math.floor(100.0 * ControlPanel.SCALE_INCREMENTS[--i]));
				} else {
					// If we are at the max scaling increment or higher, we can't zoom in
					if (scale >= ControlPanel.SCALE_INCREMENTS[ControlPanel.SCALE_INCREMENTS.length - 1]) {
						return;
					}

					// Finds the next largest scaling increment and sets the scale to that.
					int i = ControlPanel.SCALE_INCREMENTS.length - 2;
					while (i > 0 && ControlPanel.SCALE_INCREMENTS[i] > scale) {
						i--;
					}
					ControlPanel.scaleSlider.setValue((int) Math.ceil(100.0 * ControlPanel.SCALE_INCREMENTS[++i]));
				}
			}
		}
	}

	public static void setUIFont(javax.swing.plaf.FontUIResource f) {
		java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource) {
				UIManager.put(key, f);
			}
		}
	}

	public static double computeCurvature(double x, double a, double b) {
		// Computes the curvature of a*sin(bx), which is
		// ab^2sin(bx)/(1+a^2b^2cos^2(bx))^(3/2)
		return Math
				.abs((a * b * b * Math.sin(b * x))
						/ (Math.pow((1 + a * a * b * b * Math.cos(b * x) * Math.cos(b * x)), (3 / 2.0))))
				/ Curve.getMicronPixelFactor();
	}

	public static double getCurvatureError(Curve c, double a, double b) {
		int noPoints = 0;
		double totalCurvatureError = 0;

		// Compute the average curvature error for each curve.
		// We ignore the first and last x percent of the sine curve because of end
		// conditions.
		for (BezierPoint p : c.getDigitizedPoints()) {
			if (p.getX() >= currImage.getWidth() * PERCENT_END_CONDITIONS
					&& p.getX() <= currImage.getWidth() * (1 - PERCENT_END_CONDITIONS)) {
				totalCurvatureError += Math.abs(p.k - computeCurvature(p.getX(), a, b));
				noPoints++;
			}
		}
		return totalCurvatureError / (noPoints * 1.0);
	}

	// Computes the Pearson's Correlation Coefficient between a fitted b-spline's
	// curvature and the true curvature
	public static double computePearsonR(Curve c, double a, double b) {
		List<BezierPoint> points = c.getDigitizedPoints();

		// Computes the mean of the b-spline curvature and the mean of the true
		// curvature
		double m1 = 0;
		double m2 = 0;
		for (BezierPoint p : points) {
			m1 += p.k;
			m2 += computeCurvature(p.getX(), a, b);
		}
		m1 /= points.size();
		m2 /= points.size();

		// Computes the covariance of the two and the standard deviation of each
		double covariance = 0;
		double sd1 = 0;
		double sd2 = 0;
		for (BezierPoint p : points) {
			covariance += (p.k - m1) * (computeCurvature(p.getX(), a, b) - m2);
			sd1 += squared(p.k - m1);
			sd2 += squared(computeCurvature(p.getX(), a, b) - m2);
		}
		sd1 = Math.sqrt(sd1);
		sd2 = Math.sqrt(sd2);

		return covariance / (sd1 * sd2);
	}

	private static double squared(double x) {
		return x * x;
	}

	private static void setIntensityThreshold(double sigma) {
		// We set the intensity threshold to 2 standard deviations above the mean image
		// intensity
		// Compute the mean image intensity
		double avgIntensity = 0;
		for (int x = 0; x < currImage.getWidth(); x++) {
			for (int y = 0; y < currImage.getHeight(); y++) {
				avgIntensity += BezierCurve.getRGB(x, y)[0];
			}
		}
		avgIntensity /= (currImage.getWidth() * currImage.getHeight());

		// Compute the intensity standard deviation
		double stdDev = 0;
		for (int x = 0; x < currImage.getWidth(); x++) {
			for (int y = 0; y < currImage.getHeight(); y++) {
				stdDev += squared(BezierCurve.getRGB(x, y)[0] - avgIntensity);
			}
		}
		stdDev /= (currImage.getWidth() * currImage.getHeight()) - 1;
		stdDev = Math.sqrt(stdDev);

		// Set the intensity threshold
		System.out.println("Intensity Threshold: " + (int) (avgIntensity + sigma * stdDev));
		InfoPanel.dataThresholdSlider.setValue(Math.min((int) (avgIntensity + sigma * stdDev), 256 - 1));
	}

	public static void testingScript() throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter("test-data/dataSNR.csv"));
		PrintWriter out2 = new PrintWriter(new FileWriter("test-data/datamax.csv"));
		PrintWriter out3 = new PrintWriter(new FileWriter("test-data/dataPixelSize.csv"));

		// We set the parameters for fitting here. The algorithm used (PDM), and the
		// data threshold radius.
		InfoPanel.dataThresholdSlider.setValue(128);
		InfoPanel.fittingComboBox.setSelectedIndex(DEFAULT_FITTING_ALGORITHM);
		InfoPanel.thresholdRadiusSpinner.setValue(15);
		fittingAlgorithm = FITTING_ALGORITHMS[InfoPanel.fittingComboBox.getSelectedIndex()];
		double a, b;

		// Testing accuracy versus Signal to Noise Ratio
		for (int snr = 0; snr <= 25; snr++) {
			// Opens each image
			MenuBar.openImageFile(new File("test-data/output_snr_" + snr + ".tif"));

			// Computes the intensity threshold, as a given number of standard deviations
			// from the mean
			setIntensityThreshold(2);

			// Amplitude and Stretch Factor for the sine curves.
			a = 6000 / (Curve.getMicronPixelFactor() * 1000.0);
			b = (2 * Math.PI) / currImage.getWidth();

			System.out.println("SNR: " + snr);
			MenuBar.loadCurveFile(new File("test-curves/newtestcurves.kapp"));

			// Scale the testing curves so that they span the entire image.
			// The testing curves were originally drawn on a 82px x 82px image, so that's
			// the reference for our scaling.
			for (Curve c : curves) {
				c.scale(currImage.getWidth() / 82.0, ControlPanel.currentLayerSlider.getValue());
			}

			curves.setAllSelected();
			fitCurves();

			// The amplitude of the sine wave is 6000 nm. We adjust for the pixel size.
			// The stretch factor is such that one period is undergone across the image.
			for (Curve c : curves) {
				out.println(snr + "," + c.getNoCtrlPts() + "," + getCurvatureError(c, a, b) + ","
						+ computePearsonR(c, a, b));
			}

			// We export the peak curvature at each half of the image, to compare how the
			// fitting algorithm
			// performs with maximal curvatures
			for (Curve c : curves) {
				out2.println(snr + "," + c.getNoCtrlPts() + "," + Math.abs(c.getMaximum(0, currImage.getWidth() / 2.0)
						- computeCurvature(currImage.getWidth() / 4.0, a, b)));
				out2.println(snr + "," + c.getNoCtrlPts() + ","
						+ Math.abs(c.getMaximum(currImage.getWidth() / 2.0, currImage.getWidth())
								- computeCurvature((3 * currImage.getWidth()) / 4.0, a, b)));
			}
		}

		// Testing accuracy versus pixel size
		for (int nmPerPixel = 100; nmPerPixel <= 400; nmPerPixel += 10) {
			MenuBar.openImageFile(new File(String.format("test-data/output_size_%1.2f.tif", nmPerPixel / 1000.0)));

			// Computes the intensity threshold, as a given number of standard deviations
			// from the mean
			setIntensityThreshold(2);

			// Set the Micron Pixel Factor
			Curve.setMicronPixelFactor(nmPerPixel / 1000.0);
			System.out.println("Micron Pixel Factor: " + nmPerPixel / 1000.0);

			// Set the Threshold Radius Depending on how big the pixel size is.
			// We always set it to the equivalent of 2 micrometres.
			InfoPanel.thresholdRadiusSpinner.setValue((int) (2 / Curve.getMicronPixelFactor()));
			System.out.println("Threshold Radius: " + (int) (2 / Curve.getMicronPixelFactor()));

			// Amplitude and Stretch Factor for the sine curves.
			a = 6000 / (Curve.getMicronPixelFactor() * 1000.0);
			b = (2 * Math.PI) / currImage.getWidth();

			// Scale the testing curves so that they span the entire image.
			// The testing curves were originally drawn on a 82px x 82px image, so that's
			// the reference for our scaling.
			MenuBar.loadCurveFile(new File("test-curves/newtestcurves.kapp"));
			for (Curve c : curves) {
				c.scale(currImage.getWidth() / 82.0, ControlPanel.currentLayerSlider.getValue());
			}

			curves.setAllSelected();
			fitCurves();
			for (Curve c : curves) {
				out3.println(nmPerPixel / 1000.0 + "," + c.getNoCtrlPts() + "," + getCurvatureError(c, a, b) + ","
						+ computePearsonR(c, a, b));
			}
		}
		out.close();
		out2.close();
		out3.close();
	}

}
