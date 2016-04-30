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
