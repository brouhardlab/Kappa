import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

import sys; sys.path.append("../../")
import pykappa

import scyjava
import jnius

# Load Java classes
KappaFrame = jnius.autoclass('sc.fiji.kappa.gui.KappaFrame')
ImageIO = jnius.autoclass('javax.imageio.ImageIO')
File = jnius.autoclass('java.io.File')
CurvesExporter = jnius.autoclass('sc.fiji.kappa.gui.CurvesExporter')


def save(frame, fname=None):
    # Get the current image displayed on Kappa's interface (with all the overlays)
    img = frame.getCombinedImage()

    # Create a tem file
    img_format = 'png'
    if not fname:
        fname = tempfile.mktemp(suffix=f".{img_format}")
    
    # Save image on disk
    ImageIO.write(img, img_format, File(str(fname)))
    
    return fname
  

def process_example(sample, data_dir, ij):
  
  sample_dir = data_dir / sample["dataset_type"] / sample["color_type"]

  results_dir = sample_dir / "Results"
  results_dir.mkdir(exist_ok=True)

  fname = sample_dir / sample['name']
  kappa_path = fname.with_suffix(".kapp")
  assert fname.exists() and kappa_path.exists()

  frame = KappaFrame(ij.context)
  frame.getKappaMenubar().openImageFile(str(fname))
  frame.resetCurves()
  frame.drawImageOverlay()

  # Save screenshot
  screenshot_path = results_dir / f"{fname.stem}_1_Image.png"
  save(frame, fname=screenshot_path)

  frame.getKappaMenubar().loadCurveFile(str(kappa_path))
  frame.getCurves().setAllSelected()

  # Set line thickness
  frame.setBaseStrokeThickness(sample['line_thickness'])

  # Set zoom
  frame.controlPanel.getScaleSlider().setValue(sample['zoom'])
  frame.drawImageOverlay()

  # Save screenshot
  screenshot_path = results_dir / f"{fname.stem}_2_Initial.png"
  save(frame, fname=screenshot_path)
  
  # Show thresholded data contours and pixels
  frame.infoPanel.getShowRadiusCheckBox().setSelected(True)
  frame.infoPanel.getShowDatapointsCheckBox().setSelected(True)

  # Set radius for the thresold
  frame.infoPanel.getThresholdRadiusSpinner().setValue(scyjava.to_java(sample['threshold_radius']))

  # Set pixels to dark or not
  frame.infoPanel.getDataRangeComboBox().setSelectedIndex(1 if sample['is_signal_dark'] else 0)

  # Set thresold value
  frame.getInfoPanel().thresholdSlider.setValue(sample['threshold_value'])

  frame.drawImageOverlay()

  # Save screenshot
  screenshot_path = results_dir / f"{fname.stem}_3_Threshold.png"
  save(frame, fname=screenshot_path)

  if sample['adjust_ctrl_point']:
      frame.setEnableCtrlPtAdjustment(True)
  else:
      frame.setEnableCtrlPtAdjustment(False)
  frame.fitCurves()
  frame.infoPanel.getShowRadiusCheckBox().setSelected(False)
  frame.infoPanel.getShowDatapointsCheckBox().setSelected(False)
  frame.drawImageOverlay()

  # Save screenshot
  screenshot_path = results_dir / f"{fname.stem}_4_Final.png"
  save(frame, fname=screenshot_path)

  # Save curvature values
  exporter = CurvesExporter(frame)
  curvature_path = sample_dir / f"{fname.stem}_Curvatures.csv"
  exporter.exportToFile(str(curvature_path), False)

  # plot curvature values
  curvatures = pd.read_csv(curvature_path)

  plt.ioff()
  for curve_name, curvature in curvatures.groupby('Curve Name'):
    fig, ax = plt.subplots(figsize=(8, 6))

    y_data = curvature['Point Curvature (um-1)']
    n_points = len(y_data)

    assert len(curvature['Curve Length (um)'].unique()) == 1
    curve_length = curvature['Curve Length (um)'].unique()[0]

    x_data = np.linspace(0, curve_length, n_points)

    ax.plot(x_data, y_data, color="#d81920ff", lw=6)

    ax.set_xticks([])
    ax.set_yticks([])

    y_lim_max = curvatures['Point Curvature (um-1)'].max() * 1.05
    ax.set_ylim(-0.02, y_lim_max)

    kwargs = dict(font="DejaVu Sans", font_size_ratio=1, axes_color="#231f20ff",
                  x_axis_grid=False, y_axis_grid=False, tick_width=6)
    pykappa.mpl.set_ax_style(ax, **kwargs)

    ax.set_ylabel("Curvature", fontsize=28)
    ax.set_xlabel("Curve Length", fontsize=28)

    plot_path = results_dir / f"{fname.stem}_5_Curvatures_{curve_name}.png"
    fig.savefig(plot_path, dpi=300, transparent=False, bbox_inches='tight', pad_inches=0)
    plt.clf()

  plt.ion()