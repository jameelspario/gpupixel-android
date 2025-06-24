/*
 * GPUPixel
 *

 */

#pragma once

#include "gpupixel/filter/filter.h"
#include "gpupixel/gpupixel_define.h"

namespace gpupixel {
class GPUPIXEL_API ContrastFilter : public Filter {
 public:
  static std::shared_ptr<ContrastFilter> Create();
  bool Init();
  virtual bool DoRender(bool updateSinks = true) override;

  void setContrast(float contrast);

 protected:
  ContrastFilter() {};

  float contrast_factor_;
};

}  // namespace gpupixel
