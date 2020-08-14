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
package sc.fiji.kappa;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.WindowConstants;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.fiji.kappa.gui.KappaFrame;

@Plugin(type = Command.class, menuPath = "Plugins>Analyze>Kappa - Curvature Analysis")
public class KappaPlugin implements Command {

	@Parameter
	private ImageJ ij;

	@Parameter
	private LogService log;

	public static final String PLUGIN_NAME = "Kappa";
	public static final String VERSION = version();

	@Override
	public void run() {

		log.info("Running " + PLUGIN_NAME + " version " + VERSION);

		// Launch old IJ1 and not integrated Kappa GUI
		KappaFrame frame = new KappaFrame(ij.context());
		frame.setMinimumSize(new Dimension(KappaFrame.APP_MIN_WIDTH, KappaFrame.APP_MIN_HEIGHT));
		frame.setTitle(PLUGIN_NAME + " version " + VERSION);

		try {
			Image im = ImageIO.read(KappaFrame.class.getResource("/logo.png"));
			frame.setIconImage(im);
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

		} catch (IOException ex) {
			Logger.getLogger(KappaFrame.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static String version() {
		String version = null;
		final Package pack = KappaPlugin.class.getPackage();
		if (pack != null) {
			version = pack.getImplementationVersion();
		}
		return version == null ? "DEVELOPMENT" : version;
	}
}
