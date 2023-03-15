#include <sdb.h>
#include <cpu.h>
#include <sim.h>
#include <paddr.h>
#include <config.h>

int main(int argc, char** argv, char** env)
{
    init_mem();
    load_image(argv[argc - 1]);
    init_sdb();
#ifdef CONFIG_FTRACE
    init_ftrace(argv[argc]);
#endif
    init_simulation(argc - 1, argv);
    sdb_mainloop();
    end_simulation();
}
