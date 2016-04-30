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
