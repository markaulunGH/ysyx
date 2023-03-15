#include <sdb.h>
#include <cpu.h>
#include <sim.h>
#include <paddr.h>
#include <config.h>

void init_disasm(const char *triple);

int main(int argc, char** argv, char** env)
{
    init_mem();
    load_image(argv[argc - 2]);
    init_sdb();
    init_log();
#ifdef CONFIG_ITRACE
    init_disasm("riscv64-pc-linux-gnu");
#endif
#ifdef CONFIG_FTRACE
    init_ftrace(argv[argc - 1]);
#endif
    init_simulation(argc - 3, argv);
    sdb_mainloop();
    end_simulation();
}
