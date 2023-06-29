#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <sys/file.h>

static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0;

uint32_t NDL_GetTicks() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

int NDL_PollEvent(char *buf, int len) {
  if (evtdev < 0) {
    evtdev = open("/dev/events", 0);
  }
  return read(evtdev, buf, len) > 0;
}

int offset_x = 0, offset_y = 0;

void NDL_OpenCanvas(int *w, int *h) {
  int fd = open("/proc/dispinfo", 0);
  char buf[128];
  read(fd, buf, sizeof(buf));
  sscanf(buf, "WIDTH:%d\nHEIGHT:%d\n", &screen_w, &screen_h);
  if (*w == 0 && *h == 0) {
    *w = screen_w;
    *h = screen_h;
  } else {
    offset_x = (screen_w - *w) / 2;
    offset_y = (screen_h - *h) / 2;
  }
}

void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  if (fbdev < 0) {
    fbdev = open("/dev/fb", 0);
  }
  for (int i = 0; i < h; ++ i) {
    lseek(fbdev, ((offset_y + y + i) * screen_w + offset_x + x) * 4, SEEK_SET);
    write(fbdev, pixels + i * w, w * 4);
  }
}

void NDL_OpenAudio(int freq, int channels, int samples) {
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  return 0;
}

int NDL_QueryAudio() {
  return 0;
}

int NDL_Init(uint32_t flags) {
  if (getenv("NWM_APP")) {
    evtdev = 3;
  }
  return 0;
}

void NDL_Quit() {
}
