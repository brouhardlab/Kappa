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
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
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
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import ij.ImagePlus;
import ij.ImageStack;
import sc.fiji.kappa.curve.BSpline;
import sc.fiji.kappa.curve.BezierGroup;
import sc.fiji.kappa.curve.BezierPoint;
import sc.fiji.kappa.curve.Curve;

public class KappaFrame extends JFrame {
	// Debugging Constants

	private static final long serialVersionUID = 1L;
	public static final boolean DEBUG_MODE = false;

	// Whether we allow for control point adjustment or not
	public static final boolean DEFAULT_CTRL_PT_ADJUSTMENT = false;
	private boolean enableCtrlPtAdjustment = DEFAULT_CTRL_PT_ADJUSTMENT;

	// Application Constants
	public static final String APPLICATION_NAME = "Kappa";
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

	private int bsplineType;
	private int inputType;

	// 0 = Point Distance Minimization
	// 1 = Squared Distance Minimization
	public static final int DEFAULT_FITTING_ALGORITHM = 0;
	private String fittingAlgorithm;

	// The global percent increase in error we allow to simplify the fitted curve.
	private double globalThreshold = 0.04;
	private double localThreshold = 0.05;

	private static final int DEFAULT_NUMBER_POINTS = 1000;

	private Overlay overlay;

	// Bezier Curve information
	private BezierGroup curves = new BezierGroup(this);
	private List<Point2D> points = new ArrayList<>(DEFAULT_NO_CTRL_PTS);
	private Curve currEditedCurve;
	private int currCtrlPt = 0;
	private boolean controlPointSelected;
	private boolean shiftPressed;
	private boolean dragged;
	private boolean fittingRunning;
	private int prevIndex;

	private final int INIT_LAYER = 1;
	private int maxLayer;
	private int maxLayerDigits;

	// Image variables
	private ImagePlus displayedImageStack;
	private ImagePlus imageStack;
	private ImageStack[] imageStackLayers;
	private BufferedImage currImage;
	private BufferedImage scaled;
	private JLabel currImageLabel;
	private boolean[][] thresholded;
	private ScrollDrawingPane scrollPane;
	private double baseStrokeThickness = Curve.DEFAULT_STROKE_THICKNESS;
	private BufferedImage combined;

	// Panels
	private InfoPanel infoPanel;
	private ControlPanel controlPanel;
	private ToolPanel toolPanel;
	private KappaMenuBar kappaMenubar;

	@Parameter
	private Context context;

	public KappaFrame(Context context) {

		// Set up the original frame
		super(APPLICATION_NAME);

		context.inject(this);

		setSize(APP_DEFAULT_WIDTH, APP_DEFAULT_HEIGHT);
		setLocation(APP_DEFAULT_X, APP_DEFAULT_Y);

		setLayout(new BorderLayout());
		setInfoPanel(new InfoPanel(this));
		setControlPanel(new ControlPanel(this));
		setToolPanel(new ToolPanel(this));
		add(getInfoPanel(), BorderLayout.EAST);
		add(getControlPanel(), BorderLayout.SOUTH);
		add(getToolPanel(), BorderLayout.NORTH);

		// Sets the glass pane up for notifications
		setOverlay(new Overlay(this));
		this.setGlassPane(getOverlay());
		getOverlay().setOpaque(false);

		// Default Curve input
		setInputType(DEFAULT_INPUT_CURVE);
		setBsplineType(BSpline.DEFAULT_BSPLINE_TYPE);
		setFittingAlgorithm(FITTING_ALGORITHMS[DEFAULT_FITTING_ALGORITHM]);

		// We define the currentImage as a label so the centering and scaling can be
		// done by the layout manager
		setCurrImageLabel(new JLabel());
		getCurrImageLabel().setHorizontalAlignment(SwingConstants.CENTER);

		// We add the JScrollPane containing the desired Image
		setScrollPane(new ScrollDrawingPane(getCurrImageLabel()));
		getScrollPane().setVisible(true);
		add(getScrollPane());

		// Key Bindings for the Hand Tool
		getScrollPane().getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "space pressed");
		getScrollPane().getInputMap().put(KeyStroke.getKeyStroke("released SPACE"), "space released");
		getScrollPane().getActionMap().put("space pressed", (new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (!getToolPanel().isEnabled(1) || getToolPanel().isSelected(1)) {
					return;
				}
				setPrevIndex(0);
				while (!getToolPanel().isSelected(getPrevIndex())) {
					setPrevIndex(getPrevIndex() + 1);
				}
				getToolPanel().setSelected(1, true);
				getScrollPane().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
		}));
		getScrollPane().getActionMap().put("space released", (new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (!getToolPanel().isEnabled(1)) {
					return;
				}
				getToolPanel().setSelected(getPrevIndex(), true);
				getScrollPane().setCursor(ToolPanel.TOOL_CURSORS[getPrevIndex()]);
			}
		}));

		// Key Bindings for the SHIFT key
		getScrollPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,
			InputEvent.SHIFT_DOWN_MASK), "shift pressed");
		getScrollPane().getInputMap().put(KeyStroke.getKeyStroke("released SHIFT"), "shift released");
		getScrollPane().getActionMap().put("shift pressed", (new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				setShiftPressed(true);
			}
		}));
		getScrollPane().getActionMap().put("shift released", (new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				setShiftPressed(false);
			}
		}));
		setImageStack(null);

		// Adds the menubar
		this.setKappaMenubar(new KappaMenuBar(context, this));
		this.setJMenuBar(this.getKappaMenubar());

		this.setFocusable(true);
		this.requestFocusInWindow();
	}

	public InfoPanel getInfoPanel() {
		return infoPanel;
	}

	public ControlPanel getControlPanel() {
		return controlPanel;
	}

	public ToolPanel getToolPanel() {
		return toolPanel;
	}

	public Overlay getOverlay() {
		return overlay;
	}

	public BezierGroup getCurves() {
		return curves;
	}

	public List<Point2D> getPoints() {
		return points;
	}

	public ImagePlus getDisplayedImageStack() {
		return displayedImageStack;
	}

	public ImagePlus getImageStack() {
		return imageStack;
	}

	public BufferedImage getCurrImage() {
		return currImage;
	}

	public JLabel getCurrImageLabel() {
		return currImageLabel;
	}

	public KappaMenuBar getKappaMenubar() {
		return kappaMenubar;
	}

	public void fitCurves() {
		// If no curves are selected, no fitting is done
		if (getCurves().getNoSelected() == 0) {
			return;
		}

		// Shows that the fitting algorithm is running
		setFittingRunning(true);
		getOverlay().setVisible(true);

		// We draw an overlay without a built in delay because we turn it off
		// ourselves.
		// Hence the
		// delay interval is -1 by convention.
		getOverlay().drawNotification("Fitting in Progress...", getScrollPane().getVisibleRect(), -1);

		// We fit every selected B-Spline.
		for (Curve c : getCurves().getSelected()) {
			if (c instanceof BSpline) {
				// Performs curve fitting with the current B-Spline
				double error = Double.MAX_VALUE;
				double oldError;
				List<Point2D> dataPoints;
				List<Double> weights;

				// Sets the x and y coordinate to (x-1, y-1), because the image is
				// zero-indexed
				// in java,
				// we want to 'de-shift' it when we fit the curve
				c.deshiftControlPoints(this.getControlPanel().getCurrentLayerSlider().getValue());

				// If the b-spline is closed, we convert it to an open curve for
				// fitting.
				// Converts it to an open B-Spline for fitting if it was originally a
				// closed
				// spline
				boolean wasOpen = ((BSpline) c).isOpen();
				if (!((BSpline) c).isOpen()) {
					((BSpline) c).convertToOpen(this.getControlPanel().getCurrentLayerSlider().getValue());
				}
				do {
					oldError = error;

					// Checks to make sure that some data points are there
					dataPoints = c.getThresholdedPixels();

					weights = getWeights(dataPoints);
					error = ((BSpline) c).fittingIteration(dataPoints, weights, this.getControlPanel()
						.getCurrentLayerSlider().getValue());
				}
				while (oldError > error);
				error = oldError;

				// Once the fitting has been done, we remove unnecessary control points.
				if (isEnableCtrlPtAdjustment()) {
					((BSpline) c).adjustControlPoints(dataPoints, weights, this.getControlPanel()
						.getCurrentLayerSlider().getValue());
				}
				if (!wasOpen) {
					((BSpline) c).convertToClosed(this.getControlPanel().getCurrentLayerSlider().getValue());
				}

				// Sets the x and y coordinate to (x+1, y+1), because the image is
				// zero-indexed
				// in java, so there's a 1 pixel offset
				c.shiftControlPoints(this.getControlPanel().getCurrentLayerSlider().getValue());

				// Updates curve display
				getInfoPanel().repaint();
				drawImageOverlay();
				this.getInfoPanel().updateHistograms();
			}
		}

		// Shows that the execution has stopped
		setFittingRunning(false);
		getOverlay().setVisible(false);
	}

	/**
	 * Resets the set of curves and the corresponding list
	 */
	public void resetCurves() {
		getInfoPanel().setListData(new Vector<>());
		getInfoPanel().getList().setListData(getInfoPanel().getListData());
		setCurves(new BezierGroup(this));
	}

	/**
	 * Modifies a composite image that is both scaled and has all Bezier Curves
	 * drawn onto it
	 *
	 * @param scale The scale factor to scale the image by
	 */
	protected void setScaledImage(double scale) {
		if (getCurrImage() == null) {
			return;
		}

		// Optimizes the image type for drawing onto the screen
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = env.getDefaultScreenDevice();
		GraphicsConfiguration config = device.getDefaultConfiguration();

		int w = (int) (scale * getCurrImage().getWidth());
		int h = (int) (scale * getCurrImage().getHeight());
		setScaled(config.createCompatibleImage(w, h, BufferedImage.TYPE_INT_RGB));
		Graphics2D g2 = getScaled().createGraphics();
		if (getKappaMenubar().getAntialiasingMenu().getState()) {
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		}
		g2.drawImage(getCurrImage(), 0, 0, w, h, null);

		// Draws the thresholded pixels on top
		if (getInfoPanel().getBgCheckBox().isSelected()) {
			g2.setColor(Color.ORANGE);
			for (int i = 0; i < getCurrImage().getWidth(); i++) {
				for (int j = 0; j < getCurrImage().getHeight(); j++) {
					if (getThresholded()[i][j]) {
						g2.fillRect((int) Math.round(i * scale), (int) Math.round(j * scale), (int) Math.round(
							scale), (int) Math.round(scale));
					}
				}
			}
		}
		g2.dispose();
	}

	/**
	 * Draws everything on top of the scaled image
	 */
	public void drawImageOverlay() {
		if (getCurrImage() == null) {
			return;
		}
		double scale = this.getControlPanel().getScaleSlider().getValue() / 100.0;
		this.combined = new BufferedImage(getScaled().getWidth(), getScaled().getHeight(),
			BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = (Graphics2D) combined.getGraphics();
		g2.drawImage(getScaled(), 0, 0, null);

		// Draws the data threshold pixels on top
		if (getInfoPanel().getShowDatapointsCheckBox().isSelected()) {
			for (Curve c : getCurves()) {
				c.drawThresholdedPixels(g2, scale);
			}
		}

		// Draws all the Bezier Curves
		g2.setColor(Color.GRAY);
		double strokeThickness = getStrokeThickness(scale);
		g2.setStroke(new BasicStroke((int) strokeThickness));

		int currentPoint = getInfoPanel().getPointSlider().getValue();
		if (curves.getSelected().length >= 1) {
			if (currentPoint > curves.getSelected()[0].getNoPoints()) {
				currentPoint = 0;
			}
		}

		getCurves().draw(g2, scale, currentPoint, getKappaMenubar().getBoundingBoxMenu().getState(),
			getKappaMenubar().getScaleCurvesMenu().getState(), getKappaMenubar().getTangentMenu()
				.getState(), getInfoPanel().getShowRadiusCheckBox().isSelected());

		// Draws the points we've built so far if a complete Bezier Curve has not
		// been
		// formed
		if (getCurrCtrlPt() != 0) {
			g2.setColor(Color.GRAY);
			for (int i = 0; i < getCurrCtrlPt() - 1; i++) {
				g2.drawLine((int) (getPoints().get(i).getX() * scale), (int) (getPoints().get(i).getY() *
					scale), (int) (getPoints().get(i + 1).getX() * scale), (int) (getPoints().get(i + 1)
						.getY() * scale));
			}

			// If it's a closed B-Spline, then we close the polygon.
			if (getInputType() == B_SPLINE && getBsplineType() == BSpline.CLOSED) {
				g2.drawLine((int) (getPoints().get(0).getX() * scale), (int) (getPoints().get(0).getY() *
					scale), (int) (getPoints().get(getPoints().size() - 1).getX() * scale), (int) (getPoints()
						.get(getPoints().size() - 1).getY() * scale));
			}

			g2.setColor(Curve.CTRL_PT_COLOR);
			for (int i = 0; i < getCurrCtrlPt(); i++) {
				g2.fillRect((int) ((getPoints().get(i).getX() - this.getCtrlPointSize()) * scale),
					(int) ((getPoints().get(i).getY() - this.getCtrlPointSize()) * scale), (int) (2 * this
						.getCtrlPointSize() * scale), (int) (2 * this.getCtrlPointSize() * scale));
			}
		}

		getCurrImageLabel().setIcon(new ImageIcon(combined));
	}

	protected void setLayer(int layer, double scale) {
		// If there is an open image stack, it will draw it in the drawing panel
		// Also changes the frame for our bezier curves, for keyframing.
		setFrame(layer);
		setCurrImage(this.getDisplayedImageStack().getBufferedImage());
		setScaledImage(scale);
		getCurves().changeFrame(layer);

		// Updates histograms and background thresholds
		updateDisplayed();
	}

	protected void setDisplayedChannels(boolean showRed, boolean showGreen, boolean showBlue) {
		this.getInfoPanel().setHistogramVisibility(showRed, showGreen, showBlue);
		setScaledImage(this.getControlPanel().getScaleSlider().getValue() / 100.0);
		getInfoPanel().repaint();
		drawImageOverlay();
	}

	protected void updateThresholded() {
		// Update thresholded level
		int thresholdLevel = getInfoPanel().getThresholdSlider().getValue();
		int[] rgb;
		int channel, intensity;
		channel = getInfoPanel().getThresholdChannelsComboBox().getSelectedIndex();
		ImageUtils imgUtils = new ImageUtils<>();
		for (int i = 0; i < getCurrImage().getWidth(); i++) {
			for (int j = 0; j < getCurrImage().getHeight(); j++) {

				// Checks the intensity level and compares it to the threshold level
				rgb = imgUtils.getPixels(getDisplayedImageStack(), i, j);
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

				getThresholded()[i][j] = (intensity < thresholdLevel);
			}
		}
		setScaledImage(this.getControlPanel().getScaleSlider().getValue() / 100.0);
		drawImageOverlay();
	}

	/**
	 * Takes an (x, y) coordinate and translates it into the equivalent (x, y)
	 * coordinate for the *scaled* image with respect to the position of the image
	 * (ie. the top left corner of the image will correspond to (0,0)
	 *
	 * @param p The original (x,y) point
	 * @return The translated (x,y) point
	 */
	private Point mapPoint(Point p) {
		Point ref = getScrollPane().getViewport().getViewPosition();

		// If both scrollbars are visible, then the we want to translate p by the
		// coordinates of the viewpoint origin
		// 3 px is for the border for the scrollPane
		if (getScrollPane().getHorizontalScrollBar().isVisible() && getScrollPane()
			.getVerticalScrollBar().isVisible())
		{
			return new Point(p.x + ref.x - 3, p.y + ref.y - 3);
		} // If only one of the scrollbars are visible, then only one of the
			// coordinates
			// will directly correspond.
			// If none are visible, none will directly correspond, and we have to
			// obtain
			// these coordinates directly,
			// using the the width and height of the bounding box and the image.
		else if (getScrollPane().getVerticalScrollBar().isVisible()) {
			return new Point(p.x - (getCurrImageLabel().getWidth() - (int) (this.getControlPanel()
				.getScaleSlider().getValue() / 100.0 * getCurrImage().getWidth())) / 2 - 3, p.y + ref.y -
					3);
		}
		else if (getScrollPane().getHorizontalScrollBar().isVisible()) {
			return new Point(p.x + ref.x - 3, p.y - (getCurrImageLabel().getHeight() - (int) (this
				.getControlPanel().getScaleSlider().getValue() / 100.0 * getCurrImage().getHeight())) / 2 -
				3);
		}
		return new Point(p.x - (getCurrImageLabel().getWidth() - (int) (this.getControlPanel()
			.getScaleSlider().getValue() / 100.0 * getCurrImage().getWidth())) / 2 - 3, p.y -
				(getCurrImageLabel().getHeight() - (int) (this.getControlPanel().getScaleSlider()
					.getValue() / 100.0 * getCurrImage().getHeight())) / 2 - 3);
	}

	/**
	 * Takes an input number and returns the number with the given number of
	 * digits (or more). In other words, this will prepend '0's until the number
	 * is the required number of digits If it is already has more digits, this
	 * does nothing.
	 *
	 * @param number The input number
	 * @param noDigits The desired number of digits
	 * @return The number modified so that it has at least the desired number of
	 *         digits.
	 */
	public String formatNumber(int number, int noDigits) {
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
	 * @param dataPoints The data points, in an ArrayList with n elements
	 * @return An ArrayList with n elements with corresponding weight values.
	 */
	private List<Double> getWeights(List<Point2D> dataPoints) {
		List<Double> weights = new ArrayList<>(dataPoints.size());
		ImageUtils imgUtils = new ImageUtils<>();
		for (Point2D p : dataPoints) {
			int[] rgb = imgUtils.getPixels(getDisplayedImageStack(), (int) p.getX(), (int) p.getY());

			int channel = getInfoPanel().getFittingChannelsComboBox().getSelectedIndex();
			double intensity = 0;
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
				case 3:
					intensity = (rgb[0] + rgb[1] + rgb[2]) / 3;
			}

			// We want the higher weights to be for the darker pixels (with lower
			// intensities) when
			// we're looking for darker pixels in the image. Hence we adjust the
			// weights
			// here
			if (getInfoPanel().getDataRangeComboBox().getSelectedIndex() == 1) {
				if (getDisplayedImageStack().getBitDepth() == 24) // RGB Colour
				{
					intensity = 256 - intensity;
				}
				else // Grayscale, then 2^bitdepth is the max intensity.
				{
					intensity = (int) (Math.pow(2, getDisplayedImageStack().getBitDepth()) - intensity);
				}
			}
			weights.add(intensity);
		}
		return weights;
	}

	protected void updateDisplayed() {
		this.updateDisplayed(true);
	}

	protected void updateDisplayed(boolean updateHistogram) {
		// Updates the background thresholding display
		if (getInfoPanel().getBgCheckBox().isSelected()) {
			updateThresholded();
		}

		// Updates the image
		drawImageOverlay();
		if (updateHistogram) {
			this.getInfoPanel().updateHistograms();
		}
	}

	protected void enterCurve() {
		// Enters a new Bezier Curve or B-Spline when the user presses ENTER
		if (getInputType() == B_SPLINE) {
			getCurves().addCurve(getPoints(), this.getControlPanel().getCurrentLayerSlider().getValue(),
				getCurrCtrlPt(), B_SPLINE, (getBsplineType() == BSpline.OPEN), (Integer) (getInfoPanel()
					.getThresholdRadiusSpinner().getValue()));
		}
		else {
			getCurves().addCurve(getPoints(), this.getControlPanel().getCurrentLayerSlider().getValue(),
				getCurrCtrlPt(), BEZIER_CURVE, true, (Integer) (getInfoPanel().getThresholdRadiusSpinner()
					.getValue()));
		}
		this.getInfoPanel().updateHistograms();

		// Updates our list after adding the curve
		getInfoPanel().getListData().addElement("  CURVE " + getCurves().getCount());
		getInfoPanel().getList().setListData(getInfoPanel().getListData());
		getInfoPanel().getList().setSelectedIndex(getCurves().size() - 1);
		getInfoPanel().getCurvesList().revalidate();
		getInfoPanel().getPointSlider().setEnabled(true);
		getInfoPanel().getPointSlider().setValue(0);
		setCurrCtrlPt(0);
		getKappaMenubar().getEnter().setEnabled(false);
		drawImageOverlay();
	}

	protected void deleteCurve() {
		// Deletes a curve when the user presses DELETE
		// Deletes any control points not formed into a curve
		if (getCurrCtrlPt() != 0) {
			setCurrCtrlPt(0);
		}

		// We go down the list of indices so that the array indices don't get
		// changed
		// when we remove elements
		if (getInfoPanel().getList().isSelectionEmpty()) {
			drawImageOverlay();
			return;
		}
		int[] selectedIndices = getInfoPanel().getList().getSelectedIndices();
		for (int i = selectedIndices.length - 1; i >= 0; i--) {
			getInfoPanel().getListData().removeElementAt(selectedIndices[i]);
			getCurves().remove(selectedIndices[i]);
		}
		getInfoPanel().getList().setListData(getInfoPanel().getListData());
		getScrollPane().revalidate();
		drawImageOverlay();
		getInfoPanel().repaint();
		getControlPanel().repaint();
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
				getInfoPanel().getPointSlider().setEnabled(getCurves().isCurveSelected());

				// Clicking when the selection tool is enabled will select anything in
				// the
				// region.
				if (getToolPanel().isSelected(0)) {
					Curve c;

					// Selecting a control point
					boolean anythingClicked = false;
					for (Curve curve : getCurves().getSelected()) {
						if (curve.controlPointIndex(mapPoint(event.getPoint()), getControlPanel()
							.getCurrentLayerSlider().getValue(), getControlPanel().getScaleSlider().getValue() /
								100.0, true) != -1)
						{
							anythingClicked = true;
							setCurrEditedCurve(curve);
							setControlPointSelected(true);
						}
					}
					if (anythingClicked) {
						return;
					}

					// Selecting a curve
					for (int i = 0; i < getCurves().size(); i++) {
						if ((c = getCurves().get(i)).isPointOnCurve(mapPoint(event.getPoint()),
							getControlPanel().getCurrentLayerSlider().getValue(), getControlPanel()
								.getScaleSlider().getValue() / 100.0))
						{
							anythingClicked = true;
							if (!isShiftPressed()) {
								getCurves().setAllUnselected();
							}

							// If the curve is still selected, this means that shift must have
							// been pressed.
							// Consequently, clicking the curve once it's already selected
							// while shift is
							// pressed implies de-selection
							if (c.isSelected()) {
								getCurves().setUnselected(c);
								if (!isShiftPressed()) {
									getInfoPanel().getList().clearSelection();
								}
								else {
									getInfoPanel().getList().removeSelectionInterval(i, i);
								}
							} // Otherwise, we set the curve to selected
							else {
								getCurves().setSelected(c);
								getInfoPanel().updateHistograms();
								if (!isShiftPressed()) {
									getInfoPanel().getList().setSelectedIndex(i);
								}
								else {
									getInfoPanel().getList().addSelectionInterval(i, i);
								}
							}
							getInfoPanel().getCurvesList().revalidate();
						}
					}
					if (anythingClicked) {
						return;
					}

					// If a control point wasn't clicked, or a curve wasn't clicked, then
					// we
					// deselect everything (if SHIFT isn't being pressed)
					if (!isShiftPressed()) {
						getCurves().setAllUnselected();
						getInfoPanel().getList().clearSelection();
						getInfoPanel().updateHistograms();
					}
				}

				// If the hand mode is enabled, then clicking defines the start point
				// for
				// dragging
				if (getToolPanel().isSelected(1)) {
					startPoint = event.getPoint();
					startOrigin = getScrollPane().getViewport().getViewPosition();
				}

				// If the control point tool is selected, clicking defines a new control
				// point
				if (getToolPanel().isSelected(2)) {
					// Once we start a new curve, any previous curves are not selected
					// anymore
					if (getCurrCtrlPt() == 0) {
						getCurves().setAllUnselected();
						getInfoPanel().getList().clearSelection();
						getInfoPanel().updateHistograms();
						setPoints(new ArrayList<>(DEFAULT_NO_CTRL_PTS));
						getKappaMenubar().getDelete().setEnabled(true);
					}

					Point2D mappedPoint = mapPoint(event.getPoint());
					double scale = getControlPanel().getScaleSlider().getValue() / 100.0;
					getPoints().add(new Point2D.Double(mappedPoint.getX() / scale, mappedPoint.getY() /
						scale));
					setCurrCtrlPt(getCurrCtrlPt() + 1);

					// If the input type is a BSpline, then pressing enter will be enabled
					// after the
					// base case.
					if (getCurrCtrlPt() == BSpline.B_SPLINE_DEGREE + 1 && getInputType() == B_SPLINE) {
						getKappaMenubar().getEnter().setEnabled(true);
					} // The minimum size Bezier Curve we allow is a quadradic bezier
						// curve
					else if (getCurrCtrlPt() >= 3 && getInputType() == BEZIER_CURVE) {
						getKappaMenubar().getEnter().setEnabled(true);
					}
					getInfoPanel().repaint();
					drawImageOverlay();
				}
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				if (getToolPanel().isSelected(2) && getCurrCtrlPt() == 0) {
					getControlPanel().repaint();
				}

				// Releasing the mouse deselects the control point
				if (getToolPanel().isSelected(0)) {
					if (isControlPointSelected()) {
						getCurrEditedCurve().resetControlPointSelection();
					}
					setControlPointSelected(false);
					drawImageOverlay();
					getControlPanel().repaint();
				}
			}
		}

		// Inner Class to handle mouse movements
		private class MouseMotionHandler implements MouseMotionListener {

			@Override
			public void mouseMoved(MouseEvent event) {
				requestFocusInWindow();
				int index;
				if (getToolPanel().isSelected(0) && getCurves().getNoSelected() != 0) {
					for (Curve c : getCurves().getSelected()) {
						if ((index = c.controlPointIndex(mapPoint(event.getPoint()), getControlPanel()
							.getCurrentLayerSlider().getValue(), getControlPanel().getScaleSlider().getValue() /
								100.0, false)) != -1)
						{
							c.setHoveredControlPoint(index);
							drawImageOverlay();
							return;
						}
					}

					// If none of the control points were hovered over before, there's no
					// need to
					// update the screen.
					// Saves computation time.
					boolean noneHovered = true;
					for (Curve c : getCurves().getSelected()) {
						if (c.getHoveredControlPoint() != -1) {
							noneHovered = false;
						}
					}
					if (noneHovered) {
						return;
					}

					// If we haven't returned yet, none of the control points are hovered
					// over.
					for (Curve c : getCurves().getSelected()) {
						c.setHoveredControlPoint(-1);
					}
					getInfoPanel().repaint();
					drawImageOverlay();
				}
			}

			@Override
			public void mouseDragged(MouseEvent event) {
				if (getToolPanel().isSelected(0)) {
					// If the selection tool is enabled, and a control point is selected,
					// dragging
					// moves the control point
					if (isControlPointSelected()) {
						Point2D newPt = mapPoint(event.getPoint());
						double scale = getControlPanel().getScaleSlider().getValue() / 100.0;
						getCurrEditedCurve().addKeyFrame(new Point2D.Double(newPt.getX() / scale, newPt.getY() /
							scale), getControlPanel().getCurrentLayerSlider().getValue());
						getInfoPanel().updateHistograms();
						getInfoPanel().repaint();
						drawImageOverlay();
					}
				}

				// If the hand mode is enabled, dragging pans the viewport.
				if (getToolPanel().isSelected(1)) {
					Point currentPoint = event.getPoint();
					JViewport currentPort = getScrollPane().getViewport();
					int dx = (int) (currentPoint.x - startPoint.x);
					int dy = (int) (currentPoint.y - startPoint.y);
					int nx = startOrigin.x - dx;
					int ny = startOrigin.y - dy;

					// Bounds the panning to the edges of the image.
					// The maximum viewport origin point is off by the thickness of the
					// scrollbar,
					// so we modify the bounds depending on if the scrollbar is visible or
					// not.
					int mx = 4, my = 4;
					if (getScrollPane().getHorizontalScrollBar().isVisible()) {
						mx = SCROLL_BAR_THICKNESS;
					}
					if (getScrollPane().getVerticalScrollBar().isVisible()) {
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
					getScrollPane().getViewport().setViewPosition(new Point(nx, ny));
				}
			}
		}

		private class MouseWheelHandler implements MouseWheelListener {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				double scale = getControlPanel().getScaleSlider().getValue() / 100.0;
				if (notches < 0) {
					getControlPanel();
					// If we are at the min scaling increment or lower, we can't zoom out
					if (scale <= ControlPanel.SCALE_INCREMENTS[0]) {
						return;
					}

					// Finds the next smallest scaling increment and sets the scale to
					// that.
					int i = 1;
					getControlPanel();
					getControlPanel();
					while (i < ControlPanel.SCALE_INCREMENTS.length &&
						ControlPanel.SCALE_INCREMENTS[i] < scale)
					{
						i++;
					}
					getControlPanel();
					getControlPanel().getScaleSlider().setValue((int) Math.floor(100.0 *
						ControlPanel.SCALE_INCREMENTS[--i]));
				}
				else {
					getControlPanel();
					getControlPanel();
					// If we are at the max scaling increment or higher, we can't zoom in
					if (scale >= ControlPanel.SCALE_INCREMENTS[ControlPanel.SCALE_INCREMENTS.length - 1]) {
						return;
					}

					getControlPanel();
					// Finds the next largest scaling increment and sets the scale to
					// that.
					int i = ControlPanel.SCALE_INCREMENTS.length - 2;
					getControlPanel();
					while (i > 0 && ControlPanel.SCALE_INCREMENTS[i] > scale) {
						i--;
					}
					getControlPanel();
					getControlPanel().getScaleSlider().setValue((int) Math.ceil(100.0 *
						ControlPanel.SCALE_INCREMENTS[++i]));
				}
			}
		}
	}

	public void setUIFont(javax.swing.plaf.FontUIResource f) {
		java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource) {
				UIManager.put(key, f);
			}
		}
	}

	public double computeCurvature(double x, double a, double b) {
		// Computes the curvature of a*sin(bx), which is
		// ab^2sin(bx)/(1+a^2b^2cos^2(bx))^(3/2)
		return Math.abs((a * b * b * Math.sin(b * x)) / (Math.pow((1 + a * a * b * b * Math.cos(b * x) *
			Math.cos(b * x)), (3 / 2.0)))) / Curve.getMicronPixelFactor();
	}

	public double getCurvatureError(Curve c, double a, double b) {
		int noPoints = 0;
		double totalCurvatureError = 0;

		// Compute the average curvature error for each curve.
		// We ignore the first and last x percent of the sine curve because of end
		// conditions.
		for (BezierPoint p : c.getDigitizedPoints()) {
			if (p.getX() >= getCurrImage().getWidth() * PERCENT_END_CONDITIONS && p
				.getX() <= getCurrImage().getWidth() * (1 - PERCENT_END_CONDITIONS))
			{
				totalCurvatureError += Math.abs(p.k - computeCurvature(p.getX(), a, b));
				noPoints++;
			}
		}
		return totalCurvatureError / (noPoints * 1.0);
	}

	// Computes the Pearson's Correlation Coefficient between a fitted b-spline's
	// curvature and the true curvature
	public double computePearsonR(Curve c, double a, double b) {
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

	private double squared(double x) {
		return x * x;
	}

	private void setIntensityThreshold(double sigma) {
		// We set the intensity threshold to 2 standard deviations above the mean
		// image
		// intensity
		// Compute the mean image intensity

		ImageUtils imgUtils = new ImageUtils<>();

		double avgIntensity = 0;
		for (int x = 0; x < getCurrImage().getWidth(); x++) {
			for (int y = 0; y < getCurrImage().getHeight(); y++) {
				avgIntensity += imgUtils.getPixels(getDisplayedImageStack(), x, y)[0];
			}
		}
		avgIntensity /= (getCurrImage().getWidth() * getCurrImage().getHeight());

		// Compute the intensity standard deviation
		double stdDev = 0;
		for (int x = 0; x < getCurrImage().getWidth(); x++) {
			for (int y = 0; y < getCurrImage().getHeight(); y++) {
				stdDev += squared(imgUtils.getPixels(getDisplayedImageStack(), x, y)[0] - avgIntensity);
			}
		}
		stdDev /= (getCurrImage().getWidth() * getCurrImage().getHeight()) - 1;
		stdDev = Math.sqrt(stdDev);

		// Set the intensity threshold
		System.out.println("Intensity Threshold: " + (int) (avgIntensity + sigma * stdDev));
		getInfoPanel().getDataThresholdSlider().setValue(Math.min((int) (avgIntensity + sigma * stdDev),
			256 - 1));
	}

	public void setDisplayedImageStack(ImagePlus displayedImageStack) {
		this.displayedImageStack = displayedImageStack;
	}

	public void setImageStack(ImagePlus imageStack) {
		this.imageStack = imageStack;
	}

	public ImageStack[] getImageStackLayers() {
		return imageStackLayers;
	}

	public void setImageStackLayers(ImageStack[] imageStackLayers) {
		this.imageStackLayers = imageStackLayers;
	}

	public void setCurrImage(BufferedImage currImage) {
		this.currImage = currImage;
	}

	public BufferedImage getScaled() {
		return scaled;
	}

	public void setScaled(BufferedImage scaled) {
		this.scaled = scaled;
	}

	public void setCurrImageLabel(JLabel currImageLabel) {
		this.currImageLabel = currImageLabel;
	}

	protected boolean[][] getThresholded() {
		return thresholded;
	}

	protected void setThresholded(boolean[][] thresholded) {
		this.thresholded = thresholded;
	}

	public ScrollDrawingPane getScrollPane() {
		return scrollPane;
	}

	public void setScrollPane(ScrollDrawingPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	public void setInfoPanel(InfoPanel infoPanel) {
		this.infoPanel = infoPanel;
	}

	public void setControlPanel(ControlPanel controlPanel) {
		this.controlPanel = controlPanel;
	}

	public void setToolPanel(ToolPanel toolPanel) {
		this.toolPanel = toolPanel;
	}

	public void setKappaMenubar(KappaMenuBar kappaMenubar) {
		this.kappaMenubar = kappaMenubar;
	}

	public int getMaxLayerDigits() {
		return maxLayerDigits;
	}

	public void setMaxLayerDigits(int maxLayerDigits) {
		this.maxLayerDigits = maxLayerDigits;
	}

	public int getMaxLayer() {
		return maxLayer;
	}

	public void setMaxLayer(int maxLayer) {
		this.maxLayer = maxLayer;
	}

	public int getINIT_LAYER() {
		return INIT_LAYER;
	}

	public int getPrevIndex() {
		return prevIndex;
	}

	public void setPrevIndex(int prevIndex) {
		this.prevIndex = prevIndex;
	}

	public boolean isFittingRunning() {
		return fittingRunning;
	}

	public void setFittingRunning(boolean fittingRunning) {
		this.fittingRunning = fittingRunning;
	}

	public boolean isDragged() {
		return dragged;
	}

	public void setDragged(boolean dragged) {
		this.dragged = dragged;
	}

	public boolean isShiftPressed() {
		return shiftPressed;
	}

	public void setShiftPressed(boolean shiftPressed) {
		this.shiftPressed = shiftPressed;
	}

	public boolean isControlPointSelected() {
		return controlPointSelected;
	}

	public void setControlPointSelected(boolean controlPointSelected) {
		this.controlPointSelected = controlPointSelected;
	}

	public int getCurrCtrlPt() {
		return currCtrlPt;
	}

	public void setCurrCtrlPt(int currCtrlPt) {
		this.currCtrlPt = currCtrlPt;
	}

	public Curve getCurrEditedCurve() {
		return currEditedCurve;
	}

	public void setCurrEditedCurve(Curve currEditedCurve) {
		this.currEditedCurve = currEditedCurve;
	}

	public void setPoints(List<Point2D> points) {
		this.points = points;
	}

	public void setCurves(BezierGroup curves) {
		this.curves = curves;
	}

	public void setOverlay(Overlay overlay) {
		this.overlay = overlay;
	}

	public String getFittingAlgorithm() {
		return fittingAlgorithm;
	}

	public void setFittingAlgorithm(String fittingAlgorithm) {
		this.fittingAlgorithm = fittingAlgorithm;
	}

	public int getInputType() {
		return inputType;
	}

	public void setInputType(int inputType) {
		this.inputType = inputType;
	}

	public int getBsplineType() {
		return bsplineType;
	}

	public void setBsplineType(int bsplineType) {
		this.bsplineType = bsplineType;
	}

	public boolean isEnableCtrlPtAdjustment() {
		return enableCtrlPtAdjustment;
	}

	public void setEnableCtrlPtAdjustment(boolean enableCtrlPtAdjustment) {
		this.enableCtrlPtAdjustment = enableCtrlPtAdjustment;
	}

	public double getGlobalThreshold() {
		return globalThreshold;
	}

	public void setGlobalThreshold(double globalThreshold) {
		this.globalThreshold = globalThreshold;
	}

	public double getLocalThreshold() {
		return localThreshold;
	}

	public void setLocalThreshold(double localThreshold) {
		this.localThreshold = localThreshold;
	}

	public int getNumberOfPointsPerCurve() {
		int n;
		if (curves.getSelected().length >= 1) {
			n = curves.getSelected()[0].getNoPoints();
		}
		else {
			n = DEFAULT_NUMBER_POINTS;
		}
		if (infoPanel != null && infoPanel.getPointSlider() != null) {
			if (n != infoPanel.getPointSlider().getMaximum()) {
				infoPanel.getPointSlider().setMaximum(n);
				infoPanel.getPointSlider().setValue(1);
			}
		}
		return n;
	}

	public boolean isImageRGBColor() {
		return (getImageStack().getType() == ImagePlus.COLOR_RGB) || (getImageStack()
			.getType() == ImagePlus.COLOR_256);
	}

	public int getNFrames() {
		// Return the number of frames of imageStack
		if (imageStack.getNSlices() > imageStack.getNFrames()) {
			return imageStack.getNSlices();
		}
		else {
			return imageStack.getNFrames();
		}
	}

	public void setFrame(int frame) {
		if (imageStack.getNSlices() > imageStack.getNFrames()) {
			this.imageStack.setZ(frame);
		}
		else {
			this.imageStack.setT(frame);
		}
	}

	public boolean hasMultipleChannels() {
		return this.imageStack.getNChannels() > 1;
	}

	public double getStrokeThickness(double scale) {
		return this.baseStrokeThickness * scale;
	}

	public double getBaseStrokeThickness() {
		return this.baseStrokeThickness;
	}

	public void setBaseStrokeThickness(double strokeThickness) {
		this.baseStrokeThickness = strokeThickness;
	}

	public double getCtrlPointSize() {
		return Curve.CTRL_PT_SIZE * this.baseStrokeThickness;
	}

	public double getSelectedCtrlPointSize() {
		return Curve.SELECTED_CTRL_PT_SIZE * this.baseStrokeThickness;
	}

	public BufferedImage getCombinedImage() {
		return this.combined;
	}

}
