package sc.fiji.kappa.gui;

import ij.ImagePlus;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ImageUtils<T extends RealType<T>> {

	private RandomAccess<T> imgRA = null;

	public int[] getPixels(ImagePlus imp, int x, int y) {
		int[] avg = new int[3];
		if (x >= 0 && y >= 0 && x < imp.getWidth() && y < imp.getHeight()) {
			int[] rgb;

			// Gets the intensity levels depending on the image type.
			// Yes this is ugly I know that... I whish I could use Numpy here...
			switch (imp.getBitDepth()) {
			case 8:
			case 16:
			case 32:

				if (this.imgRA == null) {
					RandomAccessibleInterval<T> img = ImageJFunctions.wrapReal(imp);
					while (img.numDimensions() < 5) {
						img = Views.addDimension(img, 0, 0);
					}
					this.imgRA = img.randomAccess();
				}

				for (int i = 0; i < 3; i++) {
					this.imgRA.setPosition(x, 0);
					this.imgRA.setPosition(y, 1);
					if (imp.getNChannels() > 1) {
						this.imgRA.setPosition(i, 2);
						if (imp.getNFrames() > 1) {
							this.imgRA.setPosition(imp.getT() - 1, 3);
						}
					} else {
						if (imp.getNFrames() > 1) {
							this.imgRA.setPosition(imp.getT() - 1, 2);
						}
					}
					avg[i] = (int) this.imgRA.get().getRealFloat();
				}

				break;
			case 24: // RGB Color. Here the first 3 elements are their corresponding intensities
				rgb = imp.getPixel(x, y);
				for (int k = 0; k < 3; k++) {
					avg[k] += rgb[k];
				}
				break;
			}
		}
		return avg;
	}

}
