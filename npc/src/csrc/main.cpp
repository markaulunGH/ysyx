#include <memory>
#include <verilated.h>
#include "Vtop.h"
#include "verilated_fst_c.h"
#include <nvboard.h>

int main(int argc, char** argv, char** env)
{
    Verilated::mkdir("logs");

    const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
    contextp->debug(0);
    contextp->randReset(2);
    contextp->traceEverOn(true);
    contextp->commandArgs(argc, argv);

    const std::unique_ptr<Vtop> top{new Vtop{contextp.get(), "TOP"}};
    VerilatedFstC* tfp = new VerilatedFstC;
    top->trace(tfp, 0);
    tfp->open("logs/dump.fst");

    int cnt = 0;
    while (1)
    {
        contextp->timeInc(1);

        top->clock = ~top->clock;
        if (top->clock)
        {
            top->reset = contextp->time() <= 100;

            if (top->io_pc == 0x7ffffffc || top->reset)
            {
                top->io_inst = 0x100513;
            }
            else if (cnt < 1000)
            {
                top->io_inst = 0x150513;
                ++ cnt;
            }
            else break;
        }

        top->eval();

        tfp->dump(contextp->time());
    }

    top->final();
    tfp->close();

#if VM_COVERAGE
    Verilated::mkdir("logs");
    contextp->coveragep()->write("logs/coverage.dat");
#endif

    return 0;
}
