#include <common.h>
#include <fs.h>
#include <sys/time.h>
#include <proc.h>
#include "syscall.h"

extern void naive_uload(PCB *pcb, const char *filename);

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
      naive_uload(NULL, "/bin/menu");
      break;
    case SYS_open:
      c->GPRx = fs_open((void *)a[1], a[2], a[3]);
      break;
    case SYS_write:
      c->GPRx = fs_write(a[1], (void *)a[2], a[3]);
      break;
    case SYS_brk:
      c->GPRx = 0;
      break;
    case SYS_read:
      c->GPRx = fs_read(a[1], (void *)a[2], a[3]);
      break;
    case SYS_close:
      c->GPRx = fs_close(a[1]);
      break;
    case SYS_lseek:
      c->GPRx = fs_lseek(a[1], a[2], a[3]);
      break;
    case SYS_gettimeofday:
      uint64_t t = io_read(AM_TIMER_UPTIME).us;
      struct timeval *tv = (struct timeval *)a[1];
      tv->tv_sec = t / 1000000;
      tv->tv_usec = t % 1000000;
      c->GPRx = 1;
      break;
    case SYS_execve:
      char *fname = (char *)a[1];
      naive_uload(NULL, fname);
      break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
}
