#ifndef __NPC_H__
#define __NPC_H__

#include <common.h>
#include <config.h>

struct CPU_state
{
    word_t gpr[32];
    vaddr_t pc;
};

extern CPU_state cpu;

struct DecodeInfo
{
    union
    {
        uint32_t val;
    } inst;
};

struct Decode
{
    vaddr_t pc;
    vaddr_t dnpc; // dynamic next pc
    DecodeInfo npc;
#ifdef CONFIG_ITRACE
    char logbuf[128];
#endif
};

void reg_display();
word_t reg_str2val(const char *name, bool *success);

void init_ftrace(const char *elf_file);
void cpu_exec(uint64_t n);
void update_regs();
int is_exit_status_bad();
void init_vga();

#endif
