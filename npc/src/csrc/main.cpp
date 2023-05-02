// #include <sdb.h>
// #include <cpu.h>
// #include <sim.h>
// #include <paddr.h>
// #include <config.h>
// #include <log.h>
// #include <difftest.h>
// #include <device.h>

// void init_disasm(const char *triple);

// int main(int argc, char** argv, char** env)
// {
//     bool batch = 0;
//     int img_size = 0, diff_so_file = 0;
//     for (int i = 1; i < argc; ++ i)
//     {
//         if (strncmp(argv[i], "--img=", 6) == 0)
//         {
//             img_size = load_image(argv[i] + 6);
//         }
// #ifdef CONFIG_FTRACE
//         if (strncmp(argv[i], "--elf=", 6) == 0)
//         {
//             init_ftrace(argv[i] + 6);
//         }
// #endif
//         if (strncmp(argv[i], "--log=", 6) == 0)
//         {
//             init_log(argv[i] + 6);
//         }
//         if (strncmp(argv[i], "--batch", 7) == 0)
//         {
//             batch = 1;
//         }
//         if (strncmp(argv[i], "--diff=", 7) == 0)
//         {
//             diff_so_file = i;
//         }
//     }
//     init_mem();
//     init_device();
//     init_sdb(batch);
// #ifdef CONFIG_ITRACE
//     init_disasm("riscv64-pc-linux-gnu");
// #endif
//     cpu.pc = 0x80000000;
// #ifdef CONFIG_DIFFTEST
//     init_difftest(argv[diff_so_file] + 7, img_size);
// #endif
//     init_simulation(2, argv);
//     sdb_mainloop();
//     end_simulation();
//     return is_exit_status_bad();
// }

#include <memory>
#include <verilated.h>
#include "VTop.h"
#include "verilated_vcd_c.h"
#include <stdint.h>

uint64_t _random()
{
    return static_cast<uint64_t>(rand()) << 33 | static_cast<uint64_t>(rand()) << 2 | rand() & 3;
}

int main(int argc, char** argv, char** env) {
    if (false && argc && argv && env) {}
    Verilated::mkdir("logs");
    const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
    contextp->debug(0);
    contextp->randReset(2);
    contextp->traceEverOn(true);
    contextp->commandArgs(argc, argv);
    const std::unique_ptr<VTop> top{new VTop{contextp.get(), "TOP"}};
    VerilatedVcdC* tfp = new VerilatedVcdC;

    top->trace(tfp, 0);
    tfp->open("logs/dump.vcd");

    for (int i = 0; i < 1e5; ++ i)
    {
        top->reset = i < 99;
        tfp->dump(contextp->time());
        contextp->timeInc(1);
        top->clock = 0;
        top->eval();
        tfp->dump(contextp->time());
        contextp->timeInc(1);
        top->clock = 1;
        top->eval();
    }

    top->io_flush = 0;

    for (int i = 1; i < 1e5; ++ i)
    {
        uint64_t a = _random();
        uint64_t b = _random();
        int sign = _random() & 1;
        uint64_t quotient;
        uint64_t remainder;
        if (sign)
        {
            quotient = static_cast<int64_t>(a) / static_cast<int64_t>(b);
            remainder = static_cast<int64_t>(a) % static_cast<int64_t(b);
        }
        else
        {
            quotient = a / b;
            remainder = a % b;
        }

        top->io_dividend = a;
        top->io_divisor = b;
        top->io_signed = sign;
        top->io_in_valid = 1;
        top->io_out_ready = 0;

        do
        {
            top->eval();
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 0;
            top->eval();
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 1;
        } while (!top->io_in_ready);
        top->eval();

        top->io_in_valid = 0;
        top->io_out_ready = 1;

        do
        {
            top->eval();
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 0;
            top->eval();
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 1;
        } while (!top->io_out_valid);
        top->eval();
        
        if (top->io_quotient != quotient || top->io_remainder != remainder)
        {
            printf("FAIL\n");
            printf("sign:     %d\n", sign);
            printf("a:        %016lx\n", a);
            printf("b:        %016lx\n", b);
            printf("expected: %016lx %016lx\n", quotient, remainder);
            printf("got:      %016lx %016lx\n", top->io_quotient, top->io_remainder);
            top->final();
            tfp->close();
            return 0;
        }
    }

    top->final();
    tfp->close();
    printf("PASS\n");

    return 0;
}