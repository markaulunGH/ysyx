#include <memory>
#include <verilated.h>
#include "VTop.h"
#include "verilated_fst_c.h"
#include <nvboard.h>

const int SIZE = 1 << 20;
const long long offset = 0x80000000l;

uint8_t img[SIZE];

void load_image(char *img_file)
{
    FILE *fp = fopen(img_file, "rb");
    fseek(fp, 0, SEEK_END);
    int size = ftell(fp);
    fseek(fp, 0, SEEK_SET);
    assert(fread(img, size, 1, fp));
    fclose(fp);
}

uint32_t ifetch(uint64_t pc)
{
    // printf("%lx\n", pc);
    return *(uint32_t*) (img + pc - offset);
    return 0x100513;
}

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

int main(int argc, char** argv, char** env)
{
    load_image(argv[argc - 1]);
    init_simulation(argc - 1, argv);
    while (1)
    {
        cycle_begin();
        top->reset = contextp->time() < 100;
        top->io_inst = ifetch(top->io_pc);
        if (contextp->time() > 1000) break; 
        cycle_end();
    }
    end_simulation();
}
