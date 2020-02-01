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
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>
#include <utility>
#include <cmath>
#include <fingerprint.hpp>
#include <jni.h>
#include <sha1.hpp>
#include <android/log.h>
#include <sstream>
//#include <iostream>
//#include <algorithm>
//#include <limits>
//#include <iterator>
//#include <typeinfo>
//#include <chrono>
//#include <fstream>

std::vector<std::vector<double>> Fingerprint::stride_windows(const std::vector<double>& data,
        size_t block_size, size_t overlap) {
    //https://stackoverflow.com/questions/21344296/striding-windows/21345055
    std::vector<std::vector<double>> res;
    auto min_length = (data.size() - overlap) / (block_size - overlap);
    auto start = data.begin();
    for (auto i = 0; i < block_size; i++) {
        res.emplace_back(std::vector<double>());
        std::vector<double> &block = res.back();
        auto it = start++; // returns an Iterator
        for (auto j = 0; j < min_length; j++) {
            block.push_back(*it);
            std::advance(it, (block_size - overlap));
        }
    }
    return res;
}

std::vector<double> Fingerprint::create_window(int window_size) {
    std::vector<double> res;
    double multiplier;
    for (int i = 0; i < window_size; i++) {
        multiplier = 0.5 - 0.5 * (cos(2.0 * M_PI * i / (window_size - 1)));
        res.emplace_back(multiplier);
    }
    return res;
}

void Fingerprint::apply_window(std::vector<double>& hann_window,
                               std::vector<std::vector<double>>& data) {
    auto num_columns = data[0].size();
    auto num_rows = data.size();
    for (auto i = 0; i < num_columns; i++) {
        for (auto j = 0; j < num_rows; j++) {
            data[j][i] = data[j][i] * hann_window[j];
        }
    }
}

inline std::string replace_occurrences(std::string json, const std::string& from,
        const std::string& to) {
    size_t position = 0;
    while((position = json.find(from, position)) != std::string::npos) {
        json.replace(position, from.length(), to);
        position += to.length();
    }
    return json;
}

std::string Fingerprint::generate_hashes(std::vector<std::pair<int, int>>& v_in) {
    //sorting
    //https://stackoverflow.com/questions/279854/how-do-i-sort-a-vector-of-pairs-based-on-the-second-element-of-the-pair
    std::sort(v_in.begin(), v_in.end(), [](auto& left, auto& right) {
        if (left.second == right.second)
            return left.first < right.first;
        return left.second < right.second;
    });

    SHA1 checksum;

    std::stringstream json;
    json << "{";

    auto fingerprint_index = 0;

    for (int i = 0; i < v_in.size(); i++) {
        for (int j = 1; j < DEFAULT_FAN_VALUE; j++) {
            if ((i + j) < v_in.size()) {
                int freq1 = v_in[i].first;
                int freq2 = v_in[i + j].first;

                int time1 = v_in[i].second;
                int time2 = v_in[i + j].second;

                int t_delta = time2 - time1;

                if ((t_delta >= MIN_HASH_TIME_DELTA) and (t_delta <= MAX_HASH_TIME_DELTA)) {
                    char buffer[100];

                    snprintf(buffer, sizeof(buffer), "%d|%d|%d", freq1, freq2, t_delta);
                    std::string to_be_hashed = buffer;

                    checksum.update(to_be_hashed);

                    std::string hash = checksum.final().erase(FINGERPRINT_REDUCTION, 40); // keep the first 20 hex characters

                    // this jsonify process produces an invalid JSON
                    // the JSON becomes valid later
                    /*
                     * The below lines produces this:
                     *
                     *  {
                     *      "fingerprint_0": ["<hash>", "<time1>"]
                     *      "fingerprint_1": ["<hash>", "<time1>"] etc...
                     *  }
                     */
                    json << "\"fingerprint_" << fingerprint_index++ << "\"" << ":";
                    json << "[" << "\"" << hash << "\"" << "," << time1 << "]";
                }
            }
        }
    }

    json << "}";

    /*
     * After running replace_occurrences, we have:
     *
     *  {
     *      "fingerprint_0": ["<hash>", "<time1>"],
     *      "fingerprint_1": ["<hash>", "<time1>"], etc...
     *  }
     *
     * Notice the comma at the end of each line.
     *
     */
    std::string JSON = replace_occurrences(json.str(), "]\"", "],\"");

    __android_log_print(ANDROID_LOG_VERBOSE, "Native Code", "JSON dump: %s", JSON.c_str());

    return JSON;
}

std::vector<std::pair<int, int>> Fingerprint::get_2D_peaks(cv::Mat& data) {
    // generate binary structure and apply maximum filter
    cv::Mat tmpkernel = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(3, 3), cv::Point(-1, -1)); // equals to generate_structure(2, 1) from dejavu

    cv::Mat kernel = cv::Mat(PEAK_NEIGHBORHOOD_SIZE * 2 + 1,
                             PEAK_NEIGHBORHOOD_SIZE * 2 + 1, CV_8U, uint8_t(0));
    kernel.at<uint8_t>(PEAK_NEIGHBORHOOD_SIZE, PEAK_NEIGHBORHOOD_SIZE) = uint8_t(1);
    cv::dilate(kernel, kernel, tmpkernel, cv::Point(-1, -1), PEAK_NEIGHBORHOOD_SIZE, 1, 1);

    cv::Mat d1;
    cv::dilate(data, d1, kernel); // d1 now contain m1 with max filter applied
    cv::Mat local_max = (data == d1); // 255 if true, 0 otherwise

    // generate eroded background
    cv::Mat background = (data == 0); // 255 if element == 0, 0 otherwise
    cv::Mat eroded_background;
    cv::erode(background, eroded_background, kernel);
    cv::Mat detected_peaks = local_max ^ eroded_background; // my addition (^) -- it is '-' in the original

    // now detected_peaks.size == data.size .. iterate through data. get amp where peak == 255 (true), get indices i,j as well.
    std::vector<std::pair<int, int>> freq_time_idx_pairs;
    for (int i = 0; i < data.rows; i++) {
        for (int j = 0; j < data.cols; j++) {
            if ((detected_peaks.at<uint8_t>(i, j) == 255) and
                (data.at<double>(i, j) > DEFAULT_AMP_MIN)) {
                freq_time_idx_pairs.emplace_back(i, j);
            }
        }
    }

    return freq_time_idx_pairs;
}

std::string Fingerprint::fingerprint(short *data, int data_size) {
    std::vector<double> vec(&data[0], data + data_size);

    int num_freqs = 0; // one-sided
    if (DEFAULT_WINDOW_SIZE % 2 == 0) {
        num_freqs = int(std::floor(DEFAULT_WINDOW_SIZE / 2)) + 1;
    } else {
        num_freqs = int(std::floor((DEFAULT_WINDOW_SIZE + 1) / 2));
    }

    // mlab.py _spectral_helper C++ port begins here
    std::vector<std::vector<double>> blocks = stride_windows(vec,
            DEFAULT_WINDOW_SIZE, DEFAULT_WINDOW_SIZE * DEFAULT_OVERLAP_RATIO);
    std::vector<double> hann_window = create_window(DEFAULT_WINDOW_SIZE);
    apply_window(hann_window, blocks);

    cv::Mat dst(blocks[0].size(), blocks.size(), CV_64F);
    for (int i = 0; i < dst.rows; i++) {
        for (int j = 0; j < dst.cols; j++) {
            dst.at<double>(i, j) = blocks[j][i];
        }
    }

    cv::dft(dst, dst, cv::DftFlags::DFT_COMPLEX_OUTPUT + cv::DftFlags::DFT_ROWS, 0);
    cv::mulSpectrums(dst, dst, dst, 0, true);

    cv::Mat dst2(num_freqs, blocks.at(0).size(), CV_64F);
    for (int i = 0; i < num_freqs; i++) {
        for (int j = 0; j < dst2.cols; j++) {
            dst2.at<double>(i, j) = dst.ptr<double>(j)[2 * i];
        }
    }

    for (int i = 1; i < dst2.rows - 1; i++) {
        for (int j = 0; j < dst2.cols; j++) {
            dst2.at<double>(i, j) = dst2.at<double>(i, j) * 2; // 2 is the scaling factor in mlab.py
        }
    }

    dst2 = dst2 * (1.0 / DEFAULT_FS);
    double sum = 0.0;
    for (double &item : hann_window) {
        item = std::abs(item); // overkill, I know, since we call pow2 after that (it appears in mlab.py, though)
        item = std::pow(item, 2);
        sum += item;
    }

    dst2 = dst2 * (1.0 / sum);
    // mlab.py _spectral_helper C++ port ends here

    //see https://github.com/worldveil/dejavu/issues/118
    double threshold = 0.00000001;
    for (int i = 0; i < dst2.rows; i++) {
        for (int j = 0; j < dst2.cols; j++) {
            if ((dst2.at<double>(i, j)) < threshold) {
                dst2.at<double>(i, j) = threshold;
            }
            dst2.at<double>(i, j) = 10 * log10(dst2.at<double>(i, j));

            // my addition -- as it appears in fingerprint.py
            if (cvIsInf(dst2.at<double>(i, j))) {
                dst2.at<double>(i, j) = 0;
            }
        }
    }

    std::vector<std::pair<int, int>> v_in = get_2D_peaks(dst2);

    std::string json = generate_hashes(v_in);
    return json;
}

extern "C" {

    JNIEXPORT jstring JNICALL Java_gr_geova_soundidentifier_ResultsActivity_fingerprint(JNIEnv *env, jobject thisObject, jshortArray channel_samples) {
        auto size = env->GetArrayLength(channel_samples);
        short data[size];
        env->GetShortArrayRegion(channel_samples, 0, size, &data[0]);

        Fingerprint f;
        auto json_result_native = f.fingerprint(data, size);

        // https://stackoverflow.com/questions/11621449/send-c-string-to-java-via-jni/24564937#24564937

        // convert char * to jbyte*
        auto json_result_java = reinterpret_cast<const jbyte*>(json_result_native.c_str());

        // fill byte array with the contents of json_result_java
        auto json_result_length = json_result_native.length();
        auto bytes = env->NewByteArray(json_result_length);
        env->SetByteArrayRegion(bytes, 0, json_result_length, json_result_java);

        auto charset_class = env->FindClass("java/nio/charset/Charset");
        auto forName = env->GetStaticMethodID(charset_class,
                "forName", "(Ljava/lang/String;)Ljava/nio/charset/Charset;");
        auto utf8 = env->NewStringUTF("UTF-8");
        auto charset = env->CallStaticObjectMethod(charset_class, forName, utf8);

        auto string_class = env->FindClass("java/lang/String");
        auto method_id = env->GetMethodID(string_class, "<init>", "([BLjava/nio/charset/Charset;)V");

        // convert bytes to java.lang.String and eventually to jstring
        auto json_fingerprint = reinterpret_cast<jstring>(env->NewObject(string_class, method_id, bytes, charset));

        return json_fingerprint;
    }

}
