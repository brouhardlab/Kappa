/*
 * #%L
 * A Fiji plugin for Curvature Analysis.
 * %%
 * Copyright (C) 2016 - 2017 Fiji developers.
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

import static sc.fiji.kappa.gui.KappaFrame.APP_MIN_HEIGHT;
import static sc.fiji.kappa.gui.KappaFrame.APP_MIN_WIDTH;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import sc.fiji.kappa.gui.KappaFrame;

@Plugin(type = Command.class, menuPath = "Plugins>Analyze>Kappa - Curvature Analysis")
public class KappaPlugin implements Command {

	static {
		// NB: Needed if you mix-and-match IJ1 and IJ2 in this class.
		// And even then: do not use IJ1 classes in the API!
		LegacyInjector.preinit();
	}

	@Parameter
	private ImageJ ij;

	@Parameter
	private LogService log;

	// @Parameter(type = ItemIO.INPUT)
	// private ImageDisplay imageDisplay;
	public static final String PLUGIN_NAME = "Kappa";
	public static final String VERSION = version();

	@Override
	public void run() {

		log.info("Running " + PLUGIN_NAME + " version " + VERSION);

		// Kappa kappa = new Kappa(ij, imageDisplay);
		// kappa.init();

		// Launch Old IJ1 and not integrated Kappa GUI
		KappaFrame.frame = new KappaFrame();
		KappaFrame.frame.setMinimumSize(new Dimension(APP_MIN_WIDTH, APP_MIN_HEIGHT));
		try {
			Image im = ImageIO.read(KappaFrame.class.getResource("/logo.png"));
			KappaFrame.frame.setIconImage(im);
			KappaFrame.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			KappaFrame.frame.setLocationRelativeTo(null);
			KappaFrame.frame.setVisible(true);
		} catch (IOException ex) {
			Logger.getLogger(KappaFrame.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void main(final String... args) throws Exception {
		// Launch ImageJ as usual.
		final ImageJ ij = net.imagej.Main.launch(args);

		// Launch the command.
		ij.command().run(KappaPlugin.class, true);
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
