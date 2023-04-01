#include <dlfcn.h>
#include <paddr.h>
#include <cpu.h>
#include <log.h>
#include <sdb.h>
#include <difftest.h>

void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint64_t n) = NULL;
void (*ref_difftest_raise_intr)(word_t NO) = NULL;

static bool is_skip_ref = false;
static int skip_dut_nr_inst = 0;

void difftest_skip_ref()
{
    is_skip_ref = true;
    skip_dut_nr_inst = 0;
}

void difftest_skip_dut(int nr_ref, int nr_dut)
{
    skip_dut_nr_inst += nr_dut;
    while (nr_ref -- > 0)
    {
        ref_difftest_exec(1);
    }
}

void init_difftest(char *ref_so_file, int img_size)
{
    void *handle;
    handle = dlopen(ref_so_file, RTLD_LAZY);
    ref_difftest_memcpy = (void(*)(paddr_t, void*, size_t, bool)) dlsym(handle, "difftest_memcpy");
    ref_difftest_regcpy = (void(*)(void*, bool)) dlsym(handle, "difftest_regcpy");
    ref_difftest_exec = (void(*)(uint64_t)) dlsym(handle, "difftest_exec");
    ref_difftest_raise_intr = (void(*)(word_t)) dlsym(handle, "difftest_raise_intr");
    void (*ref_difftest_init)() = (void(*)()) dlsym(handle, "difftest_init");

    Log("Differential testing: %s", ANSI_FMT("ON", ANSI_FG_GREEN));
    Log("The result of every instruction will be compared with %s. "
        "This will help you a lot for debugging, but also significantly reduce the performance. "
        "If it is not necessary, you can turn it off in menuconfig.", ref_so_file);

    ref_difftest_init();
    ref_difftest_memcpy(MEM_BASE, guest_to_host(MEM_BASE), img_size, DIFFTEST_TO_REF);
    ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
}

void checkregs(CPU_state *ref, vaddr_t pc)
{
    bool result = true;
    if (ref->pc != cpu.pc)
    {
        printf("Difftest failed at pc = %016lx", cpu.pc);
        printf("pc = %016lx, ref.pc = %016lx", cpu.pc, ref->pc);
        result = false;
    }
    for (int i = 0; i < 32; ++ i)
    {
        if (ref->gpr[i] != cpu.gpr[i])
        {
            printf("Difftest failed at pc = %016lx", cpu.pc);
            printf("gpr[%d] = %016lx, ref.gpr[%d] = %016lx\n", i, cpu.gpr[i], i, ref->gpr[i]);
            result = false;
        }
    }
    if (!result)
    {
        npc_state.state = NPC_ABORT;
        npc_state.halt_pc = pc;
    }
}

void difftest_step(vaddr_t pc, vaddr_t next_pc)
{
    CPU_state ref_r;

    if (skip_dut_nr_inst > 0)
    {
        ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);
        if (ref_r.pc == next_pc)
        {
            skip_dut_nr_inst = 0;
            checkregs(&ref_r, next_pc);
            return;
        }
        -- skip_dut_nr_inst;
        if (skip_dut_nr_inst == 0)
        {
            printf("can not catch up with ref.pc = %016lx at pc = %016lx", ref_r.pc, pc);
            assert(0);
        }
    }

    if (is_skip_ref)
    {
        ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
        is_skip_ref = false;
        return;
    }

    ref_difftest_exec(1);
    ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);

    checkregs(&ref_r, pc);
}
