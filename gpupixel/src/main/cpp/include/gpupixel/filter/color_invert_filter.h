/*
 * GPUPixel
 *

 */

#pragma once

#include "gpupixel/filter/filter.h"
#include "gpupixel/gpupixel_define.h"

namespace gpupixel {
class GPUPIXEL_API ColorInvertFilter : public Filter {
 public:
  static std::shared_ptr<ColorInvertFilter> Create();
  bool Init();

  virtual bool DoRender(bool updateSinks = true) override;

 protected:
  ColorInvertFilter() {};
};

}  // namespace gpupixel
