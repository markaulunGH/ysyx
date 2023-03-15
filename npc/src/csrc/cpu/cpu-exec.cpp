#include <cpu.h>
#include <locale.h>
#include <elf.h>
#include <stddef.h>
#include <config.h>
#include <sdb.h>
#include <sim.h>
#include <paddr.h>

FILE *log_fp;
#define log_write(...) \
    fprintf(log_fp, __VA_ARGS__); \
    fflush(log_fp); \

void init_log(const char *log_file)
{
    log_fp = fopen(log_file, "w");
}

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
        top->io_rf_0,  top->io_rf_1,  top->io_rf_2,  top->io_rf_3,  top->io_rf_4,  top->io_rf_5,  top->io_rf_6,  top->io_rf_7,
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
        top->io_rf_0,  top->io_rf_1,  top->io_rf_2,  top->io_rf_3,  top->io_rf_4,  top->io_rf_5,  top->io_rf_6,  top->io_rf_7,
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

#define SYMTAB_SIZE 100
#define STRTAB_SIZE 500

Elf64_Shdr symshdr, strshdr;
Elf64_Sym symtab[SYMTAB_SIZE];
char strtab[STRTAB_SIZE];
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
    assert(fread(&symtab, symshdr.sh_entsize, symshdr.sh_size / symshdr.sh_entsize, elf_fp));
    fseek(elf_fp, strshdr.sh_offset, SEEK_SET);
    assert(fread(&strtab, 1, strshdr.sh_size, elf_fp));

    fclose(elf_fp);
}

static void trace_and_difftest(Decode *_this, vaddr_t dnpc)
{
#ifdef CONFIG_ITRACE
    strcpy(iringbuf[g_nr_guest_inst % IRING_BUF_SIZE], _this->logbuf);
#ifdef CONFIG_FTRACE
    if (strncmp("jal", _this->logbuf + 32, 3) == 0)
    {
        for (int i = 0; i < stack_depth; ++i)
        {
            log_write(" ");
        }
        uint64_t addr;
        if (*(_this->logbuf + 35) == 'r')
        {
            bool success = true;
            addr = reg_str2val(_this->logbuf + 37, &success);
        }
        else
        {
            sscanf(_this->logbuf + 36, "%lx", &addr);
        }
        int id = 0;
        for (; id < symshdr.sh_size / symshdr.sh_entsize; ++id)
        {
            if (ELF32_ST_TYPE(symtab[id].st_info) == STT_FUNC && symtab[id].st_value <= addr && addr < symtab[id].st_value + symtab[id].st_size)
            {
                break;
            }
        }
        log_write("call [%s@%lx]\n", strtab + symtab[id].st_name, addr);
        stack_depth += 2;
    }
    else if (strncmp("ret", _this->logbuf + 32, 3) == 0)
    {
        stack_depth -= 2;
        for (int i = 0; i < stack_depth; ++i)
        {
            log_write(" ");
        }
        log_write("ret\n");
    }
#endif
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

static void exec_once(Decode *s)
{
    s->pc = top->io_pc;
    cycle_begin();
    s->npc.inst.val = top->io_inst = paddr_read(top->io_pc, 4);
    cycle_end();
    if (top->io_ebreak)
    {
        // difftest_skip_ref();
        npc_state.state = NPC_END;
        npc_state.halt_pc = top->io_pc;
        npc_state.halt_ret= top->io_rf_10;
    }
    s->dnpc = top->io_pc;
    cpu.pc = s->dnpc;
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
#ifdef CONFIG_DEVICE
        device_update();
#endif
    }
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n)
{
    g_print_step = (n < MAX_INST_TO_PRINT);
    switch (npc_state.state)
    {
        case NPC_END:
        case NPC_ABORT:
            printf("Program execution has ended. To restart the program, exit NEMU and run again.\n");
            return;
        default:
            npc_state.state = NPC_RUNNING;
    }

    execute(n);

    switch (npc_state.state)
    {
        case NPC_RUNNING:
            npc_state.state = NPC_STOP;
            break;

        case NPC_END:
        case NPC_ABORT:
            fprintf(log_fp, "%s\n", npc_state.state == NPC_ABORT ? "ABORT" : (npc_state.halt_ret == 0 ? "HIT GOOD TRAP" : "HIT BAD TRAP"));
#ifdef CONFIG_ITRACE
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
        case NPC_QUIT:
            // printf("Quit NPC\n");
    }
}
