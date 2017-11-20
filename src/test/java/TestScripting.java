
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

import java.io.File;

import net.imagej.ImageJ;
import sc.fiji.kappa.gui.KappaFrame;
import sc.fiji.kappa.gui.MenuBar;

public class TestScripting {

	public static void main(final String... args) throws Exception {

		final ImageJ ij = new ImageJ();
		ij.launch(args);

		File imageFile = new File(
				"/home/hadim/Documents/Postdoc/Papers/Kappa Paper/Figure4/Data/dataset/pixel_size_0.38_um.tif");
		File kappaFile = new File("/home/hadim/test.kapp");

		KappaFrame.frame = new KappaFrame(ij.context());
		MenuBar.openFile(imageFile);
		MenuBar.loadCurveFile(kappaFile);

		System.out.println(KappaFrame.curves);

		ij.log().info(KappaFrame.curves.get(0).getPoints());

		KappaFrame.curves.rescaleCurves(0.5);

		ij.log().info(KappaFrame.curves.get(0).getPoints());
	}

}
