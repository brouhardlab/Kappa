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

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.scijava.Context;
import org.scijava.convert.ConvertService;

import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import ij.process.ImageConverter;
import ij.process.StackConverter;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import sc.fiji.kappa.curve.BSpline;
import sc.fiji.kappa.curve.Curve;

public class MenuBar extends JMenuBar {

	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	// X axis parameterization for histograms
	// 0 = Parameterized by X-Coordinate
	// 1 = Parameterized by Arc Length
	// 2 = Parameterized by Point Index
	public static final int DEFAULT_DISTRIBUTION_DISPLAY = 2;
	public static int distributionDisplay;

	// File handlers
	File file;
	JFileChooser kappaOpen;
	JFileChooser kappaLoad;
	JFileChooser kappaSave;

	// Menu Items
	static JMenuItem[] toolMenuItems = new JMenuItem[ToolPanel.NO_TOOLS];
	static JMenuItem zoomIn;
	static JMenuItem zoomOut;
	static JMenuItem prevFrame, nextFrame, prevKeyframe, nextKeyframe;
	static JMenuItem adjustBrightnessContrast;

	static JMenuItem delete, enter, fit;
	static JCheckBoxMenuItem boundingBoxMenu;
	static JCheckBoxMenuItem scaleCurvesMenu;
	public static JCheckBoxMenuItem antialiasingMenu;
	static JCheckBoxMenuItem tangentMenu;
	static JCheckBoxMenuItem bit8;
	static JCheckBoxMenuItem bit16;
	static JCheckBoxMenuItem bit32;
	static JCheckBoxMenuItem RGBColor;

	/**
	 * Creates a menu-bar and adds menu items to it
	 */
	public MenuBar(Context context) {
		context.inject(this);

		// Creates a new file chooser. Same native image support as ImageJ since ImageJ
		// libraries are used.
		kappaOpen = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "tif", "tiff", "jpeg", "jpg", "bmp",
				"fits", "pgm", "ppm", "pbm", "gif", "png", "dic", "dcm", "dicom", "lsm", "avi");
		kappaOpen.setFileFilter(filter);

		// File chooser for curve data
		kappaLoad = new JFileChooser();
		FileNameExtensionFilter kappaFilter = new FileNameExtensionFilter("Kappa Files", "kapp");
		kappaLoad.setFileFilter(kappaFilter);
		kappaLoad.setDialogTitle("Load Existing Curve Data");
		kappaSave = new JFileChooser();
		kappaSave.setFileFilter(kappaFilter);
		kappaSave.setDialogTitle("Save Curve Data");

		// Declares the file menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		// Menu Items for file operations
		JMenuItem openMenu = new JMenuItem("Open Image File");
		openMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, DEFAULT_MASK));
		openMenu.addActionListener(e -> {
			int returnVal = kappaOpen.showOpenDialog(KappaFrame.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				openFile(kappaOpen.getSelectedFile());
			}
		});

		fileMenu.add(openMenu);

		JMenuItem openActiveMenu = new JMenuItem("Open Active Image");
		openActiveMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, DEFAULT_MASK));
		openActiveMenu.addActionListener(e -> {
			openActiveImage(context);
		});
		fileMenu.add(openActiveMenu);
		fileMenu.addSeparator();

		JMenuItem loadMenu = new JMenuItem("Load Curve Data");
		loadMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, DEFAULT_MASK));
		loadMenu.addActionListener(e -> {
			// Handle open button action.
			int returnVal = kappaLoad.showOpenDialog(KappaFrame.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = kappaLoad.getSelectedFile();
				loadCurveFile(file);
			}
		});
		fileMenu.add(loadMenu);

		JMenuItem saveMenu = new JMenuItem("Save Curve Data");
		saveMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, DEFAULT_MASK));
		saveMenu.addActionListener(e -> {
			// Handles save button action.
			int returnVal = kappaSave.showSaveDialog(KappaFrame.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = kappaSave.getSelectedFile();
				// Appends a .kapp
				if (!file.getPath().toLowerCase().endsWith(".kapp")) {
					file = new File(file.getPath() + ".kapp");
				}
				saveCurveFile(file);
			}
		});
		fileMenu.add(saveMenu);

		this.add(fileMenu);

		// Menu Items for all the tools
		JMenu toolMenu = new JMenu("Tools");
		for (int i = 0; i < ToolPanel.NO_TOOLS; i++) {
			toolMenuItems[i] = new JMenuItem(ToolPanel.TOOL_MENU_NAMES[i]);
			toolMenuItems[i].setEnabled(false);
			toolMenuItems[i].setAccelerator(KeyStroke.getKeyStroke(ToolPanel.TOOL_MNEMONICS[i], 0));
			final int j = i;
			toolMenuItems[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					KappaFrame.toolPanel.setSelected(j, true);
					KappaFrame.scrollPane.setCursor(ToolPanel.TOOL_CURSORS[j]);
				}
			});
			toolMenu.add(toolMenuItems[i]);
		}

		// We also add a menu item for deleting Bezier Curves via the Backspace key.
		delete = new JMenuItem("Delete Curves");
		delete.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				KappaFrame.deleteCurve();
			}
		});
		delete.setEnabled(false);
		delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
		toolMenu.addSeparator();
		toolMenu.add(delete);

		enter = new JMenuItem("Enter Curve");
		enter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				KappaFrame.enterCurve();
			}
		});
		enter.setEnabled(false);
		enter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		toolMenu.add(enter);

		fit = new JMenuItem("Fit Curve");
		fit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				KappaFrame.fitCurves();
			}
		});
		fit.setEnabled(false);
		fit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
		toolMenu.add(fit);
		toolMenu.addSeparator();

		// TODO remove this later
		// JMenuItem runTestScript = new JMenuItem ("Run Testing Script");
		// runTestScript.addActionListener (new ActionListener(){
		// public void actionPerformed (ActionEvent event){
		// try{KappaFrame.testingScript();}
		// catch(IOException e){System.out.println("Script Error");}
		// }});
		// runTestScript.setAccelerator (KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
		// toolMenu.add(runTestScript);
		JCheckBoxMenuItem toggleCtrlPtAdjustment = new JCheckBoxMenuItem("Enable Control Point Adjustment");
		toggleCtrlPtAdjustment.setState(KappaFrame.enableCtrlPtAdjustment);
		toggleCtrlPtAdjustment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KappaFrame.enableCtrlPtAdjustment = !KappaFrame.enableCtrlPtAdjustment;
				;
			}
		});
		toggleCtrlPtAdjustment.setEnabled(true);
		toolMenu.add(toggleCtrlPtAdjustment);

		this.add(toolMenu);

		// Navigation Menu
		// TODO FIX action listeners to these.
		JMenu navigateMenu = new JMenu("Navigate");
		prevFrame = new JMenuItem("Previous Frame");
		nextFrame = new JMenuItem("Next Frame");
		prevKeyframe = new JMenuItem("Previous Keyframe");
		nextKeyframe = new JMenuItem("Next Keyframe");
		prevFrame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.ALT_MASK));
		nextFrame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.ALT_MASK));
		prevKeyframe.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, DEFAULT_MASK));
		nextKeyframe.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, DEFAULT_MASK));
		prevFrame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ControlPanel.currentLayerSlider.setValue(Math.max(ControlPanel.currentLayerSlider.getValue() - 1,
						ControlPanel.currentLayerSlider.getMinimum()));
			}
		});
		nextFrame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ControlPanel.currentLayerSlider.setValue(Math.min(ControlPanel.currentLayerSlider.getValue() + 1,
						ControlPanel.currentLayerSlider.getMaximum()));
			}
		});
		prevKeyframe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
			}
		});
		nextKeyframe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
			}
		});
		prevFrame.setEnabled(false);
		nextFrame.setEnabled(false);
		prevKeyframe.setEnabled(false);
		nextKeyframe.setEnabled(false);
		navigateMenu.add(prevFrame);
		navigateMenu.add(nextFrame);
		navigateMenu.add(prevKeyframe);
		navigateMenu.add(nextKeyframe);
		this.add(navigateMenu);

		// Image options.
		JMenu imageMenu = new JMenu("Image");
		JMenu imageTypeSubmenu = new JMenu("Image Type");
		ButtonGroup imageTypeGroup = new ButtonGroup();
		bit8 = new JCheckBoxMenuItem("8-Bit Grayscale");
		bit16 = new JCheckBoxMenuItem("16-Bit Grayscale");
		bit32 = new JCheckBoxMenuItem("32-Bit Grayscale");
		RGBColor = new JCheckBoxMenuItem("RGB Color");
		bit8.setEnabled(false);
		bit16.setEnabled(false);
		bit32.setEnabled(false);
		RGBColor.setEnabled(false);
		imageTypeGroup.add(bit8);
		imageTypeGroup.add(bit16);
		imageTypeGroup.add(bit32);
		imageTypeGroup.add(RGBColor);

		// 8-Bit conversion. Uses ImageJ libraries to convert, which handles unallowed
		// conversion schemes.
		bit8.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (KappaFrame.displayedImageStack.getNSlices() > 1) {
						(new StackConverter(KappaFrame.displayedImageStack)).convertToGray8();
					} else {
						(new ImageConverter(KappaFrame.displayedImageStack)).convertToGray8();
					}

					for (int i = 0; i < 3; i++) {
						ControlPanel.channelButtons[i].setSelected(false);
						ControlPanel.channelButtons[i].setEnabled(false);
					}
					KappaFrame.currImage = KappaFrame.displayedImageStack.getBufferedImage();
					KappaFrame.setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
					KappaFrame.infoPanel.repaint();
					KappaFrame.drawImageOverlay();
				} catch (IllegalArgumentException err) {
					KappaFrame.overlay.setVisible(true);
					KappaFrame.overlay.drawNotification("Illegal Conversion Option",
							KappaFrame.scrollPane.getVisibleRect());
				}
			}
		});
		imageTypeSubmenu.add(bit8);

		// 16-Bit conversion. Uses ImageJ libraries to convert, which handles unallowed
		// conversion schemes.
		bit16.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (KappaFrame.displayedImageStack.getNSlices() > 1) {
						(new StackConverter(KappaFrame.displayedImageStack)).convertToGray16();
					} else {
						(new ImageConverter(KappaFrame.displayedImageStack)).convertToGray16();
					}

					for (int i = 0; i < 3; i++) {
						ControlPanel.channelButtons[i].setSelected(false);
						ControlPanel.channelButtons[i].setEnabled(false);
					}
					KappaFrame.currImage = KappaFrame.displayedImageStack.getBufferedImage();
					KappaFrame.setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
					KappaFrame.infoPanel.repaint();
					KappaFrame.drawImageOverlay();
				} catch (IllegalArgumentException err) {
					KappaFrame.overlay.setVisible(true);
					KappaFrame.overlay.drawNotification("Illegal Conversion Option",
							KappaFrame.scrollPane.getVisibleRect());
				}
			}
		});
		imageTypeSubmenu.add(bit16);

		// 32-Bit conversion. Uses ImageJ libraries to convert, which handles unallowed
		// conversion schemes.
		bit32.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (KappaFrame.displayedImageStack.getNSlices() > 1) {
						(new StackConverter(KappaFrame.displayedImageStack)).convertToGray32();
					} else {
						(new ImageConverter(KappaFrame.displayedImageStack)).convertToGray32();
					}

					for (int i = 0; i < 3; i++) {
						ControlPanel.channelButtons[i].setSelected(false);
						ControlPanel.channelButtons[i].setEnabled(false);
					}
					KappaFrame.currImage = KappaFrame.displayedImageStack.getBufferedImage();
					KappaFrame.setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
					KappaFrame.infoPanel.repaint();
					KappaFrame.drawImageOverlay();
				} catch (IllegalArgumentException err) {
					KappaFrame.overlay.setVisible(true);
					KappaFrame.overlay.drawNotification("Illegal Conversion Option",
							KappaFrame.scrollPane.getVisibleRect());
				}
			}
		});
		imageTypeSubmenu.add(bit32);

		// RGB Color conversion.
		RGBColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Merges the red, green and blue channels from the original image. If this was
				// originally grayscale
				// the merged image is grayscale. If it was originally color, the merged image
				// is color.
				RGBStackMerge merge = new RGBStackMerge();
				KappaFrame.displayedImageStack = new ImagePlus(KappaFrame.imageStack.getTitle(),
						merge.mergeStacks(KappaFrame.imageStack.getWidth(), KappaFrame.imageStack.getHeight(),
								KappaFrame.imageStack.getNSlices(), KappaFrame.imageStackLayers[0],
								KappaFrame.imageStackLayers[1], KappaFrame.imageStackLayers[2], true));

				for (int i = 0; i < 3; i++) {
					ControlPanel.channelButtons[i].setSelected(true);
					ControlPanel.channelButtons[i].setEnabled(true);
				}
				KappaFrame.currImage = KappaFrame.displayedImageStack.getBufferedImage();
				KappaFrame.setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
				KappaFrame.infoPanel.repaint();
				KappaFrame.drawImageOverlay();
			}
		});
		imageTypeSubmenu.add(RGBColor);

		// Adds the submenu to the menu
		imageMenu.add(imageTypeSubmenu);

		// Brightness and Contrast tool. Taken from ImageJ.
		imageMenu.addSeparator();
		adjustBrightnessContrast = new JMenuItem("Adjust Brightness/Contrast");
		adjustBrightnessContrast.setEnabled(false);
		adjustBrightnessContrast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ContrastAdjuster c = new ContrastAdjuster();
				c.run("Brightness/Contrast...[C]");
			}
		});
		imageMenu.add(adjustBrightnessContrast);
		this.add(imageMenu);

		// Zoom-In and Zoom-Out Commands
		JMenu viewMenu = new JMenu("View");
		zoomIn = new JMenuItem("Zoom In");
		zoomOut = new JMenuItem("Zoom Out");
		zoomIn.addActionListener(new ZoomInListener());
		zoomOut.addActionListener(new ZoomOutListener());
		zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, DEFAULT_MASK));
		zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, DEFAULT_MASK));
		zoomIn.setEnabled(false);
		zoomOut.setEnabled(false);

		// Menu Item for showing bounding boxes
		boundingBoxMenu = new JCheckBoxMenuItem("Show Bounding Boxes");
		boundingBoxMenu.setState(false);
		boundingBoxMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				KappaFrame.drawImageOverlay();
			}
		});
		boundingBoxMenu.setEnabled(false);

		// Menu Item for choosing the x-axis values for the curvature and intensity
		// display
		// For instance, you can display x vs. curvature, or current arc length vs
		// curvature, or the point index vs curvature
		// The default is the point index.
		distributionDisplay = DEFAULT_DISTRIBUTION_DISPLAY;
		JMenu xAxisSubmenu = new JMenu("Curve Distribution X-Axis:");
		ButtonGroup xAxisGroup = new ButtonGroup();
		JMenuItem xValue = new JCheckBoxMenuItem("X-Coordinate");
		JMenuItem curveLength = new JCheckBoxMenuItem("Arc Length");
		JMenuItem pointIndex = new JCheckBoxMenuItem("Point Index");
		xAxisGroup.add(xValue);
		xAxisGroup.add(curveLength);
		xAxisGroup.add(pointIndex);
		xAxisSubmenu.add(xValue);
		xAxisSubmenu.add(curveLength);
		xAxisSubmenu.add(pointIndex);
		xValue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				distributionDisplay = 0;
				InfoPanel.updateHistograms();
			}
		});
		curveLength.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				distributionDisplay = 1;
				InfoPanel.updateHistograms();
			}
		});
		pointIndex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				distributionDisplay = 2;
				InfoPanel.updateHistograms();
			}
		});
		if (DEFAULT_DISTRIBUTION_DISPLAY == 0) {
			xValue.setSelected(true);
		} else if (DEFAULT_DISTRIBUTION_DISPLAY == 1) {
			curveLength.setSelected(true);
		} else {
			pointIndex.setSelected(true);
		}

		// Menu Item for scaling curve strokes when zooming in or out
		scaleCurvesMenu = new JCheckBoxMenuItem("Scale Curve Strokes");
		scaleCurvesMenu.setState(true);
		scaleCurvesMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				KappaFrame.drawImageOverlay();
			}
		});
		scaleCurvesMenu.setEnabled(false);

		// Menu Item for image antialiasing
		antialiasingMenu = new JCheckBoxMenuItem("Enable Antialiasing");
		antialiasingMenu.setState(false);
		antialiasingMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				KappaFrame.setScaledImage(ControlPanel.scaleSlider.getValue() / 100.0);
				KappaFrame.drawImageOverlay();
			}
		});
		antialiasingMenu.setEnabled(false);

		// Menu Item for displaying tangent and normal curves.
		tangentMenu = new JCheckBoxMenuItem("Show Tangent and Normal Vectors");
		tangentMenu.setState(false);
		tangentMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				KappaFrame.drawImageOverlay();
			}
		});
		tangentMenu.setEnabled(false);

		viewMenu.add(zoomIn);
		viewMenu.add(zoomOut);
		viewMenu.addSeparator();
		viewMenu.add(xAxisSubmenu);
		viewMenu.addSeparator();
		viewMenu.add(scaleCurvesMenu);
		viewMenu.add(tangentMenu);
		viewMenu.add(boundingBoxMenu);
		viewMenu.add(antialiasingMenu);
		this.add(viewMenu);

		// Sets a "Help" menu list
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');

		// Adds an "About" option to the menu list
		JMenuItem aboutMenuItem = new JMenuItem("About...", 'A');
		aboutMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JOptionPane.showMessageDialog(KappaFrame.frame, "Developed by the Brouhard lab, 2016-2017.",
						KappaFrame.APPLICATION_NAME, JOptionPane.INFORMATION_MESSAGE);
			}
		});

		// Adds a link to the User Manual
		JMenuItem userManualLink = new JMenuItem("User Manual");
		userManualLink.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().browse(
								new URI("https://dl.dropboxusercontent.com/u/157117/KappaFrame%20User%20Manual.pdf"));
					}
				} catch (Exception e) {
					System.out.println("Incorrect URL Syntax");
				}
				;
			}
		});

		// Adds all newly created menu items to the "Help" list
		helpMenu.add(userManualLink);
		helpMenu.add(aboutMenuItem);
		this.add(helpMenu);
	}

	static void openFile(File file) {
		ImagePlus imp = new ImagePlus(file.getPath());
		openImage(imp);
	}

	static void openActiveImage(Context context) {
		ImageDisplayService imds = context.getService(ImageDisplayService.class);
		ConvertService convert = context.getService(ConvertService.class);
		ImageDisplay imd = imds.getActiveImageDisplay();
		ImagePlus imp = convert.convert(imd, ImagePlus.class);
		openImage(imp);
	}

	static void openImage(ImagePlus imp) {
		KappaFrame.imageStack = imp;

		// Splits the image into the R, G, and B channels, but only if the image is in
		// RGB color
		if (KappaFrame.imageStack.getType() == ImagePlus.COLOR_RGB) {
			KappaFrame.imageStackLayers = ChannelSplitter.splitRGB(KappaFrame.imageStack.getImageStack(), true);
		}

		// Sets the displayed Image Stack to all the channels to begin with.
		KappaFrame.displayedImageStack = KappaFrame.imageStack;

		// Sets the image type in the menu
		bit8.setEnabled(true);
		bit16.setEnabled(true);
		bit32.setEnabled(true);
		RGBColor.setEnabled(true);

		switch (KappaFrame.displayedImageStack.getType()) {
		case ImagePlus.GRAY8:
			bit8.setSelected(true);
			break;
		case ImagePlus.GRAY16:
			bit16.setSelected(true);
			break;
		case ImagePlus.GRAY32:
			bit32.setSelected(true);
			break;
		default:
			RGBColor.setSelected(true);
			break;
		}

		KappaFrame.maxLayer = KappaFrame.imageStack.getNSlices();
		KappaFrame.maxLayerDigits = Integer.toString(KappaFrame.maxLayer).length();
		ControlPanel.currentLayerSlider.setValue(KappaFrame.INIT_LAYER);
		ControlPanel.currentLayerSlider.setMaximum(KappaFrame.maxLayer);
		ControlPanel.currentLayerSlider.setMajorTickSpacing(KappaFrame.imageStack.getNSlices() / 10);
		ControlPanel.currentLayerSlider.setPaintTicks(true);
		ControlPanel.currentLayerSlider.setEnabled(true);
		ControlPanel.scaleSlider.setEnabled(true);

		// Sets the maximum intensity depending on the bit depth of the image.
		if (RGBColor.isSelected()) {
			InfoPanel.dataThresholdSlider.setMaximum(256);
		} else {
			InfoPanel.dataThresholdSlider.setMaximum((int) (Math.pow(2, KappaFrame.displayedImageStack.getBitDepth())));
		}

		// We disable export options for stacks if this is just a single image.
		if (KappaFrame.maxLayer == 1) {
			KappaFrame.exportPanel.perStackPanel.hide();
		} else if (KappaFrame.maxLayer > 1) {
			KappaFrame.exportPanel.perStackPanel.show();
		}
		KappaFrame.exportPanel.repaint();

		// Sets the buttons to active and selected if the image type is a Color one.
		// Otherwise sets them to inactive
		for (int i = 0; i < 3; i++) {
			ControlPanel.channelButtons[i].setEnabled(KappaFrame.imageStack.getType() == ImagePlus.COLOR_RGB);
			ControlPanel.channelButtons[i].setSelected(KappaFrame.imageStack.getType() == ImagePlus.COLOR_RGB);
		}

		// Sets the scroll pane in the drawing panel to display the first layer of the
		// image now
		KappaFrame.imageStack.setZ(1);
		KappaFrame.currImage = KappaFrame.displayedImageStack.getBufferedImage();
		KappaFrame.currImageLabel.setIcon(new ImageIcon(KappaFrame.currImage));
		KappaFrame.thresholded = new boolean[KappaFrame.currImage.getWidth()][KappaFrame.currImage.getHeight()];

		// Sets the maximum scale to a value that prevents a heap space error from
		// occuring.
		// We set the maximum image size to about 2000 x 2000 pixels = 4,000,000 pixels.
		int avgPixelDim = (KappaFrame.currImage.getWidth() + KappaFrame.currImage.getHeight()) / 2;
		ControlPanel.scaleSlider.setValue(ControlPanel.DEFAULT_SCALE);
		ControlPanel.scaleSlider
				.setMaximum(Math.min(ControlPanel.MAX_SCALE, ControlPanel.MAX_AVG_PIXEL_DIM / avgPixelDim * 100));

		KappaFrame.updateThresholded();
		InfoPanel.thresholdChannelsComboBox.setEnabled(true);
		InfoPanel.thresholdSlider.setEnabled(true);
		InfoPanel.rangeAveragingSpinner.setEnabled(true);
		InfoPanel.bgCheckBox.setEnabled(true);
		InfoPanel.apply.setEnabled(true);
		InfoPanel.revert.setEnabled(true);
		fit.setEnabled(true);

		// Enables view checkboxes
		boundingBoxMenu.setEnabled(true);
		scaleCurvesMenu.setEnabled(true);
		antialiasingMenu.setEnabled(true);
		tangentMenu.setEnabled(true);

		// Enables toolbar buttons and selects the direct selection tool
		KappaFrame.toolPanel.enableAllButtons();

		// Enables Menu Items
		zoomIn.setEnabled(true);
		zoomOut.setEnabled(true);
		for (JMenuItem menuItem : toolMenuItems) {
			menuItem.setEnabled(true);
		}
		prevFrame.setEnabled(true);
		nextFrame.setEnabled(true);
		prevKeyframe.setEnabled(true);
		nextKeyframe.setEnabled(true);
		adjustBrightnessContrast.setEnabled(true);

		// Adds file name to the frame.
		KappaFrame.frame.setTitle(KappaFrame.APPLICATION_NAME + "- " + imp.getTitle());
	}

	static void loadCurveFile(File file) {
		// Tries opening the file
		try {
			KappaFrame.resetCurves();
			BufferedReader in = new BufferedReader(new FileReader(file));
			int noCurves = Integer.parseInt(in.readLine());

			for (int n = 0; n < noCurves; n++) {
				int curveType = Integer.parseInt(in.readLine());
				int noKeyframes = Integer.parseInt(in.readLine());
				int noCtrlPts = Integer.parseInt(in.readLine());
				int bsplineType = 0;
				KappaFrame.points = new ArrayList<Point2D>(noCtrlPts);

				// If the curve is a B-Spline, there is an extra parameter determining whether
				// it's open or closed
				if (curveType == KappaFrame.B_SPLINE) {
					bsplineType = Integer.parseInt(in.readLine());
				}

				// Initialize the curve
				int currentKeyframe = Integer.parseInt(in.readLine());
				for (int i = 0; i < noCtrlPts; i++) {
					KappaFrame.points.add(
							new Point2D.Double(Double.parseDouble(in.readLine()), Double.parseDouble(in.readLine())));
				}

				if (curveType == KappaFrame.B_SPLINE) {
					KappaFrame.curves.addCurve(KappaFrame.points, currentKeyframe, noCtrlPts, KappaFrame.B_SPLINE,
							bsplineType == BSpline.OPEN, (Integer) (InfoPanel.thresholdRadiusSpinner.getValue()));
				} else {
					KappaFrame.curves.addCurve(KappaFrame.points, currentKeyframe, noCtrlPts, KappaFrame.BEZIER_CURVE,
							true, (Integer) (InfoPanel.thresholdRadiusSpinner.getValue()));
				}
				InfoPanel.listData.addElement("  CURVE " + KappaFrame.curves.getCount());
				InfoPanel.list.setListData(InfoPanel.listData);

				// Load all the other keyframes for the curve
				for (int i = 1; i < noKeyframes; i++) {
					currentKeyframe = Integer.parseInt(in.readLine());
					KappaFrame.points = new ArrayList<Point2D>(noCtrlPts);

					// Adds the control points for each keyframe. We add the redundant control
					// points for closed B-Spline curves.
					if (bsplineType == BSpline.OPEN) {
						for (int j = 0; j < noCtrlPts; j++) {
							KappaFrame.points.add(new Point2D.Double(Double.parseDouble(in.readLine()),
									Double.parseDouble(in.readLine())));
						}
					} else {
						for (int j = 0; j < noCtrlPts - BSpline.B_SPLINE_DEGREE; j++) {
							KappaFrame.points.add(new Point2D.Double(Double.parseDouble(in.readLine()),
									Double.parseDouble(in.readLine())));
						}
						for (int j = 0; j < BSpline.B_SPLINE_DEGREE; j++) {
							KappaFrame.points.add(new Point2D.Double(KappaFrame.points.get(i).getX(),
									KappaFrame.points.get(i).getY()));
						}
					}
					KappaFrame.curves.get(KappaFrame.curves.size() - 1).addKeyframe(KappaFrame.points, currentKeyframe);
				}
			}

			// Scales the test curves I generated (from an 82 pixel wide image), for use on
			// images of different sizes
			if (KappaFrame.DEBUG_MODE) {
				for (Curve c : KappaFrame.curves) {
					c.scale(KappaFrame.currImage.getWidth() / 82.0, ControlPanel.currentLayerSlider.getValue());
				}
			}

			// Translates all the curves to their position at the current frame.
			KappaFrame.curves.changeFrame(ControlPanel.currentLayerSlider.getValue());

			KappaFrame.drawImageOverlay();
			in.close();
		} catch (Exception err) {
			KappaFrame.overlay.setVisible(true);
			KappaFrame.overlay.drawNotification("There was an error loading the curve data",
					KappaFrame.scrollPane.getVisibleRect());
			err.printStackTrace();
		}
	}

	static void saveCurveFile(File file) {

		try {
			PrintWriter out = new PrintWriter(new FileWriter(file));
			out.println(KappaFrame.curves.size());

			for (Curve c : KappaFrame.curves) {
				// Outputs the curve properties: it's type, the number of keyframes, the number
				// of control points, etc.
				if (c instanceof BSpline) {
					out.println(KappaFrame.B_SPLINE);
				} else {
					out.println(KappaFrame.BEZIER_CURVE);
				}
				out.println(c.getNoKeyframes());

				// Print out the correct number of control points depending on the curve type.
				if (c instanceof BSpline && !((BSpline) c).isOpen()) {
					out.println(c.getNoCtrlPts() - BSpline.B_SPLINE_DEGREE);
				} else {
					out.println(c.getNoCtrlPts());
				}

				if (c instanceof BSpline) {
					if (((BSpline) c).isOpen()) {
						out.println(BSpline.OPEN);
					} else {
						out.println(BSpline.CLOSED);
					}
				}

				// Writes the control points and what keyframe they are at for each curve.
				for (Curve.BControlPoints b : c.keyframes) {
					out.println(b.t);

					// If it's a closed B-Spline, we don't output the last redundant points that
					// make it closed
					if (c instanceof BSpline && !((BSpline) c).isOpen()) {
						for (int i = 0; i < b.defPoints.length - BSpline.B_SPLINE_DEGREE; i++) {
							Point2D p = b.defPoints[i];
							out.println(p.getX());
							out.println(p.getY());
						}
					} // Otherwise, we output all the points
					else {
						for (Point2D p : b.defPoints) {
							out.println(p.getX());
							out.println(p.getY());
						}
					}
				}
			}
			out.close();
		} catch (Exception err) {
			KappaFrame.overlay.setVisible(true);
			KappaFrame.overlay.drawNotification("There was an error saving the curve data",
					KappaFrame.scrollPane.getVisibleRect());
		}
	}
}
