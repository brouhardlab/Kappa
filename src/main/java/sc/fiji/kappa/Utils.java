/*
 * #%L
 * A Fiji plugin for Curvature Analysis.
 * %%
 * Copyright (C) 2016 - 2017 Gary Brouhard
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

import static org.scijava.ui.DialogPrompt.Result.YES_OPTION;

import org.scijava.ui.DialogPrompt;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;

/**
 *
 * @author Hadrien Mary <hadrien.mary@gmail.com>
 */
class Utils {

	/**
	 * Check if Z and Time dimensions should be swapped in a given dataset. If it
	 * does then ask user if he wants to swap them.
	 *
	 * @param ij
	 * @param dataset
	 */
	public static void askToSwapTimeAndZDimensions(ImageJ ij, Dataset dataset) {

		int zIdx = dataset.dimensionIndex(Axes.Z);
		int timeIdx = dataset.dimensionIndex(Axes.TIME);

		long timeDim = dataset.dimension(timeIdx);
		long zDim = dataset.dimension(zIdx);

		if (timeDim < zDim) {
			String mess = new String();
			mess += "It appears this image has " + timeDim + " timepoints";
			mess += " and " + zDim + " Z slices.\n";
			mess += "Do you want to swap Z and T axes ?";
			DialogPrompt.Result result = ij.ui().showDialog(mess, DialogPrompt.MessageType.QUESTION_MESSAGE,
					DialogPrompt.OptionType.YES_NO_OPTION);
			result.equals(YES_OPTION);

			if (result.equals(YES_OPTION)) {
				if (zIdx != -1) {
					dataset.axis(zIdx).setType(Axes.TIME);
				}
				if (timeIdx != -1) {
					dataset.axis(timeIdx).setType(Axes.Z);
				}
			}

		}

	}

	/**
	 * Check if Z and Time dimensions should be swapped in a given dataset. If it
	 * does then swap them without asking.
	 *
	 * @param ij
	 * @param dataset
	 */
	public static void swapTimeAndZDimensions(ImageJ ij, Dataset dataset) {

		int zIdx = dataset.dimensionIndex(Axes.Z);
		int timeIdx = dataset.dimensionIndex(Axes.TIME);

		long timeDim = dataset.dimension(timeIdx);
		long zDim = dataset.dimension(zIdx);

		if (timeDim < zDim) {
			ij.log().info("Swapping TIME and Z axis.");
			if (zIdx != -1) {
				dataset.axis(zIdx).setType(Axes.TIME);
			}
			if (timeIdx != -1) {
				dataset.axis(timeIdx).setType(Axes.Z);
			}
		}

	}

}
