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
import java.util.List;

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

import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.ChannelSplitter;
import ij.plugin.frame.RoiManager;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import sc.fiji.kappa.curve.BSpline;
import sc.fiji.kappa.curve.Curve;

public class KappaMenuBar extends JMenuBar {

	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	// X axis parameterization for histograms
	// 0 = Parameterized by X-Coordinate
	// 1 = Parameterized by Arc Length
	// 2 = Parameterized by Point Index
	public static final int DEFAULT_DISTRIBUTION_DISPLAY = 2;
	public static int distributionDisplay;

	@Parameter
	private LogService log;

	// File handlers
	private File file;
	private JFileChooser kappaOpen;
	private JFileChooser kappaLoad;
	private JFileChooser kappaSave;

	// Menu Items
	private JMenuItem[] toolMenuItems = new JMenuItem[ToolPanel.NO_TOOLS];
	private JMenuItem zoomIn;
	private JMenuItem zoomOut;
	private JMenuItem prevFrame, nextFrame, prevKeyframe, nextKeyframe;
	private JMenuItem adjustBrightnessContrast;

	private JMenuItem delete, enter, fit;
	private JCheckBoxMenuItem boundingBoxMenu;
	private JCheckBoxMenuItem scaleCurvesMenu;
	private JCheckBoxMenuItem antialiasingMenu;
	private JCheckBoxMenuItem tangentMenu;

	private KappaFrame frame;

	/**
	 * Creates a menu-bar and adds menu items to it
	 */
	public KappaMenuBar(Context context, KappaFrame frame) {
		context.inject(this);

		this.frame = frame;

		// File chooser for curve data
		FileNameExtensionFilter kappaFilter = new FileNameExtensionFilter("Kappa Files", "kapp");

		kappaLoad = new JFileChooser();
		kappaLoad.setFileFilter(kappaFilter);
		kappaLoad.setDialogTitle("Load Existing Curve Data");

		kappaSave = new JFileChooser();
		kappaSave.setFileFilter(kappaFilter);
		kappaSave.setDialogTitle("Save Curve Data");

		// Declares the file menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		/*
		 * // Menu Items for file operations // Creates a new file chooser. Same native
		 * image support as ImageJ since ImageJ // libraries are used. kappaOpen = new
		 * JFileChooser(); FileNameExtensionFilter filter = new
		 * FileNameExtensionFilter("Image Files", "tif", "tiff", "jpeg", "jpg", "bmp",
		 * "fits", "pgm", "ppm", "pbm", "gif", "png", "dic", "dcm", "dicom", "lsm",
		 * "avi"); kappaOpen.setFileFilter(filter);
		 * 
		 * JMenuItem openMenu = new JMenuItem("Open Image File");
		 * openMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, DEFAULT_MASK));
		 * openMenu.addActionListener(e -> { int returnVal =
		 * kappaOpen.showOpenDialog(this.frame); if (returnVal ==
		 * JFileChooser.APPROVE_OPTION) { openImageFile(kappaOpen.getSelectedFile()); }
		 * }); fileMenu.add(openMenu);
		 */

		JMenuItem openActiveMenu = new JMenuItem("Open Active Image");
		openActiveMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, DEFAULT_MASK));
		openActiveMenu.addActionListener(e -> {
			openActiveImage(context);
		});
		fileMenu.add(openActiveMenu);
		fileMenu.addSeparator();

		JMenuItem importROIsAsCurvesMenu = new JMenuItem("Import ROIs as curves");
		importROIsAsCurvesMenu.addActionListener(e -> {
			importROIsAsCurves(context);
		});
		fileMenu.add(importROIsAsCurvesMenu);
		fileMenu.addSeparator();

		JMenuItem loadMenu = new JMenuItem("Load Curve Data");
		loadMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, DEFAULT_MASK));
		loadMenu.addActionListener(e -> {
			// Handle open button action.
			int returnVal = kappaLoad.showOpenDialog(this.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = kappaLoad.getSelectedFile();
				loadCurveFile(file);
			}
		});
		fileMenu.add(loadMenu);

		JMenuItem saveMenu = new JMenuItem("Save Curve Data");
		saveMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, DEFAULT_MASK));
		saveMenu.addActionListener(e -> {

			String dirPath = frame.getImageStack().getOriginalFileInfo().directory;
			if (dirPath != null) {
				String kappaPath = FilenameUtils.removeExtension(frame.getImageStack().getOriginalFileInfo().fileName);
				kappaPath += ".kapp";
				File fullPath = new File(dirPath, kappaPath);
				kappaSave.setSelectedFile(fullPath);
			}

			// Handles save button action.
			int returnVal = kappaSave.showSaveDialog(this.frame);

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
					frame.getToolPanel().setSelected(j, true);
					frame.getScrollPane().setCursor(ToolPanel.TOOL_CURSORS[j]);
				}
			});
			toolMenu.add(toolMenuItems[i]);
		}

		// We also add a menu item for deleting Bezier Curves via the Backspace key.
		setDelete(new JMenuItem("Delete Curves"));
		getDelete().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				frame.deleteCurve();
			}
		});
		getDelete().setEnabled(false);
		getDelete().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		toolMenu.addSeparator();
		toolMenu.add(getDelete());

		setEnter(new JMenuItem("Enter Curve"));
		getEnter().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				frame.enterCurve();
			}
		});
		getEnter().setEnabled(false);
		getEnter().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		toolMenu.add(getEnter());

		fit = new JMenuItem("Fit Curve");
		fit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				frame.fitCurves();
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
		// try{frame.testingScript();}
		// catch(IOException e){System.out.println("Script Error");}
		// }});
		// runTestScript.setAccelerator (KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
		// toolMenu.add(runTestScript);
		JCheckBoxMenuItem toggleCtrlPtAdjustment = new JCheckBoxMenuItem("Enable Control Point Adjustment");
		toggleCtrlPtAdjustment.setState(frame.isEnableCtrlPtAdjustment());
		toggleCtrlPtAdjustment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setEnableCtrlPtAdjustment(!frame.isEnableCtrlPtAdjustment());
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
				frame.getControlPanel().getCurrentLayerSlider()
						.setValue(Math.max(frame.getControlPanel().getCurrentLayerSlider().getValue() - 1,
								frame.getControlPanel().getCurrentLayerSlider().getMinimum()));
			}
		});
		nextFrame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				frame.getControlPanel().getCurrentLayerSlider()
						.setValue(Math.min(frame.getControlPanel().getCurrentLayerSlider().getValue() + 1,
								frame.getControlPanel().getCurrentLayerSlider().getMaximum()));
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

		// Brightness and Contrast tool. Taken from ImageJ.
		adjustBrightnessContrast = new JMenuItem("Adjust Brightness/Contrast");
		adjustBrightnessContrast.setEnabled(false);
		adjustBrightnessContrast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ContrastAdjuster c = new ContrastAdjuster(frame);
				c.run("Brightness/Contrast...[C]");
			}
		});
		imageMenu.add(adjustBrightnessContrast);
		this.add(imageMenu);

		// Zoom-In and Zoom-Out Commands
		JMenu viewMenu = new JMenu("View");
		zoomIn = new JMenuItem("Zoom In");
		zoomOut = new JMenuItem("Zoom Out");
		zoomIn.addActionListener(new ZoomInListener(frame.getControlPanel()));
		zoomOut.addActionListener(new ZoomOutListener(frame.getControlPanel()));
		zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, DEFAULT_MASK));
		zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, DEFAULT_MASK));
		zoomIn.setEnabled(false);
		zoomOut.setEnabled(false);

		// Menu Item for showing bounding boxes
		setBoundingBoxMenu(new JCheckBoxMenuItem("Show Bounding Boxes"));
		getBoundingBoxMenu().setState(false);
		getBoundingBoxMenu().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				frame.drawImageOverlay();
			}
		});
		getBoundingBoxMenu().setEnabled(false);

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
				frame.getInfoPanel().updateHistograms();
			}
		});
		curveLength.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				distributionDisplay = 1;
				frame.getInfoPanel().updateHistograms();
			}
		});
		pointIndex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				distributionDisplay = 2;
				frame.getInfoPanel().updateHistograms();
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
		setScaleCurvesMenu(new JCheckBoxMenuItem("Scale Curve Strokes"));
		getScaleCurvesMenu().setState(true);
		getScaleCurvesMenu().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				frame.drawImageOverlay();
			}
		});
		getScaleCurvesMenu().setEnabled(false);

		// Menu Item for image antialiasing
		setAntialiasingMenu(new JCheckBoxMenuItem("Enable Antialiasing"));
		getAntialiasingMenu().setState(false);
		getAntialiasingMenu().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				frame.setScaledImage(frame.getControlPanel().getScaleSlider().getValue() / 100.0);
				frame.drawImageOverlay();
			}
		});
		getAntialiasingMenu().setEnabled(false);

		// Menu Item for displaying tangent and normal curves.
		setTangentMenu(new JCheckBoxMenuItem("Show Tangent and Normal Vectors"));
		getTangentMenu().setState(false);
		getTangentMenu().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				frame.drawImageOverlay();
			}
		});
		getTangentMenu().setEnabled(false);

		viewMenu.add(zoomIn);
		viewMenu.add(zoomOut);
		viewMenu.addSeparator();
		viewMenu.add(xAxisSubmenu);
		viewMenu.addSeparator();
		viewMenu.add(getScaleCurvesMenu());
		viewMenu.add(getTangentMenu());
		viewMenu.add(getBoundingBoxMenu());
		viewMenu.add(getAntialiasingMenu());
		this.add(viewMenu);

		// Sets a "Help" menu list
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');

		// Adds an "About" option to the menu list
		JMenuItem aboutMenuItem = new JMenuItem("About...", 'A');
		aboutMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JOptionPane.showMessageDialog(frame, "Developed by the Brouhard lab, 2016-2017.",
						frame.APPLICATION_NAME, JOptionPane.INFORMATION_MESSAGE);
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

	private void importROIsAsCurves(Context context) {

		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
			log.warn("RoiManager is empty. No curves imported.");
			return;
		}

		Roi[] rois = rm.getRoisAsArray();
		Roi roi;
		PolygonRoi polygonRoi;
		List<Point2D> points;
		float x;
		float y;
		for (int i = 0; i < rois.length; i++) {
			roi = rois[i];

			if (roi.getTypeAsString().equals("Polyline")) {

				polygonRoi = (PolygonRoi) roi;

				if (polygonRoi.getXCoordinates().length < 4) {
					log.warn("Polyline needs at least 4 points.");
					return;
				}

				points = new ArrayList<>();

				for (int j = 0; j < polygonRoi.getFloatPolygon().xpoints.length; j++) {
					x = polygonRoi.getFloatPolygon().xpoints[j];
					y = polygonRoi.getFloatPolygon().ypoints[j];
					points.add(new Point2D.Double(x, y));
				}

				// Enters a new Bezier Curve or B-Spline when the user presses ENTER
				if (frame.getInputType() == frame.B_SPLINE) {
					frame.getCurves().addCurve(points, frame.getControlPanel().getCurrentLayerSlider().getValue(),
							points.size(), frame.B_SPLINE, (frame.getBsplineType() == BSpline.OPEN),
							(Integer) (frame.getInfoPanel().getThresholdRadiusSpinner().getValue()));
				} else {
					frame.getCurves().addCurve(points, frame.getControlPanel().getCurrentLayerSlider().getValue(),
							points.size(), frame.BEZIER_CURVE, true,
							(Integer) (frame.getInfoPanel().getThresholdRadiusSpinner().getValue()));
				}

				// Updates our list after adding the curve
				frame.getInfoPanel().getListData().addElement("  CURVE " + frame.getCurves().getCount());
				frame.getInfoPanel().getList().setListData(frame.getInfoPanel().getListData());
				//

			}
		}
		frame.getInfoPanel().getList().setSelectedIndex(frame.getCurves().size() - 1);
		frame.getInfoPanel().getCurvesList().revalidate();
		frame.getInfoPanel().getPointSlider().setEnabled(true);
		frame.getInfoPanel().getPointSlider().setValue(0);
		frame.setCurrCtrlPt(0);
		frame.getKappaMenubar().getEnter().setEnabled(false);
		frame.drawImageOverlay();
	}

	public void openImageFile(String file) {
		openImageFile(new File(file));
	}

	public void openImageFile(File file) {
		ImagePlus imp = new ImagePlus(file.getPath());
		openImage(imp);
	}

	public void openActiveImage(Context context) {
		ImageDisplayService imds = context.getService(ImageDisplayService.class);
		ConvertService convert = context.getService(ConvertService.class);
		ImageDisplay imd = imds.getActiveImageDisplay();
		ImagePlus imp = convert.convert(imd, ImagePlus.class);
		openImage(imp);
	}

	public void openImage(ImagePlus imp) {
		frame.setImageStack(imp);

		// Splits the image into the R, G, and B channels, but only if the image is in
		// RGB color
		if (frame.getImageStack().getType() == ImagePlus.COLOR_RGB) {
			frame.setImageStackLayers(ChannelSplitter.splitRGB(frame.getImageStack().getImageStack(), true));
		}

		// Sets the displayed Image Stack to all the channels to begin with.
		frame.setDisplayedImageStack(frame.getImageStack());

		frame.getImageStack().setDisplayMode(IJ.COMPOSITE);

		frame.setMaxLayer(frame.getNFrames());
		frame.setMaxLayerDigits(Integer.toString(frame.getMaxLayer()).length());
		frame.getControlPanel().getCurrentLayerSlider().setValue(frame.getINIT_LAYER());
		frame.getControlPanel().getCurrentLayerSlider().setMaximum(frame.getMaxLayer());
		frame.getControlPanel().getCurrentLayerSlider().setMajorTickSpacing(frame.getNFrames() / 10);
		frame.getControlPanel().getCurrentLayerSlider().setPaintTicks(true);
		frame.getControlPanel().getCurrentLayerSlider().setEnabled(true);
		frame.getControlPanel().getScaleSlider().setEnabled(true);

		// Sets the maximum intensity depending on the bit depth of the image.
		if (frame.isImageRGBColor()) {
			frame.getInfoPanel().getDataThresholdSlider().setMaximum(256);
		} else {
			frame.getInfoPanel().getDataThresholdSlider()
					.setMaximum((int) (Math.pow(2, frame.getDisplayedImageStack().getBitDepth())));
		}

		// Reset channel buttons
		for (int i = 0; i < 3; i++) {
			frame.getControlPanel().getChannelButtons()[i].setEnabled(false);
			frame.getControlPanel().getChannelButtons()[i].setSelected(false);
		}

		// Sets the buttons to active and selected if the image type is a Color one.
		// Otherwise sets them to inactive
		if (frame.getImageStack().getNChannels() > 1) {
			for (int i = 0; i < frame.getImageStack().getNChannels(); i++) {
				frame.getControlPanel().getChannelButtons()[i].setEnabled(true);
				frame.getControlPanel().getChannelButtons()[i].setSelected(true);
			}
		}

		// Sets the scroll pane in the drawing panel to display the first layer of the
		// image now
		frame.setFrame(1);
		frame.setCurrImage(frame.getDisplayedImageStack().getBufferedImage());
		frame.getCurrImageLabel().setIcon(new ImageIcon(frame.getCurrImage()));
		frame.setThresholded(new boolean[frame.getCurrImage().getWidth()][frame.getCurrImage().getHeight()]);

		// Sets the maximum scale to a value that prevents a heap space error from
		// occuring.
		// We set the maximum image size to about 2000 x 2000 pixels = 4,000,000 pixels.
		int avgPixelDim = (frame.getCurrImage().getWidth() + frame.getCurrImage().getHeight()) / 2;
		frame.getControlPanel().getScaleSlider().setValue(ControlPanel.DEFAULT_SCALE);
		frame.getControlPanel().getScaleSlider()
				.setMaximum(Math.min(ControlPanel.MAX_SCALE, ControlPanel.MAX_AVG_PIXEL_DIM / avgPixelDim * 100));

		this.frame.updateThresholded();
		frame.getInfoPanel().getThresholdChannelsComboBox().setEnabled(true);
		frame.getInfoPanel().getThresholdSlider().setEnabled(true);
		frame.getInfoPanel().getRangeAveragingSpinner().setEnabled(true);
		frame.getInfoPanel().getBgCheckBox().setEnabled(true);
		frame.getInfoPanel().getApply().setEnabled(true);
		frame.getInfoPanel().getRevert().setEnabled(true);
		fit.setEnabled(true);

		// Enables view checkboxes
		getBoundingBoxMenu().setEnabled(true);
		getScaleCurvesMenu().setEnabled(true);
		getAntialiasingMenu().setEnabled(true);
		getTangentMenu().setEnabled(true);

		// Enables toolbar buttons and selects the direct selection tool
		frame.getToolPanel().enableAllButtons();

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
		this.frame.setTitle(frame.APPLICATION_NAME + "- " + imp.getTitle());

		// Load Kappa file if available
		if (imp.getOriginalFileInfo() != null) {
			String dirPath = imp.getOriginalFileInfo().directory;
			if (dirPath != null) {
				String kappaPath = FilenameUtils.removeExtension(imp.getOriginalFileInfo().fileName);
				kappaPath += ".kapp";
				File fullPath = new File(dirPath, kappaPath);
				if (fullPath.exists()) {
					loadCurveFile(fullPath);
				}
			}
		}
	}

	public void loadCurveFile(String file) {
		loadCurveFile(new File(file));
	}

	public void loadCurveFile(File file) {
		// Tries opening the file
		try {
			this.frame.resetCurves();
			BufferedReader in = new BufferedReader(new FileReader(file));
			int noCurves = Integer.parseInt(in.readLine());

			for (int n = 0; n < noCurves; n++) {
				int curveType = Integer.parseInt(in.readLine());
				int noKeyframes = Integer.parseInt(in.readLine());
				int noCtrlPts = Integer.parseInt(in.readLine());
				int bsplineType = 0;
				frame.setPoints(new ArrayList<Point2D>(noCtrlPts));

				// If the curve is a B-Spline, there is an extra parameter determining whether
				// it's open or closed
				if (curveType == frame.B_SPLINE) {
					bsplineType = Integer.parseInt(in.readLine());
				}

				// Initialize the curve
				int currentKeyframe = Integer.parseInt(in.readLine());
				for (int i = 0; i < noCtrlPts; i++) {
					frame.getPoints().add(
							new Point2D.Double(Double.parseDouble(in.readLine()), Double.parseDouble(in.readLine())));
				}

				if (curveType == frame.B_SPLINE) {
					frame.getCurves().addCurve(frame.getPoints(), currentKeyframe, noCtrlPts, frame.B_SPLINE,
							bsplineType == BSpline.OPEN,
							(Integer) (frame.getInfoPanel().getThresholdRadiusSpinner().getValue()));
				} else {
					frame.getCurves().addCurve(frame.getPoints(), currentKeyframe, noCtrlPts, frame.BEZIER_CURVE, true,
							(Integer) (frame.getInfoPanel().getThresholdRadiusSpinner().getValue()));
				}
				frame.getInfoPanel().getListData().addElement("  CURVE " + frame.getCurves().getCount());
				frame.getInfoPanel().getList().setListData(frame.getInfoPanel().getListData());

				// Load all the other keyframes for the curve
				for (int i = 1; i < noKeyframes; i++) {
					currentKeyframe = Integer.parseInt(in.readLine());
					frame.setPoints(new ArrayList<Point2D>(noCtrlPts));

					// Adds the control points for each keyframe. We add the redundant control
					// points for closed B-Spline curves.
					if (bsplineType == BSpline.OPEN) {
						for (int j = 0; j < noCtrlPts; j++) {
							frame.getPoints().add(new Point2D.Double(Double.parseDouble(in.readLine()),
									Double.parseDouble(in.readLine())));
						}
					} else {
						for (int j = 0; j < noCtrlPts - BSpline.B_SPLINE_DEGREE; j++) {
							frame.getPoints().add(new Point2D.Double(Double.parseDouble(in.readLine()),
									Double.parseDouble(in.readLine())));
						}
						for (int j = 0; j < BSpline.B_SPLINE_DEGREE; j++) {
							frame.getPoints().add(new Point2D.Double(frame.getPoints().get(i).getX(),
									frame.getPoints().get(i).getY()));
						}
					}
					frame.getCurves().get(frame.getCurves().size() - 1).addKeyframe(frame.getPoints(), currentKeyframe);
				}
			}

			// Translates all the curves to their position at the current frame.
			frame.getCurves().changeFrame(frame.getControlPanel().getCurrentLayerSlider().getValue());

			frame.drawImageOverlay();
			in.close();
		} catch (Exception err) {
			// frame.overlay.setVisible(true);
			// frame.overlay.drawNotification("There was an error loading the curve
			// data",
			// frame.scrollPane.getVisibleRect());
			err.printStackTrace();
		}
	}

	public void saveCurveFile(String file) {
		saveCurveFile(new File(file));
	}

	public void saveCurveFile(File file) {

		try {
			PrintWriter out = new PrintWriter(new FileWriter(file));
			out.println(frame.getCurves().size());

			for (Curve c : frame.getCurves()) {
				// Outputs the curve properties: it's type, the number of keyframes, the number
				// of control points, etc.
				if (c instanceof BSpline) {
					out.println(frame.B_SPLINE);
				} else {
					out.println(frame.BEZIER_CURVE);
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
				for (Curve.BControlPoints b : c.getKeyframes()) {
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
			frame.getOverlay().setVisible(true);
			frame.getOverlay().drawNotification("There was an error saving the curve data",
					frame.getScrollPane().getVisibleRect());
		}
	}

	public JMenuItem getDelete() {
		return delete;
	}

	public JMenuItem getEnter() {
		return enter;
	}

	public void setDelete(JMenuItem delete) {
		this.delete = delete;
	}

	public void setEnter(JMenuItem enter) {
		this.enter = enter;
	}

	public JCheckBoxMenuItem getTangentMenu() {
		return tangentMenu;
	}

	public void setTangentMenu(JCheckBoxMenuItem tangentMenu) {
		this.tangentMenu = tangentMenu;
	}

	public JCheckBoxMenuItem getScaleCurvesMenu() {
		return scaleCurvesMenu;
	}

	public void setScaleCurvesMenu(JCheckBoxMenuItem scaleCurvesMenu) {
		this.scaleCurvesMenu = scaleCurvesMenu;
	}

	public JCheckBoxMenuItem getBoundingBoxMenu() {
		return boundingBoxMenu;
	}

	public void setBoundingBoxMenu(JCheckBoxMenuItem boundingBoxMenu) {
		this.boundingBoxMenu = boundingBoxMenu;
	}

	public JCheckBoxMenuItem getAntialiasingMenu() {
		return antialiasingMenu;
	}

	public void setAntialiasingMenu(JCheckBoxMenuItem antialiasingMenu) {
		this.antialiasingMenu = antialiasingMenu;
	}

}
