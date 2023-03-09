#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int sprintf(char *out, const char *fmt, ...) {
  va_list arg;

  va_start (arg, fmt);
  for (; *fmt; ++ fmt) {
    if (*fmt == '%') {
      ++ fmt;
      if (*fmt == 'd') {
        int d = va_arg(arg, int);
        char tmp[20];
        int ptr = 0;
        while (d) {
          tmp[ptr ++] = d % 10 + '0';
          d /= 10;
        }
        while (ptr) {
          *out ++ = tmp[-- ptr];
        }
      }
      else if (*fmt == 's') {
        char *s = va_arg(arg, char *);
        while (*s) {
          *out ++ = *s ++;
        }
      }
    }
    else {
      *out ++ = *fmt;
    }
  }
  va_end(arg);

  return 1;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
