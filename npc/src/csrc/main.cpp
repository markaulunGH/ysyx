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

    nvboard_bind_pin(&top->ps2_clk, BIND_RATE_RT , BIND_DIR_IN , 1, PS2_CLK);
	nvboard_bind_pin(&top->ps2_data, BIND_RATE_RT , BIND_DIR_IN , 1, PS2_DAT);
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
