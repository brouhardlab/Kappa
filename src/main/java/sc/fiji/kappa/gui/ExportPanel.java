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

import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import sc.fiji.kappa.curve.BezierCurve;
import sc.fiji.kappa.curve.BezierGroup;
import sc.fiji.kappa.curve.Curve;

public class ExportPanel extends JPanel {
	// Whether we export a header for each separate curve

	boolean EXPORT_HEADERS = false;

	// File Handling
	JFileChooser kappaExport;

	// Export Panel Checkboxes- Per Curve
	static JCheckBox exportAllDataPoints;
	static JCheckBox exportAveragesOnly;
	static CheckboxGroup curveExportOptions;
	static JCheckBox exportCurveLength;
	static JCheckBox exportFitError;
	final static Rectangle EXPORT_ALL_POINTS_BOUNDS = new Rectangle(5, 27, 25, 25);
	final static Rectangle EXPORT_ALL_POINTS_LABEL_BOUNDS = new Rectangle(35, 25, 200, 25);
	final static Rectangle EXPORT_AVERAGES_BOUNDS = new Rectangle(5, 45, 25, 25);
	final static Rectangle EXPORT_AVERAGES_LABEL_BOUNDS = new Rectangle(35, 43, 200, 25);
	final static Rectangle EXPORT_CURVE_LENGTH_BOUNDS = new Rectangle(5, 75, 25, 25);
	final static Rectangle EXPORT_CURVE_LENGTH_LABEL_BOUNDS = new Rectangle(35, 73, 200, 25);
	final static Rectangle EXPORT_FIT_ERROR_BOUNDS = new Rectangle(5, 93, 25, 25);
	final static Rectangle EXPORT_FIT_ERROR_LABEL_BOUNDS = new Rectangle(35, 91, 200, 25);

	// Export Panel Checkboxes- Per Frame
	static JCheckBox exportAllCurves;
	static JCheckBox exportSelectedCurves;
	final static Rectangle EXPORT_ALL_CURVES_BOUNDS = new Rectangle(5, 27, 25, 25);
	final static Rectangle EXPORT_ALL_CURVES_LABEL_BOUNDS = new Rectangle(35, 25, 200, 25);
	final static Rectangle EXPORT_SELECTED_CURVES_BOUNDS = new Rectangle(5, 45, 25, 25);
	final static Rectangle EXPORT_SELECTED_CURVES_LABEL_BOUNDS = new Rectangle(35, 43, 200, 25);

	// Export Panel Checkboxes- Per Stack
	static JCheckBox exportEachFrame;
	static JCheckBox exportKeyframes;
	final static Rectangle EXPORT_EACH_FRAME_BOUNDS = new Rectangle(5, 27, 25, 25);
	final static Rectangle EXPORT_EACH_FRAME_LABEL_BOUNDS = new Rectangle(35, 25, 200, 25);
	final static Rectangle EXPORT_KEYFRAMES_BOUNDS = new Rectangle(5, 45, 25, 25);
	final static Rectangle EXPORT_KEYFRAMES_LABEL_BOUNDS = new Rectangle(35, 43, 200, 25);
	protected JButton exportButton;
	protected Panel perStackPanel;

	private static final long serialVersionUID = 1L;
	private PanelGroup exportPanels;

	private KappaFrame frame;

	/**
	 * Constructs a new ExportPanel object
	 */
	public ExportPanel(KappaFrame frame) {

		this.frame = frame;

		exportPanels = new PanelGroup();
		this.addMouseListener(new MouseHandler());

		// File dialog for exporting.
		kappaExport = new JFileChooser();
		kappaExport.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
		kappaExport.setDialogTitle("Export Curve Data");

		this.setLayout(null);
		setBackground(KappaFrame.PANEL_COLOR);
		setPreferredSize(new Dimension(KappaFrame.PANEL_WIDTH, 0));
		setBorder(BorderFactory.createLineBorder(Color.GRAY));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		Panel perCurvePanel = new Panel(118, "PER CURVE OPTIONS:");
		exportPanels.addPanel(perCurvePanel);
		addLabelComponent("Export All Data Points", perCurvePanel, EXPORT_ALL_POINTS_LABEL_BOUNDS);
		addLabelComponent("Export Averages Only", perCurvePanel, EXPORT_AVERAGES_LABEL_BOUNDS);
		exportAllDataPoints = new JCheckBox();
		exportAllDataPoints.setBounds(EXPORT_ALL_POINTS_BOUNDS);
		exportAllDataPoints.setSelected(true);
		exportAllDataPoints.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportAveragesOnly.setSelected(!exportAllDataPoints.isSelected());
			}
		});
		this.add(exportAllDataPoints);
		perCurvePanel.addComponent(exportAllDataPoints);
		exportAveragesOnly = new JCheckBox();
		exportAveragesOnly.setBounds(EXPORT_AVERAGES_BOUNDS);
		exportAveragesOnly.setSelected(false);
		exportAveragesOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportAllDataPoints.setSelected(!exportAveragesOnly.isSelected());
			}
		});
		this.add(exportAveragesOnly);
		perCurvePanel.addComponent(exportAveragesOnly);
		perCurvePanel.addSeparator(70);

		addLabelComponent("Export Total Curve Length", perCurvePanel, EXPORT_CURVE_LENGTH_LABEL_BOUNDS);
		addLabelComponent("Export Squared Error", perCurvePanel, EXPORT_FIT_ERROR_LABEL_BOUNDS);
		exportCurveLength = new JCheckBox();
		exportCurveLength.setBounds(EXPORT_CURVE_LENGTH_BOUNDS);
		exportCurveLength.setSelected(true);
		this.add(exportCurveLength);
		perCurvePanel.addComponent(exportCurveLength);
		exportFitError = new JCheckBox();
		exportFitError.setBounds(EXPORT_FIT_ERROR_BOUNDS);
		exportFitError.setSelected(false);
		this.add(exportFitError);
		perCurvePanel.addComponent(exportFitError);

		Panel perFramePanel = new Panel(70, "PER FRAME OPTIONS:");
		exportPanels.addPanel(perFramePanel);
		addLabelComponent("Export All Curves", perFramePanel, EXPORT_ALL_CURVES_LABEL_BOUNDS);
		addLabelComponent("Export Selected Curves Only", perFramePanel, EXPORT_SELECTED_CURVES_LABEL_BOUNDS);
		exportAllCurves = new JCheckBox();
		exportAllCurves.setBounds(EXPORT_ALL_CURVES_BOUNDS);
		exportAllCurves.setSelected(true);
		exportAllCurves.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportSelectedCurves.setSelected(!exportAllCurves.isSelected());
			}
		});
		this.add(exportAllCurves);
		perFramePanel.addComponent(exportAllCurves);
		exportSelectedCurves = new JCheckBox();
		exportSelectedCurves.setBounds(EXPORT_SELECTED_CURVES_BOUNDS);
		exportSelectedCurves.setSelected(false);
		exportSelectedCurves.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportAllCurves.setSelected(!exportSelectedCurves.isSelected());
			}
		});
		this.add(exportSelectedCurves);
		perFramePanel.addComponent(exportSelectedCurves);

		perStackPanel = new Panel(70, "PER STACK OPTIONS:");
		exportPanels.addPanel(perStackPanel);
		addLabelComponent("Export Data in Each Frame", perStackPanel, EXPORT_EACH_FRAME_LABEL_BOUNDS);
		addLabelComponent("Export Data in Keyframes Only", perStackPanel, EXPORT_KEYFRAMES_LABEL_BOUNDS);
		exportEachFrame = new JCheckBox();
		exportEachFrame.setBounds(EXPORT_EACH_FRAME_BOUNDS);
		exportEachFrame.setSelected(true);
		exportEachFrame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportKeyframes.setSelected(!exportEachFrame.isSelected());
			}
		});
		this.add(exportEachFrame);
		perStackPanel.addComponent(exportEachFrame);
		exportKeyframes = new JCheckBox();
		exportKeyframes.setBounds(EXPORT_KEYFRAMES_BOUNDS);
		exportKeyframes.setSelected(false);
		exportKeyframes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportEachFrame.setSelected(!exportKeyframes.isSelected());
			}
		});
		this.add(exportKeyframes);
		perStackPanel.addComponent(exportKeyframes);

		exportButton = new JButton("EXPORT");
		exportButton.addActionListener(new ExportActionListener());
		this.add(exportButton);
	}

	private void addLabelComponent(String labelText, Panel panel, Rectangle bounds) {
		JLabel label = new JLabel(labelText);
		label.setBounds(bounds);
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		this.add(label);
		panel.addComponent(label);
	}

	/**
	 * Repaint the export panel
	 *
	 * @param g
	 *            The Graphics context
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Draws the title
		g.setColor(Color.WHITE);
		g.fillRect(0, 2, KappaFrame.PANEL_WIDTH, 1);
		g.setColor(Color.DARK_GRAY);
		g.drawLine(0, 30, KappaFrame.PANEL_WIDTH, 30);
		g.setColor(Color.LIGHT_GRAY);
		g.drawLine(0, 29, KappaFrame.PANEL_WIDTH, 29);
		g.setColor(Color.BLACK);

		g.setFont(new Font("Sans Serif", Font.BOLD, 14));
		g.drawString("EXPORT OPTIONS:", KappaFrame.PANEL_WIDTH / 2
				- (int) g.getFontMetrics(this.getFont()).getStringBounds("EXPORT OPTIONS:", g).getWidth() / 2 - 8, 21);
		g.setFont(new Font("Sans Serif", Font.PLAIN, 12));
		g.setColor(Color.BLACK);
		exportPanels.draw(g);
		g.setColor(Color.BLACK);
	}

	// Inner class to handle mouse events
	private class MouseHandler extends MouseAdapter {

		public void mousePressed(MouseEvent event) {
			// Updates visibility of the panels
			exportPanels.toggleVisibility(event.getPoint());
			repaint();
		}
	}

	private class ExportActionListener implements ActionListener {

		private void printHeaders(PrintWriter out) {
			// Prints the headers for all the values that we're exporting. Namely, x, y, k,
			// and the dimer angle.
			// In CSV format, so the columns are separated by commas, and rows are separated
			// by new lines.

			out.print("Curve Name");

			if (exportAllDataPoints.isSelected()) {

				out.print(",X-Coordinate (um)");
				out.print(",Y-Coordinate (um)");
				out.print(",Point Curvature (um-1)");

				out.print(",Curve Length (um)");
				out.print(",Average Curvature (um-1)");
				out.print(",Curvature Std (um-1)");

				out.print(",Red Intensity");
				out.print(",Green Intensity");
				out.print(",Blue Intensity");
				out.print(",Background Intensity");
				out.println();
			} else {
				out.print(",Average X-Coordinate (um)");
				out.print(",Average Y-Coordinate (um)");
				out.print(",Point Curvature (um-1)");

				out.print(",Curve Length (um)");
				out.print(",Average Curvature (um-1)");
				out.print(",Curvature Std (um-1)");

				out.print(",Average Red Intensity");
				out.print(",Average Green Intensity");
				out.print(",Average Blue Intensity");
				out.print(",Average Background Intensity");
				out.println();
			}

		}

		private void exportCurveData(Curve c, PrintWriter out, double[][] averaged) {
			// Prints out the name of the curve.
			out.println(c.getName());
			printHeaders(out);

			c.printValues(out, averaged, exportAllDataPoints.isSelected());
			out.println();
		}

		public void actionPerformed(ActionEvent e) {
			BezierGroup curves = frame.curves;

			// Handles export button action.
			int returnVal = kappaExport.showSaveDialog(frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = kappaExport.getSelectedFile();

				// Appends a .csv
				if (!file.getPath().toLowerCase().endsWith(".csv")) {
					file = new File(file.getPath() + ".csv");
				}

				// Exports the file based on the chosen export options.
				try {
					// Get averaged background data for background normalization
					int w = frame.currImage.getWidth();
					int h = frame.currImage.getHeight();
					double[][] averaged = new double[w][h];
					for (int i = 0; i < w; i++) {
						for (int j = 0; j < h; j++) {
							long totalThresholded = 0;
							int n = 0;
							for (int dx = i - KappaFrame.BG_AVERAGING_RANGE; dx <= i
									+ KappaFrame.BG_AVERAGING_RANGE; dx++) {
								for (int dy = j - KappaFrame.BG_AVERAGING_RANGE; dy <= j
										+ KappaFrame.BG_AVERAGING_RANGE; dy++) {
									if (dx >= 0 && dy >= 0 && dx < w && dy < h && frame.thresholded[dx][dy]) {
										int channel = InfoPanel.thresholdChannelsComboBox.getSelectedIndex();
										int[] rgb = BezierCurve.getRGB(frame.displayedImageStack, dx, dy);
										switch (channel) {
										case 0:
											totalThresholded += rgb[0];
										case 1:
											totalThresholded += rgb[1];
										case 2:
											totalThresholded += rgb[2];
										}
										n++;
									}
								}
							}
							// If there are any adjacent pixels, we can compute an average.
							if (n != 0) {
								averaged[i][j] = totalThresholded * 1.0 / n;
							} else {
								averaged[i][j] = -1;
							}
						}
					}

					// We flood fill to get an estimate for all pixels without an average.
					boolean changed = false;
					do {
						changed = false;
						for (int i = 0; i < w; i++) {
							for (int j = 0; j < h; j++) {
								if (averaged[i][j] == -1) {
									int n = 0;
									int total = 0;
									if (i < w - 1 && averaged[i + 1][j] >= 0) {
										n++;
										total += averaged[i + 1][j];
									}
									if (j < h - 1 && averaged[i][j + 1] >= 0) {
										n++;
										total += averaged[i][j + 1];
									}
									if (i > 0 && averaged[i - 1][j] >= 0) {
										n++;
										total += averaged[i - 1][j];
									}
									if (j > 0 && averaged[i][j - 1] >= 0) {
										n++;
										total += averaged[i][j - 1];
									}
									if (n != 0) {
										averaged[i][j] = total * 1.0 / n;
										changed = true;
									}
								}
							}
						}
					} while (changed);

					// If there are unaveraged pixels, we just use the original value
					for (int i = 0; i < w; i++) {
						for (int j = 0; j < h; j++) {
							if (averaged[i][j] == -1) {
								int channel = InfoPanel.thresholdChannelsComboBox.getSelectedIndex();
								int[] rgb = BezierCurve.getRGB(frame.displayedImageStack, i, j);
								switch (channel) {
								case 0:
									averaged[i][j] = rgb[0];
								case 1:
									averaged[i][j] = rgb[1];
								case 2:
									averaged[i][j] = rgb[2];
								}
							}
						}
					}

					// Now we can export the data
					PrintWriter out = new PrintWriter(new FileWriter(file));

					if (EXPORT_HEADERS) {
						// If the number of images in the stack is 1, then we ignore the stack options.
						if (ControlPanel.currentLayerSlider.getMaximum() == 1) {
							// Either exports all the curves or only the selected curves, depending on the
							// export option.
							if (exportAllCurves.isSelected()) {
								for (Curve c : curves) {
									exportCurveData(c, out, averaged);
								}
							} else {
								for (Curve c : curves.getSelected()) {
									exportCurveData(c, out, averaged);
								}
							}
						} // TODO keyframe based export.
						else // All frames.
						if (exportEachFrame.isSelected()) {
							for (int frame = 1; frame <= ControlPanel.currentLayerSlider.getMaximum(); frame++) {

							}
						} else {

						}
					} else // TODO remove later
					// If the number of images in the stack is 1, then we ignore the stack options.
					if (ControlPanel.currentLayerSlider.getMaximum() == 1) {
						// Either exports all the curves or only the selected curves, depending on the
						// export option.
						printHeaders(out);
						if (exportAllCurves.isSelected()) {
							for (Curve c : curves) {
								c.printValues(out, averaged, exportAllDataPoints.isSelected());
							}
						} else {
							for (Curve c : curves.getSelected()) {
								c.printValues(out, averaged, exportAllDataPoints.isSelected());
							}
						}
					}

					out.close();
				} catch (Exception err) {
					frame.overlay.setVisible(true);
					frame.overlay.drawNotification("There was an error exporting the curve data",
							frame.scrollPane.getVisibleRect());
					err.printStackTrace();
				}
			}
		}
	}
}
