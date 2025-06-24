/*
 * GPUPixel
 *

 */

#pragma once

#include "gpupixel/filter/face_makeup_filter.h"

namespace gpupixel {
class GPUPIXEL_API BlusherFilter : public FaceMakeupFilter {
 public:
  static std::shared_ptr<BlusherFilter> Create();
  bool Init() override;

 private:
  BlusherFilter();
};

}  // namespace gpupixel
