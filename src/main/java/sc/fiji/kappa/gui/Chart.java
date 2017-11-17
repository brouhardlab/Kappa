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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Chart extends Component {

	private static final long serialVersionUID = 1L;
	private ArrayList<Point2D> data;

	private double minX, minY, maxX, maxY;
	private boolean minXFixed, minYFixed, maxXFixed, maxYFixed;
	private double[] tickX;
	private double[] tickY;

	public static final int LEFT_OFFSET = 0;
	public static final int RIGHT_OFFSET = 0;
	public static final int TOP_OFFSET = 15;
	public static final int BOTTOM_OFFSET = Panel.TITLEBAR_DEFAULT_HEIGHT;
	public static final int NO_TICKS_Y = 10;
	public static final int NO_TICKS_X = 20;
	public static final int TICK_SIZE = 2;
	public static final int DATA_PT_SIZE = 3;

	Point2D[] getMappedPoints(ArrayList<Point2D> data, int x, int y, int width, int height) {
		Point2D[] mappedPoints = new Point2D.Double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			if (maxX - minX != 0) {
				mappedPoints[i] = new Point2D.Double(x + ((data.get(i).getX() - minX) / ((maxX - minX) * 1.0)) * width,
						y + (1 - (data.get(i).getY() - minY) / ((maxY - minY) * 1.0)) * height);
			} else {
				mappedPoints[i] = new Point2D.Double(x,
						y + (1 - (data.get(i).getY() - minY) / ((maxY - minY) * 1.0)) * height);
			}
		}
		return mappedPoints;
	}

	public void setData(ArrayList<Point2D> data) {
		// Finds maximal and minimal x and y values
		// To make the graph always start at y = 0, we make minY 0
		this.data = data;
		if (data.size() != 0) {
			// Only computes maximal and minimal values if one has not been set yet.
			if (!minXFixed) {
				minX = this.data.get(0).getX();
			}
			if (!maxXFixed) {
				maxX = this.data.get(0).getX();
			}
			if (!minYFixed) {
				minY = 0;
			}
			if (!maxYFixed) {
				maxY = this.data.get(0).getY();
			}

			// Only iterates through the points if there is a value we need to obtain
			if (!minXFixed || !maxXFixed || !maxYFixed) {
				for (Point2D p : this.data) {
					if (!minXFixed && p.getX() < minX) {
						minX = p.getX();
					} else if (!maxXFixed && p.getX() > maxX) {
						maxX = p.getX();
					}
					if (!maxYFixed && p.getY() > maxY) {
						maxY = p.getY();
					}
				}
			}
		}

		// Fills in tick bounds
		double incY = (maxY - minY) / (NO_TICKS_Y - 1);
		double incX = (maxX - minX) / (NO_TICKS_X - 1);
		double y = minY;
		double x = minX;
		for (int i = 0; i < NO_TICKS_Y; i++) {
			tickY[i] = (int) (y);
			y += incY;
		}
		for (int i = 0; i < NO_TICKS_X; i++) {
			tickX[i] = (int) (x);
			x += incX;
		}
		tickX[NO_TICKS_X - 1] = maxX;
		tickY[NO_TICKS_Y - 1] = maxY;
	}

	public Chart(ArrayList<Point2D> data) {
		tickX = new double[NO_TICKS_X];
		tickY = new double[NO_TICKS_Y];
		setData(data);
	}

	public void draw(int x, int y, int width, int height, Graphics g, Color color) {
		if (this.isVisible()) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Rectangle c = new Rectangle(x + LEFT_OFFSET, y + TOP_OFFSET, width - LEFT_OFFSET - RIGHT_OFFSET,
					height - TOP_OFFSET - BOTTOM_OFFSET);

			g.drawLine(c.x, c.y - 20, c.x, c.y + c.height);
			g.drawLine(c.x, c.y + c.height, c.x + c.width, c.y + c.height);

			// Draws ticks along both axes.
			for (double dy = c.y + c.height; dy >= c.y - 1; dy -= c.height / (NO_TICKS_Y * 1.0)) {
				g.drawLine(c.x, (int) dy, c.x + TICK_SIZE, (int) dy);
			}
			for (double dx = c.x; dx <= c.x + c.width + 1; dx += c.width / (NO_TICKS_X * 1.0)) {
				g.drawLine((int) dx, c.y + c.height - TICK_SIZE, (int) dx, c.y + c.height);
			}

			// Draws all the points.
			if (data.size() != 0) {
				// Gets the coordinate values for each data point to draw on the graph, and
				// draws the line segments too.
				Point2D[] mappedPoints = getMappedPoints(data, c.x, c.y, c.width, c.height);
				g.setColor(color);
				for (int i = 0; i < mappedPoints.length - 1; i++) {
					g.drawLine((int) mappedPoints[i].getX(), (int) mappedPoints[i].getY(),
							(int) mappedPoints[i + 1].getX(), (int) mappedPoints[i + 1].getY());
				}
				g.setColor(Color.BLACK);
			}
		}
	}

	public void setMinX(double minX) {
		this.minX = minX;
		minXFixed = true;
	}

	public void setMaxX(double maxX) {
		this.maxX = maxX;
		maxXFixed = true;
	}

	public void setMinY(double minY) {
		this.minY = minY;
		minYFixed = true;
	}

	public void setMaxY(double maxY) {
		this.maxY = maxY;
		maxYFixed = true;
	}
}
