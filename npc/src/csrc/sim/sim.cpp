#include <sim.h>
#include <config.h>

const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
const std::unique_ptr<VTop> top{new VTop{contextp.get(), "TOP"}};
#ifdef CONFIG_WAVE_FST
VerilatedFstC* tfp = new VerilatedFstC;
#elif
VerilatedVcdC* tfp = new VerilatedVcdC;
#endif

void cycle_end()
{
#ifdef CONFIG_WAVE
    tfp->dump(contextp->time());
#endif
    contextp->timeInc(1);
    top->clock = 0;
    top->eval();
#ifdef CONFIG_WAVE
    tfp->dump(contextp->time());
#endif
    contextp->timeInc(1);
    top->clock = 1;
}

void reset()
{
    for (int i = 0; i < 100; ++ i)
    {
        top->reset = i < 99;
        cycle_end();
        top->eval();
    }
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
