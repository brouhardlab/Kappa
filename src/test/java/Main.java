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
