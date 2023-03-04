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

    nvboard_bind_pin(&top->io_led, BIND_RATE_SCR, BIND_DIR_OUT, 16, LD15, LD14, LD13, LD12, LD11, LD10, LD9, LD8, LD7, LD6, LD5, LD4, LD3, LD2, LD1, LD0);
    nvboard_init();

    while (1)
    {
        contextp->timeInc(1);

        top->reset = contextp->time() < 100;
        top->clock = ~top->clock;

        top->eval();
        nvboard_update();

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
