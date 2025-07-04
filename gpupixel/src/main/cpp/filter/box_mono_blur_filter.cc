/*
 * GPUPixel
 *

 */

#include "gpupixel/filter/box_mono_blur_filter.h"
#include <cmath>
#include "core/gpupixel_context.h"
#include "utils/util.h"
namespace gpupixel {

BoxMonoBlurFilter::BoxMonoBlurFilter(Type type)
    : GaussianBlurMonoFilter(type) {}

BoxMonoBlurFilter::~BoxMonoBlurFilter() {}

std::shared_ptr<BoxMonoBlurFilter> BoxMonoBlurFilter::Create(Type type,
                                                             int radius,
                                                             float sigma) {
  auto ret = std::shared_ptr<BoxMonoBlurFilter>(new BoxMonoBlurFilter(type));
  gpupixel::GPUPixelContext::GetInstance()->SyncRunWithContext([&] {
    if (ret && !ret->Init(radius, sigma)) {
      ret.reset();
    }
  });
  return ret;
}

bool BoxMonoBlurFilter::Init(int radius, float sigma) {
  if (Filter::InitWithShaderString(
          GenerateOptimizedVertexShaderString(radius, sigma),
          GenerateOptimizedFragmentShaderString(radius, sigma))) {
    return true;
  }
  return false;
}

void BoxMonoBlurFilter::SetRadius(int radius) {
  float newBlurRadius = std::round(std::round(radius / 2.0) *
                                   2.0);  // For now, only do even radii

  if (newBlurRadius != radius_) {
    radius_ = newBlurRadius;

    if (filter_program_) {
      delete filter_program_;
      filter_program_ = 0;
    }
    InitWithShaderString(GenerateOptimizedVertexShaderString(radius_, 0.0),
                         GenerateOptimizedFragmentShaderString(radius_, 0.0));
  }
}

std::string BoxMonoBlurFilter::GenerateOptimizedVertexShaderString(
    int radius,
    float sigma) {
  if (radius < 1) {
    return kDefaultVertexShader;
  }
  // From these weights we calculate the offsets to read interpolated values
  // from
  int numberOfOptimizedOffsets = std::min(radius / 2 + (radius % 2), 7);

  std::string shaderStr =
      // Header
      Util::StringFormat(
          "\
     attribute vec4 position;\n\
     attribute vec4 inputTextureCoordinate;\n\
     \n\
     uniform float texelWidthOffset;\n\
     uniform float texelHeightOffset;\n\
     \n\
     varying vec2 blurCoordinates[%lu];\n\
     \n\
     void main()\n\
     {\n\
     gl_Position = position;\n\
     \n\
     vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n",
          (unsigned long)(1 + (numberOfOptimizedOffsets * 2)));

  // Inner offset loop
  shaderStr += "blurCoordinates[0] = inputTextureCoordinate.xy;\n";
  for (uint32_t currentOptimizedOffset = 0;
       currentOptimizedOffset < numberOfOptimizedOffsets;
       currentOptimizedOffset++) {
    float optimizedOffset = (float)(currentOptimizedOffset * 2) + 1.5;

    shaderStr += Util::StringFormat(
        "\
         blurCoordinates[%lu] = inputTextureCoordinate.xy + singleStepOffset * %f;\n\
         blurCoordinates[%lu] = inputTextureCoordinate.xy - singleStepOffset * %f;\n",
        (unsigned long)((currentOptimizedOffset * 2) + 1), optimizedOffset,
        (unsigned long)((currentOptimizedOffset * 2) + 2), optimizedOffset);
  }

  // Footer
  shaderStr += "}\n";

  return shaderStr;
}

std::string BoxMonoBlurFilter::GenerateOptimizedFragmentShaderString(
    int radius,
    float sigma) {
  if (radius < 1) {
    return kDefaultFragmentShader;
  }

  uint32_t numberOfOptimizedOffsets = std::min(radius / 2 + (radius % 2), 7);
  uint32_t trueNumberOfOptimizedOffsets = radius / 2 + (radius % 2);

  // Header
#if defined(GPUPIXEL_GLES_SHADER)
  std::string shaderStr =
      // Header
      Util::StringFormat(
          "\
     uniform sampler2D inputImageTexture;\n\
     uniform highp float texelWidthOffset;\n\
     uniform highp float texelHeightOffset;\n\
     \n\
     varying highp vec2 blurCoordinates[%lu];\n\
     \n\
     void main()\n\
     {\n\
     lowp vec4 sum = vec4(0.0);\n",
          (unsigned long)(1 + (numberOfOptimizedOffsets * 2)));
#else
  std::string shaderStr =
      // Header
      Util::StringFormat(
          "\
     uniform sampler2D inputImageTexture;\n\
     uniform float texelWidthOffset;\n\
     uniform float texelHeightOffset;\n\
     \n\
     varying vec2 blurCoordinates[%lu];\n\
     \n\
     void main()\n\
     {\n\
     vec4 sum = vec4(0.0);\n",
          1 + (numberOfOptimizedOffsets * 2));
#endif

  float boxWeight = 1.0 / (float)((radius * 2) + 1);

  // Inner texture loop
  shaderStr += Util::StringFormat(
      "sum += texture2D(inputImageTexture, blurCoordinates[0]) * %f;\n",
      boxWeight);

  for (uint32_t currentBlurCoordinateIndex = 0;
       currentBlurCoordinateIndex < numberOfOptimizedOffsets;
       currentBlurCoordinateIndex++) {
    shaderStr += Util::StringFormat(
        "sum += texture2D(inputImageTexture, blurCoordinates[%lu]) * %f;\n",
        (unsigned long)((currentBlurCoordinateIndex * 2) + 1), boxWeight * 2.0);
    shaderStr += Util::StringFormat(
        "sum += texture2D(inputImageTexture, blurCoordinates[%lu]) * %f;\n",
        (unsigned long)((currentBlurCoordinateIndex * 2) + 2), boxWeight * 2.0);
  }

  // If the number of required samples exceeds the amount we can pass in via
  // varyings, we have to do dependent texture reads in the fragment shader
  if (trueNumberOfOptimizedOffsets > numberOfOptimizedOffsets) {
#if defined(GPUPIXEL_GLES_SHADER)
    shaderStr += Util::StringFormat(
        "highp vec2 singleStepOffset = vec2(texelWidthOffset, "
        "texelHeightOffset);\n");
#else
    shaderStr += Util::StringFormat(
        "vec2 singleStepOffset = vec2(texelWidthOffset, "
        "texelHeightOffset);\n");
#endif

    for (uint32_t currentOverlowTextureRead = numberOfOptimizedOffsets;
         currentOverlowTextureRead < trueNumberOfOptimizedOffsets;
         currentOverlowTextureRead++) {
      float optimizedOffset = (float)(currentOverlowTextureRead * 2) + 1.5;

      shaderStr += Util::StringFormat(
          "sum += texture2D(inputImageTexture, blurCoordinates[0] + "
          "singleStepOffset * %f) * %f;\n",
          optimizedOffset, boxWeight * 2.0);
      shaderStr += Util::StringFormat(
          "sum += texture2D(inputImageTexture, blurCoordinates[0] - "
          "singleStepOffset * %f) * %f;\n",
          optimizedOffset, boxWeight * 2.0);
    }
  }

  // Footer
  shaderStr +=
      "\
     gl_FragColor = sum;\n\
     }\n";

  return shaderStr;
}

}  // namespace gpupixel
