#include <cpu.h>
#include <locale.h>
#include <elf.h>
#include <stddef.h>
#include <config.h>
#include <sdb.h>

FILE *log_fp = fopen("../../build/log.txt", "w");
#define log_write(...) \
    fprintf(log_fp, __VA_ARGS__); \
    fflush(log_fp); \

void reg_display()
{

}

word_t reg_str2val(const char *name, bool *success)
{

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

static void exec_once(Decode *s, vaddr_t pc)
{
    s->pc = pc;
    s->snpc = pc;

    //TODO

    cpu.pc = s->dnpc;
#ifdef CONFIG_ITRACE
    char *p = s->logbuf;
    p += snprintf(p, sizeof(s->logbuf), "0x%016lx:", s->pc);
    int ilen = s->snpc - s->pc;
    int i;
    uint8_t *inst = (uint8_t *)&s->isa.inst.val;
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
    disassemble(p, s->logbuf + sizeof(s->logbuf) - p, s->pc, (uint8_t *)&s->isa.inst.val, ilen);
#endif
}

static void execute(uint64_t n)
{
    Decode s;
    for (; n > 0; n--)
    {
        exec_once(&s, cpu.pc);
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
            log_write("%s\n", npc_state.state == NPC_ABORT ? "ABORT" : (npc_state.halt_ret == 0 ? "HIT GOOD TRAP" : "HIT BAD TRAP"));
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
            printf("Quit NPC\n");
    }
}
