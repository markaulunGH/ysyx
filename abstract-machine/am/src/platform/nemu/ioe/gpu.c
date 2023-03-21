#include <am.h>
#include <nemu.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)
#define SCREEN_W 800
#define SCREEN_H 600

void __am_gpu_init() {
  int i;
  int w = SCREEN_W;
  int h = SCREEN_H;
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  for (i = 0; i < w * h; i ++) {
    fb[i] = i;
  }
  outl(SYNC_ADDR, 1);
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = SCREEN_W, .height = SCREEN_H,
    .vmemsz = SCREEN_W * SCREEN_H * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  uint32_t *pixels = (uint32_t *) ctl->pixels;
  for (int i = 0; i < ctl->h; i ++) {
    for (int j = 0; j < ctl->w; j ++) {
      fb[(ctl->y + i) * SCREEN_W + ctl->x + j] = pixels[i * ctl->w + j];
    }
  }
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
