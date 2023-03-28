#include <common.h>
#include "syscall.h"
void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;

  switch (a[0]) {
    case SYS_yield:
      yield();
      break;
    case SYS_exit:
      halt(a[1]);
      break;
    case SYS_write:
      int fd = a[1];
      void *buf = (void *)a[2];
      size_t count = a[3];
      if (fd == 1 || fd == 2) {
        for (int i = 0; i < count; ++ i) {
          putch(((char *)buf)[i]);
        }
      }
      c->GPRx = count;
      break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
