#ifndef __PADDR_H__
#define __PADDR_H__

#include <common.h>

#define MEM_SIZE 0x8000000
#define MEM_BASE 0x80000000

uint8_t *guest_to_host(paddr_t paddr);
paddr_t host_to_guest(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - MEM_BASE < MEM_SIZE;
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

void init_mem();
int load_image(char *img_file);

#endif
