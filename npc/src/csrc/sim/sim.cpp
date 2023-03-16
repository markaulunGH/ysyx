#include <sim.h>

const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
const std::unique_ptr<VTop> top{new VTop{contextp.get(), "TOP"}};
VerilatedFstC* tfp = new VerilatedFstC;

void cycle_begin()
{
    top->clock = 1;
    top->eval();
}

void cycle_end()
{
    tfp->dump(contextp->time());
    contextp->timeInc(1);
    top->clock = 0;
    top->eval();
    tfp->dump(contextp->time());
    contextp->timeInc(1);
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

void init_simulation(int argc, char** argv)
{
    Verilated::mkdir("logs");

    contextp->debug(0);
    contextp->randReset(2);
    contextp->traceEverOn(true);
    contextp->commandArgs(argc, argv);

    top->trace(tfp, 0);
    tfp->open("logs/dump.fst");

    reset();
}

void end_simulation()
{
    top->final();
    tfp->close();
}
