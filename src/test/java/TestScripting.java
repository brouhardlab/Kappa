
/*
 * #%L
 * A Fiji plugin for Curvature Analysis.
 * %%
 * Copyright (C) 2016 - 2020 Gary Brouhard
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

import java.io.File;

import net.imagej.ImageJ;
import sc.fiji.kappa.gui.CurvesExporter;
import sc.fiji.kappa.gui.KappaFrame;

public class TestScripting {

	public static void main(final String... args) throws Exception {

		final ImageJ ij = new ImageJ();
		ij.launch(args);

		File imageFile = new File(
				"/home/hadim/Documents/Postdoc/Papers/Kappa Paper/Figure4/Data/dataset/variable_pixel_size/pixel_size_0.04_um.tif");
		File kappaFile = new File(
				"/home/hadim/Documents/Postdoc/Papers/Kappa Paper/Figure4/Data/dataset/variable_pixel_size/pixel_size_0.04_um.kapp");
		File curvaturesFile = new File("/home/hadim/curvatures.csv");

		KappaFrame frame = new KappaFrame(ij.context());
		frame.getKappaMenubar().openImageFile(imageFile);
		frame.getKappaMenubar().loadCurveFile(kappaFile);

		frame.setEnableCtrlPtAdjustment(true);
		frame.getInfoPanel().getThresholdRadiusSpinner().setValue(60);
		frame.getInfoPanel().getThresholdSlider().setValue(160);
		frame.getInfoPanel().getConversionField().setText(Double.toString(0.04));

		frame.fitCurves();

		CurvesExporter exporter = new CurvesExporter(frame);
		exporter.exportToFile(curvaturesFile, false);
	}

}
