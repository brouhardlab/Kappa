import copy
from collections import OrderedDict
import pandas as pd
import numpy as np


def get_next(iterator):
    return iterator.__next__()[1].values[0]
    
    
def get_next_int(iterator):
    return int(get_next(iterator))
    
    
def get_next_float(iterator):
    return float(get_next(iterator))
    

def parse_kappa(fname):
    
    kapp_df = pd.read_csv(fname, header=None)
    
    data = []
    iterator = kapp_df.iterrows()
    n = get_next_int(iterator)
    for i in range(n):

        datum = {}

        datum["curveType"] = get_next_int(iterator)
        datum["noKeyframes"] = get_next_int(iterator)
        datum["noCtrlPts"] = get_next_int(iterator)

        datum["bsplineType"] = 0
        if datum["curveType"] == 1:
            datum["bsplineType"] = get_next_int(iterator)

        currentKeyframe = get_next_int(iterator)

        datum["curve_pos"] = []
        for c in range(datum["noCtrlPts"]):
            x = get_next_float(iterator)
            y = get_next_float(iterator)
            datum["curve_pos"].append((x, y))
        datum["curve_pos"] = np.array(datum["curve_pos"])

        for c in range(1, datum["noKeyframes"]):
            print("More than keyframe detected")
            
        data.append(datum)
        
    return data


def write_kappa(data, fname):
    out = f"{len(data)}"
    for datum in data:
        out += f"\n{datum['curveType']}"
        out += f"\n{datum['noKeyframes']}"
        out += f"\n{datum['noCtrlPts']}"
        
        if datum["curveType"] == 1:
            out += f"\n{datum['bsplineType']}"
            
        out += f"\n{1}".format(1)  # Current keyframe
        
        for pos in datum["curve_pos"]:
            out += f"\n{pos[0]}"
            out += f"\n{pos[1]}"
            
        for c in range(1, datum["noKeyframes"]):
            print("More than keyframe detected")
            
    out += "\n"

    with open(fname, "w") as f:
        f.write(out)

        
def transform_positions(data, transform_func):
    data = copy.deepcopy(data)
    for datum in data:
        datum["curve_pos"] = transform_func(datum["curve_pos"])
    return data
