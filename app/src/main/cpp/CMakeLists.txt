cmake_minimum_required(VERSION 3.22.1)
project(fcatpdfium CXX)

# PDFIUM_DIR holds one unpacked bblanchon/pdfium-binaries archive per ABI, put there by the fetchPdfium*
# Gradle tasks. AGP packages the imported libpdfium.so into the APK next to our library.
add_library(pdfium SHARED IMPORTED)
set_target_properties(pdfium PROPERTIES IMPORTED_LOCATION ${PDFIUM_DIR}/${ANDROID_ABI}/lib/libpdfium.so)

add_library(fcatpdfium SHARED pdfium_jni.cpp)
target_include_directories(fcatpdfium PRIVATE ${PDFIUM_DIR}/${ANDROID_ABI}/include)
target_compile_features(fcatpdfium PRIVATE cxx_std_17)
target_compile_options(fcatpdfium PRIVATE -Wall -Wextra -Werror)
target_link_libraries(fcatpdfium PRIVATE pdfium jnigraphics)
