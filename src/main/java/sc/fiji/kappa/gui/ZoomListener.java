package sc.fiji.kappa.gui;

import java.awt.event.ActionListener;

public abstract class ZoomListener implements ActionListener {

	protected ControlPanel controlPanel;

	public ZoomListener(ControlPanel controlPanel) {
		this.controlPanel = controlPanel;
	}

}
