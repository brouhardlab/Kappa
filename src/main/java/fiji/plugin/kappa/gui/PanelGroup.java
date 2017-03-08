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

import java.awt.Graphics;
import java.util.ArrayList;
import java.awt.*;

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
        //Adjusts placement of each of the panels
        for (int i = 0; i < panels.size(); i++) {
            if (panels.get(i).isVisible() && panels.get(i).isPointOnToggle(p)) {
                this.toggleVisibility(i);
            }
        }
    }

    public void toggleVisibility(String title) {
        //Hides a panel with string title.
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
