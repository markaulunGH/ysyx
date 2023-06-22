#include <cpu.h>
#include <locale.h>
#include <elf.h>
#include <stddef.h>
#include <config.h>
#include <sdb.h>
#include <sim.h>
#include <paddr.h>
#include <log.h>
#include <difftest.h>
#include <utils.h>
#include <device.h>

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

void reg_display()
{
    word_t gpr[] =
    {
        0,             top->io_rf_1,  top->io_rf_2,  top->io_rf_3,  top->io_rf_4,  top->io_rf_5,  top->io_rf_6,  top->io_rf_7,
        top->io_rf_8,  top->io_rf_9,  top->io_rf_10, top->io_rf_11, top->io_rf_12, top->io_rf_13, top->io_rf_14, top->io_rf_15,
        top->io_rf_16, top->io_rf_17, top->io_rf_18, top->io_rf_19, top->io_rf_20, top->io_rf_21, top->io_rf_22, top->io_rf_23,
        top->io_rf_24, top->io_rf_25, top->io_rf_26, top->io_rf_27, top->io_rf_28, top->io_rf_29, top->io_rf_30, top->io_rf_31
    };
    for (int i = 0; i < 32; ++ i)
    {
        printf("%-15s0x%-18lx%ld\n", regs[i], gpr[i], gpr[i]);
    }
}

word_t reg_str2val(const char *name, bool *success)
{
    word_t gpr[] =
    {
        0,             top->io_rf_1,  top->io_rf_2,  top->io_rf_3,  top->io_rf_4,  top->io_rf_5,  top->io_rf_6,  top->io_rf_7,
        top->io_rf_8,  top->io_rf_9,  top->io_rf_10, top->io_rf_11, top->io_rf_12, top->io_rf_13, top->io_rf_14, top->io_rf_15,
        top->io_rf_16, top->io_rf_17, top->io_rf_18, top->io_rf_19, top->io_rf_20, top->io_rf_21, top->io_rf_22, top->io_rf_23,
        top->io_rf_24, top->io_rf_25, top->io_rf_26, top->io_rf_27, top->io_rf_28, top->io_rf_29, top->io_rf_30, top->io_rf_31
    };
    for (int i = 0; i < 32; ++ i)
    {
        if (strcmp(name, regs[i]) == 0)
        {
            return gpr[i];
        }
    }
    *success = false;
    return 0;
}

void update_regs()
{
    word_t gpr[] =
    {
        0,             top->io_rf_1,  top->io_rf_2,  top->io_rf_3,  top->io_rf_4,  top->io_rf_5,  top->io_rf_6,  top->io_rf_7,
        top->io_rf_8,  top->io_rf_9,  top->io_rf_10, top->io_rf_11, top->io_rf_12, top->io_rf_13, top->io_rf_14, top->io_rf_15,
        top->io_rf_16, top->io_rf_17, top->io_rf_18, top->io_rf_19, top->io_rf_20, top->io_rf_21, top->io_rf_22, top->io_rf_23,
        top->io_rf_24, top->io_rf_25, top->io_rf_26, top->io_rf_27, top->io_rf_28, top->io_rf_29, top->io_rf_30, top->io_rf_31
    };
    for (int i = 0; i < 32; ++ i)
    {
        cpu.gpr[i] = gpr[i];
    }
    cpu.pc = top->io_pc;
}

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

#define IRING_BUF_SIZE 30
char iringbuf[IRING_BUF_SIZE][128];

Elf64_Shdr symshdr, strshdr;
Elf64_Sym *symtab;
char *strtab;
int stack_depth;

void init_ftrace(const char *elf_file)
{
    FILE *elf_fp = fopen(elf_file, "r");

    Elf64_Ehdr ehdr;
    assert(fread(&ehdr, sizeof(ehdr), 1, elf_fp));
    fseek(elf_fp, ehdr.e_shoff, SEEK_SET);

    Elf64_Shdr shdr;
    for (int i = 0; i < ehdr.e_shnum; ++i)
    {
        assert(fread(&shdr, ehdr.e_shentsize, 1, elf_fp));
        if ((shdr.sh_type == SHT_SYMTAB))
        {
            symshdr = shdr;
        }
        else if (shdr.sh_type == SHT_STRTAB && i != ehdr.e_shstrndx)
        {
            strshdr = shdr;
        }
    }

    fseek(elf_fp, symshdr.sh_offset, SEEK_SET);
    symtab = (Elf64_Sym*) malloc(symshdr.sh_size);
    assert(fread(symtab, symshdr.sh_entsize, symshdr.sh_size / symshdr.sh_entsize, elf_fp));
    fseek(elf_fp, strshdr.sh_offset, SEEK_SET);
    strtab = (char*) malloc(strshdr.sh_size);
    assert(fread(strtab, 1, strshdr.sh_size, elf_fp));

    fclose(elf_fp);
}

#define BITMASK(bits) ((1ull << (bits)) - 1)
#define BITS(x, hi, lo) (((x) >> (lo)) & BITMASK((hi) - (lo) + 1))
#define SEXT(x, len) ({ struct { int64_t n : len; } __x = { .n = x }; (uint64_t)__x.n; })

static void trace_and_difftest(Decode *_this, vaddr_t dnpc)
{
#ifdef CONFIG_ITRACE
#ifdef CONFIG_ITRACE_RING
    strcpy(iringbuf[g_nr_guest_inst % IRING_BUF_SIZE], _this->logbuf);
#else
    log_write("%s\n", _this->logbuf);
#endif
#endif
#ifdef CONFIG_FTRACE
    uint32_t inst = top->io_inst;
    int rd = BITS(inst, 11, 7);
    int rs1 = BITS(inst, 19, 15);
    bool jal = BITS(inst, 6, 0) == 0x6f, jalr = BITS(inst, 6, 0) == 0x67;
    if ((jal || jalr) && rd == 1)
    {
        for (int i = 0; i < stack_depth; ++ i)
        {
            log_write(" ");
        }
        uint64_t addr = _this->dnpc;
        int id = 0;
        for (; id < symshdr.sh_size / symshdr.sh_entsize; ++ id)
        {
            if (ELF32_ST_TYPE(symtab[id].st_info) == STT_FUNC && symtab[id].st_value <= addr && addr < symtab[id].st_value + symtab[id].st_size)
            {
                break;
            }
        }
        log_write("call [%s@%lx]\n", strtab + symtab[id].st_name, addr);
        stack_depth += 2;
    }
    else if (jalr && rs1 == 1)
    {
        stack_depth -= 2;
        for (int i = 0; i < stack_depth; ++ i)
        {
            log_write(" ");
        }
        log_write("ret\n");
    }
#endif
    if (g_print_step)
    {
#ifdef CONFIG_ITRACE
        puts(_this->logbuf);
#endif
    }
#ifdef CONFIG_DIFFTEST
    difftest_step(_this->pc, dnpc);
#endif

    if (scan_wp())
    {
        npc_state.state = NPC_STOP;
    }
}

#define SERIAL_PORT 0xa00003f8
#define RTC_ADDR    0xa0000048

#define likely(x) __builtin_expect(!!(x), 1)

static void exec_once(Decode *s)
{
    if (contextp->time() > 8750000000)
        wave_enable = true;
    s->pc = top->io_pc;
    s->npc.inst.val = top->io_inst;
    static uint64_t skip_pc = 0;
    do
    {
        top->eval();
        if (top->io_mm_ren)
        {
            if (in_pmem(top->io_mm_raddr))
            {
#ifdef CONFIG_MTRACE
                log_write("read  memory 0x%lx at 0x%lx\n", top->io_mm_raddr, top->io_mm_pc);
#endif
                top->io_mm_rdata = paddr_read(top->io_mm_raddr, 8);
            }
            else
            {
#ifdef CONFIG_DTRACE
                log_write("read  device 0x%lx at 0x%lx\n", top->io_mm_raddr, top->io_mm_pc);
#endif
                top->io_mm_rdata = mmio_read(top->io_mm_raddr, 8);
                skip_pc = top->io_mm_pc;
            }
            top->eval();
        }
        if (top->io_mm_wen)
        {
            if (likely(in_pmem(top->io_mm_waddr)))
            {
#ifdef CONFIG_MTRACE
                log_write("write memory 0x%lx at 0x%lx\n", top->io_mm_waddr, top->io_mm_pc);
#endif
                switch (top->io_mm_mask)
                {
                    case 0x1:  paddr_write(top->io_mm_waddr, 1, top->io_mm_wdata); break;
                    case 0x3:  paddr_write(top->io_mm_waddr, 2, top->io_mm_wdata); break;
                    case 0xf:  paddr_write(top->io_mm_waddr, 4, top->io_mm_wdata); break;
                    case 0xff: paddr_write(top->io_mm_waddr, 8, top->io_mm_wdata); break;
                }
            }
            else
            {
#ifdef CONFIG_DTRACE
                log_write("write device 0x%lx at 0x%lx\n", top->io_mm_waddr, top->io_mm_pc);
#endif
                switch (top->io_mm_mask)
                {
                    case 0x1:  mmio_write(top->io_mm_waddr, 1, top->io_mm_wdata); break;
                    case 0x3:  mmio_write(top->io_mm_waddr, 2, top->io_mm_wdata); break;
                    case 0xf:  mmio_write(top->io_mm_waddr, 4, top->io_mm_wdata); break;
                    case 0xff: mmio_write(top->io_mm_waddr, 8, top->io_mm_wdata); break;
                }
                skip_pc = top->io_mm_pc;
            }
        }
        cycle_end();
    } while (!top->io_inst_end || top->io_pc == 0x00000000);
    if (top->io_pc == skip_pc)
    {
        difftest_skip_ref();
        skip_pc = 0;
    }
    top->eval();
    update_regs();
    if (top->io_ebreak)
    {
        difftest_skip_ref();
        npc_state.state = NPC_END;
        npc_state.halt_pc = top->io_pc;
        npc_state.halt_ret= top->io_rf_10;
    }
    s->dnpc = top->io_pc;
    cpu.pc = s->dnpc;
    printf("pc = %lx\n", s->dnpc);
    printf("%d\n", contextp->time());
#ifdef CONFIG_ITRACE
    char *p = s->logbuf;
    p += snprintf(p, sizeof(s->logbuf), "0x%016lx:", s->pc);
    int ilen = 4;
    int i;
    uint8_t *inst = (uint8_t *)&s->npc.inst.val;
    for (i = ilen - 1; i >= 0; i--)
    {
        p += snprintf(p, 4, " %02x", inst[i]);
    }
    int ilen_max = 8;
    int space_len = ilen_max - ilen;
    if (space_len < 0)
        space_len = 0;
    space_len = space_len * 3 + 1;
    memset(p, ' ', space_len);
    p += space_len;

    void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
    disassemble(p, s->logbuf + sizeof(s->logbuf) - p, s->pc, (uint8_t *)&s->npc.inst.val, ilen);
#endif
}

static void execute(uint64_t n)
{
    Decode s;
    for (; n > 0; n--)
    {
        exec_once(&s);
        g_nr_guest_inst++;
        trace_and_difftest(&s, cpu.pc);
        if (npc_state.state != NPC_RUNNING)
            break;
    }
}

static void statistic() {
  Log("host time spent = %lu us", g_timer);
  Log("total guest instructions = %lu", g_nr_guest_inst);
  if (g_timer > 0) Log("simulation frequency = %lu inst/s", g_nr_guest_inst * 1000000 / g_timer);
  else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n)
{
    g_print_step = (n < MAX_INST_TO_PRINT);
    switch (npc_state.state)
    {
        case NPC_END:
        case NPC_ABORT:
            printf("Program execution has ended. To restart the program, exit npc and run again.\n");
            return;
        default:
            npc_state.state = NPC_RUNNING;
    }

    uint64_t timer_start = get_time();

    execute(n);

    uint64_t timer_end = get_time();
    g_timer += timer_end - timer_start;

    switch (npc_state.state)
    {
        case NPC_RUNNING:
            npc_state.state = NPC_STOP;
            break;

        case NPC_END:
        case NPC_ABORT:
            Log("npc: %s at pc = 0x%016lx",
                (npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
                (npc_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
                ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
                npc_state.halt_pc);
            Log("simulation time: %ld", contextp->time());
#ifdef CONFIG_ITRACE_RING
            for (int i = 0; i < IRING_BUF_SIZE; ++i)
            {
                if (g_nr_guest_inst % IRING_BUF_SIZE == i)
                {
                    log_write("--> ");
                }
                else
                {
                    log_write("    ");
                }
                log_write("%s\n", iringbuf[i]);
            }
#endif
        // fall through
        case NPC_QUIT: statistic();
    }
}

int is_exit_status_bad()
{
    return !((npc_state.state == NPC_END && npc_state.halt_ret == 0) || (npc_state.state == NPC_QUIT));
}
