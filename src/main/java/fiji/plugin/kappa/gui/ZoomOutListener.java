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
package fiji.plugin.kappa.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ZoomOutListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        double scale = ControlPanel.scaleSlider.getValue() / 100.0;

        //If we are at the min scaling increment or lower, we can't zoom out
        if (scale <= ControlPanel.SCALE_INCREMENTS[0]) {
            return;
        }

        //Finds the next smallest scaling increment and sets the scale to that.
        int i = 1;
        while (i < ControlPanel.SCALE_INCREMENTS.length && ControlPanel.SCALE_INCREMENTS[i] < scale) {
            i++;
        }
        ControlPanel.scaleSlider.setValue((int) Math.floor(100.0 * ControlPanel.SCALE_INCREMENTS[--i]));
    }
}
