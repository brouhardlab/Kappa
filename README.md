**IMPORTANT: THIS REPOSITORY HAS MOVED under the Fiji organization umbrella. Please visit https://github.com/fiji/Kappa for contributions or report a bug. You can also visit [the Image.sc forum](https://forum.image.sc/tag/kappa).**

---

![Kappa logo](logo.png)

[![](https://travis-ci.org/brouhardlab/Kappa.svg?branch=master)](https://travis-ci.org/brouhardlab/Kappa)
[![Binder](https://mybinder.org/badge.svg)](https://mybinder.org/v2/gh/brouhardlab/Kappa/master?urlpath=lab/tree/Analysis/Notebooks)

# Kappa

`Kappa` is **A Fiji plugin for Curvature Analysis**.

It allows a user to measure curvature in images in a convenient way. You can trace an initial shape with a B-Spline curve in just a few clicks and then fit that curve to image data with a minimization algorithm. It’s fast and robust.

![Kappa Screenshot](screenshot.png)

See also the [ImageJ Wiki page](https://imagej.net/Kappa).

## Publication

Analysis done for the paper is available and can be reproduced from this older. See [`Analysis`](./Analysis).

You can fire a remote Jupyter notebook server and reproduce the analysis using Binder: <https://mybinder.org/v2/gh/brouhardlab/Kappa/master?urlpath=lab/tree/Analysis/Notebooks.>

## Installation

- Start [Fiji](https://imagej.net/Fiji/Downloads).
- Click on `Help ▶ Update...`.
- In the new window, click on `Manage update sites`.
- Scroll to find `Kappa` in the column `Name`. Click on it.
- Click `Close` and then `Apply changes`.
- Restart Fiji.
- Open your image.
- Then you can start the plugin with `Plugins ► Analyze ► Kappa - Curvature Analysis`.

## Documentation

See documentation in the [docs/ folder](./docs/).

## Authors

`Kappa` has been created originally by [Kevan Lu](http://www.kevan.lu/) and is now maintained by [Hadrien Mary](mailto:hadrien.mary@gmail.com).

This work started in 2013 in the [Gary Brouhard laboratory](http://brouhardlab.mcgill.ca/) at the University of McGill.

## License

MIT license. See [LICENSE.txt](LICENSE.txt)
