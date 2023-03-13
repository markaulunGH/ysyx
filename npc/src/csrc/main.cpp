#include <memory>
#include <verilated.h>
#include "VTop.h"
#include "verilated_fst_c.h"
#include <nvboard.h>

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

void cycle_begin()
{
    contextp->timeInc(1);
    top->clock = 0;
    top->eval();
    tfp->dump(contextp->time())
    contextp->timeInc(1);
    top->clock = 1;
    top->eval();
}

void cycle_end()
{
    tfp->dump(contextp->time());
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

const int SIZE = 1 << 20;
const long long offset = 0x80000000l;

int size;
uint8_t img[SIZE];

void load_image(char *img_file)
{
    FILE *fp = fopen(img_file, "rb");
    fseek(fp, 0, SEEK_END);
    size = ftell(fp);
    fseek(fp, 0, SEEK_SET);
    assert(fread(img, size, 1, fp));
    fclose(fp);
}

uint32_t ifetch(uint64_t pc)
{
    if (pc - offset < 0 || pc - offset > size)
    {
        end_simulation();
        exit(1);
    }
    return *(uint32_t*) (img + pc - offset);
}

int main(int argc, char** argv, char** env)
{
    load_image(argv[argc - 1]);
    init_simulation(argc - 1, argv);
    while (1)
    {
        cycle_begin();
        top->reset = contextp->time() < 100;
        if (!top->reset)
        {
            top->io_inst = ifetch(top->io_pc);
        }
        cycle_end();
    }
    end_simulation();
}
