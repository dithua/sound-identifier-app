/*
Original author: Will Drevo (aka, worldveil).
GitHub repository: https://github.com/worldveil/dejavu
Latest commit used: https://github.com/worldveil/dejavu/commit/d2b8761eb39f8e2479503f936e9f9948addea8ea
Re-written to Java by George Vasios.
 */
/*
MIT License
Copyright (c) 2013 Will Drevo

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package gr.geova.soundidentifier;


public class Fingerprint {

    private static final int IDX_FREQ_I = 0, IDX_TIME_J = 1;

    /*
     Sampling rate, related to the Nyquist conditions,
     which affects the range frequencies we can detect.
     */
    private static final int SAMPLING_RATE = 44100;

    /*
     Size of the FFT window, affects frequency granularity
     */
    private static final int DEFAULT_WINDOW_SIZE = 4096;

    /*
    Degree to which a fingerprint can be paired with its neighbors --
    higher will cause more fingerprints, but potentially better accuracy.
     */
    private static final double DEFAULT_OVERLAP_RATIO = 0.5;

    /*
    Minimum amplitude in spectrogram in order to be considered a peak.
    This can be raised to reduce number of fingerprints, but can negatively
    affect accuracy.
     */
    private static final int DEFAULT_AMP_MIN = 10;

    /*
    Number of cells around an amplitude peak in the spectrogram in order
    for Dejavu to consider it a spectral peak. Higher values mean less
    fingerprints and faster matching, but can potentially affect accuracy.
     */
    private static final int PEAK_NEIGHBORHOOD_SIZE = 20;

    /*
    Thresholds on how close or far fingerprints can be in time in order
    to be paired as a fingerprint. If your max is too low, higher values of
    DEFAULT_FAN_VALUE may not perform as expected.
     */
    private static final int MIN_HASH_TIME_DELTA = 0;
    private static final int MAX_HASH_TIME_DELTA = 200;

    /*
    If True, will sort peaks temporally for fingerprinting;
    not sorting will cut down number of fingerprints, but potentially
    affect performance.
     */
    private static final boolean PEAK_SORT = true;

    /*
    Number of bits to grab from the front of the SHA1 hash in the
    fingerprint calculation. The more you grab, the more memory storage,
    with potentially lesser collisions of matches.
     */
    private static final int FINGERPRINT_REDUCTION = 20;

    public Fingerprint() {}


}
