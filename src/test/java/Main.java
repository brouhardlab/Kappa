
import fiji.plugin.kappa.KappaPlugin;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;

public class Main {

    public static void main(final String... args) throws Exception {
        // Launch ImageJ as usual.
        final ImageJ ij = net.imagej.Main.launch(args);

        // Load image and rois test data
        Main.loadTestData(ij);

        // Launch the command.
        ij.command().run(KappaPlugin.class, true);
    }

    public static void loadTestData(ImageJ ij) throws IOException {

        // Open image
        String fpath;
        fpath = Main.class.getResource("/test-data/curve.tif").getPath();
        Dataset ds = ij.dataset().open(fpath);
        ij.display().createDisplay(ds);

        ij.log().info("Load test data.");
    }
}
