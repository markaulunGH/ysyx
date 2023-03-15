#include <sdb.h>
#include <cpu.h>
#include <sim.h>
#include <paddr.h>
#include <config.h>

int main(int argc, char** argv, char** env)
{
    init_mem();
    load_image(argv[argc - 2]);
    init_sdb();
#ifdef CONFIG_FTRACE
    init_ftrace(argv[argc - 1]);
#endif
    init_simulation(argc - 3, argv);
    sdb_mainloop();
    end_simulation();
}
