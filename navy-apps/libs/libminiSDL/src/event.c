#include <NDL.h>
#include <SDL.h>
#include <string.h>

#define keyname(k) #k,

static const char *keyname[] = {
  "NONE",
  _KEYS(keyname)
};

int SDL_PushEvent(SDL_Event *ev) {
  return 0;
}

int SDL_PollEvent(SDL_Event *ev) {
  char buf[128];
  if (NDL_PollEvent(buf, sizeof(buf))) {
    if (strncmp(buf, "kd", 2) == 0) {
      ev->type = SDL_KEYDOWN;
    } else if (strncmp(buf, "ku", 2) == 0) {
      ev->type = SDL_KEYUP;
    }
    for (int i = 0; ; ++ i) {
      if (strcmp(buf + 3, keyname[i]) == 0) {
        ev->key.keysym.sym = i;
        return 1;
      }
    }
  }
  return 0;
}

int SDL_WaitEvent(SDL_Event *event) {
  while (SDL_PollEvent(event) == 0);
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  return NULL;
}
