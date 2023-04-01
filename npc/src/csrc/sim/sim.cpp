#include <sim.h>

const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
const std::unique_ptr<VTop> top{new VTop{contextp.get(), "TOP"}};
// VerilatedFstC* tfp = new VerilatedFstC;

void cycle_end()
{
    // tfp->dump(contextp->time());
    contextp->timeInc(1);
    top->clock = 0;
    top->eval();
    // tfp->dump(contextp->time());
    contextp->timeInc(1);
    top->clock = 1;
    top->eval();
}

void reset()
{
    for (int i = 0; i < 100; ++ i)
    {
        top->reset = i < 99;
        cycle_end();
    }
}

void init_simulation(int argc, char** argv)
{
    Verilated::mkdir("logs");

    contextp->debug(0);
    contextp->randReset(2);
    // contextp->traceEverOn(true);
    // contextp->commandArgs(argc, argv);

    // top->trace(tfp, 0);
    // tfp->open("logs/dump.fst");

    reset();
}

void end_simulation()
{
    top->final();
    tfp->close();
}
