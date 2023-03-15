#ifndef __PADDR_H__
#define __PADDR_H__

#include <common.h>

#define MEM_SIZE 0x8000000
#define MEM_BASE 0x80000000

uint64_t paddr_read(uint64_t addr, int len);
void paddr_write(uint64_t addr, int len, uint64_t data);

#endif
