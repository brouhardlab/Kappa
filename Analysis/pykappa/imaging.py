import numpy as np

def awgn(input_signal, snr_dB):
    original_shape = input_signal.shape
    flatten_signal = input_signal.flatten()

    # Calculate actual symbol energy
    energy = np.sum(np.abs(flatten_signal) ** 2)

    # SNR to linear scale
    snr_linear = 10 ** (snr_dB / 10)

    # Find the noise spectral density
    spectral_density = energy / snr_linear

    # Standard deviation for AWGN Noise when signal is real
    noise_sigma = np.sqrt(spectral_density)

    # Computed noise
    noise = noise_sigma * np.random.randn(len(flatten_signal))

    output_signal = flatten_signal + noise
    output_signal = output_signal.reshape(original_shape)

    return output_signal


def gaussian_kernel(size=21, sigma=3):
    x, y = np.meshgrid(np.linspace(-1, 1, size), np.linspace(-1, 1, size))
    d = np.sqrt(x * x + y * y)
    mu = 0.0
    kernel = np.exp(-( (d - mu)**2 / ( 2.0 * sigma**2 ) ) )
    return kernel


def rescale_to_8bit(im):
    im = (im - im.min()) / (im.max() - im.min())
    return (im * 255).astype("uint8")