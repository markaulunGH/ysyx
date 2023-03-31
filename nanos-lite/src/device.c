#include <common.h>

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
# define MULTIPROGRAM_YIELD() yield()
#else
# define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) \
  [AM_KEY_##key] = #key,

static const char *keyname[256] __attribute__((used)) = {
  [AM_KEY_NONE] = "NONE",
  AM_KEYS(NAME)
};

size_t serial_write(const void *buf, size_t offset, size_t len) {
  for (int i = 0; i < len; ++ i) {
    putch(((char *)buf)[i]);
  }
  return len;
}

size_t events_read(void *buf, size_t offset, size_t len) {
  bool has_kbd = io_read(AM_INPUT_CONFIG).present;
  if (has_kbd) {
    AM_INPUT_KEYBRD_T ev = io_read(AM_INPUT_KEYBRD);
    if (ev.keycode != AM_KEY_NONE) {
      if (ev.keydown) {
        sprintf(buf, "kd %s", keyname[ev.keycode]);
      } else {
        sprintf(buf, "ku %s", keyname[ev.keycode]);
      }
      return strlen(buf);
    }
  }
  return 0;
}

size_t dispinfo_read(void *buf, size_t offset, size_t len) {
  AM_GPU_CONFIG_T info = io_read(AM_GPU_CONFIG);
  snprintf(buf, len, "WIDTH:%d\nHEIGHT:%d\n", info.width, info.height);
  return len;
}

size_t fb_write(const void *buf, size_t offset, size_t len) {
  AM_GPU_CONFIG_T info = io_read(AM_GPU_CONFIG);
  int x = (offset / 4) % info.width;
  int y = (offset / 4) / info.width;
  int w = len / 4;
  int h = 1;
  io_write(AM_GPU_FBDRAW, x, y, (void*) buf, w, h, true);
  return len;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
