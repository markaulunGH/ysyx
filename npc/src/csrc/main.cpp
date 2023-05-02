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
    return static_cast<uint64_t>(rand()) << 32 | rand();
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
        uint64_t a_64 = _random();
        uint64_t b_64 = _random();
        uint32_t a_32 = _random() & 0xffffffff;
        uint32_t b_32 = _random() & 0xffffffff;
        int mulw = _random() & 1;
        int sign = _random() & 3;
        while (sign == 1)
        {
            sign = _random() & 3;
        }
        __uint128_t res;
        switch (sign)
        {
            case 0: res = static_cast<__uint128_t>(static_cast<__uint128_t>(mulw ? a_32 : a_64) * static_cast<__uint128_t>(mulw ? b_32 : b_64)); break;
            case 2: res = static_cast<__uint128_t>(static_cast<__int128_t >(mulw ? a_32 : a_64) * static_cast<__uint128_t>(mulw ? b_32 : b_64)); break;
            case 3: res = static_cast<__uint128_t>(static_cast<__int128_t >(mulw ? a_32 : a_64) * static_cast<__int128_t >(mulw ? b_32 : b_64)); break;
        }

        top->io_multiplicand = mulw ? a_32 : a_64;
        top->io_multiplier = mulw ? b_32 : b_64;
        top->io_mulw = 0;
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
        
        if (top->io_result_hi != static_cast<uint64_t>(res >> 64) || top->io_result_lo != static_cast<uint64_t>(res & 0xffffffffffffffff))
        {
            printf("FAIL\n");
            printf("mulw:     %d\n", mulw);
            printf("sign:     %d\n", sign);
            printf("a:        %016lx\n", static_cast<uint64_t>(mulw ? a_32 : a_64) & 0xffffffffffffffff);
            printf("b:        %016lx\n", static_cast<uint64_t>(mulw ? b_32 : b_64) & 0xffffffffffffffff);
            printf("expected: %016lx %016lx\n", static_cast<uint64_t>(res >> 64), static_cast<uint64_t>(res & 0xffffffffffffffff));
            printf("got:      %016lx %016lx\n", top->io_result_hi, top->io_result_lo);
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