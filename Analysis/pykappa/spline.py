import numpy as np


def bspline_to_bezier(ctrl_points, degree, knot_vector=None):
    """Extraction of the Constituent Bezier Curves using Boehm's Algorithm.
    This implementation only works for cubic B-Splines. 
    Derivation is based on the algorithm described in Sederberg's "Computer Aided
    Geometric Design", on "Extracting Bezier Curves from B-Splines
    using polar coordinates".
    
    """
    assert degree == 3, "Only a degree of 3 is supported."
    
    count = len(ctrl_points)
    
    if not knot_vector:
        knot_vector = np.concatenate(([0] * degree, np.arange(1, count - degree), [count - degree] * degree))

    n_bezier_curves = count - degree
    list_bezier_ctrl_points = []
    for i in range(n_bezier_curves):
        bezier_ctrl_points = np.zeros((4, 2))

        # Second Control Point derivation
        scale_factor = (knot_vector[i + 2] - knot_vector[i + 1]) / (knot_vector[i + 4] - knot_vector[i + 1])
        bezier_ctrl_points[1] = ctrl_points[i + 1] + (ctrl_points[i + 2] - ctrl_points[i + 1]) * scale_factor

        # Third Control Point derivation
        scale_factor = (knot_vector[i + 3] - knot_vector[i + 1]) / (knot_vector[i + 4] - knot_vector[i + 1])
        bezier_ctrl_points[2] = ctrl_points[i + 1] + (ctrl_points[i + 2] - ctrl_points[i + 1]) * scale_factor

        # First Control Point derivation
        scale_factor = (knot_vector[i + 2] - knot_vector[i]) / (knot_vector[i + 3] - knot_vector[i])
        temp_point = ctrl_points[i] + (ctrl_points[i + 1] - ctrl_points[i]) * scale_factor
        scale_factor = (knot_vector[i + 2] - knot_vector[i + 1]) / (knot_vector[i + 3] - knot_vector[i + 1])
        bezier_ctrl_points[0] = temp_point + (bezier_ctrl_points[1] - temp_point) * scale_factor

        # Fourth Control Point derivation
        scale_factor = (knot_vector[i + 3] - knot_vector[i + 2]) / (knot_vector[i + 5] - knot_vector[i + 2])
        temp_point = ctrl_points[i + 2] + (ctrl_points[i + 3] - ctrl_points[i + 2]) * scale_factor
        scale_factor = (knot_vector[i + 3] - knot_vector[i + 2]) / (knot_vector[i + 4] - knot_vector[i + 2])
        bezier_ctrl_points[3] = bezier_ctrl_points[2] + (temp_point - bezier_ctrl_points[2]) * scale_factor

        list_bezier_ctrl_points.append(bezier_ctrl_points)

    list_bezier_ctrl_points = np.array(list_bezier_ctrl_points)
    return list_bezier_ctrl_points


def evaluate_bezier(bezier_ctrl_points, degree, n=100):
    """Evaluate a list of control points.
    
    bezier_ctrl_points: array of shape (N, 4, 2).
    """
    assert degree == 3, "Only a degree of 3 is supported."

    # Bezier basis function (degree = 3)
    bezier_basis = np.array([[1, -3, 3, -1],
                             [0, 3, -6, 3],
                             [0, 0, 3, -3],
                             [0, 0, 0, 1]])

    bezier_points = np.empty((0, 2))
    for control_points in bezier_ctrl_points:
        for t in np.linspace(0, 1, n):   
            basis = np.array([1, t, t**2, t**3])
            p = np.linalg.multi_dot([control_points.T, bezier_basis, basis])
            bezier_points = np.vstack([bezier_points, p])

    return bezier_points
