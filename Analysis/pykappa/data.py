from pathlib import Path

from .io import parse_kappa

def get_sine_curves_path():
    return Path(__file__).parent / "Original_Sine_Curves.kapp"


def get_sine_curves():
    fname = get_sine_curves_path()
    return parse_kappa(fname)
    
    
def get_spiral_curves_path():
    return Path(__file__).parent / "Original_Spiral_Curves.kapp"


def get_spiral_curves():
    fname = get_spiral_curves_path()
    return parse_kappa(fname)
