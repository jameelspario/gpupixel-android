
cmake_minimum_required(VERSION 3.22.1)

# Define project name and initialize project
set(gpupixel_libs_name "gpupixel")
project(${gpupixel_libs_name})

# Set C++ standard
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Define common source files
set(common_source_files
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_program.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_context.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_framebuffer.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_framebuffer_factory.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/source/source.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/source/source_raw_data.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/source/source_image.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/sink/sink_raw_data.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/sink/sink_render.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/sink/sink.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/utils/math_toolbox.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/utils/dispatch_queue.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/utils/util.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/contrast_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/glass_sphere_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/brightness_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/ios_blur_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/hsb_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/sobel_edge_detection_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/sphere_refraction_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/directional_sobel_edge_detection_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/blusher_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/box_high_pass_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/luminance_range_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/box_blur_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/sketch_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/directional_non_maximum_suppression_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/toon_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/pixellation_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/beauty_face_unit_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/single_component_gaussian_blur_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/non_maximum_suppression_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/canny_edge_detection_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/bilateral_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/color_matrix_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/exposure_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/rgb_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/face_makeup_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/hue_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/nearby_sampling3x3_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/posterize_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/color_invert_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/single_component_gaussian_blur_mono_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/gaussian_blur_mono_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/convolution3x3_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/weak_pixel_inclusion_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/halftone_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/saturation_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/emboss_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/grayscale_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/lipstick_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/box_mono_blur_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/box_difference_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/crosshatch_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/filter_group.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/gaussian_blur_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/beauty_face_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/face_reshape_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/white_balance_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/filter/smooth_toon_filter.cc)

# Add face detection source files based on options
option(GPUPIXEL_ENABLE_FACE_DETECTOR "Enable face detector" ON)
if(GPUPIXEL_ENABLE_FACE_DETECTOR)
    list(APPEND common_source_files
            ${CMAKE_CURRENT_SOURCE_DIR}/face_detector/face_detector.cc)
endif()

# Define JNI source files for Android
set(jni_source_files
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_gpupixel.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_filter.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_source_raw_data.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_source.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_helpers.h
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_helpers.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_source_image.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_face_detector.cc
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_sink_raw_data.cc)

# Combine source files for Android
set(lib_source_code_files ${common_source_files})
list(APPEND lib_source_code_files ${jni_source_files})

# Define header files
set(public_common_header_files
        ${PROJECT_SOURCE_DIR}/include/gpupixel/gpupixel.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/gpupixel_define.h)

# Add face detection header files based on options
if(GPUPIXEL_ENABLE_FACE_DETECTOR)
    set(public_face_detector_header_files
            ${PROJECT_SOURCE_DIR}/include/gpupixel/face_detector/face_detector.h)
else()
    set(public_face_detector_header_files "")
endif()

set(public_source_header_files
        ${PROJECT_SOURCE_DIR}/include/gpupixel/source/source.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/source/source_raw_data.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/source/source_image.h)

set(public_sink_header_files
        ${PROJECT_SOURCE_DIR}/include/gpupixel/sink/sink_raw_data.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/sink/sink.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/sink/sink_render.h)

set(public_utils_header_files
        ${PROJECT_SOURCE_DIR}/include/gpupixel/utils/math_toolbox.h)

set(public_filter_header_files
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/gaussian_blur_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/non_maximum_suppression_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/weak_pixel_inclusion_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/crosshatch_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/box_high_pass_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/rgb_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/white_balance_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/smooth_toon_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/brightness_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/single_component_gaussian_blur_mono_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/grayscale_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/toon_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/contrast_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/lipstick_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/directional_non_maximum_suppression_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/glass_sphere_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/sketch_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/convolution3x3_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/luminance_range_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/posterize_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/exposure_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/sphere_refraction_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/color_matrix_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/hue_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/blusher_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/bilateral_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/face_reshape_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/box_mono_blur_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/nearby_sampling3x3_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/gaussian_blur_mono_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/face_makeup_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/emboss_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/canny_edge_detection_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/box_difference_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/beauty_face_unit_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/hsb_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/directional_sobel_edge_detection_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/sobel_edge_detection_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/beauty_face_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/single_component_gaussian_blur_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/box_blur_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/saturation_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/color_invert_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/filter_group.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/ios_blur_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/halftone_filter.h
        ${PROJECT_SOURCE_DIR}/include/gpupixel/filter/pixellation_filter.h)

set(internal_core_header_files
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_program.h
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_framebuffer_factory.h
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_context.h
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_framebuffer.h
        ${CMAKE_CURRENT_SOURCE_DIR}/core/gpupixel_gl_include.h)

set(internal_utils_header_files
        ${CMAKE_CURRENT_SOURCE_DIR}/utils/dispatch_queue.h
        ${CMAKE_CURRENT_SOURCE_DIR}/utils/util.h)

set(internal_jni_header_files
        ${CMAKE_CURRENT_SOURCE_DIR}/jni_helpers.h)

set(lib_header_code_files
        ${public_common_header_files}
        ${public_face_detector_header_files}
        ${public_source_header_files}
        ${public_sink_header_files}
        ${public_filter_header_files}
        ${public_utils_header_files}
        ${internal_core_header_files}
        ${internal_utils_header_files}
        ${internal_jni_header_files})

# Define resource files
set(lib_image_resource_files
        ${CMAKE_CURRENT_SOURCE_DIR}/res/mouth.png
        ${CMAKE_CURRENT_SOURCE_DIR}/res/blusher.png
        ${CMAKE_CURRENT_SOURCE_DIR}/res/lookup_gray.png
        ${CMAKE_CURRENT_SOURCE_DIR}/res/lookup_light.png
        ${CMAKE_CURRENT_SOURCE_DIR}/res/lookup_custom.png
        ${CMAKE_CURRENT_SOURCE_DIR}/res/lookup_origin.png
        ${CMAKE_CURRENT_SOURCE_DIR}/res/lookup_skin.png)

set(lib_models_resource_files
        ${PROJECT_SOURCE_DIR}/third_party/mars-face-kit/models/face_align.mars_model
        ${PROJECT_SOURCE_DIR}/third_party/mars-face-kit/models/face_det.mars_model)

# Configure third-party libraries
# ---- Mars face library configuration ----
if(GPUPIXEL_ENABLE_FACE_DETECTOR)
    add_library(marsface SHARED IMPORTED GLOBAL)
    set(mars_shared_lib_path
            ${CMAKE_CURRENT_SOURCE_DIR}/third_party/mars-face-kit/libs/android/${ANDROID_ABI}/libmars-face-kit.so
    )
    set(mars_face_header ${CMAKE_CURRENT_SOURCE_DIR}/third_party/mars-face-kit/include)
    set_target_properties(
            marsface PROPERTIES
            IMPORTED_LOCATION ${mars_shared_lib_path}
            INTERFACE_INCLUDE_DIRECTORIES ${mars_face_header}
    )
    add_library(marsface::marsface ALIAS marsface)
endif()

# ---- libyuv configuration ----
add_subdirectory(third_party/libyuv EXCLUDE_FROM_ALL)
add_library(libyuv::yuv ALIAS yuv)
target_include_directories(yuv PUBLIC $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/third_party/libyuv/include>)
set_target_properties(yuv PROPERTIES POSITION_INDEPENDENT_CODE ON)

# ---- stb image library configuration ----
add_library(stb INTERFACE)
target_include_directories(stb INTERFACE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/third_party/stb/include>)
add_library(stb::stb ALIAS stb)

# ---- ghc_filesystem configuration ----
add_library(ghc_filesystem INTERFACE)
target_include_directories(ghc_filesystem INTERFACE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/third_party/ghc/include>)
add_library(ghc::filesystem ALIAS ghc_filesystem)

# Library configuration
if(GPUPIXEL_BUILD_SHARED_LIBS)
    add_library(
            ${gpupixel_libs_name} SHARED
            ${lib_source_code_files} ${lib_header_code_files}
            ${lib_image_resource_files} ${lib_models_resource_files})
else()
    add_library(
            ${gpupixel_libs_name} STATIC
            ${lib_source_code_files} ${lib_header_code_files}
            ${lib_image_resource_files} ${lib_models_resource_files})
endif()

add_library(gpupixel::gpupixel ALIAS ${gpupixel_libs_name})

set_target_properties(
        ${gpupixel_libs_name} PROPERTIES POSITION_INDEPENDENT_CODE ON
        FOLDER ${gpupixel_libs_name})

target_include_directories(
        ${gpupixel_libs_name} PUBLIC ${PROJECT_SOURCE_DIR}/include
        ${CMAKE_CURRENT_SOURCE_DIR})

# Android-specific configurations
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)
if(NOT CMAKE_BUILD_TYPE MATCHES Debug)
    target_compile_options(
            ${gpupixel_libs_name} PRIVATE -fvisibility=hidden
            -fvisibility-inlines-hidden)
endif()

if(GPUPIXEL_EXTERNAL_CODE)
    target_compile_definitions(${gpupixel_libs_name} PRIVATE GPUPIXEL_EXTERNAL_CODE)
endif()

# Android platform dependencies
target_link_libraries(
        ${gpupixel_libs_name}
        PRIVATE log
        android
        GLESv3
        EGL
        jnigraphics
        libyuv::yuv
        stb::stb
        ghc::filesystem)

if(GPUPIXEL_ENABLE_FACE_DETECTOR)
    target_link_libraries(${gpupixel_libs_name} PRIVATE marsface::marsface)
endif()

# Install resources for build output (optional)
if(GPUPIXEL_INSTALL)
    install(DIRECTORY ${PROJECT_SOURCE_DIR}/include/ DESTINATION include)
    install(
            TARGETS ${gpupixel_libs_name}
            ARCHIVE DESTINATION lib
            LIBRARY DESTINATION lib
            RUNTIME DESTINATION lib)
    install(
            FILES ${lib_image_resource_files}
            DESTINATION res
            COMPONENT resource)
    install(
            FILES ${lib_models_resource_files}
            DESTINATION models
            COMPONENT resource)
    foreach(image ${lib_image_resource_files})
        file(COPY ${image} DESTINATION ${BUILD_OUTPUT_DIR}/res)
    endforeach()
    foreach(model ${lib_models_resource_files})
        file(COPY ${model} DESTINATION ${BUILD_OUTPUT_DIR}/models)
    endforeach()
endif()