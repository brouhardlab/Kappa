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
import net.imagej.axis.Axes;
import org.scijava.ui.DialogPrompt;
import static org.scijava.ui.DialogPrompt.Result.YES_OPTION;

/**
 *
 * @author Hadrien Mary <hadrien.mary@gmail.com>
 */
class Utils {

    /**
     * Check if Z and Time dimensions should be swapped in a given dataset. If it does then ask user
     * if he wants to swap them.
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
            DialogPrompt.Result result = ij.ui().showDialog(mess,
                    DialogPrompt.MessageType.QUESTION_MESSAGE,
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
     * Check if Z and Time dimensions should be swapped in a given dataset. If it does then swap
     * them without asking.
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
