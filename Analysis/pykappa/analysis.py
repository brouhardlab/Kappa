from scipy import interpolate
from scipy import stats
import pandas as pd


def get_theoretical_sampled(new_x, theoretical_path):
    theoretical = pd.read_csv(theoretical_path)
    interp = interpolate.interp1d(theoretical["x coordinate (um)"],
                                  theoretical["curvature (1/um)"],
                                  kind="linear", bounds_error=False)
    theoretical_sampled = pd.DataFrame(new_x, columns=["x"])
    theoretical_sampled["k"] = interp(theoretical_sampled['x'])
    return theoretical_sampled


def get_sampled_and_residuals(curvatures, theoretical_sampled, fname, feature_type):
    data = pd.DataFrame()
    new_x = theoretical_sampled['x']

    for name, curvature in curvatures.groupby("Curve Name"):
        df = pd.DataFrame(new_x, columns=["x"])

        # Set metadata
        df['curve_name'] = name
        df["feature_name"] = "_".join(fname.stem.split("_")[:-2])
        df["feature"] = feature_type(fname.stem.split("_")[-2])

        # Interpolate curvature
        interp = interpolate.interp1d(curvature["X-Coordinate (um)"],
                                      curvature["Point Curvature (um-1)"],
                                      kind="linear", bounds_error=False)
                                      #fill_value="extrapolate")
        df['k'] = interp(df['x'])

        # Compute residuals
        df['residuals'] = theoretical_sampled["k"] - df["k"]
        df['residuals_absolute'] = df['residuals'].abs()

        data = data.append(df)

    return data


# def compute_errors(curve_names, all_curvatures_sampled, all_residuals):

#     data = []
#     for name, curvatures_sampled, residuals in zip(curve_names, all_curvatures_sampled, all_residuals):
#         datum = {}
#         datum["curve_name"] = name
#         datum["feature_name"] = paths["feature_name"]
#         datum["feature"] = paths["feature"]

#         # The absolute error is the mean of the difference of the curvature value for all the x positions.
#         datum["absolute_error"] = residuals["k"].abs().mean()

#         # The relative error is the mean of the ratio between the residuals and the theoretical curve.
#         # Only values of curvature below a threshold are selected to avoid high errors with low values.
#         selector = theoretical_sampled["k"] > k_value_threshold
#         datum["relative_error"] = (residuals[selector]["k"] / theoretical_sampled[selector]["k"]).abs().mean()
#         datum["relative_error"] *= 100

#         # Get the Pearson correlation
#         datum["pearson_coef"] = theoretical_sampled["k"].corr(curvatures_sampled["k"], method='pearson')

#         data.append(datum)

#     data = pd.DataFrame(data)
#     return data
