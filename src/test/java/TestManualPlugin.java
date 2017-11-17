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

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import sc.fiji.kappa.KappaPlugin;

public class TestManualPlugin {

	public static void main(final String... args) throws Exception {
		// Launch ImageJ as usual.
		final ImageJ ij = new ImageJ();
		ij.launch(args);
				
		// Load image and rois test data
		// Main.loadTestData(ij);

		// Launch the command.
		ij.command().run(KappaPlugin.class, true);
	}

	public static void loadTestData(ImageJ ij) throws IOException {

		// Open image
		String fpath;
		fpath = TestManualPlugin.class.getResource("/test-data/curve.tif").getPath();
		Dataset ds = ij.dataset().open(fpath);
		ij.display().createDisplay(ds);

		ij.log().info("Load test data.");
	}
}
