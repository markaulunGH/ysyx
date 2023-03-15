#include <sdb.h>
#include <cpu.h>
#include <sim.h>
#include <paddr.h>
#include <config.h>

void init_disasm(const char *triple);

int main(int argc, char** argv, char** env)
{
    init_mem();
    load_image(argv[argc - 3]);
    init_sdb();
#ifdef CONFIG_ITRACE
    init_disasm("riscv64-pc-linux-gnu");
#endif
#ifdef CONFIG_FTRACE
    init_ftrace(argv[argc - 2]);
#endif
    init_log(argv[argc - 1]);
    init_simulation(argc - 4, argv);
    sdb_mainloop();
    end_simulation();
}
