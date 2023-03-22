#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  char buf[8192];
  va_list ap;
  va_start(ap, fmt);
  vsprintf(buf, fmt, ap);
  va_end(ap);
  for (int i = 0; buf[i]; ++ i) {
    putch(buf[i]);
  }
  return 1;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  for (; *fmt; ++ fmt) {
    switch (*fmt) {
    case '%':
      ++ fmt;
      int width = 0;
      char blank = ' ';
      if (*fmt == '0') {
        blank = '0';
        ++ fmt;
      }
      while ('0' <= *fmt && *fmt <= '9') {
        width = width * 10 + *fmt - '0';
        ++ fmt;
      }
      switch (*fmt) {
      case 's':
        char *s = va_arg(ap, char *);
        int len = strlen(s);
        for (int i = 0; i < width - len; ++ i) {
          *out ++ = blank;
        }
        while (*s) {
          *out ++ = *s ++;
        }
        break;

      case 'c':
        for (int i = 0; i < width - 1; ++ i) {
          *out ++ = blank;
        }
        *out ++ = va_arg(ap, int);
        break;

      case 'd':
        int d = va_arg(ap, int);
        char tmp[50];
        int ptr = 0;
        if (d < 0) {
          tmp[ptr ++] = '-';
          d = -d;
        }
        else if (d == 0) {
          tmp[ptr ++] = '0';
        }
        while (d) {
          tmp[ptr ++] = d % 10 + '0';
          d /= 10;
        }
        for (int i = 0; i < width - ptr; ++ i) {
          *out ++ = blank;
        }
        while (ptr) {
          *out ++ = tmp[-- ptr];
        }
        break;
      }
    case '\\':
      ++ fmt;
      switch (*fmt)
      {
      case 'n':
        *out ++ = '\n';
        break;
      }
    
    default:
      *out ++ = *fmt;
      break;
    }
  }
  *out = '\0';
  return 1;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  vsprintf(out, fmt, ap);
  va_end(ap);
  return 1;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
