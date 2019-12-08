#pragma once

#include <memory>
#include <string>

struct Fingerprint {
public:
    /*
    Degree to which a fingerprint can be paired with its neighbors --
    higher will cause more fingerprints, but potentially better accuracy.
     */
    const int DEFAULT_FAN_VALUE = 15;

    /*
    Thresholds on how close or far fingerprints can be in time in order
    to be paired as a fingerprint. If your max is too low, higher values of
    DEFAULT_FAN_VALUE may not perform as expected.
     */
    const int MIN_HASH_TIME_DELTA = 0;
    const int MAX_HASH_TIME_DELTA = 200;

    /*
    Number of bits to grab from the front of the SHA1 hash in the
    fingerprint calculation. The more you grab, the more memory storage,
    with potentially lesser collisions of matches.
     */
    const int FINGERPRINT_REDUCTION = 20;

    /*
    Number of cells around an amplitude peak in the spectrogram in order
    for Dejavu to consider it a spectral peak. Higher values mean less
    fingerprints and faster matching, but can potentially affect accuracy.
     */
    const int PEAK_NEIGHBORHOOD_SIZE = 20;

    /*
    Minimum amplitude in spectrogram in order to be considered a peak.
    This can be raised to reduce number of fingerprints, but can negatively
    affect accuracy.
     */
    const double DEFAULT_AMP_MIN = 10;

    /*
     Size of the FFT window, affects frequency granularity
     */
    const int DEFAULT_WINDOW_SIZE = 4096;

    /*
    Degree to which a fingerprint can be paired with its neighbors --
    higher will cause more fingerprints, but potentially better accuracy.
     */
    const double DEFAULT_OVERLAP_RATIO = 0.5;

    /*
     Sampling rate, related to the Nyquist conditions,
     which affects the range frequencies we can detect.
     */
    const double DEFAULT_FS = 44100.0;

    std::string fingerprint(short *, int);
private:
    std::vector<std::vector<double>> stride_windows(const std::vector<double>&, size_t, size_t);
    std::vector<double> create_window(int);
    void apply_window(std::vector<double>&, std::vector<std::vector<double>>&);
    std::string generate_hashes(std::vector<std::pair<int,int>>&);
    std::vector<std::pair<int,int>> get_2D_peaks (cv::Mat&);
};
