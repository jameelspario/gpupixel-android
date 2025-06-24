/*
 * GPUPixel
 *

 */

#pragma once

#include "gpupixel/filter/filter.h"
#include "gpupixel/gpupixel_define.h"

namespace gpupixel {
class GPUPIXEL_API BrightnessFilter : public Filter {
 public:
  static std::shared_ptr<BrightnessFilter> Create(float brightness = 0.0);
  bool Init(float brightness);
  virtual bool DoRender(bool updateSinks = true) override;

  void setBrightness(float brightness);

 protected:
  BrightnessFilter() {};

  float brightness_factor_;
};

}  // namespace gpupixel
