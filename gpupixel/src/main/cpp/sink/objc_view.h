/*
 * GPUPixel
 *

 */

#include "gpupixel/gpupixel_define.h"
#if defined(GPUPIXEL_IOS)
#import <UIKit/UIKit.h>
#else
#import <AppKit/NSOpenGLView.h>
#endif

#include "gpupixel/sink/sink_render.h"

#if defined(GPUPIXEL_IOS)
GPUPIXEL_API
@interface ObjcView : UIView

@property(nonatomic) CAEAGLLayer* currentlayer;
@property(nonatomic) CGRect currentFrame;

#else
GPUPIXEL_API
@interface ObjcView : NSOpenGLView
#endif
@property(readwrite, nonatomic) gpupixel::SinkRender::FillMode fillMode;
@property(readonly, nonatomic) CGSize sizeInPixels;

// Implementation of methods directly instead of through protocol
- (void)DoRender;
- (void)SetInputFramebuffer:
            (std::shared_ptr<gpupixel::GPUPixelFramebuffer>)framebuffer
               withRotation:(gpupixel::RotationMode)rotationMode
                    atIndex:(int)textureIndex;
- (BOOL)IsReady;
- (void)unPrepared;

@end
