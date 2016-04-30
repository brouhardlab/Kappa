package fiji.plugin.kappa.curve;

import fiji.plugin.kappa.Settings;
import fiji.plugin.kappa.gui.KappaFrame;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class BezierGroup extends ArrayList<Curve> {

    private static final long serialVersionUID = 1L;
    private int noSelected;
    private int count;

    public BezierGroup() {
        super();
        noSelected = 0;
        count = 0;
    }

    @Override
    public boolean add(Curve curve) {
        this.setAllUnselected();
        noSelected = 1;
        return super.add(curve);
    }

    public void addCurve(ArrayList<Point2D> defPoints, int t, int noCtrlPts, int curveType,
            boolean isOpen, int dataRadius) {
        
        if (curveType == KappaFrame.BEZIER_CURVE) {
            this.add(new BezierCurve(defPoints, t, noCtrlPts, "CURVE " + ++count, dataRadius));
        } else if (isOpen) {
            this.add(new BSpline(defPoints, t, noCtrlPts, "CURVE " + ++count, true, dataRadius));
        } else {
            this.add(new BSpline(defPoints, t, noCtrlPts, "CURVE " + ++count, false, dataRadius));
        }
    }

    @Override
    public Curve remove(int i) {
        noSelected--;
        return super.remove(i);
    }

    public void draw(Graphics2D g, double scale, int currentPoint, boolean showBoundingBox, boolean scaleCurveStrokes,
            boolean showTangent, boolean showThresholdedRegion) {
        for (Curve curve : this) {
            curve.draw(scale, g, currentPoint, showBoundingBox, scaleCurveStrokes, showTangent, showThresholdedRegion);
        }
    }

    /**
     * Returns an array of selected Bezier Curves from the group. If none are selected, this returns
     * null
     *
     * @return	The Bezier Curves that are selected in this group. Or null if none are selected
     */
    public Curve[] getSelected() {
        //If only one curve is selected, odds are that it's the last one. (in use)
        //Hence it's a little more efficient to go down our list.
        Curve[] selectedCurves = new Curve[noSelected];
        int n = noSelected;
        int j = 0;
        for (int i = this.size() - 1; i >= 0 && n > 0; i--) {
            if (this.get(i).isSelected()) {
                selectedCurves[j++] = this.get(i);
                n--;
            }
        }
        return selectedCurves;
    }

    public void setSelected(int[] selectedIndices) {
        setAllUnselected();
        for (int i : selectedIndices) {
            this.get(i).setSelected(true);
        }
        noSelected = selectedIndices.length;
    }

    public double getAvgAverageCurvature(boolean selectedOnly) {
        double total = 0;
        for (Curve curve : this) {
            if ((selectedOnly && curve.isSelected()) || !selectedOnly) {
                total += curve.getAverageCurvature();
            }
        }
        if (!selectedOnly) {
            return total / this.size();
        }
        return total / this.noSelected;
    }

    public double getAvgApproxCurveLength(boolean selectedOnly) {
        double length = 0;
        for (Curve curve : this) {
            if ((selectedOnly && curve.isSelected()) || !selectedOnly) {
                length += curve.getApproxCurveLength();
            }
        }
        if (!selectedOnly) {
            return length / this.size();
        }
        return length / this.noSelected;
    }

    public double getAvgPointCurvature(int currentPoint, boolean selectedOnly) {
        double total = 0;
        for (Curve curve : this) {
            if ((selectedOnly && curve.isSelected()) || !selectedOnly) {
                total += curve.getPointCurvature(currentPoint);
            }
        }
        if (!selectedOnly) {
            return total / this.size();
        }
        return total / this.noSelected;
    }

    public double getStdDevOfAvgCurvature(boolean selectedOnly) {
        double variance = 0;
        double mu = this.getAvgAverageCurvature(selectedOnly);
        for (Curve curve : this) {
            variance += (curve.getAverageCurvature() - mu) * (curve.getAverageCurvature() - mu);
        }
        if (selectedOnly) {
            variance /= this.size() - 1;
        } else {
            variance /= noSelected - 1;
        }
        return Math.sqrt(variance);
    }

    public void setSelected(Curve curve) {
        curve.setSelected(true);
        noSelected++;
    }

    public void setUnselected(Curve curve) {
        curve.setSelected(false);
        noSelected--;
    }

    public void setAllUnselected() {
        for (Curve curve : this) {
            curve.setSelected(false);
        }
        noSelected = 0;
    }

    public void setAllSelected() {
        for (Curve curve : this) {
            curve.setSelected(true);
        }
        noSelected = this.size();
    }

    public void updateIntensities() {
        for (Curve curve : this) {
            curve.updateIntensities();
        }
    }

    public void changeFrame(int newFrame) {
        for (Curve curve : this) {
            curve.translateCurve(newFrame);
        }
    }

    public void recalculateCurvature(int t) {
        for (Curve curve : this) {
            curve.recalculateCurvature(t);
        }
    }

    public int getNoSelected() {
        return noSelected;
    }

    public int getCount() {
        return count;
    }

    public boolean isCurveSelected() {
        return noSelected != 0;
    }
}
