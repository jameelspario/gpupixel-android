/*
 * GPUPixel
 *
 * Created by PixPark on 2021/6/24.
 * Copyright © 2021 PixPark. All rights reserved.
 */

#include "gpupixel/filter/weak_pixel_inclusion_filter.h"
#include "core/gpupixel_context.h"
namespace gpupixel {

#if defined(GPUPIXEL_GLES_SHADER)
const std::string kWeakPixelInclusionFragmentShaderString = R"(
    precision mediump float; uniform sampler2D inputImageTexture;

    varying vec2 textureCoordinate;
    varying vec2 vLeftTexCoord;
    varying vec2 vRightTexCoord;

    varying vec2 vTopTexCoord;
    varying vec2 vTopLeftTexCoord;
    varying vec2 vTopRightTexCoord;

    varying vec2 vBottomTexCoord;
    varying vec2 vBottomLeftTexCoord;
    varying vec2 vBottomRightTexCoord;

    void main() {
      float bottomLeftIntensity =
          texture2D(inputImageTexture, vBottomLeftTexCoord).r;
      float topRightIntensity =
          texture2D(inputImageTexture, vTopRightTexCoord).r;
      float topLeftIntensity = texture2D(inputImageTexture, vTopLeftTexCoord).r;
      float bottomRightIntensity =
          texture2D(inputImageTexture, vBottomRightTexCoord).r;
      float leftIntensity = texture2D(inputImageTexture, vLeftTexCoord).r;
      float rightIntensity = texture2D(inputImageTexture, vRightTexCoord).r;
      float bottomIntensity = texture2D(inputImageTexture, vBottomTexCoord).r;
      float topIntensity = texture2D(inputImageTexture, vTopTexCoord).r;
      float centerIntensity = texture2D(inputImageTexture, textureCoordinate).r;

      float pixelIntensitySum =
          bottomLeftIntensity + topRightIntensity + topLeftIntensity +
          bottomRightIntensity + leftIntensity + rightIntensity +
          bottomIntensity + topIntensity + centerIntensity;
      float sumTest = step(1.5, pixelIntensitySum);
      float pixelTest = step(0.01, centerIntensity);

      gl_FragColor = vec4(vec3(sumTest * pixelTest), 1.0);
    })";
#elif defined(GPUPIXEL_GL_SHADER)
const std::string kWeakPixelInclusionFragmentShaderString = R"(
    uniform sampler2D inputImageTexture;

    varying vec2 textureCoordinate;
    varying vec2 vLeftTexCoord;
    varying vec2 vRightTexCoord;

    varying vec2 vTopTexCoord;
    varying vec2 vTopLeftTexCoord;
    varying vec2 vTopRightTexCoord;

    varying vec2 vBottomTexCoord;
    varying vec2 vBottomLeftTexCoord;
    varying vec2 vBottomRightTexCoord;

    void main() {
      float bottomLeftIntensity =
          texture2D(inputImageTexture, vBottomLeftTexCoord).r;
      float topRightIntensity =
          texture2D(inputImageTexture, vTopRightTexCoord).r;
      float topLeftIntensity = texture2D(inputImageTexture, vTopLeftTexCoord).r;
      float bottomRightIntensity =
          texture2D(inputImageTexture, vBottomRightTexCoord).r;
      float leftIntensity = texture2D(inputImageTexture, vLeftTexCoord).r;
      float rightIntensity = texture2D(inputImageTexture, vRightTexCoord).r;
      float bottomIntensity = texture2D(inputImageTexture, vBottomTexCoord).r;
      float topIntensity = texture2D(inputImageTexture, vTopTexCoord).r;
      float centerIntensity = texture2D(inputImageTexture, textureCoordinate).r;

      float pixelIntensitySum =
          bottomLeftIntensity + topRightIntensity + topLeftIntensity +
          bottomRightIntensity + leftIntensity + rightIntensity +
          bottomIntensity + topIntensity + centerIntensity;
      float sumTest = step(1.5, pixelIntensitySum);
      float pixelTest = step(0.01, centerIntensity);

      gl_FragColor = vec4(vec3(sumTest * pixelTest), 1.0);
    })";
#endif

std::shared_ptr<WeakPixelInclusionFilter> WeakPixelInclusionFilter::Create() {
  auto ret =
      std::shared_ptr<WeakPixelInclusionFilter>(new WeakPixelInclusionFilter());
  gpupixel::GPUPixelContext::GetInstance()->SyncRunWithContext([&] {
    if (ret && !ret->Init()) {
      ret.reset();
    }
  });
  return ret;
}

bool WeakPixelInclusionFilter::Init() {
  if (InitWithFragmentShaderString(kWeakPixelInclusionFragmentShaderString)) {
    return true;
  }
  return false;
}

}  // namespace gpupixel
