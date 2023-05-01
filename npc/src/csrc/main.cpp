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

    for (int i = 0; i < 100; ++ i)
    {
        top->reset = i < 99;
        tfp->dump(contextp->time());
        contextp->timeInc(1);
        top->clock = 0;
        top->eval();
        tfp->dump(contextp->time());
        contextp->timeInc(1);
        top->clock = 1;
    }

    for (int i = 1; i < 10; ++ i)
    {
        __uint128_t a = rand();
        __uint128_t b = rand();
        int mulw = 0;
        int sign = 3;

        top->io_multiplicand = a;
        top->io_multiplier = b;
        top->io_mulw = mulw;
        top->io_signed = sign;
        top->io_in_valid = 1;
        top->io_out_ready = 0;
        do
        {
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 0;
            top->eval();
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 1;
            top->eval();
        } while (!top->io_in_ready);

        top->io_in_valid = 0;
        top->io_out_ready = 1;
        do
        {
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 0;
            top->eval();
            tfp->dump(contextp->time());
            contextp->timeInc(1);
            top->clock = 1;
            top->eval();
        } while (!top->io_out_valid);

        assert(top->io_result_hi == (a * b) >> 64);
        assert(top->io_result_lo == (a * b) & 0xffffffffffffffff);
    }

    top->final();
    tfp->close();

    return 0;
}