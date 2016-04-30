/*
 * The MIT License
 *
 * Copyright 2016 Fiji.
 *
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
