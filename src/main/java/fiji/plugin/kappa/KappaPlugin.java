/*
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2016 - 2017 Fiji
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.kappa;

import fiji.plugin.kappa.gui.KappaFrame;
import static fiji.plugin.kappa.gui.KappaFrame.APP_MIN_HEIGHT;
import static fiji.plugin.kappa.gui.KappaFrame.APP_MIN_WIDTH;
import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

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

//    @Parameter(type = ItemIO.INPUT)
//    private ImageDisplay imageDisplay;
    public static final String PLUGIN_NAME = "Kappa";
    public static final String VERSION = version();

    @Override
    public void run() {

        log.info("Running " + PLUGIN_NAME + " version " + VERSION);

//        Kappa kappa = new Kappa(ij, imageDisplay);
//        kappa.init();

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
