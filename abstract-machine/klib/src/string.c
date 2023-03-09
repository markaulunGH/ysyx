#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  int i = 0;
  while (*s ++) {
    ++ i;
  }
  return i - 1;
}

char *strcpy(char *dst, const char *src) {
  char *tmp = dst;
  while (*src) {
    *dst ++ = *src ++;
  }
  *dst = '\0';
  return tmp;
}

char *strncpy(char *dst, const char *src, size_t n) {
  char *tmp = dst;
  while (*src && n -- > 0) {
    *dst ++ = *src ++;
  }
  while (n -- > 0) {
    *dst ++ = '\0';
  }
  return tmp;
}

char *strcat(char *dst, const char *src) {
  char *tmp = dst;
  while (*dst) {
    ++ dst;
  }
  while (*src) {
    *dst ++ = *src ++;
  }
  *dst = '\0';
  return tmp;
}

int strcmp(const char *s1, const char *s2) {
  while (*s1 && *s2) {
    if (*s1 != *s2) {
      return *s1 - *s2;
    }
    ++ s1;
    ++ s2;
  }
  return *s1 - *s2;
}

int strncmp(const char *s1, const char *s2, size_t n) {
  while (*s1 && *s2 && n -- > 0) {
    if (*s1 != *s2) {
      return *s1 - *s2;
    }
    ++ s1;
    ++ s2;
  }
  return 0;
}

void *memset(void *s, int c, size_t n) {
  uint8_t *_s = (uint8_t *) s;
  for (; n != 0; -- n) {
    *_s ++ = c;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  uint8_t *_dst = (uint8_t *) dst, *_src = (uint8_t *) src;
  for (; n != 0; -- n) {
    *_dst ++ = *_src ++;
  }
  return dst;
}

void *memcpy(void *out, const void *in, size_t n) {
  uint8_t *_out = (uint8_t *) out, *_in = (uint8_t *) in;
  for (; n != 0; -- n) {
    *_out ++ = *_in ++;
  }
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
  uint8_t *_s1 = (uint8_t *) s1, *_s2 = (uint8_t *) s2;
  while (*_s1 && *_s2 && n -- > 0) {
    if (*_s1 != *_s2) {
      return *_s1 - *_s2;
    }
    ++ _s1;
    ++ _s2;
  }
  return 0;
}

#endif
