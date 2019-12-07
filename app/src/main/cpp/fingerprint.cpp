#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>
#include <algorithm>
#include <vector>
#include <limits>
#include <iterator>
#include <iostream>
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
//#include <boost/property_tree/ptree.hpp>
//#include <boost/property_tree/json_parser.hpp>
//#include <boost/uuid/sha1.hpp>

//using boost::property_tree::ptree;

// originally double
std::vector<std::vector<double>> Fingerprint::stride_windows(const std::vector<double>& data,
        size_t blocksize, size_t overlap){
  //https://stackoverflow.com/questions/21344296/striding-windows/21345055
  std::vector<std::vector<double>> res;
  auto minlen = (data.size() - overlap)/(blocksize - overlap);
  auto start = data.begin();
  for (auto i=0; i < blocksize; i++)
  {
    res.emplace_back(std::vector<double>());
    std::vector<double >& block = res.back();
    auto it = start++;
    for (auto j = 0; j < minlen; ++j)
    {
      block.push_back(*it);
      std::advance(it, (blocksize-overlap));
    }
  }
  return res;
}

int Fingerprint::detrend(std::vector<std::vector<double>>& data){
    size_t nocols = data[0].size();
    size_t norows = data.size();
    double mean = 0;
    for (size_t i=0; i<nocols; ++i){
        for (size_t j=0; j<norows; ++j){
          mean = mean + data[j][i];
        }
    }
    mean = mean/(norows*nocols);
    for (size_t i=0; i<nocols; ++i){
        for (size_t j=0; j<norows; ++j){
            data[j][i] = data[j][i] - mean;
        }
    }
    return 0;
}

std::vector<double> Fingerprint::create_window(int wsize){
    std::vector<double> res;
    double multiplier;
    for (int i = 0; i < wsize; i++) {
        multiplier = 0.5 - 0.5 *(cos(2.0*M_PI*i/(wsize-1)));
        res.emplace_back(multiplier);
    }
    return res;
}

void Fingerprint::apply_window(std::vector<double> &hann_window,std::vector<std::vector<double>>& data) {
    size_t nocols = data[0].size();
    size_t norows = data.size();
    for (size_t i=0; i<nocols; ++i){
        for (size_t j=0; j<norows; ++j){
            data[j][i] = data[j][i] * hann_window[j];
        }
    }
}


/*std::string get_sha1(const std::string& p_arg) {
    boost::uuids::detail::sha1 sha1;
    sha1.process_bytes(p_arg.data(), p_arg.size());
    unsigned hash[5] = {0};
    sha1.get_digest(hash);
    // Back to string
    char buf[41] = {0};
    for (int i = 0; i < 5; i++) {
        std::sprintf(buf + (i << 3), "%08x", hash[i]);
    }

    return std::string(buf);
}*/

std::string Fingerprint::generate_hashes(std::vector<std::pair<int,int>> &v_in){
  //sorting
  //https://stackoverflow.com/questions/279854/how-do-i-sort-a-vector-of-pairs-based-on-the-second-element-of-the-pair
    std::sort(v_in.begin(), v_in.end(), [](auto &left, auto &right) {
        if (left.second == right.second)
            return left.first < right.first;
        return left.second < right.second;
    });

    SHA1 checksum;
    nlohmann::json json;

    for(int i=0; i<v_in.size(); i++){
        for(int j=1; j<DEFAULT_FAN_VALUE; j++){
            if ((i+j) < v_in.size()){
                int freq1 = v_in[i].first;
                int freq2 = v_in[i+j].first;
                int time1 = v_in[i].second;
                int time2 = v_in[i+j].second;
                int t_delta = time2 - time1;
                if ((t_delta >= MIN_HASH_TIME_DELTA) and (t_delta <= MAX_HASH_TIME_DELTA)){
                    char buffer [100];
                    snprintf(buffer, sizeof(buffer),"%d|%d|%d", freq1,freq2,t_delta);
                    std::string to_be_hashed = buffer;
                    //std::string hash_result = get_sha1(to_be_hashed).erase(FINGERPRINT_REDUCTION,40);
                    checksum.update(to_be_hashed);
                    std::string hash_result = checksum.final().erase(FINGERPRINT_REDUCTION, 40);
                    json.push_back(hash_result);
                }
            }
        }
    }
    __android_log_print(ANDROID_LOG_VERBOSE, "My App", "%s", json.dump().c_str());
    return json.dump();
}

std::vector<std::pair<int,int>> Fingerprint::get_2D_peaks (cv::Mat  data){
  /* generate binary structure and apply maximum filter*/
    cv::Mat tmpkernel = cv::getStructuringElement(cv::MORPH_CROSS,cv::Size(3,3),cv::Point(-1,-1));
    cv::Mat kernel = cv::Mat(PEAK_NEIGHBORHOOD_SIZE *2 + 1,
            PEAK_NEIGHBORHOOD_SIZE * 2 + 1, CV_8U, uint8_t(0));
    kernel.at<uint8_t>(PEAK_NEIGHBORHOOD_SIZE, PEAK_NEIGHBORHOOD_SIZE) = uint8_t(1);
    cv::dilate(kernel, kernel, tmpkernel,cv::Point(-1, -1), PEAK_NEIGHBORHOOD_SIZE,1,1);
    cv::Mat d1;
    cv::dilate(data, d1, kernel);/* d1 now contain m1 with max filter applied */
    /* generate eroded background */
    cv::Mat background = (data == 0); // 255 if element == 0 , 0 otherwise
    cv::Mat local_max = (data == d1); // 255 if true, 0 otherwise
    cv::Mat eroded_background;
    cv::erode(background, eroded_background, kernel);
    cv::Mat detected_peaks = local_max - eroded_background;
    /* now detected peaks.size == m1.size .. iterate through m1. get amp where peak == 255 (true), get indices i,j as well.*/
    std::vector<std::pair<int,int>> freq_time_idx_pairs;
    for(int i=0; i<data.rows; ++i){
        for(int j=0; j<data.cols; ++j){
            if ((detected_peaks.at<uint8_t>(i, j) == 255) and (data.at<double>(i,j) > DEFAULT_AMP_MIN)) {
                freq_time_idx_pairs.push_back(std::make_pair(i,j));
            }
        }
    }

    return freq_time_idx_pairs;
}

void Fingerprint::max_filter(std::vector<std::vector<double>>& data){
      //https://gist.github.com/otmb/014107e7b6c6d6a79f0ac1ccc456580a
    cv::Mat m1(data.size(), data.at(0).size(), CV_32F);
    for(int i=0; i<m1.rows; ++i) {
        for(int j=0; j<m1.cols; ++j) {
            m1.at<double>(i, j) = data.at(i).at(j);
        }
    }
    /* generate binary structure and apply maximum filter*/
    cv::Mat tmpkernel = cv::getStructuringElement(cv::MORPH_CROSS,cv::Size(3,3),cv::Point(-1,-1));
    cv::Mat kernel = cv::Mat(PEAK_NEIGHBORHOOD_SIZE*2+1,PEAK_NEIGHBORHOOD_SIZE*2+1, CV_8U, uint8_t(0));
    kernel.at<uint8_t>(PEAK_NEIGHBORHOOD_SIZE,PEAK_NEIGHBORHOOD_SIZE) = uint8_t(1);
    cv::dilate(kernel, kernel, tmpkernel,cv::Point(-1, -1), PEAK_NEIGHBORHOOD_SIZE,1,1);
    cv::Mat d1;
    cv::dilate(m1, d1, kernel);
    /* d1 now contain m1 with max filter applied */
    /* generate eroded background */
    cv::Mat background = (m1 == 0);
    cv::Mat local_max = (m1 == d1);
    cv::Mat eroded_background;
    cv::erode(background, eroded_background, kernel);
    cv::Mat detected_peaks = local_max - eroded_background;
    std::vector<std::pair<int,int>> freq_time_idx_pairs;
    for(int i=0; i<m1.rows; ++i){
        for(int j=0; j<m1.cols; ++j){
            if ((detected_peaks.at<uint8_t>(i, j) == 255) and (m1.at<double>(i,j) > DEFAULT_AMP_MIN)) {
                freq_time_idx_pairs.push_back(std::make_pair(i,j));
            }
        }
    }
}

std::string Fingerprint::fingerprint(std::vector<double> vec) {
    //std::vector<double> vec(&data[0], data + data_size);
    // see mlab.py on how to decide number of frequencies
    int max_freq = 0; //onesided
    if (DEFAULT_WINDOW_SIZE % 2 == 0){
        max_freq =  int(std::floor(DEFAULT_WINDOW_SIZE / 2)) + 1;
    }else{
        max_freq =  int(std::floor((DEFAULT_WINDOW_SIZE + 1) / 2));
    }

    std::vector<std::vector<double>> blocks = stride_windows(vec, DEFAULT_WINDOW_SIZE, DEFAULT_WINDOW_SIZE*DEFAULT_OVERLAP_RATIO);
    std::vector<double> hann_window = create_window(DEFAULT_WINDOW_SIZE);
    apply_window(hann_window,blocks);

    cv::Mat dst(blocks[0].size(),blocks.size(), CV_32F);
    for(int i=0; i<dst.rows; ++i) {
        for(int j=0; j<dst.cols; ++j){
            dst.at<double>(i, j) = blocks[j][i];
        }
    }

    cv::dft(dst,dst,cv::DftFlags::DFT_COMPLEX_OUTPUT+cv::DftFlags::DFT_ROWS,0);
    cv::mulSpectrums(dst,dst,dst,0,true);

    cv::Mat dst2(max_freq,blocks.at(0).size(), CV_32F);
    for(int i=0; i<max_freq; ++i) {
        for(int j=0; j<dst2.cols; ++j){
            dst2.at<double>(i, j) = dst.ptr<double>(j)[2*i];
        }
    }

    for(int i=1; i<dst2.rows -1; ++i) {
        for(int j=0; j<dst2.cols; ++j) {
            dst2.at<double>(i, j) = dst2.at<double>(i, j)*2;
        }
    }

    dst2 = dst2 * (1.0/FS);
    double sum = 0.0;
    double tmp = 0.0;
    for(unsigned int i = 0; i < hann_window.size(); i++){
        if(hann_window[i] < 0) {
            tmp = hann_window[i]* -1;
        } else {
            tmp = hann_window[i];
        }
        sum = sum + (tmp*tmp);
    }
    dst2 = dst2 * (1.0/sum);
    //see https://github.com/worldveil/dejavu/issues/118
    double threshold = 0.00000001;
    for(int i=0; i<dst2.rows; ++i){
        for(int j=0; j<dst2.cols; ++j){
            if ((dst2.at<double>(i, j)) < threshold){
                dst2.at<double>(i, j) = threshold;
            }
            dst2.at<double>(i, j) = 10 * log10(dst2.at<double>(i, j));
        }
    }

    std::vector<std::pair<int,int>> v_in = get_2D_peaks(dst2);
    std::string json = generate_hashes(v_in);
    return json;
}

extern "C" {

JNIEXPORT void JNICALL Java_gr_geova_soundidentifier_AudioRecordActivity_fingerprint(JNIEnv *env, jobject thisObject, jdoubleArray channel_samples) {
    auto size = env->GetArrayLength(channel_samples);
    std::vector<double> data(size);
    env->GetDoubleArrayRegion(channel_samples, 0, size, &data[0]);

    Fingerprint f;
    f.fingerprint(data);
}

}


/*int main() {
  //std::system("ffmpeg -hide_banner -loglevel panic -i test.mp3 -f s16le -acodec pcm_s16le -ss 0 -ac 1 -ar 22050 - > raw_data ");
  //https://www.daniweb.com/programming/software-development/threads/128352/read-a-raw-pcm-file-and-then-play-it-with-sound-in-c-or-c
  //https://stackoverflow.com/questions/49161854/reading-raw-audio-file
  //std::fstream f_in;
    short speech;
    double data[200000];
  //f_in.open("raw_data", std::ios::in | std::ios::binary);
  //int i = 0;
    while (true) {
        f_in.read((char *)&speech, 2);
        if (!f_in.good()){
           break;
        }
        data[i] = speech;
        i++;
    }
  //f_in.close();
    std::string json = fingerprint(data, i);
    cout << json << std::endl;
    return 0;
}*/