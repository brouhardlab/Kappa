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

import java.awt.*;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

public class Overlay extends JComponent {

    public static final double NOTIFICATION_DRAW_LOCATION = 0.9;
    public static final int DEFAULT_OVERLAY_DURATION = 2000;

    private static final long serialVersionUID = 1L;

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public Overlay() {
    }

    public void drawNotification(String text, Rectangle viewrect) {
        drawNotification(text, viewrect, DEFAULT_OVERLAY_DURATION);
    }

    public void drawNotification(String text, Rectangle viewrect, int overlayDurationMillis) {
        Graphics g = this.getGraphics();
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(new Font("Sans Serif", Font.PLAIN, 18));

        //Finds the drawing coordinates on the viewing port and the text length.
        //Uses NOTIFICATION_DRAW_LOCATION to determine at what height the notification will be drawn, percentage wise.
        Rectangle2D textBounds = g.getFontMetrics(new Font("Sans Serif", Font.PLAIN, 18)).getStringBounds(text, g);
        int textLength = (int) (textBounds.getWidth());
        int textHeight = (int) (textBounds.getHeight());
        int drawX = (int) (viewrect.getWidth() / 2);
        int drawY = (int) (50 + viewrect.getHeight() * NOTIFICATION_DRAW_LOCATION);

        g.setColor(new Color(0, 0, 0, (float) 0.5));
        g.fillRoundRect(drawX - textLength / 2 - 20, drawY - textHeight / 2 - 12, textLength + 40, textHeight + 24, 20, 20);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRoundRect(drawX - textLength / 2 - 20, drawY - textHeight / 2 - 12, textLength + 40, textHeight + 24, 20, 20);

        g.setColor(Color.WHITE);
        g.drawString(text, drawX - textLength / 2, drawY - textHeight / 2 + 16);

        repaint();

        //If the overlay duration is negative, we don't add a delay before hiding.
        if (overlayDurationMillis >= 0) {
            Thread delayHide = new Thread(new DelayThread(overlayDurationMillis));
            delayHide.run();
        }
    }

    //A thread to delay the hiding of a notification for a certain amount of time.
    private class DelayThread implements Runnable {

        private int timeMillis;

        public DelayThread(int timeMillis) {
            this.timeMillis = timeMillis;
        }

        public void run() {
            try {
                Thread.sleep(timeMillis);
            } catch (Exception e) {
            }
            KappaFrame.overlay.setVisible(false);
        }
    }
}
