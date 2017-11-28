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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.border.EtchedBorder;

public class ToolPanel extends JPanel {

	public static final int NO_TOOLS = 3;
	public static final int NO_OTHER_BUTTONS = 2;

	// Information for the tool icons and their corresponding menu icons
	final static String[] TOOL_TOOLTIPS = { "Selection Tool (V)", "Hand Tool (H)", "Control Point Tool (B)" };
	final static String[] TOOL_MENU_NAMES = { "Selection Tool", "Hand Tool", "Control Point Tool" };
	final static String[] TOOL_FILENAMES = { "direct-selection", "hand", "points" };
	final static int[] TOOL_MNEMONICS = { KeyEvent.VK_V, KeyEvent.VK_H, KeyEvent.VK_B };
	final static Cursor[] TOOL_CURSORS = { Cursor.getDefaultCursor(), Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
			Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) };
	private JToggleButton[] toolButtons = new JToggleButton[NO_TOOLS];
	private ButtonGroup toolGroup;
	private ImageIcon[] toolIcons = new ImageIcon[NO_TOOLS];

	// The other buttons in the tool bar
	final static String[] OTHER_TOOLTIPS = { "Zoom In", "Zoom Out" };
	final static String[] OTHER_FILENAMES = { "zoom-in", "zoom-out" };
	private JButton[] otherButtons = new JButton[NO_OTHER_BUTTONS];
	private ImageIcon[] otherIcons = new ImageIcon[NO_OTHER_BUTTONS];
	protected JButton export = new JButton(new ImageIcon(ToolPanel.class.getResource("/icons/export.png")));
	protected JButton exportAverage = new JButton(new ImageIcon(ToolPanel.class.getResource("/icons/export.png")));
	protected JToggleButton inspector = new JToggleButton(
			new ImageIcon(ToolPanel.class.getResource("/icons/inspector.png")));

	private static final long serialVersionUID = 1L;

	private KappaFrame frame;

	/**
	 * Constructs a new ToolPanel object
	 */
	public ToolPanel(KappaFrame frame) {

		this.frame = frame;

		setBackground(KappaFrame.PANEL_COLOR);
		setPreferredSize(new Dimension(0, 35));
		setBorder(BorderFactory.createLineBorder(Color.GRAY));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		toolGroup = new ButtonGroup();

		// Adds all the tool buttons
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		for (int i = 0; i < NO_TOOLS; i++) {
			toolIcons[i] = new ImageIcon(ToolPanel.class.getResource("/icons/" + TOOL_FILENAMES[i] + ".png"));
			toolButtons[i] = new JToggleButton(toolIcons[i]);
			toolButtons[i].setToolTipText(TOOL_TOOLTIPS[i]);
			toolButtons[i].setEnabled(false);
			final int j = i;
			toolButtons[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent a) {
					frame.scrollPane.setCursor(TOOL_CURSORS[j]);
				}
			});
			this.add(toolButtons[i]);
			toolGroup.add(toolButtons[i]);
		}

		for (int i = 0; i < NO_OTHER_BUTTONS; i++) {
			otherIcons[i] = new ImageIcon(ToolPanel.class.getResource("/icons/" + OTHER_FILENAMES[i] + ".png"));
			otherButtons[i] = new JButton(otherIcons[i]);
			otherButtons[i].setToolTipText(OTHER_TOOLTIPS[i]);
			otherButtons[i].setEnabled(false);

			// Adds spacers at desired intervals
			if (i == 0 || i == 3) {
				addSpacer(4);
			}
			this.add(otherButtons[i]);
		}

		// The inspector and export toggles.
		this.add(Box.createHorizontalGlue());
		export.setToolTipText("Export All Points");
		export.addActionListener(new ExportListener());
		exportAverage.setToolTipText("Export Averages Points");
		exportAverage.addActionListener(new ExportAverageListener());
		this.add(export);
		this.add(exportAverage);

		inspector.setToolTipText("Inspector");
		inspector.setSelected(true);
		inspector.addActionListener(new InspectorListener());
		this.add(inspector);

		// Sets up the listeners and keyboard shortcuts for our toolbar buttons
		otherButtons[0].addActionListener(new ZoomInListener());
		otherButtons[1].addActionListener(new ZoomOutListener());
	}

	/**
	 * Draws a separator flanked by a space of a desired number of pixels
	 *
	 * @param spaceSize
	 *            The size of the space
	 */
	private void addSpacer(int spaceSize) {
		this.add(Box.createRigidArea(new Dimension(spaceSize, 0)));
		JSeparator spacer = new JSeparator(JSeparator.VERTICAL);
		spacer.setMaximumSize(new Dimension(10, 35));
		this.add(spacer);
		this.add(Box.createRigidArea(new Dimension(spaceSize, 0)));
	}

	public void enableAllButtons() {
		for (JToggleButton b : toolButtons) {
			b.setEnabled(true);
		}
		for (JButton b : otherButtons) {
			b.setEnabled(true);
		}
		toolButtons[0].setSelected(true);
		toolButtons[0].setCursor(TOOL_CURSORS[0]);
	}

	private class ExportListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			CurvesExporter exporter = new CurvesExporter(frame);
			try {
				exporter.export(false);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class ExportAverageListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			CurvesExporter exporter = new CurvesExporter(frame);
			try {
				exporter.export(true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class InspectorListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			if (inspector.isSelected()) {
				frame.add(frame.infoPanel, BorderLayout.EAST);
			}
			frame.infoPanel.setVisible(inspector.isSelected());
		}
	}

	public boolean isSelected(int i) {
		return toolButtons[i].isSelected();
	}

	public boolean isEnabled(int i) {
		return toolButtons[i].isEnabled();
	}

	public void setSelected(int i, boolean selected) {
		toolButtons[i].setSelected(selected);
	}

	public void setEnabled(int i, boolean enabled) {
		toolButtons[i].setEnabled(enabled);
	}
}
