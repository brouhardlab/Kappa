package fiji.plugin.kappa.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ZoomInListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        double scale = ControlPanel.scaleSlider.getValue() / 100.0;

        //If we are at the max scaling increment or higher, we can't zoom in
        if (scale >= ControlPanel.SCALE_INCREMENTS[ControlPanel.SCALE_INCREMENTS.length - 1]) {
            return;
        }

        //Finds the next largest scaling increment and sets the scale to that.
        int i = ControlPanel.SCALE_INCREMENTS.length - 2;
        while (i > 0 && ControlPanel.SCALE_INCREMENTS[i] > scale) {
            i--;
        }
        ControlPanel.scaleSlider.setValue((int) Math.ceil(100.0 * ControlPanel.SCALE_INCREMENTS[++i]));
    }
}
