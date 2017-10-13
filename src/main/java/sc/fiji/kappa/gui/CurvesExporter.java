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

import com.opencsv.CSVWriter;

import sc.fiji.kappa.curve.BezierGroup;
import sc.fiji.kappa.curve.Curve;

public class CurvesExporter {

	JFileChooser kappaExport;

	public CurvesExporter() {
		kappaExport = new JFileChooser();
		kappaExport.setFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
		kappaExport.setDialogTitle("Export Curve Data");
	}

	public void export() throws IOException {
		export(true);
	}

	public void export(boolean exportAveragePerCurve) throws IOException {

		BezierGroup curves = KappaFrame.curves;

		// Handles export button action.
		int returnVal = kappaExport.showSaveDialog(KappaFrame.frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = kappaExport.getSelectedFile();

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

}
