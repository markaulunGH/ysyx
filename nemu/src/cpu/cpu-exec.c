/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <locale.h>

#include <elf.h>
#include <stddef.h>

/* The assembly code of instructions executed is only output to the screen
 * when the number of instructions executed is less than this value.
 * This is useful when you use the `si' command.
 * You can modify this value as you want.
 */
#define MAX_INST_TO_PRINT 10

CPU_state cpu = {};
uint64_t g_nr_guest_inst = 0;
static uint64_t g_timer = 0; // unit: us
static bool g_print_step = false;

void device_update();

#define IRING_BUF_SIZE 30
char iringbuf[IRING_BUF_SIZE][128];

FILE *elf_fp;
Elf64_Off symoff, stroff;
uint64_t symsize, strsize;
int stack_depth;

void init_ftrace(const char *elf_file) {
  elf_fp = fopen(elf_file, "r");

  Elf64_Ehdr ehdr;
  assert(fread(&ehdr, sizeof(ehdr), 1, elf_fp));
  fseek(elf_fp, ehdr.e_shoff, SEEK_SET);

  Elf64_Shdr shdr;
  for (int i = 0; i < ehdr.e_shnum; ++ i) {
    assert(fread(&shdr, sizeof(shdr), 1, elf_fp));
    if ((shdr.sh_type == SHT_SYMTAB)) {
      symoff = shdr.sh_offset;
      symsize = shdr.sh_size;
    }
    else if (shdr.sh_type == SHT_STRTAB && (shdr.sh_flags & SHF_ALLOC)) {
      stroff = shdr.sh_offset;
      strsize = shdr.sh_size;
    }
  }
}

static void trace_and_difftest(Decode *_this, vaddr_t dnpc) {
#ifdef CONFIG_ITRACE_COND
  if (ITRACE_COND) {
    strcpy(iringbuf[g_nr_guest_inst % IRING_BUF_SIZE], _this->logbuf);
    if (strncmp("jal", _this->logbuf + 32, 3) == 0) {
      for (int i = 0; i < stack_depth; ++ i) {
        log_write(" ");
      }
      log_write("call [");
      if (*(_this->logbuf + 35) == 'r') {
        bool success = true;
        log_write("0x%lx]\n", isa_reg_str2val(_this->logbuf + 37, &success));
      }
      else {
        log_write("%s]\n", _this->logbuf + 36);
      }
      stack_depth += 2;
    }
    else if (strncmp("ret", _this->logbuf + 32, 3) == 0) {
      for (int i = 0; i < stack_depth; ++ i) {
        log_write(" ");
      }
      log_write("ret\n");
      stack_depth -= 2;
    }
  }
#endif
  if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(_this->logbuf)); }
  IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));

  if (scan_wp()) {
    nemu_state.state = NEMU_STOP;
  }
}

static void exec_once(Decode *s, vaddr_t pc) {
  s->pc = pc;
  s->snpc = pc;
  isa_exec_once(s);
  cpu.pc = s->dnpc;
#ifdef CONFIG_ITRACE
  char *p = s->logbuf;
  p += snprintf(p, sizeof(s->logbuf), FMT_WORD ":", s->pc);
  int ilen = s->snpc - s->pc;
  int i;
  uint8_t *inst = (uint8_t *)&s->isa.inst.val;
  for (i = ilen - 1; i >= 0; i --) {
    p += snprintf(p, 4, " %02x", inst[i]);
  }
  int ilen_max = MUXDEF(CONFIG_ISA_x86, 8, 4);
  int space_len = ilen_max - ilen;
  if (space_len < 0) space_len = 0;
  space_len = space_len * 3 + 1;
  memset(p, ' ', space_len);
  p += space_len;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  disassemble(p, s->logbuf + sizeof(s->logbuf) - p,
      MUXDEF(CONFIG_ISA_x86, s->snpc, s->pc), (uint8_t *)&s->isa.inst.val, ilen);
#endif
}

static void execute(uint64_t n) {
  Decode s;
  for (;n > 0; n --) {
    exec_once(&s, cpu.pc);
    g_nr_guest_inst ++;
    trace_and_difftest(&s, cpu.pc);
    if (nemu_state.state != NEMU_RUNNING) break;
    IFDEF(CONFIG_DEVICE, device_update());
  }
}

static void statistic() {
  IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
  Log("host time spent = " NUMBERIC_FMT " us", g_timer);
  Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
  if (g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
  else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}

void assert_fail_msg() {
  isa_reg_display();
  statistic();
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) {
  g_print_step = (n < MAX_INST_TO_PRINT);
  switch (nemu_state.state) {
    case NEMU_END: case NEMU_ABORT:
      printf("Program execution has ended. To restart the program, exit NEMU and run again.\n");
      return;
    default: nemu_state.state = NEMU_RUNNING;
  }

  uint64_t timer_start = get_time();

  execute(n);

  uint64_t timer_end = get_time();
  g_timer += timer_end - timer_start;

  switch (nemu_state.state) {
    case NEMU_RUNNING: nemu_state.state = NEMU_STOP; break;

    case NEMU_END: case NEMU_ABORT:
      Log("nemu: %s at pc = " FMT_WORD,
          (nemu_state.state == NEMU_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
           (nemu_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
            ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
          nemu_state.halt_pc);
#ifdef CONFIG_ITRACE_COND
  if (ITRACE_COND)
      for (int i = 0; i < IRING_BUF_SIZE; ++ i) {
        if (g_nr_guest_inst % IRING_BUF_SIZE == i) {
          log_write("--> ");
        }
        else {
          log_write("    ");
        }
        log_write("%s\n", iringbuf[i]);
      }
#endif
      // fall through
    case NEMU_QUIT: statistic();
  }
}
