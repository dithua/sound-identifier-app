#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>
#include <algorithm>
#include <vector>
#include <limits>
#include <iterator>
#include <typeinfo>
#include <math.h>
#include <chrono>
#include <fstream>
#include <utility>
#include <cmath>
#include <fingerprint.hpp>
#include <jni.h>
#include <sha1.hpp>
#include <json.hpp>
#include <android/log.h>

// originally std::vector<std::vector<double>>
std::vector<std::vector<double>> Fingerprint::stride_windows(const std::vector<double>& data,
        size_t block_size, size_t overlap) {
    //https://stackoverflow.com/questions/21344296/striding-windows/21345055
    std::vector<std::vector<double>> res;
    auto min_length = (data.size() - overlap)/(block_size - overlap);
    auto start = data.begin();
    for (auto i=0; i < block_size; ++i) {
        res.emplace_back(std::vector<double>());
        std::vector<double>& block = res.back();
        auto it = start++; // returns an Iterator
        for (auto j = 0; j < min_length; ++j) {
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
        multiplier = 0.5 - 0.5 *(cos(2.0 * M_PI * i / (window_size - 1)));
        res.emplace_back(multiplier);
    }
    return res;
}

void Fingerprint::apply_window(std::vector<double>& hann_window,
        std::vector<std::vector<double>>& data) {
    auto num_columns = data[0].size();
    auto num_rows = data.size();
    for (auto i = 0; i < num_columns; ++i){
        for (auto j = 0; j < num_rows; ++j){
            data[j][i] = data[j][i] * hann_window[j];
        }
    }
}

std::string Fingerprint::generate_hashes(std::vector<std::pair<int, int>> &v_in) {
    //sorting
    //https://stackoverflow.com/questions/279854/how-do-i-sort-a-vector-of-pairs-based-on-the-second-element-of-the-pair
    std::sort(v_in.begin(), v_in.end(), [](auto &left, auto &right) {
        if (left.second == right.second)
            return left.first < right.first;
        return left.second < right.second;
    });

    SHA1 checksum;
    nlohmann::json json;

    for(int i = 0; i < v_in.size(); i++) {
        for(int j = 1; j < DEFAULT_FAN_VALUE; j++) {
            if ((i + j) < v_in.size()) {
                int freq1 = v_in[i].first;
                int freq2 = v_in[i + j].first;

                int time1 = v_in[i].second;
                int time2 = v_in[i + j].second;

                int t_delta = time2 - time1;

                if ((t_delta >= MIN_HASH_TIME_DELTA) and (t_delta <= MAX_HASH_TIME_DELTA)) {
                    char buffer[100];

                    snprintf(buffer, sizeof(buffer), "%d|%d|%d", freq1, freq2, t_delta);
                    //__android_log_print(ANDROID_LOG_VERBOSE, "My App", "buffer %s", buffer);
                    std::string to_be_hashed = buffer;

                    checksum.update(to_be_hashed);

                   std::string hash = checksum.final().erase(FINGERPRINT_REDUCTION, 40);
                    __android_log_print(ANDROID_LOG_VERBOSE, "My App", "HASH %s, Time %d", hash.c_str(), time1);
                    json.push_back(hash);
                }
            }
        }
    }
    //__android_log_print(ANDROID_LOG_VERBOSE, "My App", "JSON dump: %s", json.dump().c_str());
    return json.dump();
}

std::vector<std::pair<int,int>> Fingerprint::get_2D_peaks(cv::Mat& data) {
    // generate binary structure and apply maximum filter
    cv::Mat tmpkernel = cv::getStructuringElement(cv::MORPH_CROSS, cv::Size(3,3), cv::Point(-1,-1)); // equals to generate_structure(2, 1) from dejavu

    cv::Mat kernel = cv::Mat(PEAK_NEIGHBORHOOD_SIZE * 2 + 1,
            PEAK_NEIGHBORHOOD_SIZE * 2 + 1, CV_8U, uint8_t(0));
    kernel.at<uint8_t>(PEAK_NEIGHBORHOOD_SIZE, PEAK_NEIGHBORHOOD_SIZE) = uint8_t(1);
    cv::dilate(kernel, kernel, tmpkernel, cv::Point(-1, -1), PEAK_NEIGHBORHOOD_SIZE, 1, 1); // equals to neighborhood = iterate_structure(struct, PEAK_NEIGHBORHOOD_SIZE) from dejavu (probably...)

    cv::Mat d1;
    cv::dilate(data, d1, kernel); // d1 now contain m1 with max filter applied
    cv::Mat local_max = (data == d1); // 255 if true, 0 otherwise

    // generate eroded background
    cv::Mat background = (data == 0); // 255 if element == 0, 0 otherwise
    cv::Mat eroded_background;
    cv::erode(background, eroded_background, kernel);
    cv::Mat detected_peaks = local_max ^ eroded_background;
    //cv::Mat detected_peaks;
    //cv::bitwise_xor(local_max, eroded_background, detected_peaks);

    auto i = 1;
    auto j = 6;
    while (i <= 3) {
        __android_log_print(ANDROID_LOG_VERBOSE, "my app", "FIRST LOCAL_MAX[11][%d], %d %d %s", j, i, local_max.at<uint8_t>(11,j), (local_max.at<uint8_t>(11,j) == 255)?"true":"false");
        j++;
        i++;
    }

    i = 1;
    j = 4;
    while (i <= 3) {
        __android_log_print(ANDROID_LOG_VERBOSE, "my app", "SECOND LOCAL_MAX[942][%d], %d %d %s", j, i, local_max.at<uint8_t>(942,j), (local_max.at<uint8_t>(942,j) == 255)?"true":"false");

        j++;i++;
    }

    i = 1;
    j = 90;
    while (i <= 3) {
        __android_log_print(ANDROID_LOG_VERBOSE, "my app", "THIRD LOCAL_MAX[637][%d], %d %d %s", j, i, local_max.at<uint8_t>(637,j), (local_max.at<uint8_t>(637,j) == 255)?"true":"false");
        i++;j++;
    }

    i = 1;
    j = 43;
    while (i <= 3) {
        __android_log_print(ANDROID_LOG_VERBOSE, "my app", "FIRST DETECTED_PEAKS[11][%d], %d %d %s", j, i, detected_peaks.at<uint8_t>(11,j), (detected_peaks.at<uint8_t>(11,j) == 255)?"true":"false");
        j++;
        i++;
    }

    i = 1;
    j = 3;
    while (i <= 3) {
        __android_log_print(ANDROID_LOG_VERBOSE, "my app", "SECOND DETECTED_PEAKS[29][%d], %d %d %s", j, i, detected_peaks.at<uint8_t>(942,j), (detected_peaks.at<uint8_t>(942,j) == 255)?"true":"false");

        j++;i++;
    }

    i = 1;
    j = 6;
    while (i <= 3) {
        __android_log_print(ANDROID_LOG_VERBOSE, "my app", "THIRD DETECTED_PEAKS[818][%d], %d %d %s", j, i, detected_peaks.at<uint8_t>(637,j), (detected_peaks.at<uint8_t>(637,j) == 255)?"true":"false");
        i++;j++;
    }

    std::vector<double> amps;
    // now detected_peaks.size == data.size .. iterate through data. get amp where peak == 255 (true), get indices i,j as well.
    std::vector<std::pair<int,int>> freq_time_idx_pairs;
    for(int i = 0; i < data.rows; ++i) {
        for(int j = 0; j < data.cols; ++j) {
            if ((detected_peaks.at<uint8_t>(i, j) == 255) and (data.at<double>(i, j) > DEFAULT_AMP_MIN)) {
                freq_time_idx_pairs.push_back(std::make_pair(i, j));
            }
        }
    }

    for (auto i = 0; i < amps.size(); i++) {
        __android_log_print(ANDROID_LOG_VERBOSE, "my app", "amps %lf", amps[i]);
    }

    return freq_time_idx_pairs;
}

std::string Fingerprint::fingerprint(short *data, int data_size) {
    std::vector<double> vec(&data[0], data + data_size);
    // see mlab.py on how to decide number of frequencies
    int num_freqs = 0; // onesided
    if (DEFAULT_WINDOW_SIZE % 2 == 0) {
        num_freqs = int(std::floor(DEFAULT_WINDOW_SIZE / 2)) + 1;
    } else {
        num_freqs = int(std::floor((DEFAULT_WINDOW_SIZE + 1) / 2));
    }

    // mlab.py _spectral_helper C++ port begins here
    std::vector<std::vector<double>> blocks = stride_windows(vec, DEFAULT_WINDOW_SIZE,
            DEFAULT_WINDOW_SIZE/2); //* DEFAULT_OVERLAP_RATIO);
    __android_log_print(ANDROID_LOG_VERBOSE, "My App", "Stride Windows blocks[0][2] %lf", blocks[0][2]);
    std::vector<double> hann_window = create_window(DEFAULT_WINDOW_SIZE);
    apply_window(hann_window, blocks);
    __android_log_print(ANDROID_LOG_VERBOSE, "My App", "blocks[2008][73] AFTER apply_window %lf", blocks[2008][73]);

    if (false) {
        // original
        /*cv::Mat dst((int) blocks[0].size(), (int) blocks.size(), CV_32F);
        for(int i = 0; i < dst.rows; ++i)
            for(int j = 0; j < dst.cols; ++j) {
                dst.at<double>(i, j) = blocks[j][i];
            }*/
        cv::Mat dst((int) blocks.size(), (int) blocks[0].size(), CV_64F);
        for (int i = 0; i < dst.rows; ++i) {
            for (int j = 0; j < dst.cols; ++j) {
                dst.at<double>(i, j) = blocks[i][j];
            }
        }
        //__android_log_print(ANDROID_LOG_VERBOSE, "My App", "blocks after window, blocks[2008][73] %lf", blocks[2008][73]);
        //__android_log_print(ANDROID_LOG_VERBOSE, "My App", "dst which is a copy of blocks, dst[2008][73] %lf", dst.at<double>(2008,73));

        cv::dft(dst, dst, cv::DftFlags::DFT_COMPLEX_OUTPUT + cv::DftFlags::DFT_ROWS, 0);
        //__android_log_print(ANDROID_LOG_VERBOSE, "My App", "AFTER DFT,... dst[1520][52] %lf", dst.at<std::complex<double>>(1520,52).real());
        //cv::mulSpectrums(dst, dst, dst, 0, true); // computes the conjugate of dst

        // original
        cv::Mat dst2(num_freqs, (int) blocks.at(0).size(), CV_64F);
        /*for(int i = 0; i < num_freqs; ++i)
            for(int j = 0; j < dst2.cols; ++j) {
                dst2.at<double>(i, j) = dst.ptr<double>(j)[2 * i];
            }*/
        for (int i = 0; i < num_freqs; ++i) {
            for (int j = 0; j < dst2.cols; ++j) {
                dst2.at<double>(i, j) = dst.at<double>(i, j);
            }
        }
        //__android_log_print(ANDROID_LOG_VERBOSE, "My App", "AFTER DFT,... dst2 SIZE %dx%d", dst2.rows, dst2.cols);
        //__android_log_print(ANDROID_LOG_VERBOSE, "My App", "AFTER DFT,... dst2[1520][52] %lf", dst2.at<std::complex<double>>(1520,52).real());

        dst.release();

        cv::Mat dst3(dst2.rows, dst2.cols, CV_64F);
        cv::mulSpectrums(dst2, dst2, dst3, 0, true);

        dst2.release();

        for (int i = 1; i < dst3.rows - 1; ++i) {
            for (int j = 0; j < dst3.cols; ++j) {
                dst3.at<double>(i, j) =
                        dst3.at<double>(i, j) * 2;  // 2 is the scaling factor in mlab.py
            }
        }
        __android_log_print(ANDROID_LOG_VERBOSE, "My App", "AFTER DFT,... dst3[0][0] %lf",
                            dst3.at<double>(0, 0));

        dst3 = dst3 * (1.0 / FS);
        double sum = 0.0;
        for (double &i : hann_window) {
            i = std::abs(i);
            i = std::pow(i, 2);
            sum = sum + i;
        }
        dst3 = dst3 * (1.0 / sum);
        // mlab.py _spectral_helper C++ port ends here

        //see https://github.com/worldveil/dejavu/issues/118
        double threshold = 1E-8;
        for (int i = 0; i < dst3.rows; ++i) {
            for (int j = 0; j < dst3.cols; ++j) {
                if ((dst3.at<double>(i, j)) < threshold) {
                    dst3.at<double>(i, j) = threshold;
                }
                dst3.at<double>(i, j) = 10 * log10(dst3.at<double>(i, j));

                if (cvIsInf(dst3.at<double>(i, j))) {
                    dst3.at<double>(i, j) = 0;
                }
            }
        }

        std::vector<std::pair<int, int>> v_in = get_2D_peaks(dst3);
    } else {
        cv::Mat dst(blocks[0].size(),blocks.size(), CV_64F);
        for(int i=0; i<dst.rows; ++i)
            for(int j=0; j<dst.cols; ++j){
                dst.at<double>(i, j) = blocks[j][i];
            }
        cv::dft(dst,dst,cv::DftFlags::DFT_COMPLEX_OUTPUT+cv::DftFlags::DFT_ROWS,0);
        cv::mulSpectrums(dst,dst,dst,0,true);

        cv::Mat dst2(num_freqs,blocks.at(0).size(), CV_64F);
        for(int i=0; i<num_freqs; ++i)
            for(int j=0; j<dst2.cols; ++j){
                dst2.at<double>(i, j) = dst.ptr<double>(j)[2*i];
            }

        for(int i=1; i<dst2.rows -1; ++i)
            for(int j=0; j<dst2.cols; ++j)
                dst2.at<double>(i, j) = dst2.at<double>(i, j)*2;

        dst2 = dst2 * (1.0/FS);
        double sum = 0.0;
        for (double &i : hann_window) {
            i = std::abs(i);
            i = std::pow(i, 2);
            sum = sum + i;
        }
        dst2 = dst2 * (1.0/sum);
        //see https://github.com/worldveil/dejavu/issues/118
        float threshold = 0.00000001;
        for(int i=0; i<dst2.rows; ++i){
            for(int j=0; j<dst2.cols; ++j){
                if ((dst2.at<double>(i, j)) < threshold){
                    dst2.at<double>(i, j) = threshold;
                }
                dst2.at<double>(i, j) = 10 * log10(dst2.at<double>(i, j));

                /*if (cvIsInf(dst2.at<double>(i, j))) {
                    dst2.at<double>(i, j) = 0;
                }*/
            }
        }

        std::vector<std::pair<int,int>> v_in = get_2D_peaks(dst2);

        std::string json = generate_hashes(v_in);
        return json;
    }
    //dst3.release();

    //return json;
}

extern "C" {

    JNIEXPORT void JNICALL Java_gr_geova_soundidentifier_AudioRecordActivity_fingerprint(JNIEnv *env, jobject thisObject, jshortArray channel_samples) {
        auto size = env->GetArrayLength(channel_samples);
        short data[size];
        env->GetShortArrayRegion(channel_samples, 0, size, &data[0]);

        __android_log_print(ANDROID_LOG_VERBOSE, "My App", "CHANNEL_SAMPLES size %d", size);
        __android_log_print(ANDROID_LOG_VERBOSE, "My App", "Last value of Channel %hd", data[size - 1]);

        Fingerprint f;
        f.fingerprint(data, size);
    }
}
