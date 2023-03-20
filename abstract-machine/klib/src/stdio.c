#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  // char buf[1024];
  // va_list arg;
  // va_start(arg, fmt);
  // sprintf(buf, fmt, arg);
  // va_end(arg);
  // // for (int i = 0; buf[i]; ++ i) {
  // //   putch(buf[i]);
  // // }


  va_list arg;

  // for (int i = 0; fmt[i]; ++ i)
  //   putch(fmt[i]);

  va_start (arg, fmt);
  for (; *fmt; ++ fmt) {
    switch (*fmt) {
    case '%':
      ++ fmt;
      switch (*fmt)
      {
      case 's':
        char *s = va_arg(arg, char *);
        while (*s) {
          putch(*s ++);
        }
        break;

      case 'c':
        putch(va_arg(arg, int));
        break;

      case 'd':
        int d = va_arg(arg, int);
        char tmp[20];
        int ptr = 0;
        while (d) {
          tmp[ptr ++] = d % 10 + '0';
          d /= 10;
        }
        while (ptr) {
          putch(tmp[-- ptr]);
        }
        break;
      }
    case '\\':
      ++ fmt;
      switch (*fmt)
      {
      case 'n':
        putch('\n');
        break;
      }
    
    default:
      putch(*fmt);
      break;
    }
  }
  va_end(arg);

  return 1;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int sprintf(char *out, const char *fmt, ...) {
  va_list arg;

  // for (int i = 0; fmt[i]; ++ i)
  //   putch(fmt[i]);

  va_start (arg, fmt);
  for (; *fmt; ++ fmt) {
    switch (*fmt) {
    case '%':
      ++ fmt;
      switch (*fmt)
      {
      case 's':
        char *s = va_arg(arg, char *);
        while (*s) {
          putch(*s);
          *out ++ = *s ++;
          
        }
        break;

      case 'c':
        char ch = va_arg(arg, int);
        putch(ch);
        *out ++ = ch;
        // *out ++ = va_arg(arg, int);
        break;

      case 'd':
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
  va_end(arg);
  *out = '\0';

  return 1;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
