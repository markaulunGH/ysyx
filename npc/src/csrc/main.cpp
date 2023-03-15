#include <memory>
#include <verilated.h>
#include "VTop.h"
#include "verilated_fst_c.h"
#include <nvboard.h>
#include <paddr.h>
#include <sdb.h>
#include <cpu.h>
#include <config.h>

const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
const std::unique_ptr<VTop> top{new VTop{contextp.get(), "TOP"}};
VerilatedFstC* tfp = new VerilatedFstC;

void init_simulation(int argc, char** argv)
{
    Verilated::mkdir("logs");

    contextp->debug(0);
    contextp->randReset(2);
    contextp->traceEverOn(true);
    contextp->commandArgs(argc, argv);

    top->trace(tfp, 0);
    tfp->open("logs/dump.fst");
}

void end_simulation()
{
    top->final();
    tfp->close();

#if VM_COVERAGE
    Verilated::mkdir("logs");
    contextp->coveragep()->write("logs/coverage.dat");
#endif
}

void cycle_begin()
{
    contextp->timeInc(1);
    top->clock = 0;
    top->eval();
    tfp->dump(contextp->time());
    contextp->timeInc(1);
    top->clock = 1;
    top->eval();
}

void cycle_end()
{
    top->eval();
    tfp->dump(contextp->time());
}

void reset()
{
    top->reset = 1;
    for (int i = 0; i < 100; ++ i)
    {
        cycle_begin();
        cycle_end();
    }
    top->reset = 0;
}

uint32_t ifetch(uint64_t pc)
{
    return paddr_read(pc, 4);
}

int main(int argc, char** argv, char** env)
{
    init_mem();
    load_image(argv[argc - 1]);
    init_sdb();
#ifdef CONFIG_FTRACE
    init_ftrace(argv[argc]);
#endif
    init_simulation(argc - 1, argv);

    reset();
    sdb_mainloop();

    end_simulation();
}
