package fiji.plugin.kappa;

import net.imagej.ImageJ;
import net.imagej.display.ImageDisplay;
import net.imagej.patcher.LegacyInjector;

import org.scijava.ItemIO;
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

    @Parameter(type = ItemIO.INPUT)
    private ImageDisplay imageDisplay;

    public static final String PLUGIN_NAME = "Kappa";
    public static final String VERSION = version();

    @Override
    public void run() {

        log.info("Running " + PLUGIN_NAME + " version " + VERSION);

        Kappa kappa = new Kappa(ij, imageDisplay);
        kappa.init();
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
