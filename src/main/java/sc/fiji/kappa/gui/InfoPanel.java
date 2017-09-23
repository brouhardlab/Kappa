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
	protected static int offsetRadius = DEFAULT_OFFSET_RADIUS;

	// Constants for sidebar coordinates
	final static String[] DEFAULT_TITLES = { "Selected Curve: ", "Avg. Curvature: ", "Curve Length (~): ",
			"Curvature σ²", "Point Curvature: " };
	final static String[] DEFAULT_VALUES = { "NO CURVES SELECTED", "", "", "", "", "" };
	final static String[] MULT_SELECTED_TITLES = { "Selected Curves: ", "Avg. Mean Curvature: ",
			"Avg. Curve Length (~)", "σ² of Avg. Curvatures: ", "Avg. Point Curvature: " };
	final static int NO_STATS = DEFAULT_TITLES.length;
	JLabel pointLabel;
	JLabel[] statLabels;
	JLabel[] valueLabels;
	JLabel[] unitLabels;
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
	private static Chart curvatureChart;
	private static Chart debugCurvatureChart;
	private static Chart intensityChartRed;
	private static Chart intensityChartGreen;
	private static Chart intensityChartBlue;
	static Panel curvaturePanel;
	static Panel intensityPanel;

	// List data
	public static JList<String> list;
	public static Vector<String> listData;
	JTextField nameField;
	static JScrollPane curvesList;

	public static JTextField conversionField;
	public static JComboBox<String> dataRangeComboBox;
	public static JComboBox<String> fittingChannelsComboBox;

	// Variables for the Curve Fitting Panel
	public static JCheckBox bgCheckBox;
	public JComboBox<String> curveComboBox;
	public JComboBox<String> bsplineComboBox;
	public static JComboBox<String> fittingComboBox;
	public static JComboBox<String> thresholdChannelsComboBox;
	public static JCheckBox showRadiusCheckBox;
	public static JCheckBox showDatapointsCheckBox;
	public static JSpinner rangeAveragingSpinner;
	public static JSpinner thresholdRadiusSpinner;
	public static JButton apply;
	public static JButton revert;

	JLabel bgThresholdLabel;
	JLabel bsplineOptionLabel;
	JLabel dataThresholdLabel;
	public static JSlider dataThresholdSlider;
	public static JSlider pointSlider;
	public static JSlider thresholdSlider;

	public static void updateHistograms() {
		// Updates the histograms
		if (KappaFrame.curves.getNoSelected() == 0) {
			return;
		}
		Curve currEditedCurve = KappaFrame.curves.getSelected()[0];
		currEditedCurve.updateIntensities();
		curvatureChart.setData(currEditedCurve.getCurveData());
		intensityChartRed.setData(currEditedCurve.getIntensityDataRed());
		intensityChartGreen.setData(currEditedCurve.getIntensityDataGreen());
		intensityChartBlue.setData(currEditedCurve.getIntensityDataBlue());
		if (KappaFrame.DEBUG_MODE) {
			debugCurvatureChart.setData(currEditedCurve.getDebugCurveData());
		}
		KappaFrame.infoPanel.repaint();
	}

	protected static void setHistogramVisibility(boolean showRed, boolean showGreen, boolean showBlue) {
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
	 * Constructs a new InfoPanel object
	 */
	public InfoPanel() {
		panels = new PanelGroup();
		this.addMouseListener(new MouseHandler());

		this.setLayout(null);
		setBackground(KappaFrame.PANEL_COLOR);
		setPreferredSize(new Dimension(KappaFrame.PANEL_WIDTH, 0));
		setBorder(BorderFactory.createLineBorder(Color.GRAY));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		// Internal JScrollPane for our list of curves
		listData = new Vector<String>();
		list = new JList<String>(listData);

		// List Selection Listener to match list selection to curve selection (and
		// whether the delete curve option is enabled)
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				if (event.getSource() == list && !event.getValueIsAdjusting()) {
					int[] selectedIndices = list.getSelectedIndices();
					if (selectedIndices.length == 0) {
						KappaFrame.curves.setAllUnselected();
						updateHistograms();
						MenuBar.delete.setEnabled(false);

						// Hides the Histogram Panels when no curves are selected
						panels.hide("CURVATURE DISTRIBUTION");
						panels.hide("INTENSITY DISTRIBUTION");
					} else {
						KappaFrame.curves.setSelected(selectedIndices);
						MenuBar.delete.setEnabled(true);
						if (selectedIndices.length == 1) {
							updateHistograms();

							// Shows the Histogram Panels when curves are selected
							panels.show("CURVATURE DISTRIBUTION");
							panels.show("INTENSITY DISTRIBUTION");
						}
					}
					KappaFrame.drawImageOverlay();
					pointSlider.setEnabled(KappaFrame.curves.isCurveSelected());
					if (!KappaFrame.curves.isCurveSelected()) {
						pointSlider.setValue(1);
					}
					repaint();
					KappaFrame.controlPanel.repaint();
				}
			}
		});

		curvesList = new JScrollPane(list);
		curvesList.setBounds(0, 22, KappaFrame.PANEL_WIDTH - 2, 75);
		curvesList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.add(curvesList);
		Panel curvesListPanel = new Panel(0, 0, 95, "CURVES");
		curvesListPanel.setExpanded(false);
		panels.addPanel(curvesListPanel);
		curvesListPanel.addComponent(curvesList);

		// Slider and Label for traversal of Points along a Bezier Curve
		pointSlider = new JSlider(JSlider.HORIZONTAL, 0, KappaFrame.UNIT_SCALE, 0);
		pointSlider.addChangeListener(new PointChanger());
		pointSlider.setBounds(POINT_SLIDER_BOUNDS);
		pointSlider.setEnabled(false);
		pointLabel = new JLabel(
				"Point " + KappaFrame.formatNumber(pointSlider.getValue(), BezierCurve.NO_CURVE_POINTS_DIGITS) + " / "
						+ KappaFrame.UNIT_SCALE);
		pointLabel.setFont(pointLabel.getFont().deriveFont(Font.PLAIN));
		pointLabel.setForeground(Color.GRAY);
		pointLabel.setPreferredSize(new Dimension(65, Short.MAX_VALUE));
		pointLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		pointLabel.setBounds(POINT_LABEL_BOUNDS);

		// Adds the scale selection field
		conversionField = new JTextField(5);
		conversionField.setText(Double.toString(Curve.DEFAULT_MICRON_PIXEL_FACTOR));
		conversionField.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (((c < '0') || (c > '9')) && (c != '.') && (c != KeyEvent.VK_ENTER)
						&& (c != KeyEvent.VK_BACK_SPACE)) {
					e.consume();
				}
				if (c == KeyEvent.VK_ENTER) {
					Double oldScaleFactor = Curve.getMicronPixelFactor();
					try {
						double newScaleFactor = Double.parseDouble(conversionField.getText());
						Curve.setMicronPixelFactor(newScaleFactor);
						KappaFrame.curves.recalculateCurvature(ControlPanel.currentLayerSlider.getValue());
					} catch (Exception err) {
						Curve.setMicronPixelFactor(oldScaleFactor);
					}
					repaint();
				}
			}
		});
		conversionField.setBounds(CONVERSION_FIELD_BOUNDS);

		Panel inputOptionsPanel = new Panel(75, "CURVE INPUT OPTIONS");
		panels.addPanel(inputOptionsPanel);
		addLabelComponent("Curve Input Type: ", inputOptionsPanel, INPUT_OPTION_LABEL_BOUNDS);

		curveComboBox = new JComboBox<String>(KappaFrame.CURVE_TYPES);
		curveComboBox.setSelectedIndex(1);
		KappaFrame.inputType = curveComboBox.getSelectedIndex();
		curveComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Bezier Curves are viable with 3 control points but Cubic B-Splines need at
				// least 4.
				KappaFrame.inputType = curveComboBox.getSelectedIndex();

				bsplineComboBox.setEnabled(KappaFrame.inputType == KappaFrame.B_SPLINE);
				bsplineOptionLabel.setEnabled(KappaFrame.inputType == KappaFrame.B_SPLINE);
				if (KappaFrame.currCtrlPt == 3) {
					MenuBar.enter.setEnabled(KappaFrame.inputType == KappaFrame.BEZIER_CURVE);
				}
				KappaFrame.scrollPane.requestFocusInWindow();
			}
		});
		curveComboBox.setBounds(CURVE_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			curveComboBox.setSize(curveComboBox.getWidth(), KappaFrame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(curveComboBox);
		inputOptionsPanel.addComponent(curveComboBox);

		bsplineOptionLabel = new JLabel("B-Spline Type: ");
		bsplineOptionLabel.setBounds(BSPLINE_OPTION_LABEL_BOUNDS);
		bsplineOptionLabel.setFont(bsplineOptionLabel.getFont().deriveFont(Font.PLAIN));
		bsplineOptionLabel.setEnabled(KappaFrame.inputType == KappaFrame.B_SPLINE);
		this.add(bsplineOptionLabel);
		inputOptionsPanel.addComponent(bsplineOptionLabel);

		bsplineComboBox = new JComboBox<String>(KappaFrame.BSPLINE_TYPES);
		bsplineComboBox.setSelectedIndex(0);
		bsplineComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KappaFrame.bsplineType = bsplineComboBox.getSelectedIndex();
				KappaFrame.scrollPane.requestFocusInWindow();
				KappaFrame.drawImageOverlay();
			}
		});
		bsplineComboBox.setEnabled(KappaFrame.inputType == KappaFrame.B_SPLINE);
		bsplineComboBox.setBounds(BSPLINE_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			bsplineComboBox.setSize(bsplineComboBox.getWidth(), KappaFrame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(bsplineComboBox);
		inputOptionsPanel.addComponent(bsplineComboBox);

		Panel statisticsPanel = new Panel(210, "DATA AND STATISTICS");
		panels.addPanel(statisticsPanel);
		addLabelComponent("Scale (μm/pixel):", statisticsPanel, CONVERSION_LABEL_BOUNDS);
		this.add(pointLabel);
		this.add(pointSlider);
		this.add(conversionField);
		statisticsPanel.addComponent(pointSlider);
		statisticsPanel.addComponent(pointLabel);
		statisticsPanel.addComponent(conversionField);

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

		dataThresholdSlider = new JSlider(JSlider.HORIZONTAL, 0, 256, DEFAULT_DATA_THRESHOLD);
		dataThresholdSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent c) {
				if (MenuBar.RGBColor.isSelected()) {
					dataThresholdLabel.setText(dataThresholdSlider.getValue() + " / " + "256");
				} else {
					dataThresholdLabel.setText(dataThresholdSlider.getValue() + " / "
							+ (int) Math.pow(2, KappaFrame.displayedImageStack.getBitDepth()));
				}
				for (Curve curve : KappaFrame.curves) {
					curve.evaluateThresholdedPixels();
				}
				KappaFrame.drawImageOverlay();
			}
		});
		dataThresholdSlider.setBounds(DATA_THRESHOLD_SLIDER_BOUNDS);
		this.add(dataThresholdSlider);
		curveFittingPanel.addComponent(dataThresholdSlider);

		dataThresholdLabel = new JLabel(DEFAULT_DATA_THRESHOLD + " / " + "256");
		dataThresholdLabel.setBounds(DATA_THRESHOLD_LABEL_BOUNDS);
		dataThresholdLabel.setForeground(Color.GRAY);
		dataThresholdLabel.setFont(dataThresholdLabel.getFont().deriveFont(Font.PLAIN));
		this.add(dataThresholdLabel);
		curveFittingPanel.addComponent(dataThresholdLabel);

		fittingChannelsComboBox = new JComboBox<String>(FITTING_CHANNELS);
		fittingChannelsComboBox.setSelectedIndex(3);
		fittingChannelsComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Reevaluate all the thresholded pixels
				for (Curve c : KappaFrame.curves) {
					c.evaluateThresholdedPixels();
				}
				KappaFrame.drawImageOverlay();
				;
			}
		});
		fittingChannelsComboBox.setBounds(FITTING_CHANNEL_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			fittingChannelsComboBox.setSize(fittingChannelsComboBox.getWidth(), KappaFrame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(fittingChannelsComboBox);
		curveFittingPanel.addComponent(fittingChannelsComboBox);

		fittingComboBox = new JComboBox<String>(KappaFrame.FITTING_ALGORITHMS);
		fittingComboBox.setSelectedIndex(KappaFrame.DEFAULT_FITTING_ALGORITHM);
		fittingComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KappaFrame.fittingAlgorithm = KappaFrame.FITTING_ALGORITHMS[fittingComboBox.getSelectedIndex()];
				KappaFrame.scrollPane.requestFocusInWindow();
			}
		});
		fittingComboBox.setBounds(FITTING_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			fittingComboBox.setSize(fittingComboBox.getWidth(), KappaFrame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(fittingComboBox);
		curveFittingPanel.addComponent(fittingComboBox);

		dataRangeComboBox = new JComboBox<String>(DATA_RANGE_OPTIONS);
		dataRangeComboBox.setSelectedIndex(0);
		dataRangeComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (Curve c : KappaFrame.curves) {
					c.evaluateThresholdedPixels();
				}
				KappaFrame.drawImageOverlay();
			}
		});
		dataRangeComboBox.setBounds(DATA_RANGE_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			dataRangeComboBox.setSize(dataRangeComboBox.getWidth(), KappaFrame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(dataRangeComboBox);
		curveFittingPanel.addComponent(dataRangeComboBox);

		thresholdRadiusSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 200, 1));
		thresholdRadiusSpinner.setBounds(THRESHOLD_RADIUS_SPINNER_BOUNDS);
		thresholdRadiusSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent c) {
				for (Curve curve : KappaFrame.curves) {
					curve.setDataRadius((Integer) thresholdRadiusSpinner.getValue());
				}
				KappaFrame.drawImageOverlay();
			}
		});
		this.add(thresholdRadiusSpinner);
		curveFittingPanel.addComponent(thresholdRadiusSpinner);

		showRadiusCheckBox = new JCheckBox();
		showRadiusCheckBox.setBounds(SHOW_RADIUS_CHECKBOX_BOUNDS);
		showRadiusCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KappaFrame.drawImageOverlay();
			}
		});
		this.add(showRadiusCheckBox);
		curveFittingPanel.addComponent(showRadiusCheckBox);

		showDatapointsCheckBox = new JCheckBox();
		showDatapointsCheckBox.setBounds(SHOW_DATAPOINTS_CHECKBOX_BOUNDS);
		showDatapointsCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KappaFrame.drawImageOverlay();
			}
		});
		this.add(showDatapointsCheckBox);
		curveFittingPanel.addComponent(showDatapointsCheckBox);

		// Thresholding Interface Elements
		Panel thresholdingPanel = new Panel(190, "BACKGROUND PARAMETERS");
		thresholdingPanel.setExpanded(false);
		panels.addPanel(thresholdingPanel);

		addLabelComponent("Evaluate Background Pixels Using: ", thresholdingPanel, THRESHOLD_CHANNEL_LABEL_BOUNDS);
		thresholdChannelsComboBox = new JComboBox<String>(FITTING_CHANNELS);
		thresholdChannelsComboBox.setSelectedIndex(0);
		thresholdChannelsComboBox.setBounds(THRESHOLD_CHANNEL_COMBO_BOX_BOUNDS);
		if (System.getProperty("os.name").equals("Mac OS X")) {
			thresholdChannelsComboBox.setSize(thresholdChannelsComboBox.getWidth(), KappaFrame.COMBO_BOX_HEIGHT_OSX);
		}
		this.add(thresholdChannelsComboBox);
		thresholdingPanel.addComponent(thresholdChannelsComboBox);

		bgThresholdLabel = new JLabel("Background Threshold: " + KappaFrame.DEFAULT_BG_THRESHOLD + " / " + "256");
		bgThresholdLabel.setBounds(THRESHOLD_LABEL_BOUNDS);
		bgThresholdLabel.setFont(bgThresholdLabel.getFont().deriveFont(Font.PLAIN));
		this.add(bgThresholdLabel);
		thresholdingPanel.addComponent(bgThresholdLabel);

		thresholdSlider = new JSlider(JSlider.HORIZONTAL, 0, 256, KappaFrame.DEFAULT_BG_THRESHOLD);
		thresholdSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent c) {
				bgThresholdLabel.setText("Background Threshold: " + thresholdSlider.getValue() + "/" + "256");
			}
		});
		thresholdSlider.setBounds(THRESHOLD_SLIDER_BOUNDS);
		this.add(thresholdSlider);
		thresholdingPanel.addComponent(thresholdSlider);

		addLabelComponent("Pixel Range for Averaging:", thresholdingPanel, AVERAGING_LABEL_BOUNDS);
		addLabelComponent("Show Thresholded Region:", thresholdingPanel, SHOW_THRESHOLDING_BOUNDS);

		rangeAveragingSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 6, 1));
		rangeAveragingSpinner.setBounds(AVERAGING_SPINNER_BOUNDS);
		this.add(rangeAveragingSpinner);
		thresholdingPanel.addComponent(rangeAveragingSpinner);
		bgCheckBox = new JCheckBox();
		bgCheckBox.setBounds(BG_CHECKBOX_BOUNDS);
		this.add(bgCheckBox);
		thresholdingPanel.addComponent(bgCheckBox);

		apply = new JButton("Apply Changes");
		apply.setFont(apply.getFont().deriveFont(Font.PLAIN));
		revert = new JButton("Reset");
		revert.setFont(revert.getFont().deriveFont(Font.PLAIN));
		apply.setBounds(APPLY_BUTTON_BOUNDS);
		revert.setBounds(REVERT_BUTTON_BOUNDS);
		thresholdChannelsComboBox.setEnabled(false);
		thresholdSlider.setEnabled(false);
		rangeAveragingSpinner.setEnabled(false);
		bgCheckBox.setEnabled(false);
		apply.setEnabled(false);
		revert.setEnabled(false);
		this.add(apply);
		this.add(revert);
		thresholdingPanel.addComponent(apply);
		thresholdingPanel.addComponent(revert);
		apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KappaFrame.updateThresholded();
			}
		});
		revert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				thresholdSlider.setValue(KappaFrame.DEFAULT_BG_THRESHOLD);
				bgCheckBox.setSelected(false);
				rangeAveragingSpinner.setValue(3);
				KappaFrame.setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
				KappaFrame.drawImageOverlay();
			}
		});

		curvaturePanel = new Panel(110, "CURVATURE DISTRIBUTION");
		curvatureChart = new Chart(new ArrayList<Point2D>());
		if (KappaFrame.DEBUG_MODE) {
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

		intensityPanel = new Panel(110, "INTENSITY DISTRIBUTION");
		intensityPanel.addComponent(intensityChartRed);
		intensityPanel.addComponent(intensityChartGreen);
		intensityPanel.addComponent(intensityChartBlue);
		panels.addPanel(intensityPanel);

		// Hides the Histogram Panels by default
		panels.hide("CURVATURE DISTRIBUTION");
		panels.hide("INTENSITY DISTRIBUTION");
	}

	/**
	 * Repaint the info panel
	 *
	 * @param g
	 *            The Graphics context
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		BezierGroup curves = KappaFrame.curves;

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
					String.format("%8f", currentCurve.getPointCurvature(pointSlider.getValue())) };
			for (int i = 0; i < NO_STATS; i++) {
				valueLabels[i].setText(values[i]);
			}
		} else {
			String[] values = { curves.getNoSelected() + " CURVES SELECTED",
					String.format("%8f", curves.getAvgAverageCurvature(true)),
					String.format("%5.4f", curves.getAvgApproxCurveLength(true)),
					String.format("%8f", curves.getStdDevOfAvgCurvature(true)),
					String.format("%8f", curves.getAvgPointCurvature(pointSlider.getValue(), true)) };
			for (int i = 0; i < NO_STATS; i++) {
				valueLabels[i].setText(values[i]);
			}
		}

		// Draws the curvature chart.
		if (curvatureChart.isVisible()) {
			curvatureChart.draw(curvaturePanel.getX(), curvaturePanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
					KappaFrame.PANEL_WIDTH, curvaturePanel.getH(), g, Color.BLUE);
			if (KappaFrame.DEBUG_MODE) {
				debugCurvatureChart.draw(curvaturePanel.getX(), curvaturePanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
						KappaFrame.PANEL_WIDTH, curvaturePanel.getH(), g, Color.PINK);
			}
		}

		intensityChartRed.draw(intensityPanel.getX(), intensityPanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
				KappaFrame.PANEL_WIDTH, intensityPanel.getH(), g, Color.RED);
		intensityChartGreen.draw(intensityPanel.getX(), intensityPanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
				KappaFrame.PANEL_WIDTH, intensityPanel.getH(), g, Color.GREEN);
		intensityChartBlue.draw(intensityPanel.getX(), intensityPanel.getY() + Panel.TITLEBAR_DEFAULT_HEIGHT,
				KappaFrame.PANEL_WIDTH, intensityPanel.getH(), g, Color.BLUE);
	}

	// Refreshes the info panel value when the point slider changes
	private class PointChanger implements ChangeListener {

		public void stateChanged(ChangeEvent ce) {
			pointLabel.setText(
					"Point " + KappaFrame.formatNumber(pointSlider.getValue(), BezierCurve.NO_CURVE_POINTS_DIGITS)
							+ " / " + KappaFrame.UNIT_SCALE);
			KappaFrame.drawImageOverlay();
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
}
