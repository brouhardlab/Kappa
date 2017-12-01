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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sc.fiji.kappa.curve.BezierCurve;
import sc.fiji.kappa.curve.BezierGroup;
import sc.fiji.kappa.curve.Curve;

public class InfoPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	// Offset Curve Radius
	public static final int DEFAULT_OFFSET_RADIUS = 10;
	private int offsetRadius = DEFAULT_OFFSET_RADIUS;

	// Constants for sidebar coordinates
	final static String[] DEFAULT_TITLES = { "Selected Curve: ", "Avg. Curvature: ", "Curve Length (~): ",
			"Curvature σ²", "Point Curvature: " };
	final static String[] DEFAULT_VALUES = { "NO CURVES SELECTED", "", "", "", "", "" };
	final static String[] MULT_SELECTED_TITLES = { "Selected Curves: ", "Avg. Mean Curvature: ",
			"Avg. Curve Length (~)", "σ² of Avg. Curvatures: ", "Avg. Point Curvature: " };
	final static int NO_STATS = DEFAULT_TITLES.length;
	private JLabel pointLabel;
	private JLabel[] statLabels;
	private JLabel[] valueLabels;
	private JLabel[] unitLabels;
	final static int[] TITLE_X = { 10, 10, 10, 10, 10 };
	final static int[] TITLE_Y = { 25, 59, 76, 93, 110 };
	final static int[] VALUE_X = { 10, 135, 135, 135, 135 };
	final static int[] VALUE_Y = { 42, 59, 76, 93, 110 };
	final static String[] UNIT_LABELS = { "", "µm⁻¹", "µm", "µm⁻¹", "µm⁻¹" };

	final static Rectangle POINT_SLIDER_BOUNDS = new Rectangle(8, 153, KappaFrame.PANEL_WIDTH - 16, 25);
	final static Rectangle POINT_LABEL_BOUNDS = new Rectangle(10, 133, KappaFrame.PANEL_WIDTH - 20, 25);
	final static Rectangle CONVERSION_FIELD_BOUNDS = new Rectangle(110, 180, KappaFrame.PANEL_WIDTH - 120, 20);
	final static Rectangle CONVERSION_LABEL_BOUNDS = new Rectangle(10, 177, 100, 25);

	// Constants for curve input parameter bounds
	final static Rectangle INPUT_OPTION_LABEL_BOUNDS = new Rectangle(10, 25, 200, 25);
	final static Rectangle CURVE_COMBO_BOX_BOUNDS = new Rectangle(112, 27, 100, 20);
	final static Rectangle BSPLINE_OPTION_LABEL_BOUNDS = new Rectangle(10, 47, 200, 25);
	final static Rectangle BSPLINE_COMBO_BOX_BOUNDS = new Rectangle(112, 50, 100, 20);

	// Constants for thresholding parameters
	final static Rectangle THRESHOLD_CHANNEL_LABEL_BOUNDS = new Rectangle(10, 25, 200, 25);
	final static Rectangle THRESHOLD_CHANNEL_COMBO_BOX_BOUNDS = new Rectangle(10, 50, KappaFrame.PANEL_WIDTH - 25, 20);
	final static Rectangle THRESHOLD_SLIDER_BOUNDS = new Rectangle(8, 93, KappaFrame.PANEL_WIDTH - 16, 25);
	final static Rectangle THRESHOLD_LABEL_BOUNDS = new Rectangle(10, 70, 200, 25);
	final static Rectangle AVERAGING_SPINNER_BOUNDS = new Rectangle(174, 118, 36, 17);
	final static Rectangle AVERAGING_LABEL_BOUNDS = new Rectangle(10, 115, 200, 25);
	final static Rectangle SHOW_THRESHOLDING_BOUNDS = new Rectangle(10, 133, 200, 25);
	final static Rectangle BG_CHECKBOX_BOUNDS = new Rectangle(192, 133, 25, 25);
	final static Rectangle APPLY_BUTTON_BOUNDS = new Rectangle(10, 162, 120, 20);
	final static Rectangle REVERT_BUTTON_BOUNDS = new Rectangle(140, 162, 70, 20);

	// Constants for curve fitting parameters
	final static Rectangle FITTING_CHANNEL_LABEL_BOUNDS = new Rectangle(10, 25, 200, 25);
	final static Rectangle FITTING_LABEL_BOUNDS = new Rectangle(10, 70, 200, 25);
	final static Rectangle DATA_RADIUS_LABEL_BOUNDS = new Rectangle(10, 165, 200, 25);
	final static Rectangle SHOW_DATA_THRESHOLD_LABEL_BOUNDS = new Rectangle(10, 183, 200, 25);
	final static Rectangle SHOW_DATA_POINTS_LABEL_BOUNDS = new Rectangle(10, 201, 200, 25);
	final static Rectangle THRESHOLD_RADIUS_SPINNER_BOUNDS = new Rectangle(169, 165, 45, 23);
	final static Rectangle SHOW_RADIUS_CHECKBOX_BOUNDS = new Rectangle(192, 185, 25, 25);
	final static Rectangle SHOW_DATAPOINTS_CHECKBOX_BOUNDS = new Rectangle(192, 202, 25, 25);
	public static final String[] FITTING_CHANNELS = { "The Red Channel", "The Green Channel", "The Blue Channel",
			"All Channels" };
	public static final String[] DATA_RANGE_OPTIONS = { "Brighter", "Darker" };
	final static Rectangle FITTING_CHANNEL_COMBO_BOX_BOUNDS = new Rectangle(10, 50, KappaFrame.PANEL_WIDTH - 25, 20);
	final static Rectangle FITTING_COMBO_BOX_BOUNDS = new Rectangle(10, 95, KappaFrame.PANEL_WIDTH - 25, 20);
	public static final Rectangle DATA_RANGE_COMBO_BOX_BOUNDS = new Rectangle(96, 121, 80, 18);
	public static final Rectangle CHOOSE_RANGE_BOUNDS_1 = new Rectangle(10, 121, 100, 20);
	public static final Rectangle CHOOSE_RANGE_BOUNDS_2 = new Rectangle(183, 121, 50, 20);
	public static final Rectangle DATA_THRESHOLD_SLIDER_BOUNDS = new Rectangle(7, 143, KappaFrame.PANEL_WIDTH - 75, 25);
	public static final Rectangle DATA_THRESHOLD_LABEL_BOUNDS = new Rectangle(KappaFrame.PANEL_WIDTH - 65, 142, 60, 25);
	public static final int DEFAULT_DATA_THRESHOLD = 128;

	// Charting info
	private PanelGroup panels;
	private Chart curvatureChart;
	private Chart debugCurvatureChart;
	private Chart intensityChartRed;
	private Chart intensityChartGreen;
	private Chart intensityChartBlue;
	private Panel curvaturePanel;
	private Panel intensityPanel;

	// List data
	private JList<String> list;
	private Vector<String> listData;
	private JTextField nameField;
	private JScrollPane curvesList;

	private JTextField conversionField;
	private JComboBox<String> dataRangeComboBox;
	private JComboBox<String> fittingChannelsComboBox;

	// Variables for the Curve Fitting Panel
	private JCheckBox bgCheckBox;
	private JComboBox<String> curveComboBox;
	private JComboBox<String> bsplineComboBox;
	private JComboBox<String> fittingComboBox;
	private JComboBox<String> thresholdChannelsComboBox;
	private JCheckBox showRadiusCheckBox;
	private JCheckBox showDatapointsCheckBox;
	private JSpinner rangeAveragingSpinner;
	private JSpinner thresholdRadiusSpinner;
	private JButton apply;
	private JButton revert;

	private JLabel bgThresholdLabel;
	private JLabel bsplineOptionLabel;
	private JLabel dataThresholdLabel;
	private JSlider dataThresholdSlider;
	private JSlider pointSlider;
	private JSlider thresholdSlider;

	private KappaFrame frame;

	/**
	 * Constructs a new InfoPanel object
	 */
	public InfoPanel(KappaFrame frame) {

		this.frame = frame;

		panels = new PanelGroup();
		this.addMouseListener(new MouseHandler());

		this.setLayout(null);
		setBackground(frame.PANEL_COLOR);
		setPreferredSize(new Dimension(frame.PANEL_WIDTH, 0));
		setBorder(BorderFactory.createLineBorder(Color.GRAY));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		// Internal JScrollPane for our list of curves
		setListData(new Vector<String>());
		setList(new JList<String>(getListData()));

		// List Selection Listener to match list selection to curve selection (and
		// whether the delete curve option is enabled)
		getList().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				if (event.getSource() == getList() && !event.getValueIsAdjusting()) {
					int[] selectedIndices = getList().getSelectedIndices();
					if (selectedIndices.length == 0) {
						frame.getCurves().setAllUnselected();
						updateHistograms();
						frame.getKappaMenubar().getDelete().setEnabled(false);

						// Hides the Histogram Panels when no curves are selected
						panels.hide("Curvature Distribution (absolute values)");
						panels.hide("Intensity Distribution");
					} else {
						frame.getCurves().setSelected(selectedIndices);
						frame.getKappaMenubar().getDelete().setEnabled(true);
						if (selectedIndices.length == 1) {
							updateHistograms();

							// Shows the Histogram Panels when curves are selected
							panels.show("Curvature Distribution (absolute values)");
							panels.show("Intensity Distribution");
						}
					}
					frame.drawImageOverlay();
					getPointSlider().setEnabled(frame.getCurves().isCurveSelected());
					if (!frame.getCurves().isCurveSelected()) {
						getPointSlider().setValue(1);
					}
					repaint();
					frame.getControlPanel().repaint();
				}
			}
		});

		setCurvesList(new JScrollPane(getList()));
		getCurvesList().setBounds(0, 22, frame.PANEL_WIDTH - 2, 75);
		getCurvesList().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.add(getCurvesList());
		Panel curvesListPanel = new Panel(0, 0, 95, "CURVES");
		curvesListPanel.setExpanded(false);
		panels.addPanel(curvesListPanel);
		curvesListPanel.addComponent(getCurvesList());

		// Slider and Label for traversal of Points along a Bezier Curve
		setPointSlider(new JSlider(JSlider.HORIZONTAL, 0, frame.UNIT_SCALE, 0));
		getPointSlider().addChangeListener(new PointChanger());
		getPointSlider().setBounds(POINT_SLIDER_BOUNDS);
		getPointSlider().setEnabled(false);
		pointLabel = new JLabel(
				"Point " + frame.formatNumber(getPointSlider().getValue(), BezierCurve.NO_CURVE_POINTS_DIGITS) + " / "
						+ frame.UNIT_SCALE);
		pointLabel.setFont(pointLabel.getFont().deriveFont(Font.PLAIN));
		pointLabel.setForeground(Color.GRAY);
		pointLabel.setPreferredSize(new Dimension(65, Short.MAX_VALUE));
		pointLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		pointLabel.setBounds(POINT_LABEL_BOUNDS);

		// Adds the scale selection field
		setConversionField(new JTextField(5));
		getConversionField().setText(Double.toString(Curve.DEFAULT_MICRON_PIXEL_FACTOR));
		getConversionField().addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (((c < '0') || (c > '9')) && (c != '.') && (c != KeyEvent.VK_ENTER)
						&& (c != KeyEvent.VK_BACK_SPACE)) {
					e.consume();
				}
				if (c == KeyEvent.VK_ENTER) {
					updateConversionField(getConversionField().getText());
				}
			}
		});
		getConversionField().setBounds(CONVERSION_FIELD_BOUNDS);

		Panel inputOptionsPanel = new Panel(75, "CURVE INPUT OPTIONS");
		panels.addPanel(inputOptionsPanel);
		addLabelComponent("Curve Input Type: ", inputOptionsPanel, INPUT_OPTION_LABEL_BOUNDS);

		setCurveComboBox(new JComboBox<String>(frame.CURVE_TYPES));
		getCurveComboBox().setSelectedIndex(1);
		frame.setInputType(getCurveComboBox().getSelectedIndex());
		getCurveComboBox().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Bezier Curves are viable with 3 control points but Cubic B-Splines need at
				// least 4.
				frame.setInputType(getCurveComboBox().getSelectedIndex());

				getBsplineComboBox().setEnabled(frame.getInputType() == frame.B_SPLINE);
				getBsplineOptionLabel().setEnabled(frame.getInputType() == frame.B_SPLINE);
				if (frame.getCurrCtrlPt() == 3) {
					frame.getKappaMenubar().getEnter().setEnabled(frame.getInputType() == frame.BEZIER_CURVE);
				}
				frame.getScrollPane().requestFocusInWindow();
			}
		});
		getCurveComboBox().setBounds(CURVE_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			getCurveComboBox().setSize(getCurveComboBox().getWidth(), frame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(getCurveComboBox());
		inputOptionsPanel.addComponent(getCurveComboBox());

		setBsplineOptionLabel(new JLabel("B-Spline Type: "));
		getBsplineOptionLabel().setBounds(BSPLINE_OPTION_LABEL_BOUNDS);
		getBsplineOptionLabel().setFont(getBsplineOptionLabel().getFont().deriveFont(Font.PLAIN));
		getBsplineOptionLabel().setEnabled(frame.getInputType() == frame.B_SPLINE);
		this.add(getBsplineOptionLabel());
		inputOptionsPanel.addComponent(getBsplineOptionLabel());

		setBsplineComboBox(new JComboBox<String>(frame.BSPLINE_TYPES));
		getBsplineComboBox().setSelectedIndex(0);
		getBsplineComboBox().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setBsplineType(getBsplineComboBox().getSelectedIndex());
				frame.getScrollPane().requestFocusInWindow();
				frame.drawImageOverlay();
			}
		});
		getBsplineComboBox().setEnabled(frame.getInputType() == frame.B_SPLINE);
		getBsplineComboBox().setBounds(BSPLINE_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			getBsplineComboBox().setSize(getBsplineComboBox().getWidth(), frame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(getBsplineComboBox());
		inputOptionsPanel.addComponent(getBsplineComboBox());

		Panel statisticsPanel = new Panel(210, "DATA AND STATISTICS");
		panels.addPanel(statisticsPanel);
		addLabelComponent("Scale (μm/pixel):", statisticsPanel, CONVERSION_LABEL_BOUNDS);
		this.add(pointLabel);
		this.add(getPointSlider());
		this.add(getConversionField());
		statisticsPanel.addComponent(getPointSlider());
		statisticsPanel.addComponent(pointLabel);
		statisticsPanel.addComponent(getConversionField());

		// Initialize all the labels for the statistics
		statLabels = new JLabel[NO_STATS];
		valueLabels = new JLabel[NO_STATS];
		unitLabels = new JLabel[NO_STATS];
		for (int i = 0; i < NO_STATS; i++) {
			statLabels[i] = new JLabel(DEFAULT_TITLES[i]);
			statLabels[i].setBounds(TITLE_X[i], TITLE_Y[i], 200, 25);
			statLabels[i].setFont(statLabels[i].getFont().deriveFont(Font.PLAIN));
			this.add(statLabels[i]);
			statisticsPanel.addComponent(statLabels[i]);

			valueLabels[i] = new JLabel(DEFAULT_VALUES[i]);
			valueLabels[i].setBounds(VALUE_X[i], VALUE_Y[i], 200, 25);
			valueLabels[i].setFont(valueLabels[i].getFont().deriveFont(Font.PLAIN));
			valueLabels[i].setForeground(Color.GRAY);
			this.add(valueLabels[i]);
			statisticsPanel.addComponent(valueLabels[i]);

			unitLabels[i] = new JLabel(UNIT_LABELS[i]);
			unitLabels[i].setBounds(VALUE_X[i] + 57, VALUE_Y[i], 50, 25);
			unitLabels[i].setFont(new Font("Sans Serif", Font.PLAIN, 11));
			unitLabels[i].setForeground(Color.LIGHT_GRAY);
			this.add(unitLabels[i]);
			statisticsPanel.addComponent(unitLabels[i]);
		}

		Panel curveFittingPanel = new Panel(227, "CURVE FITTING OPTIONS");
		panels.addPanel(curveFittingPanel);
		addLabelComponent("Fit Points Using: ", curveFittingPanel, FITTING_CHANNEL_LABEL_BOUNDS);
		addLabelComponent("Data Fitting Algorithm: ", curveFittingPanel, FITTING_LABEL_BOUNDS);
		addLabelComponent("Data Threshold Radius: ", curveFittingPanel, DATA_RADIUS_LABEL_BOUNDS);
		addLabelComponent("Show Thresholded Region: ", curveFittingPanel, SHOW_DATA_THRESHOLD_LABEL_BOUNDS);
		addLabelComponent("Show Data Points: ", curveFittingPanel, SHOW_DATA_POINTS_LABEL_BOUNDS);
		addLabelComponent("Choose points", curveFittingPanel, CHOOSE_RANGE_BOUNDS_1);
		addLabelComponent("than:", curveFittingPanel, CHOOSE_RANGE_BOUNDS_2);

		setDataThresholdSlider(new JSlider(JSlider.HORIZONTAL, 0, 256, DEFAULT_DATA_THRESHOLD));
		getDataThresholdSlider().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent c) {
				if (frame.getKappaMenubar().getRGBColor().isSelected()) {
					getDataThresholdLabel().setText(getDataThresholdSlider().getValue() + " / " + "256");
				} else {
					getDataThresholdLabel().setText(getDataThresholdSlider().getValue() + " / "
							+ (int) Math.pow(2, frame.getDisplayedImageStack().getBitDepth()));
				}
				for (Curve curve : frame.getCurves()) {
					curve.evaluateThresholdedPixels();
				}
				frame.drawImageOverlay();
			}
		});
		getDataThresholdSlider().setBounds(DATA_THRESHOLD_SLIDER_BOUNDS);
		this.add(getDataThresholdSlider());
		curveFittingPanel.addComponent(getDataThresholdSlider());

		setDataThresholdLabel(new JLabel(DEFAULT_DATA_THRESHOLD + " / " + "256"));
		getDataThresholdLabel().setBounds(DATA_THRESHOLD_LABEL_BOUNDS);
		getDataThresholdLabel().setForeground(Color.GRAY);
		getDataThresholdLabel().setFont(getDataThresholdLabel().getFont().deriveFont(Font.PLAIN));
		this.add(getDataThresholdLabel());
		curveFittingPanel.addComponent(getDataThresholdLabel());

		setFittingChannelsComboBox(new JComboBox<String>(FITTING_CHANNELS));
		getFittingChannelsComboBox().setSelectedIndex(3);
		getFittingChannelsComboBox().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Reevaluate all the thresholded pixels
				for (Curve c : frame.getCurves()) {
					c.evaluateThresholdedPixels();
				}
				frame.drawImageOverlay();
				;
			}
		});
		getFittingChannelsComboBox().setBounds(FITTING_CHANNEL_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			getFittingChannelsComboBox().setSize(getFittingChannelsComboBox().getWidth(), frame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(getFittingChannelsComboBox());
		curveFittingPanel.addComponent(getFittingChannelsComboBox());

		setFittingComboBox(new JComboBox<String>(frame.FITTING_ALGORITHMS));
		getFittingComboBox().setSelectedIndex(frame.DEFAULT_FITTING_ALGORITHM);
		getFittingComboBox().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setFittingAlgorithm(frame.FITTING_ALGORITHMS[getFittingComboBox().getSelectedIndex()]);
				frame.getScrollPane().requestFocusInWindow();
			}
		});
		getFittingComboBox().setBounds(FITTING_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			getFittingComboBox().setSize(getFittingComboBox().getWidth(), frame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(getFittingComboBox());
		curveFittingPanel.addComponent(getFittingComboBox());

		setDataRangeComboBox(new JComboBox<String>(DATA_RANGE_OPTIONS));
		getDataRangeComboBox().setSelectedIndex(0);
		getDataRangeComboBox().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (Curve c : frame.getCurves()) {
					c.evaluateThresholdedPixels();
				}
				frame.drawImageOverlay();
			}
		});
		getDataRangeComboBox().setBounds(DATA_RANGE_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			getDataRangeComboBox().setSize(getDataRangeComboBox().getWidth(), frame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(getDataRangeComboBox());
		curveFittingPanel.addComponent(getDataRangeComboBox());

		setThresholdRadiusSpinner(new JSpinner(new SpinnerNumberModel(5, 1, 200, 1)));
		getThresholdRadiusSpinner().setBounds(THRESHOLD_RADIUS_SPINNER_BOUNDS);
		getThresholdRadiusSpinner().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent c) {
				for (Curve curve : frame.getCurves()) {
					curve.setDataRadius((Integer) getThresholdRadiusSpinner().getValue());
				}
				frame.drawImageOverlay();
			}
		});
		this.add(getThresholdRadiusSpinner());
		curveFittingPanel.addComponent(getThresholdRadiusSpinner());

		setShowRadiusCheckBox(new JCheckBox());
		getShowRadiusCheckBox().setBounds(SHOW_RADIUS_CHECKBOX_BOUNDS);
		getShowRadiusCheckBox().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.drawImageOverlay();
			}
		});
		this.add(getShowRadiusCheckBox());
		curveFittingPanel.addComponent(getShowRadiusCheckBox());

		setShowDatapointsCheckBox(new JCheckBox());
		getShowDatapointsCheckBox().setBounds(SHOW_DATAPOINTS_CHECKBOX_BOUNDS);
		getShowDatapointsCheckBox().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.drawImageOverlay();
			}
		});
		this.add(getShowDatapointsCheckBox());
		curveFittingPanel.addComponent(getShowDatapointsCheckBox());

		// Thresholding Interface Elements
		Panel thresholdingPanel = new Panel(190, "BACKGROUND PARAMETERS");
		thresholdingPanel.setExpanded(false);
		panels.addPanel(thresholdingPanel);

		addLabelComponent("Evaluate Background Pixels Using: ", thresholdingPanel, THRESHOLD_CHANNEL_LABEL_BOUNDS);
		setThresholdChannelsComboBox(new JComboBox<String>(FITTING_CHANNELS));
		getThresholdChannelsComboBox().setSelectedIndex(0);
		getThresholdChannelsComboBox().setBounds(THRESHOLD_CHANNEL_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			getThresholdChannelsComboBox().setSize(getThresholdChannelsComboBox().getWidth(),
					frame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(getThresholdChannelsComboBox());
		thresholdingPanel.addComponent(getThresholdChannelsComboBox());

		setBgThresholdLabel(new JLabel("Background Threshold: " + frame.DEFAULT_BG_THRESHOLD + " / " + "256"));
		getBgThresholdLabel().setBounds(THRESHOLD_LABEL_BOUNDS);
		getBgThresholdLabel().setFont(getBgThresholdLabel().getFont().deriveFont(Font.PLAIN));
		this.add(getBgThresholdLabel());
		thresholdingPanel.addComponent(getBgThresholdLabel());

		setThresholdSlider(new JSlider(JSlider.HORIZONTAL, 0, 256, frame.DEFAULT_BG_THRESHOLD));
		getThresholdSlider().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent c) {
				getBgThresholdLabel().setText("Background Threshold: " + getThresholdSlider().getValue() + "/" + "256");
			}
		});
		getThresholdSlider().setBounds(THRESHOLD_SLIDER_BOUNDS);
		this.add(getThresholdSlider());
		thresholdingPanel.addComponent(getThresholdSlider());

		addLabelComponent("Pixel Range for Averaging:", thresholdingPanel, AVERAGING_LABEL_BOUNDS);
		addLabelComponent("Show Thresholded Region:", thresholdingPanel, SHOW_THRESHOLDING_BOUNDS);

		setRangeAveragingSpinner(new JSpinner(new SpinnerNumberModel(3, 0, 6, 1)));
		getRangeAveragingSpinner().setBounds(AVERAGING_SPINNER_BOUNDS);
		this.add(getRangeAveragingSpinner());
		thresholdingPanel.addComponent(getRangeAveragingSpinner());
		setBgCheckBox(new JCheckBox());
		getBgCheckBox().setBounds(BG_CHECKBOX_BOUNDS);
		this.add(getBgCheckBox());
		thresholdingPanel.addComponent(getBgCheckBox());

		setApply(new JButton("Apply Changes"));
		getApply().setFont(getApply().getFont().deriveFont(Font.PLAIN));
		setRevert(new JButton("Reset"));
		getRevert().setFont(getRevert().getFont().deriveFont(Font.PLAIN));
		getApply().setBounds(APPLY_BUTTON_BOUNDS);
		getRevert().setBounds(REVERT_BUTTON_BOUNDS);
		getThresholdChannelsComboBox().setEnabled(false);
		getThresholdSlider().setEnabled(false);
		getRangeAveragingSpinner().setEnabled(false);
		getBgCheckBox().setEnabled(false);
		getApply().setEnabled(false);
		getRevert().setEnabled(false);
		this.add(getApply());
		this.add(getRevert());
		thresholdingPanel.addComponent(getApply());
		thresholdingPanel.addComponent(getRevert());
		getApply().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.updateThresholded();
			}
		});
		getRevert().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getThresholdSlider().setValue(frame.DEFAULT_BG_THRESHOLD);
				getBgCheckBox().setSelected(false);
				getRangeAveragingSpinner().setValue(3);
				frame.setScaledImage(frame.getControlPanel().getScaleSlider().getValue() / 100.0);
				frame.drawImageOverlay();
			}
		});

		curvaturePanel = new Panel(110, "Curvature Distribution (absolute values)");
		curvatureChart = new Chart(new ArrayList<Point2D>());
		if (frame.DEBUG_MODE) {
			debugCurvatureChart = new Chart(new ArrayList<Point2D>());
			curvaturePanel.addComponent(debugCurvatureChart);
		}
		curvaturePanel.addComponent(curvatureChart);
		panels.addPanel(curvaturePanel);

		intensityChartRed = new Chart(new ArrayList<Point2D>());
		intensityChartGreen = new Chart(new ArrayList<Point2D>());
		intensityChartBlue = new Chart(new ArrayList<Point2D>());

		intensityChartRed.setMaxY(256);
		intensityChartGreen.setMaxY(256);
		intensityChartBlue.setMaxY(256);

		intensityPanel = new Panel(110, "Intensity Distribution");
		intensityPanel.addComponent(intensityChartRed);
		intensityPanel.addComponent(intensityChartGreen);
		intensityPanel.addComponent(intensityChartBlue);
		panels.addPanel(intensityPanel);

		// Hides the Histogram Panels by default
		panels.hide("Curvature Distribution (absolute values)");
		panels.hide("Intensity Distribution");
	}

	public void updateHistograms() {

		// Updates the histograms
		if (frame.getCurves().getNoSelected() == 0) {
			return;
		}
		Curve currEditedCurve = frame.getCurves().getSelected()[0];
		currEditedCurve.updateIntensities();

		List<Point2D> redIntensities = currEditedCurve.getIntensityDataRed();
		List<Point2D> greenIntensities = currEditedCurve.getIntensityDataGreen();
		List<Point2D> blueIntensities = currEditedCurve.getIntensityDataBlue();

		double maxRedValue = redIntensities.stream().map(u -> u.getY()).max(Double::compareTo).get();
		double maxGreenValue = redIntensities.stream().map(u -> u.getY()).max(Double::compareTo).get();
		double maxBlueValue = redIntensities.stream().map(u -> u.getY()).max(Double::compareTo).get();

		double maxValue = Math.max(maxRedValue, maxGreenValue);
		maxValue = Math.max(maxValue, maxBlueValue);

		intensityChartRed.setMaxY(maxValue);
		intensityChartGreen.setMaxY(maxValue);
		intensityChartBlue.setMaxY(maxValue);

		curvatureChart.setData(currEditedCurve.getCurveData());
		intensityChartRed.setData(redIntensities);
		intensityChartGreen.setData(greenIntensities);
		intensityChartBlue.setData(blueIntensities);

		if (frame.DEBUG_MODE) {
			debugCurvatureChart.setData(currEditedCurve.getDebugCurveData());
		}
		frame.getInfoPanel().repaint();
	}

	protected void setHistogramVisibility(boolean showRed, boolean showGreen, boolean showBlue) {
		if (!intensityPanel.isExpanded()) {
			return;
		}
		intensityChartRed.setVisible(showRed);
		intensityChartGreen.setVisible(showGreen);
		intensityChartBlue.setVisible(showBlue);
		updateHistograms();
	}

	private void addLabelComponent(String labelText, Panel panel, Rectangle bounds) {
		JLabel label = new JLabel(labelText);
		label.setBounds(bounds);
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		this.add(label);
		panel.addComponent(label);
	}

	/**
	 * Repaint the info panel
	 *
	 * @param g
	 *            The Graphics context
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		BezierGroup curves = frame.getCurves();

		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		panels.draw(g);
		g.setColor(Color.BLACK);

		// Draws different statistics depending on whether or not multiple curves have
		// been selected
		if (curves.getNoSelected() <= 1) {
			for (int i = 0; i < NO_STATS; i++) {
				statLabels[i].setText(DEFAULT_TITLES[i]);
			}
		} else {
			for (int i = 0; i < NO_STATS; i++) {
				statLabels[i].setText(MULT_SELECTED_TITLES[i]);
			}
		}

		g.setColor(Color.GRAY);
		if (!curves.isCurveSelected()) {
			for (int i = 0; i < NO_STATS; i++) {
				valueLabels[i].setText(DEFAULT_VALUES[i]);
			}
		} else if (curves.getNoSelected() == 1) {
			Curve currentCurve = curves.getSelected()[0];
			String[] values = { currentCurve.getName(), String.format("%8f", currentCurve.getAverageCurvature()),
					String.format("%5.4f", currentCurve.getApproxCurveLength()),
					String.format("%8f", currentCurve.getCurvatureStdDev()),
					String.format("%8f", currentCurve.getPointCurvature(getPointSlider().getValue())) };
			for (int i = 0; i < NO_STATS; i++) {
				valueLabels[i].setText(values[i]);
			}
		} else {
			String[] values = { curves.getNoSelected() + " CURVES SELECTED",
					String.format("%8f", curves.getAvgAverageCurvature(true)),
					String.format("%5.4f", curves.getAvgApproxCurveLength(true)),
					String.format("%8f", curves.getStdDevOfAvgCurvature(true)),
					String.format("%8f", curves.getAvgPointCurvature(getPointSlider().getValue(), true)) };
			for (int i = 0; i < NO_STATS; i++) {
				valueLabels[i].setText(values[i]);
			}
		}

		// Draws the curvature chart.
		if (curvatureChart.isVisible()) {
			curvatureChart.draw(curvaturePanel.getX(), curvaturePanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
					frame.PANEL_WIDTH, curvaturePanel.getH(), g, Color.BLUE);
			if (frame.DEBUG_MODE) {
				debugCurvatureChart.draw(curvaturePanel.getX(), curvaturePanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
						frame.PANEL_WIDTH, curvaturePanel.getH(), g, Color.PINK);
			}
		}

		intensityChartRed.draw(intensityPanel.getX(), intensityPanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
				frame.PANEL_WIDTH, intensityPanel.getH(), g, Color.RED);
		intensityChartGreen.draw(intensityPanel.getX(), intensityPanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
				frame.PANEL_WIDTH, intensityPanel.getH(), g, Color.GREEN);
		intensityChartBlue.draw(intensityPanel.getX(), intensityPanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
				frame.PANEL_WIDTH, intensityPanel.getH(), g, Color.BLUE);
	}

	public JList<String> getList() {
		return list;
	}

	public void setList(JList<String> list) {
		this.list = list;
	}

	public Vector<String> getListData() {
		return listData;
	}

	public void setListData(Vector<String> listData) {
		this.listData = listData;
	}

	public JTextField getNameField() {
		return nameField;
	}

	public void setNameField(JTextField nameField) {
		this.nameField = nameField;
	}

	public JScrollPane getCurvesList() {
		return curvesList;
	}

	public void setCurvesList(JScrollPane curvesList) {
		this.curvesList = curvesList;
	}

	public JTextField getConversionField() {
		return conversionField;
	}

	public void setConversionField(JTextField conversionField) {
		this.conversionField = conversionField;
	}

	public JComboBox<String> getDataRangeComboBox() {
		return dataRangeComboBox;
	}

	public void setDataRangeComboBox(JComboBox<String> dataRangeComboBox) {
		this.dataRangeComboBox = dataRangeComboBox;
	}

	public JComboBox<String> getFittingChannelsComboBox() {
		return fittingChannelsComboBox;
	}

	public void setFittingChannelsComboBox(JComboBox<String> fittingChannelsComboBox) {
		this.fittingChannelsComboBox = fittingChannelsComboBox;
	}

	public JCheckBox getBgCheckBox() {
		return bgCheckBox;
	}

	public void setBgCheckBox(JCheckBox bgCheckBox) {
		this.bgCheckBox = bgCheckBox;
	}

	public JComboBox<String> getCurveComboBox() {
		return curveComboBox;
	}

	public void setCurveComboBox(JComboBox<String> curveComboBox) {
		this.curveComboBox = curveComboBox;
	}

	public JComboBox<String> getBsplineComboBox() {
		return bsplineComboBox;
	}

	public void setBsplineComboBox(JComboBox<String> bsplineComboBox) {
		this.bsplineComboBox = bsplineComboBox;
	}

	public JComboBox<String> getFittingComboBox() {
		return fittingComboBox;
	}

	public void setFittingComboBox(JComboBox<String> fittingComboBox) {
		this.fittingComboBox = fittingComboBox;
	}

	public JComboBox<String> getThresholdChannelsComboBox() {
		return thresholdChannelsComboBox;
	}

	public void setThresholdChannelsComboBox(JComboBox<String> thresholdChannelsComboBox) {
		this.thresholdChannelsComboBox = thresholdChannelsComboBox;
	}

	public JCheckBox getShowRadiusCheckBox() {
		return showRadiusCheckBox;
	}

	public void setShowRadiusCheckBox(JCheckBox showRadiusCheckBox) {
		this.showRadiusCheckBox = showRadiusCheckBox;
	}

	public JCheckBox getShowDatapointsCheckBox() {
		return showDatapointsCheckBox;
	}

	public void setShowDatapointsCheckBox(JCheckBox showDatapointsCheckBox) {
		this.showDatapointsCheckBox = showDatapointsCheckBox;
	}

	public JSpinner getRangeAveragingSpinner() {
		return rangeAveragingSpinner;
	}

	public void setRangeAveragingSpinner(JSpinner rangeAveragingSpinner) {
		this.rangeAveragingSpinner = rangeAveragingSpinner;
	}

	public JSpinner getThresholdRadiusSpinner() {
		return thresholdRadiusSpinner;
	}

	public void setThresholdRadiusSpinner(JSpinner thresholdRadiusSpinner) {
		this.thresholdRadiusSpinner = thresholdRadiusSpinner;
	}

	public JButton getApply() {
		return apply;
	}

	public void setApply(JButton apply) {
		this.apply = apply;
	}

	public JButton getRevert() {
		return revert;
	}

	public void setRevert(JButton revert) {
		this.revert = revert;
	}

	public JLabel getBgThresholdLabel() {
		return bgThresholdLabel;
	}

	public void setBgThresholdLabel(JLabel bgThresholdLabel) {
		this.bgThresholdLabel = bgThresholdLabel;
	}

	public JLabel getBsplineOptionLabel() {
		return bsplineOptionLabel;
	}

	public void setBsplineOptionLabel(JLabel bsplineOptionLabel) {
		this.bsplineOptionLabel = bsplineOptionLabel;
	}

	public JLabel getDataThresholdLabel() {
		return dataThresholdLabel;
	}

	public void setDataThresholdLabel(JLabel dataThresholdLabel) {
		this.dataThresholdLabel = dataThresholdLabel;
	}

	public JSlider getDataThresholdSlider() {
		return dataThresholdSlider;
	}

	public void setDataThresholdSlider(JSlider dataThresholdSlider) {
		this.dataThresholdSlider = dataThresholdSlider;
	}

	public JSlider getPointSlider() {
		return pointSlider;
	}

	public void setPointSlider(JSlider pointSlider) {
		this.pointSlider = pointSlider;
	}

	public JSlider getThresholdSlider() {
		return thresholdSlider;
	}

	public void setThresholdSlider(JSlider thresholdSlider) {
		this.thresholdSlider = thresholdSlider;
	}

	protected int getOffsetRadius() {
		return offsetRadius;
	}

	protected void setOffsetRadius(int offsetRadius) {
		this.offsetRadius = offsetRadius;
	}

	// Refreshes the info panel value when the point slider changes
	private class PointChanger implements ChangeListener {

		public void stateChanged(ChangeEvent ce) {
			pointLabel.setText(
					"Point " + frame.formatNumber(getPointSlider().getValue(), BezierCurve.NO_CURVE_POINTS_DIGITS)
							+ " / " + frame.UNIT_SCALE);
			frame.drawImageOverlay();
			repaint();
		}
	}

	// Inner class to handle mouse events
	private class MouseHandler extends MouseAdapter {

		public void mousePressed(MouseEvent event) {
			// Updates visibility of the panels
			panels.toggleVisibility(event.getPoint());
			repaint();
		}
	}

	public void updateConversionField(String newValue) {
		Double oldScaleFactor = Curve.getMicronPixelFactor();
		try {
			double newScaleFactor = Double.parseDouble(newValue);
			Curve.setMicronPixelFactor(newScaleFactor);
			frame.getCurves().recalculateCurvature(frame.getControlPanel().getCurrentLayerSlider().getValue());
		} catch (Exception err) {
			Curve.setMicronPixelFactor(oldScaleFactor);
		}
		repaint();
	}
}
