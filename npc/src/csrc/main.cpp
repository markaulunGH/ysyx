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
    nvboard_bind_pin(&top->seg0, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG0A, SEG0B, SEG0C, SEG0D, SEG0E, SEG0F, SEG0G, DEC0P);
    nvboard_bind_pin(&top->seg1, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG1A, SEG1B, SEG1C, SEG1D, SEG1E, SEG1F, SEG1G, DEC1P);
    nvboard_bind_pin(&top->seg2, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG2A, SEG2B, SEG2C, SEG2D, SEG2E, SEG2F, SEG2G, DEC2P);
    nvboard_bind_pin(&top->seg3, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG3A, SEG3B, SEG3C, SEG3D, SEG3E, SEG3F, SEG3G, DEC3P);
    nvboard_bind_pin(&top->seg4, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG4A, SEG4B, SEG4C, SEG4D, SEG4E, SEG4F, SEG4G, DEC4P);
    nvboard_bind_pin(&top->seg5, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG5A, SEG5B, SEG5C, SEG5D, SEG5E, SEG5F, SEG5G, DEC5P);
    nvboard_bind_pin(&top->seg6, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG6A, SEG6B, SEG6C, SEG6D, SEG6E, SEG6F, SEG6G, DEC6P);
    nvboard_bind_pin(&top->seg7, BIND_RATE_SCR, BIND_DIR_OUT, 8, SEG7A, SEG7B, SEG7C, SEG7D, SEG7E, SEG7F, SEG7G, DEC7P);

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
