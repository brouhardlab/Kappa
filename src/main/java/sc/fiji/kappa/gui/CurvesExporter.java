/*-
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import com.opencsv.CSVWriter;

import sc.fiji.kappa.curve.BezierGroup;
import sc.fiji.kappa.curve.Curve;

public class CurvesExporter {

	public void export() throws IOException {
		export(true);
	}

	public void export(boolean exportAveragePerCurve) throws IOException {

		JFileChooser kappaExport = new JFileChooser();

		String dirPath = KappaFrame.imageStack.getOriginalFileInfo().directory;
		if (dirPath != null) {
			String kappaPath = FilenameUtils.removeExtension(KappaFrame.imageStack.getOriginalFileInfo().fileName);
			kappaPath += ".csv";
			File fullPath = new File(dirPath, kappaPath);
			kappaExport.setSelectedFile(fullPath);
		}

		kappaExport.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
		kappaExport.setDialogTitle("Export Curve Data");

		// Handles export button action.
		int returnVal = kappaExport.showSaveDialog(KappaFrame.frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = kappaExport.getSelectedFile();
			exportToFile(file, exportAveragePerCurve);
		}
	}

	public void exportToFile(String file, boolean exportAveragePerCurve) throws IOException {
		exportToFile(new File(file), exportAveragePerCurve);
	}

	public void exportToFile(File file, boolean exportAveragePerCurve) throws IOException {

		BezierGroup curves = KappaFrame.curves;

		// Appends a .csv
		if (!file.getPath().toLowerCase().endsWith(".csv")) {
			file = new File(file.getPath() + ".csv");
		}

		CSVWriter writer = new CSVWriter(new FileWriter(file.getAbsoluteFile()));

		// Write headers
		List<String> headers = new ArrayList<String>();
		headers.add("Curve Name");

		headers.add("Curve Length (um)");
		headers.add("Average Curvature (um-1)");
		headers.add("Curvature Std (um-1)");

		headers.add("X-Coordinate (um)");
		headers.add("Y-Coordinate (um)");
		headers.add("Point Curvature (um-1)");

		headers.add("Red Intensity");
		headers.add("Green Intensity");
		headers.add("Blue Intensity");
		writer.writeNext(headers.stream().toArray(String[]::new));

		// Not used anymore
		int w = KappaFrame.currImage.getWidth();
		int h = KappaFrame.currImage.getHeight();
		double[][] averaged = new double[w][h];

		StringWriter out = new StringWriter();
		PrintWriter printWriter = new PrintWriter(out);

		for (Curve c : curves) {
			c.printValues(printWriter, averaged, !exportAveragePerCurve);
			writer.flush();
			String[] lines = out.toString().split("\n");

			for (String line : lines) {
				writer.writeNext(line.split(","));
			}
		}

		// close the writer
		writer.close();
	}

}
