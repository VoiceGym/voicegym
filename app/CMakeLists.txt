# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set (CMAKE_CXX_STANDARD 11)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(CPP_FOLDER src/main/cpp/)
set(NATIVE_LIB_SRC  ${CPP_FOLDER}/native-lib.cpp)
add_library(native-lib SHARED ${NATIVE_LIB_SRC})

# External Libraries
add_library( fftw3 STATIC IMPORTED )
set_property(TARGET fftw3 PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../fftw3/lib/${ANDROID_ABI}/libfftw3.a)

find_library(log-lib log)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../fftw3/include/)
link_directories(${CMAKE_CURRENT_SOURCE_DIR}/../fftw3/lib/${ANDROID_ABI}/)

target_link_libraries(native-lib ${log-lib})
target_link_libraries(native-lib fftw3)