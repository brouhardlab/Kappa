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

import com.google.gson.Gson;

import java.io.File;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

/**
 * Used to store settings used in the whole application. Mainly a set of public fields.
 *
 * This class is largely inspired from the fiji.plugin.trackmate.Settings class.
 *
 * @author Hadrien Mary <hadrien.mary@gmail.com>
 */
public class Settings {

    @Parameter
    private transient ConvertService convert;

    @Parameter
    private transient LogService log;

    public Settings(final Context context) {
        context.inject(this);
    }

    // Image related settings
    @Parameter(label = "Dataset")
    public transient Dataset dataset;

    @Parameter(label = "Time Interval")
    public double dt = 1;
    @Parameter(label = "X spatial resolution")
    public double dx = 1;
    @Parameter(label = "Y spatial resolution")
    public double dy = 1;
    @Parameter(label = "Z spatial resolution")
    public double dz = 1;

    @Parameter(label = "Width")
    public int width;
    @Parameter(label = "Height")
    public int height;
    @Parameter(label = "Time axis size")
    public int zSize;
    @Parameter(label = "Z axis size")
    public int timeSize;
    @Parameter(label = "Channel axis size")
    public int channelSize;

    @Parameter(label = "Folder contaning dataset")
    public String imageFolder = "";
    @Parameter(label = "Filename of the dataset")
    public String imageFileName = "";
    
    public void fromDataset(Dataset dataset) {
        this.dataset = dataset;

        // This code will be nicer when the following PR will be merged
        // https://github.com/imagej/imagej-common/pull/59
        // Get index for each dimensions
        int xIndex = this.dataset.dimensionIndex(Axes.X);
        int yIndex = this.dataset.dimensionIndex(Axes.Y);
        int zIndex = this.dataset.dimensionIndex(Axes.Z);
        int timeIndex = this.dataset.dimensionIndex(Axes.TIME);
        int channelIndex = this.dataset.dimensionIndex(Axes.X);

        // Set image dimensions
        this.width = (int) this.dataset.dimension(xIndex);
        this.height = (int) this.dataset.dimension(yIndex);
        this.zSize = (int) this.dataset.dimension(zIndex);
        this.timeSize = (int) this.dataset.dimension(timeIndex);
        this.channelSize = (int) this.dataset.dimension(channelIndex);

        // We never know :-)
        this.zSize = this.zSize == 0 ? 1 : this.zSize;
        this.timeSize = this.timeSize == 0 ? 1 : this.timeSize;
        this.channelSize = this.channelSize == 0 ? 1 : this.channelSize;

        // Set image calibrations of dimensions
        this.dx = xIndex != -1 ? this.dataset.axis(xIndex).calibratedValue(1) : 1;
        this.dy = yIndex != -1 ? this.dataset.axis(yIndex).calibratedValue(1) : 1;
        this.dz = zIndex != -1 ? this.dataset.axis(zIndex).calibratedValue(1) : 1;
        this.dt = timeIndex != -1 ? this.dataset.axis(timeIndex).calibratedValue(1) : 1;

        File file = new File(this.dataset.getImgPlus().getSource());
        this.imageFileName = file.getName();
        this.imageFolder = file.getParent();
    }

    public String toString(String prefix) {
        String s = new String();
        s += prefix + "Dataset : " + this.dataset.toString() + "\n";
        s += prefix + "Width : " + this.width + "\n";
        s += prefix + "Height : " + this.height + "\n";
        s += prefix + "Depth : " + this.zSize + "\n";
        s += prefix + "Timepoints : " + this.timeSize + "\n";
        s += prefix + "Channel Numbers : " + this.channelSize + "\n";
        s += prefix + "X Spatial Resolution : " + this.dx + "\n";
        s += prefix + "Y Spatial Resolution : " + this.dy + "\n";
        s += prefix + "Z Spatial Resolution : " + this.dz + "\n";
        s += prefix + "Time Interval : " + this.dt + "\n";
        s += prefix + "Image Folder : " + this.imageFolder + "\n";
        s += prefix + "Image Filename : " + this.imageFileName + "\n";

        return s;
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Settings fromJSON(String json) {
        Gson gson = new Gson();
        Settings settings = gson.fromJson(json, Settings.class);

        return settings;
    }
}
