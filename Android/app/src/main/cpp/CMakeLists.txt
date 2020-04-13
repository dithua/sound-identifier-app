#https://medium.com/beesightsoft/android-set-up-an-environment-for-opencv-c-ndk18-6dd741267ec1

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED TRUE)

set(target fingerprint-lib)
project(${target} CXX)

set(ANDROID_OPENCV_COMPONENTS "opencv_java" CACHE STRING "")

message(STATUS "ANDROID_ABI=${ANDROID_ABI}")

find_package(OpenCV REQUIRED COMPONENTS ${ANDROID_OPENCV_COMPONENTS})

file(GLOB srcs *.cpp *.c)
file(GLOB hdrs *.hpp *.h)

include_directories("${CMAKE_CURRENT_LIST_DIR}")

add_library(${target} SHARED ${srcs} ${hdrs})

find_library(log-lib log) # Useful for logging

target_link_libraries(${target} ${ANDROID_OPENCV_COMPONENTS} ${log-lib})
