#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include <common.h>

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

extern void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(uint64_t n);
extern void (*ref_difftest_raise_intr)(word_t NO);

void init_difftest(char *ref_so_file, int img_size);
void difftest_step(vaddr_t pc, vaddr_t next_pc);
void difftest_skip_ref();

#endif
