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
package sc.fiji.kappa.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;

public class PanelGroup extends Component {

	private static final long serialVersionUID = 1L;
	private ArrayList<Panel> panels;

	public PanelGroup() {
		panels = new ArrayList<Panel>();
	}

	public void addPanel(Panel panel) {
		if (panels.size() >= 1) {
			Panel prevPanel = panels.get(panels.size() - 1);
			panel.setY(prevPanel.getY() + prevPanel.getH() + 2);
		}
		panels.add(panel);
	}

	public void toggleVisibility(int i) {
		panels.get(i).toggleExpanded();
		for (int j = 1 + i; j < panels.size(); j++) {
			panels.get(j).setY(panels.get(j - 1).getY() + panels.get(j - 1).getH() + 2);
		}
	}

	public void toggleVisibility(Point p) {
		// Adjusts placement of each of the panels
		for (int i = 0; i < panels.size(); i++) {
			if (panels.get(i).isVisible() && panels.get(i).isPointOnToggle(p)) {
				this.toggleVisibility(i);
			}
		}
	}

	public void toggleVisibility(String title) {
		// Hides a panel with string title.
		for (int i = 0; i < panels.size(); i++) {
			if (panels.get(i).getTitle().equals(title)) {
				this.toggleVisibility(i);
			}
		}
	}

	public void hide(String title) {
		for (int i = 0; i < panels.size(); i++) {
			if (panels.get(i).isExpanded() && panels.get(i).getTitle().equals(title)) {
				this.toggleVisibility(i);
			}
		}
	}

	public void show(String title) {
		for (int i = 0; i < panels.size(); i++) {
			if (!panels.get(i).isExpanded() && panels.get(i).getTitle().equals(title)) {
				this.toggleVisibility(i);
			}
		}
	}

	public void draw(Graphics g) {
		for (Panel p : panels) {
			p.draw(g);
		}
	}
}
