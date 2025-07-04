/*
 * GPUPixel
 *

 */

#include "gpupixel/filter/single_component_gaussian_blur_mono_filter.h"
#include <cmath>
#include "core/gpupixel_context.h"
#include "utils/util.h"

namespace gpupixel {

SingleComponentGaussianBlurMonoFilter::SingleComponentGaussianBlurMonoFilter(
    Type type /* = HORIZONTAL*/)
    : GaussianBlurMonoFilter(type) {}

std::shared_ptr<SingleComponentGaussianBlurMonoFilter>
SingleComponentGaussianBlurMonoFilter::Create(Type type /* = HORIZONTAL*/,
                                              int radius /* = 4*/,
                                              float sigma /* = 2.0*/) {
  auto ret = std::shared_ptr<SingleComponentGaussianBlurMonoFilter>(
      new SingleComponentGaussianBlurMonoFilter(type));
  gpupixel::GPUPixelContext::GetInstance()->SyncRunWithContext([&] {
    if (ret && !ret->Init(radius, sigma)) {
      ret.reset();
    }
  });
  return ret;
}

std::string
SingleComponentGaussianBlurMonoFilter::GenerateOptimizedVertexShaderString(
    int radius,
    float sigma) {
  if (radius < 1 || sigma <= 0.0) {
    return kDefaultVertexShader;
  }

  // 1. generate the normal Gaussian weights for a given sigma
  float* standardGaussianWeights = new float[radius + 1];
  float sumOfWeights = 0.0;
  for (int i = 0; i < radius + 1; ++i) {
    standardGaussianWeights[i] = (1.0 / sqrt(2.0 * M_PI * pow(sigma, 2.0))) *
                                 exp(-pow(i, 2.0) / (2.0 * pow(sigma, 2.0)));
    if (i == 0) {
      sumOfWeights += standardGaussianWeights[i];
    } else {
      sumOfWeights += 2.0 * standardGaussianWeights[i];
    }
  }

  // 2. normalize these weights to prevent the clipping of the Gaussian curve at
  // the end of the discrete samples from reducing luminance
  for (int i = 0; i < radius + 1; ++i) {
    standardGaussianWeights[i] = standardGaussianWeights[i] / sumOfWeights;
  }

  // 3. From these weights we calculate the offsets to read interpolated values
  // from
  int numberOfOptimizedOffsets = fmin(radius / 2 + (radius % 2), 7);
  float* optimizedGaussianOffsets = new float[numberOfOptimizedOffsets];

  for (int i = 0; i < numberOfOptimizedOffsets; ++i) {
    float firstWeight = standardGaussianWeights[i * 2 + 1];
    float secondWeight = standardGaussianWeights[i * 2 + 2];

    float optimizedWeight = firstWeight + secondWeight;

    optimizedGaussianOffsets[i] =
        (firstWeight * (i * 2 + 1) + secondWeight * (i * 2 + 2)) /
        optimizedWeight;
  }
#if defined(GPUPIXEL_GLES_SHADER)
  std::string shaderStr = Util::StringFormat(
      "\
               attribute vec4 position;\n\
               attribute vec4 inputTextureCoordinate;\n\
               uniform float texelWidthOffset;\n\
               uniform float texelHeightOffset;\n\
               varying highp vec2 blurCoordinates[%d];\n\
               void main()\n\
               {\n\
               gl_Position = position;\n\
               vec2 texelSpacing = vec2(texelWidthOffset, texelHeightOffset);\n\
               ",
      numberOfOptimizedOffsets * 2 + 1);
#elif defined(GPUPIXEL_GL_SHADER)
  std::string shaderStr = Util::StringFormat(
      "\
               attribute vec4 position;\n\
               attribute vec4 inputTextureCoordinate;\n\
               uniform float texelWidthOffset;\n\
               uniform float texelHeightOffset;\n\
               varying vec2 blurCoordinates[%d];\n\
               void main()\n\
               {\n\
               gl_Position = position;\n\
               vec2 texelSpacing = vec2(texelWidthOffset, texelHeightOffset);\n\
               ",
      numberOfOptimizedOffsets * 2 + 1);
#endif

  shaderStr =
      shaderStr +
      Util::StringFormat("blurCoordinates[0] = inputTextureCoordinate.xy;\n");
  for (int i = 0; i < numberOfOptimizedOffsets; ++i) {
    shaderStr =
        shaderStr +
        Util::StringFormat(
            "blurCoordinates[%d] = inputTextureCoordinate.xy + texelSpacing * (%f);\n\
            blurCoordinates[%d] = inputTextureCoordinate.xy - texelSpacing * (%f);",
            i * 2 + 1, optimizedGaussianOffsets[i], i * 2 + 2,
            optimizedGaussianOffsets[i]);
  }

  shaderStr += "}\n";

  delete[] standardGaussianWeights;
  delete[] optimizedGaussianOffsets;

  return shaderStr;
}

std::string
SingleComponentGaussianBlurMonoFilter::GenerateOptimizedFragmentShaderString(
    int radius,
    float sigma) {
  if (radius < 1 || sigma <= 0.0) {
    return kDefaultFragmentShader;
  }

  // 1. generate the normal Gaussian weights for a given sigma
  float* standardGaussianWeights = new float[radius + 1];
  float sumOfWeights = 0.0;
  for (int i = 0; i < radius + 1; ++i) {
    standardGaussianWeights[i] = (1.0 / sqrt(2.0 * M_PI * pow(sigma, 2.0))) *
                                 exp(-pow(i, 2.0) / (2.0 * pow(sigma, 2.0)));
    if (i == 0) {
      sumOfWeights += standardGaussianWeights[i];
    } else {
      sumOfWeights += 2.0 * standardGaussianWeights[i];
    }
  }

  // 2. normalize these weights to prevent the clipping of the Gaussian curve at
  // the end of the discrete samples from reducing luminance
  for (int i = 0; i < radius + 1; ++i) {
    standardGaussianWeights[i] = standardGaussianWeights[i] / sumOfWeights;
  }

  // 3. From these weights we calculate the offsets to read interpolated values
  // from
  int trueNumberOfOptimizedOffsets = radius / 2 + (radius % 2);
  int numberOfOptimizedOffsets = fmin(trueNumberOfOptimizedOffsets, 7);

#if defined(GPUPIXEL_GLES_SHADER)
  std::string shaderStr = Util::StringFormat(
      "\
               uniform sampler2D inputImageTexture;\n\
               uniform highp float texelWidthOffset;\n\
               uniform highp float texelHeightOffset;\n\
               varying highp vec2 blurCoordinates[%d];\n\
               void main()\n\
               {\n\
               lowp float sum = 0.0;\n",
      numberOfOptimizedOffsets * 2 + 1);
#elif defined(GPUPIXEL_GL_SHADER)
  std::string shaderStr = Util::StringFormat(
      "\
               uniform sampler2D inputImageTexture;\n\
               uniform float texelWidthOffset;\n\
               uniform float texelHeightOffset;\n\
               varying vec2 blurCoordinates[%d];\n\
               void main()\n\
               {\n\
               float sum = 0.0;\n",
      numberOfOptimizedOffsets * 2 + 1);
#endif
  shaderStr += Util::StringFormat(
      "gl_FragColor += texture2D(inputImageTexture, "
      "blurCoordinates[0]) * %f;\n",
      standardGaussianWeights[0]);
  for (int i = 0; i < numberOfOptimizedOffsets; ++i) {
    float firstWeight = standardGaussianWeights[i * 2 + 1];
    float secondWeight = standardGaussianWeights[i * 2 + 2];
    float optimizedWeight = firstWeight + secondWeight;

    shaderStr += Util::StringFormat(
        "sum += texture2D(inputImageTexture, blurCoordinates[%d]).r * %f;\n",
        i * 2 + 1, optimizedWeight);
    shaderStr += Util::StringFormat(
        "sum += texture2D(inputImageTexture, blurCoordinates[%d]).r * %f;\n",
        i * 2 + 2, optimizedWeight);
  }

  // If the number of required samples exceeds the amount we can pass in via
  // varyings, we have to do dependent texture reads in the fragment shader
  if (trueNumberOfOptimizedOffsets > numberOfOptimizedOffsets) {
    shaderStr += Util::StringFormat(
        "highp vec2 texelSpacing = vec2(texelWidthOffset, "
        "texelHeightOffset);\n");

    for (int i = numberOfOptimizedOffsets; i < trueNumberOfOptimizedOffsets;
         i++) {
      float firstWeight = standardGaussianWeights[i * 2 + 1];
      float secondWeight = standardGaussianWeights[i * 2 + 2];

      float optimizedWeight = firstWeight + secondWeight;
      float optimizedOffset =
          (firstWeight * (i * 2 + 1) + secondWeight * (i * 2 + 2)) /
          optimizedWeight;

      shaderStr += Util::StringFormat(
          "sum += texture2D(inputImageTexture, "
          "blurCoordinates[0] + texelSpacing * %f).r * %f;\n",
          optimizedOffset, optimizedWeight);

      shaderStr += Util::StringFormat(
          "sum += texture2D(inputImageTexture, "
          "blurCoordinates[0] - texelSpacing * %f).r * %f;\n",
          optimizedOffset, optimizedWeight);
    }
  }

  shaderStr +=
      "gl_FragColor = vec4(sum, sum, sum, 1.0);\n\
    }";

  delete[] standardGaussianWeights;
  standardGaussianWeights = 0;
  return shaderStr;
}

}  // namespace gpupixel
