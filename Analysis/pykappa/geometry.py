import numpy as np


def get_point_from_vector(vec, point, distance):
    """Given a vector get the coordinate of the point
    at a certain distance from the input point.
    
    Args:
        vec: array, vector.
        point: array, input point.
        distance: float, the distance.
    """
    vec = np.array(vec)
    point = np.array(point)
    norm = np.sqrt(np.sum(vec ** 2))
    return point + (vec / norm) * distance


def discretize_line(line, spacing):
    """Return a list points located at equidistance on the input line.
    
    The list will also include the input line points.
    
    Args:
        line: array, shape=2x2
        spacing: float, the distance between each points.
    
    """
    
    vec = line[1] - line[0]
    norm = np.sqrt(np.sum(vec ** 2))

    points = [line[0]]

    distances = np.arange(0, np.round(norm), spacing)
    for d in distances:
        p = get_point_from_vector(vec, points[0], d)
        points.append(p)

    points.append(line[1])
    return np.array(points)


def get_normal_points(vec, points, distance):
    """From a vector and a point, get the point perpendicular 
    to the vector at a specific ditance from the input point.
    
    Args:
        vec: array, vector.
        points: array, input point (can be a single point
            or an array of points).
        distance: float.
    """
    norm = np.sqrt(np.sum(vec ** 2))

    dx = vec[0]
    dy = vec[1]

    # Get the normal vectors
    n1 = np.array([-vec[1], vec[0]])
    n2 = np.array([vec[1], -vec[0]])

    # Distance ratio
    t = distance / norm

    points1 = (1 - t) * points + t * (points + n1)
    points2 = (1 - t) * points + t * (points + n2)
    
    return np.array([points1.T, points2.T])
    