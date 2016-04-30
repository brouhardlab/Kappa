package fiji.plugin.kappa.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.awt.*;

import javax.swing.ImageIcon;

public class Panel {

    private int x;
    private int y;
    private int h;
    private int defaultHeight;
    public static final int TITLEBAR_DEFAULT_HEIGHT = 21;
    private ArrayList<Integer> separatorHeights;

    private String title;
    private boolean expanded;
    private boolean visible;
    private ArrayList<Component> components;
    public static final Image VISIBLE_BUTTON = new ImageIcon(Panel.class.getResource("/icons/opened_panel.png").getPath()).getImage();
    public static final Image HIDDEN_BUTTON = new ImageIcon(Panel.class.getResource("/icons/closed_panel.png").getPath()).getImage();

    /**
     * Draws a header for the info panel
     *
     * @param titleText	The text for the header
     * @param x	The x-coordinate for the text
     * @param y	The y-coordinate for the text
     * @param g	The graphics context to draw in
     */
    private void drawHeader(String titleText, int x, int y, Graphics g) {
        //Draws dividers
        g.setColor((new Color(225, 225, 225)));
        g.fillRect(0, y, KappaFrame.PANEL_WIDTH, 20);
        g.setColor((new Color(100, 100, 100)));
        g.drawRect(0, y, KappaFrame.PANEL_WIDTH, 20);
        g.setColor(new Color(180, 180, 180));
        g.drawLine(0, y + 21, KappaFrame.PANEL_WIDTH, y + 21);

        //Draws the text for the header
        g.setColor(Color.BLACK);
        g.drawString(titleText, x + 7, y + 16);

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (expanded) {
            g.drawImage(VISIBLE_BUTTON, x + KappaFrame.PANEL_WIDTH - 18, y + 3, 15, 15, null);
        } else {
            g.drawImage(HIDDEN_BUTTON, x + KappaFrame.PANEL_WIDTH - 18, y + 3, 15, 15, null);
        }
    }

    public Panel(int x, int y, int h, String title) {
        this.x = x;
        this.y = y;
        this.defaultHeight = h;
        this.h = defaultHeight;
        this.title = title;
        this.expanded = true;
        this.visible = true;
        components = new ArrayList<Component>();
        separatorHeights = new ArrayList<Integer>();
    }

    public Panel(int h, String title) {
        this(0, 0, h, title);
    }

    public void draw(Graphics g) {
        if (!this.visible) {
            return;
        }
        drawHeader(title, x, y, g);
        if (this.expanded) {
            for (int y : separatorHeights) {
                g.setColor(new Color(245, 245, 245));
                g.drawLine(0, y, KappaFrame.PANEL_WIDTH, y);
                g.setColor(Color.GRAY);
                g.drawLine(0, y + 1, KappaFrame.PANEL_WIDTH, y + 1);
            }
        }
        g.setColor(Color.BLACK);
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
        for (Component c : components) {
            c.setVisible(expanded);
        }
        if (expanded == false) {
            this.h = TITLEBAR_DEFAULT_HEIGHT;
        } else {
            this.h = defaultHeight;
        }
    }

    public void hide() {
        this.visible = false;
        for (Component c : components) {
            c.setVisible(visible);
        }
        this.h = 0;
    }

    public void show() {
        this.visible = true;
        for (Component c : components) {
            c.setVisible(visible);
        }
        this.h = defaultHeight;
    }

    public boolean isVisible() {
        return visible == true;
    }

    ;
	public void addSeparator(int y) {
        separatorHeights.add(y + this.y);
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        for (int i = 0; i < separatorHeights.size(); i++) {
            separatorHeights.set(i, y + separatorHeights.get(i) - this.y);
        }
        for (Component c : components) {
            c.setBounds(this.x + c.getX(), y + c.getY() - this.y, c.getWidth(), c.getHeight());
        }
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getH() {
        return this.h;
    }

    public String getTitle() {
        return this.title;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            toggleExpanded();
        }
    }

    public void addComponent(Component c) {
        c.setBounds(this.x + c.getX(), this.y + c.getY(), c.getWidth(), c.getHeight());
        c.setVisible(expanded);
        components.add(c);
    }

    public boolean isPointOnToggle(Point p) {
        if (p.getX() > this.x && p.getY() > this.y
                && p.getX() <= this.x + KappaFrame.PANEL_WIDTH && p.getY() < this.y + 20) {
            return true;
        }
        return false;
    }
}
