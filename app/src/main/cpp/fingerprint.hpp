/*MIT License

Copyright (c) 2019 Suliman Alsowelim

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE

***

MIT License

Copyright (c) 2013 Will Drevo

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

***

OpenCV License Agreement
For Open Source Computer Vision Library
(3-clause BSD License)

Copyright (C) 2000-2019, Intel Corporation, all rights reserved.
Copyright (C) 2009-2011, Willow Garage Inc., all rights reserved.
Copyright (C) 2009-2016, NVIDIA Corporation, all rights reserved.
Copyright (C) 2010-2013, Advanced Micro Devices, Inc., all rights reserved.
Copyright (C) 2015-2016, OpenCV Foundation, all rights reserved.
Copyright (C) 2015-2016, Itseez Inc., all rights reserved.
Third party copyrights are property of their respective owners.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
Neither the names of the copyright holders nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.
This software is provided by the copyright holders and contributors “as is” and any express or implied warranties, including, but not limited to, the implied warranties of merchantability and fitness for a particular purpose are disclaimed. In no event shall copyright holders or contributors be liable for any direct, indirect, incidental, special, exemplary, or consequential damages (including, but not limited to, procurement of substitute goods or services; loss of use, data, or profits; or business interruption) however caused and on any theory of liability, whether in contract, strict liability, or tort (including negligence or otherwise) arising in any way out of the use of this software, even if advised of the possibility of such damage.
*/
// Original GitHub repository https://github.com/salsowelim/dejavu_cpp_port/
// Modified by George Vasios

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
    const double FS = 44100.0;

    std::string fingerprint(short *, int);
private:
    std::vector<std::vector<double>> stride_windows(const std::vector<double>&, size_t, size_t);
    std::vector<double> create_window(int);
    void apply_window(std::vector<double>&, std::vector<std::vector<double>>&);
    std::string generate_hashes(std::vector<std::pair<int,int>>&);
    std::vector<std::pair<int,int>> get_2D_peaks (cv::Mat&);
};
