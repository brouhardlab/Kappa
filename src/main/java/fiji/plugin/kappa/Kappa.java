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

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.display.ImageDisplay;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

/**
 *
 * @author Hadrien Mary <hadrien.mary@gmail.com>
 */
class Kappa {
    
    @Parameter
    private ImageJ ij;
    
    @Parameter
    private LogService log;
    
    @Parameter
    private Context context;
    
    private final ImageDisplay image;
    private final Dataset dataset;
    
    private Settings settings;
    
    //Curve Variables and Constants
    public static final String[] FITTING_ALGORITHMS = {"Point Distance Minimization", "Squared Distance Minimization"};
    public static final String[] curveTypes = {"Bézier Curve", "B-Spline"};
    public static final int BEZIER_CURVE = 0;
    public static final int B_SPLINE = 1;
    public static final int DEFAULT_INPUT_CURVE = B_SPLINE;
    public static final String[] bsplineTypes = {"Open", "Closed"};
    public static int bsplineType;
    public static int inputType;

    //0 = Point Distance Minimization
    //1 = Squared Distance Minimization
    public static final int DEFAULT_FITTING_ALGORITHM = 0;
    public static String fittingAlgorithm;
    
    public Kappa(ImageJ ij, ImageDisplay image){
        this.ij = ij;
        this.ij.context().inject(this);
        this.image = image;
        this.dataset = (Dataset) this.image.getActiveView().getData();
    }

    void init() {
        
        // Check if T and Z need to be swapped.
        Utils.swapTimeAndZDimensions(this.ij, dataset);
        
        // Create a settings object and fill it with dataset
        this.settings = new Settings(context);
        settings.fromDataset(this.dataset);

        log.info("Current settings are :");
        log.info(settings.toString("\t"));
    }
    
}
