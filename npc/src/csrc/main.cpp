#include <sdb.h>
#include <cpu.h>
#include <sim.h>
#include <paddr.h>
#include <config.h>
#include <difftest.h>

void init_disasm(const char *triple);

int main(int argc, char** argv, char** env)
{
    bool batch = 0;
    int img_size = 0, diff_so_file = 0;
    for (int i = 1; i < argc; ++ i)
    {
        if (strncmp(argv[i], "--img=", 6) == 0)
        {
            img_size = load_image(argv[i] + 6);
        }
#ifdef CONFIG_FTRACE
        if (strncmp(argv[i], "--elf=", 6) == 0)
        {
            init_ftrace(argv[i] + 6);
        }
#endif
        if (strncmp(argv[i], "--log=", 6) == 0)
        {
            init_log(argv[i] + 6);
        }
        if (strncmp(argv[i], "--batch", 7) == 0)
        {
            batch = 1;
        }
        if (strncpy(argv[i], "--diff=", 7) == 0)
        {
            diff_so_file = i;
        }
    }
    init_mem();
    init_sdb(batch);
#ifdef CONFIG_ITRACE
    init_disasm("riscv64-pc-linux-gnu");
#endif
#ifdef CONFIG_DIFFTEST
    init_difftest(argv[diff_so_file] + 7, img_size);
#endif
    init_simulation(1, argv);
    sdb_mainloop();
    end_simulation();
}
