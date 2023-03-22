#include <time.h>
#include <sys/time.h>
#include <timer.h>

uint64_t get_time_internal()
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec * 1000000000 + ts.tv_nsec;
}

uint64_t get_time()
{
    static uint64_t boot_time = 0;
    if (boot_time == 0)
    {
        boot_time = get_time_internal();
    }
    return get_time_internal() - boot_time;
}
