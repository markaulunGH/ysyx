#include <common.h>
#include "syscall.h"
void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;

  switch (a[0]) {
    case EVENT_YIELD:
      Log("yield");
      break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
