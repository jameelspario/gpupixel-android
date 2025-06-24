/*
 * GPUPixel
 *

 */

#pragma once
#include "gpupixel/filter/filter.h"
#include "gpupixel/gpupixel_define.h"

namespace gpupixel {
class GPUPIXEL_API ExposureFilter : public Filter {
 public:
  static std::shared_ptr<ExposureFilter> Create();
  bool Init();
  virtual bool DoRender(bool updateSinks = true) override;

  void SetExposure(float exposure);

 protected:
  ExposureFilter() {};

  float exposure_factor_;
};

}  // namespace gpupixel
