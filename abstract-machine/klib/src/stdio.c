#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  char buf[1024];
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
      switch (*fmt)
      {
      case 's':
        char *s = va_arg(ap, char *);
        while (*s) {
          putch(*s);
          *out ++ = *s ++;
          
        }
        break;

      case 'c':
        char ch = va_arg(ap, int);
        putch(ch);
        *out ++ = ch;
        // *out ++ = va_arg(arg, int);
        break;

      case 'd':
        int d = va_arg(ap, int);
        char tmp[20];
        int ptr = 0;
        while (d) {
          tmp[ptr ++] = d % 10 + '0';
          d /= 10;
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
